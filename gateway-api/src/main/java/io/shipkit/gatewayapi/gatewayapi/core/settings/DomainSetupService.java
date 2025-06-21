package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DockerControlGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.InternalServerException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainSetupService {

    private final PlatformSettingRepository repository;
    private final DockerControlGrpcClient dockerClient;

    @Value("${nginx.vhost.output-dir:/nginx}")
    private String nginxOutputDir;

    @Value("${nginx.reload.container-name:nginx}")
    private String nginxContainerName;

    private static final String VHOST_TEMPLATE_PATH = "/templates/nginx_vhost.ftl";

    @Transactional
    public void configureDomain(String domain, boolean skipValidation) {
        if (!skipValidation) {
            validateDomain(domain);
        }

        if (!repository.existsByFqdn(domain)) {
            PlatformSetting entity = PlatformSetting.builder()
                    .fqdn(domain)
                    .build();
            repository.save(entity);
        }

        writeVhostFile(domain);
        reloadNginx();
    }

    // Retain backwards compatibility
    public void configureDomain(String domain) {
        configureDomain(domain, false);
    }

    private void validateDomain(String domain) {
        // Basic syntax check
        if (!domain.matches("^[a-zA-Z0-9.-]+$")) {
            throw new BadRequestException("Invalid domain format");
        }

        String expectedIp = fetchPublicIp();
        try {
            InetAddress resolved = InetAddress.getByName(domain);
            String resolvedIp = resolved.getHostAddress();
            if (!expectedIp.equals(resolvedIp)) {
                throw new BadRequestException("Domain does not resolve to this server's IP. Configure an A record for '" + domain + "' pointing to " + expectedIp + " or continue anyway.");
            }
        } catch (IOException e) {
            throw new InternalServerException("Failed to resolve domain");
        }
    }

    private String fetchPublicIp() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/ip"))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("Could not determine public IP");
        }
    }

    private void writeVhostFile(String domain) {
        try (var in = getClass().getResourceAsStream(VHOST_TEMPLATE_PATH)) {
            if (in == null) {
                throw new InternalServerException("Template not found: " + VHOST_TEMPLATE_PATH);
            }
            String template = new String(in.readAllBytes());
            String rendered = template.replace("${domain}", domain);
            Path outPath = Path.of(nginxOutputDir, domain + ".conf");
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath, rendered);
            log.info("Wrote nginx vhost to {}", outPath);
        } catch (IOException e) {
            throw new InternalServerException("Failed to write NGINX vhost file");
        }
    }

    private void reloadNginx() {
        var result = dockerClient.reloadNginx(nginxContainerName);
        if (result.getStatus() != 0) {
            throw new InternalServerException("Failed to reload NGINX: " + result.getMessage());
        }
    }
} 
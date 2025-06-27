package io.shipkit.gatewayapi.gatewayapi.core.settings;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DockerControlGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.InternalServerException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainSetupService {

    private final PlatformSettingRepository repository;
    private final DockerControlGrpcClient dockerClient;
    private final Configuration freemarkerConfig;

    @Value("${nginx.vhost.output-dir:/nginx}")
    private String nginxOutputDir;

    @Value("${nginx.reload.container-name:nginx}")
    private String nginxContainerName;

    private static final String VHOST_TEMPLATE_NAME = "nginx_vhost.ftl";

    @Transactional
    public void configureDomain(String domain, boolean skipValidation, boolean sslEnabled, boolean forceSsl) {
        Path vhostPath = Path.of(nginxOutputDir, domain + ".conf");
        boolean vhostExistedBefore = Files.exists(vhostPath);
        String previousVhostContent = null;
        if (vhostExistedBefore) {
            try {
                previousVhostContent = Files.readString(vhostPath);
            } catch (IOException ioe) {
                log.warn("Could not read existing vhost content for rollback: {}", ioe.getMessage());
                vhostExistedBefore = false;
            }
        }

        try {
            if (!skipValidation) {
                validateDomain(domain);
            }

            PlatformSetting entity = repository.findByFqdn(domain).orElse(new PlatformSetting());
            entity.setFqdn(domain);
            entity.setSslEnabled(sslEnabled);
            entity.setForceSsl(forceSsl);
            repository.save(entity);

            if (sslEnabled) {
                issueCertificate(domain);
            }

            writeVhostFile(domain, sslEnabled, forceSsl);
            reloadNginx();
        } catch (RuntimeException ex) {
            try {
                if (vhostExistedBefore) {
                    Files.writeString(vhostPath, previousVhostContent);
                    log.info("Rolled back vhost file to previous version at {}", vhostPath);
                } else {
                    Files.deleteIfExists(vhostPath);
                    log.info("Removed newly created vhost file at {} due to failure", vhostPath);
                }
            } catch (IOException ioe) {
                log.warn("Failed to rollback vhost file: {}", ioe.getMessage());
            }

            try {
                reloadNginx();
            } catch (Exception re) {
                log.warn("Failed to reload NGINX during rollback: {}", re.getMessage());
            }

            throw ex;
        }
    }

    private void issueCertificate(String domain) {
        var result = dockerClient.issueCertificate(domain);
        if (result.getStatus() != 0) {
            throw new InternalServerException("Failed to issue certificate for " + domain + ": " + result.getMessage());
        }
    }

    private void validateDomain(String domain) {
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

    private void writeVhostFile(String domain, boolean sslEnabled, boolean forceSsl) {
        try {
            Template template = freemarkerConfig.getTemplate(VHOST_TEMPLATE_NAME);
            Map<String, Object> model = Map.of(
                    "domain", domain,
                    "sslEnabled", sslEnabled,
                    "forceSsl", forceSsl
            );
            String rendered = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            Path outPath = Path.of(nginxOutputDir, domain + ".conf");
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath, rendered);
            log.info("Wrote nginx vhost to {}", outPath);

            // Overwrite (or create) default.conf so that public IP returns 404 after setup
            Path defaultConfPath = Path.of(nginxOutputDir, "default.conf");
            try {
                String defaultContent = "server {\n    listen 80 default_server;\n    server_name _;\n    return 404;\n}\n";
                Files.writeString(defaultConfPath, defaultContent);
                log.info("Replaced default nginx config at {}", defaultConfPath);
            } catch (IOException ioe) {
                log.warn("Could not update default nginx config: {}", ioe.getMessage());
            }
        } catch (IOException | TemplateException e) {
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
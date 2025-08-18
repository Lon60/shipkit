package io.shipkit.gatewayapi.gatewayapi.core.settings;
import io.shipkit.gatewayapi.gatewayapi.core.config.K8sTemplateRenderer;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.K3sControlGrpcClient;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.shipkit.gatewayapi.gatewayapi.core.exceptions.InternalServerException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.DomainValidationException;


@Slf4j
@Service
@RequiredArgsConstructor
public class DomainSetupService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final PlatformSettingRepository repository;
    private final K8sTemplateRenderer renderer;
    private final K3sControlGrpcClient k3sClient;

    @Value("${kubernetes.namespace:shipkit-system}")
    private String kubernetesNamespace;

    private final String TRAEFIK_GROUP = "traefik.io";
    private final String TRAEFIK_VERSION = "v1alpha1";

    @Transactional
    public void configureDomain(String domain,
                                boolean skipValidation,
                                boolean sslEnabled,
                                boolean forceSsl) {

        if (!skipValidation) {
            validateDomain(domain);
        }

        PlatformSetting entity = repository.findByFqdn(domain)
                .orElse(new PlatformSetting());
        entity.setFqdn(domain);
        entity.setSslEnabled(sslEnabled);
        entity.setForceSsl(forceSsl);
        repository.save(entity);

        try {
            applyIngressRoute(domain, sslEnabled, forceSsl);
        } catch (Exception ex) {
            rollbackIngressRoute(domain);
            throw new InternalServerException("Failed to configure Traefik IngressRoute: " + ex.getMessage(), ex);
        }
    }

    private void validateDomain(String domain) {
        if (!domain.matches("^[a-zA-Z0-9.-]+$")) {
            throw new DomainValidationException("Invalid domain format");
        }

        String expectedIp = fetchPublicIp();
        try {
            InetAddress resolved = InetAddress.getByName(domain);
            String resolvedIp = resolved.getHostAddress();

            if (!expectedIp.equals(resolvedIp)) {
                throw new DomainValidationException(
                        "Domain does not resolve to this server's IP. "
                                + "Configure an A record for '" + domain + "' pointing to " + expectedIp
                                + " or continue anyway.");
            }
        } catch (IOException e) {
            throw new DomainValidationException("Failed to resolve domain");
        }
    }

    private String fetchPublicIp() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/ip"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body().trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("Could not determine public IP");
        }
    }

    private void applyIngressRoute(String domain, boolean sslEnabled, boolean forceSsl) {
        try {
            // Delete any previous versions (HTTP and HTTPS variants)
            deleteIngressRoute("shipkit-gateway-api");
            deleteIngressRoute("shipkit-gateway-api-https");
            deleteIngressRoute("shipkit-frontend");
            deleteIngressRoute("shipkit-frontend-https");

            String gatewayApiHttpName = "shipkit-gateway-api";
            String gatewayApiHttpsName = "shipkit-gateway-api-https";
            String frontendHttpName   = "shipkit-frontend";
            String frontendHttpsName  = "shipkit-frontend-https";

            List<Map<String, Object>> redirectMw = List.of(Map.of("name", "shipkit-https-redirect", "namespace", kubernetesNamespace));
            List<Map<String, Object>> httpMiddlewares = (sslEnabled && forceSsl) ? redirectMw : List.of();

            Map<String, Object> commonGatewayModel = Map.of(
                    "name", gatewayApiHttpName,
                    "namespace", kubernetesNamespace,
                    "sslEnabled", false,
                    "match", "Host(`" + domain + "`) && PathPrefix(`/api`)",
                    "serviceName", "gateway-api",
                    "servicePort", 8080,
                    "serviceNamespace", kubernetesNamespace,
                    "middlewares", httpMiddlewares
            );
            Map<String, Object> commonFrontendModel = Map.of(
                    "name", frontendHttpName,
                    "namespace", kubernetesNamespace,
                    "sslEnabled", false,
                    "match", "Host(`" + domain + "`) && PathPrefix(`/`)",
                    "serviceName", "shipkit-frontend",
                    "servicePort", 3000,
                    "serviceNamespace", kubernetesNamespace,
                    "middlewares", httpMiddlewares
            );

            String gatewayHttpYaml = renderer.render("ingressroute.ftl.yaml", commonGatewayModel);
            String frontendHttpYaml = renderer.render("ingressroute.ftl.yaml", commonFrontendModel);

            applyManifestYaml(gatewayHttpYaml);
            applyManifestYaml(frontendHttpYaml);

            if (sslEnabled) {
                Map<String, Object> gatewayTlsModel = new java.util.HashMap<>();
                gatewayTlsModel.put("name", gatewayApiHttpsName);
                gatewayTlsModel.put("namespace", kubernetesNamespace);
                gatewayTlsModel.put("sslEnabled", true);
                gatewayTlsModel.put("match", "Host(`" + domain + "`) && PathPrefix(`/api`)");
                gatewayTlsModel.put("serviceName", "gateway-api");
                gatewayTlsModel.put("servicePort", 8080);
                gatewayTlsModel.put("serviceNamespace", kubernetesNamespace);
                gatewayTlsModel.put("middlewares", List.of());
                gatewayTlsModel.put("certResolver", "letsencrypt");

                Map<String, Object> frontendTlsModel = new java.util.HashMap<>();
                frontendTlsModel.put("name", frontendHttpsName);
                frontendTlsModel.put("namespace", kubernetesNamespace);
                frontendTlsModel.put("sslEnabled", true);
                frontendTlsModel.put("match", "Host(`" + domain + "`) && PathPrefix(`/`)");
                frontendTlsModel.put("serviceName", "shipkit-frontend");
                frontendTlsModel.put("servicePort", 3000);
                frontendTlsModel.put("serviceNamespace", kubernetesNamespace);
                frontendTlsModel.put("middlewares", List.of());
                frontendTlsModel.put("certResolver", "letsencrypt");

                String gatewayHttpsYaml = renderer.render("ingressroute.ftl.yaml", gatewayTlsModel);
                String frontendHttpsYaml = renderer.render("ingressroute.ftl.yaml", frontendTlsModel);

                applyManifestYaml(gatewayHttpsYaml);
                applyManifestYaml(frontendHttpsYaml);
            }

        } catch (Exception e) {
            log.error("Error applying IngressRoute configuration: {}", e.getMessage(), e);
            throw new InternalServerException("Failed to configure Traefik IngressRoute: " + e.getMessage());
        }
    }

    private void deleteIngressRoute(String name) {
        try {
            k3sClient.deleteResource(TRAEFIK_GROUP, TRAEFIK_VERSION, "IngressRoute", kubernetesNamespace, name);
            log.info("Delete requested for IngressRoute '{}' via k3s-control", name);
        } catch (Exception e) {
            log.warn("Failed to request delete for IngressRoute '{}': {}", name, e.getMessage());
        }
    }

    private void rollbackIngressRoute(String domain) {
        try {
            deleteIngressRoute("shipkit-gateway-api");
            deleteIngressRoute("shipkit-gateway-api-https");
            deleteIngressRoute("shipkit-frontend");
            deleteIngressRoute("shipkit-frontend-https");
        } catch (Exception e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }

    private void applyManifestYaml(String yaml) {
        var res = k3sClient.applyManifest(yaml);
        if (res.getStatus() != 0) {
            throw new InternalServerException("Failed to apply manifest: " + res.getMessage());
        }
    }
}

package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
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

import static io.kubernetes.client.util.Config.defaultClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainSetupService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final PlatformSettingRepository repository;

    @Value("${kubernetes.namespace:shipkit-system}")
    private String kubernetesNamespace;

    private final String TRAEFIK_GROUP = "traefik.io";
    private final String TRAEFIK_VERSION = "v1alpha1";
    private final String INGRESSROUTE_PLURAL = "ingressroutes";

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
            ApiClient client = defaultClient();
            Configuration.setDefaultApiClient(client);
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);

            // Delete old IngressRoutes
            deleteIngressRoute("shipkit-gateway-api", customObjectsApi);
            deleteIngressRoute("shipkit-frontend", customObjectsApi);

            // Create new IngressRoutes
            String gatewayApiIngressRouteName = "shipkit-gateway-api-" + domain.replace(".", "-");
            String frontendIngressRouteName = "shipkit-frontend-" + domain.replace(".", "-");

            Map<String, Object> gatewayApiIngressRoute = createIngressRoute(
                    gatewayApiIngressRouteName,
                    "Host(`" + domain + "`) && PathPrefix(`/api`)",
                    "gateway-api",
                    8080,
                    sslEnabled,
                    forceSsl
            );

            Map<String, Object> frontendIngressRoute = createIngressRoute(
                    frontendIngressRouteName,
                    "Host(`" + domain + "`) && PathPrefix(`/`)",
                    "shipkit-frontend",
                    3000,
                    sslEnabled,
                    forceSsl
            );

            createOrUpdateIngressRoute(gatewayApiIngressRouteName, gatewayApiIngressRoute, customObjectsApi);
            createOrUpdateIngressRoute(frontendIngressRouteName, frontendIngressRoute, customObjectsApi);

        } catch (Exception e) {
            String errorMessage;
            if (e instanceof ApiException) {
                ApiException ae = (ApiException) e;
                errorMessage = String.format("API Error: %s (Code: %d, Body: %s)", ae.getMessage(), ae.getCode(), ae.getResponseBody());
            } else {
                errorMessage = e.getMessage();
            }
            log.error("Error applying IngressRoute configuration: {}", errorMessage, e);
            throw new InternalServerException("Failed to configure Traefik IngressRoute: " + errorMessage);
        }
    }

    private void createOrUpdateIngressRoute(String name, Map<String, Object> ingressRoute, CustomObjectsApi api) throws ApiException {
        try {
            api.getNamespacedCustomObject(TRAEFIK_GROUP, TRAEFIK_VERSION, kubernetesNamespace, INGRESSROUTE_PLURAL, name);
            api.replaceNamespacedCustomObject(TRAEFIK_GROUP, TRAEFIK_VERSION, kubernetesNamespace, INGRESSROUTE_PLURAL, name, ingressRoute, null, null);
            log.info("Updated IngressRoute '{}'", name);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                api.createNamespacedCustomObject(TRAEFIK_GROUP, TRAEFIK_VERSION, kubernetesNamespace, INGRESSROUTE_PLURAL, ingressRoute, null, null, null);
                log.info("Created IngressRoute '{}'", name);
            } else {
                throw e;
            }
        }
    }

    private void deleteIngressRoute(String name, CustomObjectsApi api) {
        try {
            api.deleteNamespacedCustomObject(TRAEFIK_GROUP, TRAEFIK_VERSION, kubernetesNamespace, INGRESSROUTE_PLURAL, name, 0, null, null, null, null);
            log.info("Deleted IngressRoute '{}'", name);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Failed to delete IngressRoute '{}'. Code: {}. Response Body: {}", name, e.getCode(), e.getResponseBody(), e);
            }
        }
    }

    private Map<String, Object> createIngressRoute(String name, String match, String serviceName, int port, boolean sslEnabled, boolean forceSsl) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("name", name);
        metadata.put("namespace", kubernetesNamespace);

        Map<String, Object> spec = new java.util.HashMap<>();
        spec.put("entryPoints", List.of("web"));

        Map<String, Object> route = new java.util.HashMap<>();
        route.put("match", match);
        route.put("kind", "Rule");
        route.put("services", List.of(
                Map.of("name", serviceName, "port", port)
        ));

        if (sslEnabled && forceSsl) {
            route.put("middlewares", List.of(
                Map.of("name", "shipkit-https-redirect", "namespace", "traefik")
            ));
        }

        spec.put("routes", List.of(route));

        if (sslEnabled) {
            spec.put("tls", Map.of("certResolver", "letsencrypt"));
        }

        Map<String, Object> ingressRoute = new java.util.HashMap<>();
        ingressRoute.put("apiVersion", TRAEFIK_GROUP + "/" + TRAEFIK_VERSION);
        ingressRoute.put("kind", "IngressRoute");
        ingressRoute.put("metadata", metadata);
        ingressRoute.put("spec", spec);

        return ingressRoute;
    }

    private void rollbackIngressRoute(String domain) {
        try {
            ApiClient client = defaultClient();
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
            String gatewayApiIngressRouteName = "shipkit-gateway-api-" + domain.replace(".", "-");
            String frontendIngressRouteName = "shipkit-frontend-" + domain.replace(".", "-");
            deleteIngressRoute(gatewayApiIngressRouteName, customObjectsApi);
            deleteIngressRoute(frontendIngressRouteName, customObjectsApi);
        } catch (Exception e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }
}
package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.shipkit.gatewayapi.gatewayapi.core.config.K8sTemplateRenderer;
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
    private final K8sTemplateRenderer renderer;

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

            // Delete any previous versions (HTTP and HTTPS variants)
            deleteIngressRoute("shipkit-gateway-api", customObjectsApi);
            deleteIngressRoute("shipkit-gateway-api-https", customObjectsApi);
            deleteIngressRoute("shipkit-frontend", customObjectsApi);
            deleteIngressRoute("shipkit-frontend-https", customObjectsApi);

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

            createOrUpdateIngressRoute(gatewayApiHttpName, yamlToMap(gatewayHttpYaml), customObjectsApi);
            createOrUpdateIngressRoute(frontendHttpName, yamlToMap(frontendHttpYaml), customObjectsApi);

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

                createOrUpdateIngressRoute(gatewayApiHttpsName, yamlToMap(gatewayHttpsYaml), customObjectsApi);
                createOrUpdateIngressRoute(frontendHttpsName, yamlToMap(frontendHttpsYaml), customObjectsApi);
            }

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

    private Map<String, Object> yamlToMap(String yaml) {
        org.yaml.snakeyaml.Yaml parser = new org.yaml.snakeyaml.Yaml();
        Object obj = parser.load(yaml);
        if (!(obj instanceof Map)) {
            throw new IllegalStateException("Rendered YAML is not a map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) obj;
        return map;
    }

    private void rollbackIngressRoute(String domain) {
        try {
            ApiClient client = defaultClient();
            CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
            deleteIngressRoute("shipkit-gateway-api", customObjectsApi);
            deleteIngressRoute("shipkit-gateway-api-https", customObjectsApi);
            deleteIngressRoute("shipkit-frontend", customObjectsApi);
            deleteIngressRoute("shipkit-frontend-https", customObjectsApi);
        } catch (Exception e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }
}

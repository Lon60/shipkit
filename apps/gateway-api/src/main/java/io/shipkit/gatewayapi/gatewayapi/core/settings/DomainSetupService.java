package io.shipkit.gatewayapi.gatewayapi.core.settings;

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

    @Value("${kubernetes.namespace:shipkit-system}")
    private String kubernetesNamespace;

    @Transactional
    public void configureDomain(String domain,
                                boolean skipValidation,
                                boolean sslEnabled,
                                boolean forceSsl) {

        try {
            if (!skipValidation) {
                validateDomain(domain);
            }

            PlatformSetting entity = repository.findByFqdn(domain)
                    .orElse(new PlatformSetting());
            entity.setFqdn(domain);
            entity.setSslEnabled(sslEnabled);
            entity.setForceSsl(forceSsl);
            repository.save(entity);

            // Certificate issuance now handled by Traefik – skip

            applyIngress(domain, sslEnabled, forceSsl);

            // Traefik picks up config automatically – no need to reload

        } catch (RuntimeException ex) {
            // Rollback logic for Traefik ingress if needed
            rollbackIngress(domain);
            throw ex;
        }
    }

    private void validateDomain(String domain) {
        if (!domain.matches("^[a-zA-Z0-9.-]+$")) {
            throw new DomainValidationException("Invalid domain format");
        }

        String expectedIp = fetchPublicIp();
        try {
            InetAddress resolved   = InetAddress.getByName(domain);
            String      resolvedIp = resolved.getHostAddress();

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

    private void applyIngress(String domain, boolean sslEnabled, boolean forceSsl) {
        try {
            // Create or update a standard Kubernetes Ingress that Traefik watches
            String ingressName = ("shipkit-" + domain).replaceAll("[^.a-z0-9-]", "-").toLowerCase();

            io.kubernetes.client.openapi.ApiClient client = io.kubernetes.client.util.Config.defaultClient();
            io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

            var networkingApi = new io.kubernetes.client.openapi.apis.NetworkingV1Api(client);

            var metadata = new io.kubernetes.client.openapi.models.V1ObjectMeta()
                    .name(ingressName)
                    .namespace(kubernetesNamespace)
                    .putLabelsItem("app.kubernetes.io/managed-by", "shipkit");

            var paths = java.util.List.of(
                    new io.kubernetes.client.openapi.models.V1HTTPIngressPath()
                            .path("/api")
                            .pathType("Prefix")
                            .backend(new io.kubernetes.client.openapi.models.V1IngressBackend()
                                    .service(new io.kubernetes.client.openapi.models.V1IngressServiceBackend()
                                            .name("gateway-api")
                                            .port(new io.kubernetes.client.openapi.models.V1ServiceBackendPort().number(8080))
                                    )
                            ),
                    new io.kubernetes.client.openapi.models.V1HTTPIngressPath()
                            .path("/")
                            .pathType("Prefix")
                            .backend(new io.kubernetes.client.openapi.models.V1IngressBackend()
                                    .service(new io.kubernetes.client.openapi.models.V1IngressServiceBackend()
                                            .name("shipkit-frontend")
                                            .port(new io.kubernetes.client.openapi.models.V1ServiceBackendPort().number(3000))
                                    )
                            )
            );

            var rule = new io.kubernetes.client.openapi.models.V1IngressRule()
                    .host(domain)
                    .http(new io.kubernetes.client.openapi.models.V1HTTPIngressRuleValue().paths(paths));

            var spec = new io.kubernetes.client.openapi.models.V1IngressSpec()
                    .addRulesItem(rule)
                    .ingressClassName("traefik");

            if (sslEnabled) {
                spec.addTlsItem(new io.kubernetes.client.openapi.models.V1IngressTLS().addHostsItem(domain));
            }

            var ingress = new io.kubernetes.client.openapi.models.V1Ingress()
                    .apiVersion("networking.k8s.io/v1")
                    .kind("Ingress")
                    .metadata(metadata)
                    .spec(spec);

            try {
                networkingApi.readNamespacedIngress(ingressName, kubernetesNamespace, null);
                // Exists → replace
                networkingApi.replaceNamespacedIngress(ingressName, kubernetesNamespace, ingress, null, null, null, null);
                log.info("Updated Ingress '{}' for domain {}", ingressName, domain);
            } catch (io.kubernetes.client.openapi.ApiException ex) {
                if (ex.getCode() == 404) {
                    networkingApi.createNamespacedIngress(kubernetesNamespace, ingress, null, null, null, null);
                    log.info("Created Ingress '{}' for domain {}", ingressName, domain);
                } else {
                    throw ex;
                }
            }

        } catch (Exception e) {
            throw new InternalServerException("Failed to configure Traefik Ingress: " + e.getMessage());
        }
    }

    private void rollbackIngress(String domain) {
        try {
            String ingressName = ("shipkit-" + domain).replaceAll("[^.a-z0-9-]", "-").toLowerCase();

            io.kubernetes.client.openapi.ApiClient client = io.kubernetes.client.util.Config.defaultClient();
            var networkingApi = new io.kubernetes.client.openapi.apis.NetworkingV1Api(client);
            try {
                networkingApi.deleteNamespacedIngress(ingressName, kubernetesNamespace, null, null, null, null, null, null);
                log.info("Rolled back Ingress '{}' due to failure", ingressName);
            } catch (io.kubernetes.client.openapi.ApiException ex) {
                if (ex.getCode() != 404) {
                    log.warn("Rollback ingress deletion failed: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }
}

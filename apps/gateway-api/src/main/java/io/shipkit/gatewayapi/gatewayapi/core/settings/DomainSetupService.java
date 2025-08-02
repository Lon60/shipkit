package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
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
            applyIngress(domain, sslEnabled, forceSsl);
        } catch (Exception ex) {
            rollbackIngress(domain);
            throw new InternalServerException("Failed to configure Traefik Ingress: " + ex.getMessage());
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
            String ingressName = ("shipkit-" + domain).replaceAll("[^.a-z0-9-]", "-").toLowerCase();

            ApiClient client = defaultClient();
            Configuration.setDefaultApiClient(client);

            var networkingApi = new NetworkingV1Api(client);

            var metadata = new V1ObjectMeta()
                    .name(ingressName)
                    .namespace(kubernetesNamespace)
                    .putLabelsItem("app.kubernetes.io/managed-by", "shipkit");

            if (sslEnabled) {
                metadata.putAnnotationsItem("traefik.ingress.kubernetes.io/router.tls.certresolver", "letsencrypt");

                if (forceSsl) {
                    metadata.putAnnotationsItem("traefik.ingress.kubernetes.io/router.middlewares", "traefik-shipkit-https-redirect@kubernetescrd");
                }
            }

            var paths = java.util.List.of(
                    new V1HTTPIngressPath()
                            .path("/api")
                            .pathType("Prefix")
                            .backend(new V1IngressBackend()
                                    .service(new V1IngressServiceBackend()
                                            .name("gateway-api")
                                            .port(new V1ServiceBackendPort().number(8080))
                                    )
                            ),
                    new V1HTTPIngressPath()
                            .path("/")
                            .pathType("Prefix")
                            .backend(new V1IngressBackend()
                                    .service(new V1IngressServiceBackend()
                                            .name("shipkit-frontend")
                                            .port(new V1ServiceBackendPort().number(3000))
                                    )
                            )
            );

            var rule = new V1IngressRule()
                    .host(domain)
                    .http(new V1HTTPIngressRuleValue().paths(paths));

            var spec = new V1IngressSpec()
                    .addRulesItem(rule)
                    .ingressClassName("traefik");

            if (sslEnabled) {
                spec.addTlsItem(new V1IngressTLS().addHostsItem(domain));
            }

            var ingress = new V1Ingress()
                    .apiVersion("networking.k8s.io/v1")
                    .kind("Ingress")
                    .metadata(metadata)
                    .spec(spec);

            try {
                networkingApi.readNamespacedIngress(ingressName, kubernetesNamespace, null);
                networkingApi.replaceNamespacedIngress(ingressName, kubernetesNamespace, ingress, null, null, null, null);
                log.info("Updated Ingress '{}' for domain {}", ingressName, domain);
            } catch (ApiException ex) {
                if (ex.getCode() == 404) {
                    networkingApi.createNamespacedIngress(kubernetesNamespace, ingress, null, null, null, null);
                    log.info("Created Ingress '{}' for domain {}", ingressName, domain);
                } else {
                    throw ex;
                }
            }

            try {
                networkingApi.deleteNamespacedIngress(
                        "shipkit-default",
                        kubernetesNamespace,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new V1DeleteOptions());
                log.info("Deleted default ingress 'shipkit-default'");
            } catch (ApiException e) {
                if (e.getCode() == 404) {
                    log.debug("Default ingress 'shipkit-default' already absent");
                } else {
                    log.error("Failed to delete default ingress, rolling back.", e);
                    throw new InternalServerException("Failed to delete default ingress: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new InternalServerException("Failed to configure Traefik Ingress: " + e.getMessage());
        }
    }

    private void rollbackIngress(String domain) {
        try {
            String ingressName = ("shipkit-" + domain).replaceAll("[^.a-z0-9-]", "-").toLowerCase();

            ApiClient client = defaultClient();
            var networkingApi = new NetworkingV1Api(client);
            try {
                networkingApi.deleteNamespacedIngress(ingressName, kubernetesNamespace, null, null, null, null, null, null);
                log.info("Rolled back Ingress '{}' due to failure", ingressName);
            } catch (ApiException ex) {
                if (ex.getCode() != 404) {
                    log.warn("Rollback ingress deletion failed: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Rollback failed: {}", e.getMessage());
        }
    }
}

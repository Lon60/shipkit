package io.shipkit.gatewayapi.gatewayapi.core.settings;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.custom.V1Patch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class TraefikConfigurator {

    private static final String TRAEFIK_NAMESPACE = "traefik";
    private static final String TRAEFIK_CONFIGMAP_NAME = "traefik-config";
    private static final String TRAEFIK_DEPLOYMENT_NAME = "traefik";

    public void configureAcmeEmail(String email) {
        try {
            var client = Config.defaultClient();
            var coreV1Api = new CoreV1Api(client);

            log.info("Fetching Traefik ConfigMap '{}' in namespace '{}'", TRAEFIK_CONFIGMAP_NAME, TRAEFIK_NAMESPACE);
            V1ConfigMap configMap = coreV1Api.readNamespacedConfigMap(TRAEFIK_CONFIGMAP_NAME, TRAEFIK_NAMESPACE, null);

            String traefikYml = configMap.getData().get("traefik.yml");
            if (traefikYml != null && traefikYml.contains("email:")) {
                log.info("ACME email already configured in Traefik ConfigMap. Skipping.");
                return;
            }

            String updatedTraefikYml = traefikYml.replace("# email will be populated by shipkit-gateway-api", "email: " + email);
            configMap.getData().put("traefik.yml", updatedTraefikYml);

            log.info("Updating Traefik ConfigMap with ACME email: {}", email);
            coreV1Api.replaceNamespacedConfigMap(TRAEFIK_CONFIGMAP_NAME, TRAEFIK_NAMESPACE, configMap, null, null, null, null);

            // Restart Traefik to apply the new static configuration
            log.info("Triggering rollout restart for Traefik deployment...");
            var appsV1Api = new AppsV1Api(client);
            String patchString = "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"kubectl.kubernetes.io/restartedAt\":\"" + java.time.OffsetDateTime.now() + "\"}}}}}";

            PatchUtils.patch(
                    V1Deployment.class,
                    () -> appsV1Api.patchNamespacedDeploymentCall(
                            TRAEFIK_DEPLOYMENT_NAME,
                            TRAEFIK_NAMESPACE,
                            new V1Patch(patchString),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ),
                    V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
                    client
            );

            log.info("Traefik deployment rollout restarted successfully.");

        } catch (IOException e) {
            log.error("Failed to initialize Kubernetes client", e);
            throw new RuntimeException("Could not connect to Kubernetes", e);
        } catch (ApiException e) {
            log.error("Kubernetes API error while configuring Traefik: {} - {}", e.getCode(), e.getResponseBody(), e);
            throw new RuntimeException("Kubernetes API error: " + e.getMessage(), e);
        }
    }
} 
package io.shipkit.gatewayapi.gatewayapi.core.config;

import com.google.gson.Gson;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.kubernetes.client.util.Config.defaultClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraefikConfigService {

    private final String TRAEFIK_NAMESPACE = "traefik";
    private final String TRAEFIK_CONFIGMAP_NAME = "traefik-config";
    private final String TRAEFIK_DEPLOYMENT_NAME = "traefik";

    public void configureAcmeEmail(String email) {
        try {
            ApiClient client = defaultClient();
            CoreV1Api api = new CoreV1Api(client);

            V1ConfigMap configMap = api.readNamespacedConfigMap(TRAEFIK_CONFIGMAP_NAME, TRAEFIK_NAMESPACE, null);

            String currentConfig = configMap.getData().get("traefik.yml");
            String newConfig = currentConfig;

            if (email != null && !email.isBlank()) {
                if (currentConfig.contains("email:")) {
                    newConfig = currentConfig.replaceAll(
                        "email:.*", "email: \"" + email + "\""
                    );
                } else {
                    newConfig = currentConfig.replace(
                        "acme:",
                        "acme:\n      email: \"" + email + "\""
                    );
                }
            }

            Map<String, String> newData = new HashMap<>();
            newData.put("traefik.yml", newConfig);
            configMap.setData(newData);

            api.replaceNamespacedConfigMap(TRAEFIK_CONFIGMAP_NAME, TRAEFIK_NAMESPACE, configMap, null, null, null, null);
            log.info("Updated traefik-config with ACME email: {}", email);
            
            restartTraefikDeployment(client);

        } catch (IOException | ApiException e) {
            log.error("Failed to update Traefik config", e);
        }
    }

    private void restartTraefikDeployment(ApiClient client) throws ApiException {
        AppsV1Api appsApi = new AppsV1Api(client);
        String now = OffsetDateTime.now().toString();

        Map<String, Object> patch = new HashMap<>();
        patch.put("op", "replace");
        patch.put("path", "/spec/template/metadata/annotations/kubectl.kubernetes.io~1restartedAt");
        patch.put("value", now);

        List<Map<String, Object>> patchList = new ArrayList<>();
        patchList.add(patch);
        
        String patchJson = new Gson().toJson(patchList);
        
        appsApi.patchNamespacedDeployment(
            TRAEFIK_DEPLOYMENT_NAME,
            TRAEFIK_NAMESPACE,
            new V1Patch(patchJson),
            null,
            null,
            null,
            null,
            null
        );
        log.info("Triggered a rolling restart of the Traefik deployment.");
    }
}

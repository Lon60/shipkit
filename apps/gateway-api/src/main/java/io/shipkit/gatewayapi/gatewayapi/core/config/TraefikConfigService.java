package io.shipkit.gatewayapi.gatewayapi.core.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.kubernetes.client.util.Config.defaultClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraefikConfigService {

    private final String TRAEFIK_NAMESPACE = "traefik";
    private final String TRAEFIK_CONFIGMAP_NAME = "traefik-config";

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
        } catch (IOException | ApiException e) {
            log.error("Failed to update Traefik config", e);
        }
    }
}

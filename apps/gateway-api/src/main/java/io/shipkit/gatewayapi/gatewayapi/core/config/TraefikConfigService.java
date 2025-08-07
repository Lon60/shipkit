package io.shipkit.gatewayapi.gatewayapi.core.config;

import com.google.gson.Gson;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.nio.charset.StandardCharsets;

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
    private final String TRAEFIK_SECRET_NAME = "traefik-acme";
    private final String TRAEFIK_DEPLOYMENT_NAME = "traefik";
    private final String ACME_EMAIL_KEY = "TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_EMAIL";

    public void configureAcmeEmail(String email) {
        email = email == null ? null : email.trim();
        try {
            ApiClient client = defaultClient();
            CoreV1Api api = new CoreV1Api(client);

            if (email == null || email.isBlank()) {
                log.warn("ACME email is blank – skipping Traefik secret update");
                return;
            }

            V1Secret secret;
            boolean exists = true;
            try {
                secret = api.readNamespacedSecret(TRAEFIK_SECRET_NAME, TRAEFIK_NAMESPACE, null);
            } catch (ApiException ae) {
                if (ae.getCode() == 404) {
                    exists = false;
                    secret = new V1Secret();
                    V1ObjectMeta metadata = new V1ObjectMeta();
                    metadata.setName(TRAEFIK_SECRET_NAME);
                    metadata.setNamespace(TRAEFIK_NAMESPACE);
                    secret.setMetadata(metadata);
                    secret.setType("Opaque");
                } else {
                    throw ae;
                }
            }

            Map<String, byte[]> data = secret.getData();
            if (data == null) {
                data = new HashMap<>();
            }
            data.put(ACME_EMAIL_KEY, email.getBytes(StandardCharsets.UTF_8));
            secret.setData(data);

            if (exists) {
                api.replaceNamespacedSecret(TRAEFIK_SECRET_NAME, TRAEFIK_NAMESPACE, secret, null, null, null, null);
                log.info("Updated Traefik ACME email to {}", email);
            } else {
                api.createNamespacedSecret(TRAEFIK_NAMESPACE, secret, null, null, null, null);
                log.info("Created Traefik ACME secret with email {}", email);
            }

            
            restartTraefikDeployment(client);

        } catch (IOException | ApiException e) {
            log.error("Failed to update Traefik config", e);
        }
    }

    private void restartTraefikDeployment(ApiClient client) throws ApiException {
        AppsV1Api appsApi = new AppsV1Api(client);
        String now = OffsetDateTime.now().toString();

        Map<String, Object> patch = new HashMap<>();
        patch.put("op", "add");
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

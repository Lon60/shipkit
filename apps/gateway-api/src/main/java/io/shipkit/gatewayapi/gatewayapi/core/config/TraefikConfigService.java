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
    private final String ACME_STORAGE_KEY = "TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_STORAGE";
    private final String ACME_HTTPCHALLENGE_ENTRYPOINT_KEY = "TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME_HTTPCHALLENGE_ENTRYPOINT";

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
            data.put(ACME_STORAGE_KEY, "/data/acme.json".getBytes(StandardCharsets.UTF_8));
            data.put(ACME_HTTPCHALLENGE_ENTRYPOINT_KEY, "web".getBytes(StandardCharsets.UTF_8));
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

        var deployment = appsApi.readNamespacedDeployment(TRAEFIK_DEPLOYMENT_NAME, TRAEFIK_NAMESPACE, null);
        var podTemplate = deployment.getSpec().getTemplate();
        if (podTemplate.getMetadata() == null) {
            podTemplate.setMetadata(new io.kubernetes.client.openapi.models.V1ObjectMeta());
        }
        Map<String, String> annotations = podTemplate.getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = new HashMap<>();
            podTemplate.getMetadata().setAnnotations(annotations);
        }
        annotations.put("kubectl.kubernetes.io/restartedAt", now);
        appsApi.replaceNamespacedDeployment(TRAEFIK_DEPLOYMENT_NAME, TRAEFIK_NAMESPACE, deployment, null, null, null, null);
        log.info("Triggered a rolling restart of the Traefik deployment.");
    }
}

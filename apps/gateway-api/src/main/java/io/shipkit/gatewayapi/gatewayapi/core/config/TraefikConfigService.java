package io.shipkit.gatewayapi.gatewayapi.core.config;

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
    private final String ACME_ENABLED_KEY = "TRAEFIK_CERTIFICATESRESOLVERS_LETSENCRYPT_ACME";

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
            data.put(ACME_ENABLED_KEY, "true".getBytes(StandardCharsets.UTF_8));
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

            sanitizeTraefikArgs(client);
            restartTraefikDeployment(client);

        } catch (IOException | ApiException e) {
            log.error("Failed to update Traefik config", e);
        }
    }

    private void sanitizeTraefikArgs(ApiClient client) throws ApiException {
        AppsV1Api appsApi = new AppsV1Api(client);
        var deployment = appsApi.readNamespacedDeployment(TRAEFIK_DEPLOYMENT_NAME, TRAEFIK_NAMESPACE, null);
        var podSpec = deployment.getSpec().getTemplate().getSpec();
        if (podSpec == null || podSpec.getContainers() == null || podSpec.getContainers().isEmpty()) {
            return;
        }
        var container = podSpec.getContainers().get(0);
        List<String> args = container.getArgs();
        if (args == null || args.isEmpty()) {
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String a : args) {
            if (a == null) continue;
            if (a.startsWith("--certificatesresolvers.letsencrypt.acme.email")) continue;
            if (a.startsWith("--entryPoints.websecure.http.tls.certResolver")) continue;
            filtered.add(a);
        }
        if (!filtered.equals(args)) {
            String valueJson = new com.google.gson.Gson().toJson(filtered);
            String patchJson = "[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/args\",\"value\":" + valueJson + "}]";
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
            log.info("Sanitized Traefik container args to remove conflicting ACME/certResolver flags");
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

        String patchJson = new com.google.gson.Gson().toJson(patchList);
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

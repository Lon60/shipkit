package io.shipkit.gatewayapi.gatewayapi.core.config;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.K3sControlGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

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

	private final K3sControlGrpcClient k3sClient;

	public void configureAcmeEmail(String email) {
		email = email == null ? null : email.trim();
		if (email == null || email.isBlank()) {
			log.warn("ACME email is blank – skipping Traefik secret update");
			return;
		}

		String secretYaml = buildTraefikSecretYaml(email);
		String restartYaml = buildTraefikRestartYaml();

		var res = k3sClient.applyManifest(secretYaml + "\n---\n" + restartYaml);
		if (res.getStatus() != 0) {
			log.error("Failed to apply Traefik ACME config: {}", res.getMessage());
			throw new IllegalStateException("Failed to apply Traefik ACME config: " + res.getMessage());
		}
		log.info("Traefik ACME email configured and restart triggered");
	}

	private String buildTraefikSecretYaml(String email) {
		return "apiVersion: v1\n" +
				"kind: Secret\n" +
				"metadata:\n" +
				"  name: " + TRAEFIK_SECRET_NAME + "\n" +
				"  namespace: " + TRAEFIK_NAMESPACE + "\n" +
				"type: Opaque\n" +
				"stringData:\n" +
				"  " + ACME_EMAIL_KEY + ": \"" + email + "\"\n" +
				"  " + ACME_ENABLED_KEY + ": \"true\"\n" +
				"  " + ACME_STORAGE_KEY + ": \"/data/acme.json\"\n" +
				"  " + ACME_HTTPCHALLENGE_ENTRYPOINT_KEY + ": \"web\"\n";
	}

	private String buildTraefikRestartYaml() {
		String ts = OffsetDateTime.now().toString();
		return "apiVersion: apps/v1\n" +
				"kind: Deployment\n" +
				"metadata:\n" +
				"  name: " + TRAEFIK_DEPLOYMENT_NAME + "\n" +
				"  namespace: " + TRAEFIK_NAMESPACE + "\n" +
				"spec:\n" +
				"  template:\n" +
				"    metadata:\n" +
				"      annotations:\n" +
				"        kubectl.kubernetes.io/restartedAt: \"" + ts + "\"\n";
	}
}

package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.KubernetesPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Fabric8KubernetesAdapter.
 * <p>
 * These tests require a running Kubernetes cluster (kind, minikube, or k3s).
 * They are disabled by default. Remove @Disabled to run against a local cluster.
 * </p>
 * <p>
 * Prerequisites:
 * - kubectl configured with a valid context
 * - Sufficient permissions to create namespaces and resources
 * </p>
 */
@Disabled("Integration tests require a running Kubernetes cluster")
class Fabric8KubernetesAdapterIntegrationTest {

    private static final String TEST_NAMESPACE = "shipkit-test";
    
    private static final String NGINX_DEPLOYMENT = """
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: nginx
        spec:
          replicas: 1
          selector:
            matchLabels:
              app: nginx
          template:
            metadata:
              labels:
                app: nginx
            spec:
              containers:
              - name: nginx
                image: nginx:1.25-alpine
                ports:
                - containerPort: 80
                readinessProbe:
                  httpGet:
                    path: /
                    port: 80
                  initialDelaySeconds: 5
                  periodSeconds: 5
        """;

    @Test
    void shouldCreateNamespace() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            
            // Verify by trying to create again (should not fail)
            assertDoesNotThrow(() -> adapter.createNamespace(TEST_NAMESPACE));
            
        } finally {
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldApplyAndDeleteDeployment() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            
            // Apply deployment
            Map<String, String> applied = adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            assertNotNull(applied);
            assertTrue(applied.containsKey("Deployment"));
            assertEquals("nginx", applied.get("Deployment"));
            
            // Delete deployment
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            
        } finally {
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldCreateServiceAndIngress() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            // Create service
            Map<String, String> selector = Map.of("app", "nginx");
            assertDoesNotThrow(() -> 
                adapter.createOrUpdateService(TEST_NAMESPACE, "nginx-service", selector, 80, 80)
            );
            
            // Create ingress (requires ingress controller in cluster)
            assertDoesNotThrow(() ->
                adapter.createOrUpdateIngress(
                    TEST_NAMESPACE,
                    "nginx-ingress",
                    "nginx.local",
                    "nginx-service",
                    80,
                    "nginx",
                    false, // TLS disabled for test
                    null
                )
            );
            
        } finally {
            adapter.deleteResource(TEST_NAMESPACE, "Ingress", "nginx-ingress");
            adapter.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldWaitForDeploymentReady() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            // Wait for deployment to be ready
            boolean ready = adapter.waitForDeploymentReady(TEST_NAMESPACE, "nginx", Duration.ofMinutes(2));
            
            assertTrue(ready, "Deployment should become ready");
            
            // Check status
            KubernetesPort.DeploymentStatus status = adapter.getDeploymentStatus(TEST_NAMESPACE, "nginx");
            assertEquals(1, status.replicas());
            assertEquals(1, status.readyReplicas());
            assertEquals(1, status.availableReplicas());
            assertTrue(status.available());
            
        } finally {
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldHandleNonExistentDeployment() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            
            // Try to get status of non-existent deployment
            assertThrows(com.shipkit.api.exception.ResourceNotFoundException.class, () ->
                adapter.getDeploymentStatus(TEST_NAMESPACE, "non-existent")
            );
            
        } finally {
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldApplyManifestIdempotently() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            
            // Apply deployment first time
            Map<String, String> applied1 = adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            assertNotNull(applied1);
            
            // Apply same deployment again (should be idempotent)
            Map<String, String> applied2 = adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            assertNotNull(applied2);
            assertEquals(applied1, applied2);
            
        } finally {
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldCreateIngressWithTLS() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            adapter.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            Map<String, String> selector = Map.of("app", "nginx");
            adapter.createOrUpdateService(TEST_NAMESPACE, "nginx-service", selector, 80, 80);
            
            // Create ingress with TLS
            assertDoesNotThrow(() ->
                adapter.createOrUpdateIngress(
                    TEST_NAMESPACE,
                    "nginx-ingress-tls",
                    "nginx-secure.local",
                    "nginx-service",
                    80,
                    "nginx",
                    true, // TLS enabled
                    "letsencrypt-staging" // cert-manager cluster issuer
                )
            );
            
            // Verify TLS configuration was applied (requires cert-manager in cluster)
            
        } finally {
            adapter.deleteResource(TEST_NAMESPACE, "Ingress", "nginx-ingress-tls");
            adapter.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }

    @Test
    void shouldHandleMultiDocumentYaml() {
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        String multiDocYaml = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: nginx
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: nginx
              template:
                metadata:
                  labels:
                    app: nginx
                spec:
                  containers:
                  - name: nginx
                    image: nginx:1.25-alpine
                    ports:
                    - containerPort: 80
            ---
            apiVersion: v1
            kind: Service
            metadata:
              name: nginx-service
            spec:
              selector:
                app: nginx
              ports:
              - port: 80
                targetPort: 80
            """;
        
        try {
            adapter.createNamespace(TEST_NAMESPACE);
            
            // Apply multi-document YAML
            Map<String, String> applied = adapter.applyManifest(TEST_NAMESPACE, multiDocYaml);
            
            assertNotNull(applied);
            assertTrue(applied.containsKey("Deployment"));
            assertTrue(applied.containsKey("Service"));
            
        } finally {
            adapter.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            adapter.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            adapter.deleteNamespace(TEST_NAMESPACE);
            adapter.close();
        }
    }
}

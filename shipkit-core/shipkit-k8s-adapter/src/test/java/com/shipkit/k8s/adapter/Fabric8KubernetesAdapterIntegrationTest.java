package com.shipkit.k8s.adapter;

import com.shipkit.api.exception.KubernetesOperationException;
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
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            
            // Verify by trying to create again (should not fail)
            assertDoesNotThrow(() -> k8sPort.createNamespace(TEST_NAMESPACE));
            
        } finally {
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldApplyAndDeleteDeployment() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            
            // Apply deployment
            Map<String, String> applied = k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            assertNotNull(applied);
            assertTrue(applied.containsKey("Deployment"));
            assertEquals("nginx", applied.get("Deployment"));
            
            // Delete deployment
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            
        } finally {
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldCreateServiceAndIngress() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            // Create service
            Map<String, String> selector = Map.of("app", "nginx");
            assertDoesNotThrow(() -> 
                k8sPort.createOrUpdateService(TEST_NAMESPACE, "nginx-service", selector, 80, 80)
            );
            
            // Create ingress (requires ingress controller in cluster)
            assertDoesNotThrow(() ->
                k8sPort.createOrUpdateIngress(
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
            k8sPort.deleteResource(TEST_NAMESPACE, "Ingress", "nginx-ingress");
            k8sPort.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldWaitForDeploymentReady() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            // Wait for deployment to be ready
            boolean ready = k8sPort.waitForDeploymentReady(TEST_NAMESPACE, "nginx", Duration.ofMinutes(2));
            
            assertTrue(ready, "Deployment should become ready");
            
            // Check status
            KubernetesPort.DeploymentStatus status = k8sPort.getDeploymentStatus(TEST_NAMESPACE, "nginx");
            assertEquals(1, status.getReplicas());
            assertEquals(1, status.getReadyReplicas());
            assertEquals(1, status.getAvailableReplicas());
            assertTrue(status.isAvailable());
            
        } finally {
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldHandleNonExistentDeployment() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            
            // Try to get status of non-existent deployment
            assertThrows(com.shipkit.api.exception.ResourceNotFoundException.class, () ->
                k8sPort.getDeploymentStatus(TEST_NAMESPACE, "non-existent")
            );
            
        } finally {
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldApplyManifestIdempotently() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            
            // Apply deployment first time
            Map<String, String> applied1 = k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            assertNotNull(applied1);
            
            // Apply same deployment again (should be idempotent)
            Map<String, String> applied2 = k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            assertNotNull(applied2);
            assertEquals(applied1, applied2);
            
        } finally {
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldCreateIngressWithTLS() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        try {
            k8sPort.createNamespace(TEST_NAMESPACE);
            k8sPort.applyManifest(TEST_NAMESPACE, NGINX_DEPLOYMENT);
            
            Map<String, String> selector = Map.of("app", "nginx");
            k8sPort.createOrUpdateService(TEST_NAMESPACE, "nginx-service", selector, 80, 80);
            
            // Create ingress with TLS
            assertDoesNotThrow(() ->
                k8sPort.createOrUpdateIngress(
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
            k8sPort.deleteResource(TEST_NAMESPACE, "Ingress", "nginx-ingress-tls");
            k8sPort.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }

    @Test
    void shouldHandleMultiDocumentYaml() {
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
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
            k8sPort.createNamespace(TEST_NAMESPACE);
            
            // Apply multi-document YAML
            Map<String, String> applied = k8sPort.applyManifest(TEST_NAMESPACE, multiDocYaml);
            
            assertNotNull(applied);
            assertTrue(applied.containsKey("Deployment"));
            assertTrue(applied.containsKey("Service"));
            
        } finally {
            k8sPort.deleteResource(TEST_NAMESPACE, "Service", "nginx-service");
            k8sPort.deleteResource(TEST_NAMESPACE, "Deployment", "nginx");
            k8sPort.deleteNamespace(TEST_NAMESPACE);
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }
}

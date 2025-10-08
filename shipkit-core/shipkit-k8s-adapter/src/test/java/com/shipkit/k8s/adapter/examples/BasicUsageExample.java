package com.shipkit.k8s.adapter.examples;

import com.shipkit.api.ports.KubernetesPort;
import com.shipkit.k8s.adapter.Fabric8KubernetesAdapter;

import java.time.Duration;
import java.util.Map;

/**
 * Example demonstrating basic usage of the Kubernetes adapter.
 * <p>
 * This example shows how to:
 * 1. Create a namespace
 * 2. Deploy an nginx application
 * 3. Expose it with a Service
 * 4. Create an Ingress
 * 5. Wait for the deployment to be ready
 * </p>
 */
public class BasicUsageExample {

    static void main() {
        // Initialize the adapter (use in-cluster config in production, false for local dev)
        Fabric8KubernetesAdapter adapter = new Fabric8KubernetesAdapter(false, null);
        
        String namespace = "shipkit-demo";
        
        try {
            // 1. Create namespace
            System.out.println("Creating namespace: " + namespace);
            adapter.createNamespace(namespace);
            
            // 2. Apply nginx deployment
            String deploymentYaml = """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: nginx-demo
                spec:
                  replicas: 2
                  selector:
                    matchLabels:
                      app: nginx-demo
                  template:
                    metadata:
                      labels:
                        app: nginx-demo
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
            
            System.out.println("Applying deployment...");
            Map<String, String> applied = adapter.applyManifest(namespace, deploymentYaml);
            System.out.println("Applied resources: " + applied);
            
            // 3. Create service
            System.out.println("Creating service...");
            Map<String, String> selector = Map.of("app", "nginx-demo");
            adapter.createOrUpdateService(namespace, "nginx-demo-service", selector, 80, 80);
            
            // 4. Create ingress (optional - requires ingress controller)
            System.out.println("Creating ingress...");
            adapter.createOrUpdateIngress(
                namespace,
                "nginx-demo-ingress",
                "nginx-demo.local",
                "nginx-demo-service",
                80,
                "nginx", // ingress class
                false,   // TLS disabled for demo
                null
            );
            
            // 5. Wait for deployment to be ready
            System.out.println("Waiting for deployment to be ready...");
            boolean ready = adapter.waitForDeploymentReady(namespace, "nginx-demo", Duration.ofMinutes(5));
            
            if (ready) {
                System.out.println("✓ Deployment is ready!");
                
                // Check status
                KubernetesPort.DeploymentStatus status = adapter.getDeploymentStatus(namespace, "nginx-demo");
                System.out.println("Status:");
                System.out.println("  - Replicas: " + status.replicas());
                System.out.println("  - Ready: " + status.readyReplicas());
                System.out.println("  - Available: " + status.availableReplicas());
                System.out.println("  - Message: " + status.statusMessage());
            } else {
                System.out.println("✗ Deployment did not become ready in time");
            }
            
            // Cleanup (comment out to keep resources)
            System.out.println("\nCleaning up...");
            adapter.deleteResource(namespace, "Ingress", "nginx-demo-ingress");
            adapter.deleteResource(namespace, "Service", "nginx-demo-service");
            adapter.deleteResource(namespace, "Deployment", "nginx-demo");
            adapter.deleteNamespace(namespace);
            System.out.println("Cleanup complete");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            adapter.close();
        }
    }
}

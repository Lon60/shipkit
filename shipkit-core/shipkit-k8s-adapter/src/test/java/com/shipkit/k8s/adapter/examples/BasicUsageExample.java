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

    public static void main(String[] args) {
        // Initialize the adapter (use in-cluster config in production, false for local dev)
        KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
        
        String namespace = "shipkit-demo";
        
        try {
            // 1. Create namespace
            System.out.println("Creating namespace: " + namespace);
            k8sPort.createNamespace(namespace);
            
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
            Map<String, String> applied = k8sPort.applyManifest(namespace, deploymentYaml);
            System.out.println("Applied resources: " + applied);
            
            // 3. Create service
            System.out.println("Creating service...");
            Map<String, String> selector = Map.of("app", "nginx-demo");
            k8sPort.createOrUpdateService(namespace, "nginx-demo-service", selector, 80, 80);
            
            // 4. Create ingress (optional - requires ingress controller)
            System.out.println("Creating ingress...");
            k8sPort.createOrUpdateIngress(
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
            boolean ready = k8sPort.waitForDeploymentReady(namespace, "nginx-demo", Duration.ofMinutes(5));
            
            if (ready) {
                System.out.println("✓ Deployment is ready!");
                
                // Check status
                KubernetesPort.DeploymentStatus status = k8sPort.getDeploymentStatus(namespace, "nginx-demo");
                System.out.println("Status:");
                System.out.println("  - Replicas: " + status.getReplicas());
                System.out.println("  - Ready: " + status.getReadyReplicas());
                System.out.println("  - Available: " + status.getAvailableReplicas());
                System.out.println("  - Message: " + status.getStatusMessage());
            } else {
                System.out.println("✗ Deployment did not become ready in time");
            }
            
            // Cleanup (comment out to keep resources)
            System.out.println("\nCleaning up...");
            k8sPort.deleteResource(namespace, "Ingress", "nginx-demo-ingress");
            k8sPort.deleteResource(namespace, "Service", "nginx-demo-service");
            k8sPort.deleteResource(namespace, "Deployment", "nginx-demo");
            k8sPort.deleteNamespace(namespace);
            System.out.println("Cleanup complete");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always close the adapter
            if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
                adapter.close();
            }
        }
    }
}

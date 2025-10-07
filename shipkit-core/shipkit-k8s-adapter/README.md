# Shipkit Kubernetes Adapter

This module provides a Fabric8-based implementation of the `KubernetesPort` interface, following hexagonal architecture principles.

## Overview

The adapter provides a clean abstraction over Kubernetes operations without exposing implementation details to the domain layer. It supports:

- **Namespace Management**: Create and delete namespaces
- **Resource Management**: Apply, update, and delete Kubernetes resources using server-side apply
- **Service & Ingress**: Create services and ingresses with TLS support via cert-manager
- **Readiness Checks**: Wait for deployments to become ready with exponential backoff
- **Status Queries**: Get deployment status information

## Dependencies

- **Fabric8 Kubernetes Client**: 6.13.4
- **SLF4J**: For logging

## Usage

### Basic Usage

```java
// Create adapter with in-cluster config
KubernetesPort k8sPort = new Fabric8KubernetesAdapter();

// Or with out-of-cluster config for development
KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, "/path/to/kubeconfig");
```

### Create a Namespace

```java
k8sPort.createNamespace("my-namespace");
```

### Apply a Deployment Manifest

```java
String manifest = """
    apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: nginx
    spec:
      replicas: 2
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
            image: nginx:latest
            ports:
            - containerPort: 80
    """;

Map<String, String> applied = k8sPort.applyManifest("my-namespace", manifest);
// Returns: {"Deployment": "nginx"}
```

### Create a Service

```java
Map<String, String> selector = Map.of("app", "nginx");
k8sPort.createOrUpdateService("my-namespace", "nginx-service", selector, 80, 80);
```

### Create an Ingress with TLS

```java
k8sPort.createOrUpdateIngress(
    "my-namespace",           // namespace
    "nginx-ingress",          // ingress name
    "nginx.example.com",      // host/FQDN
    "nginx-service",          // service name
    80,                       // service port
    "nginx",                  // ingress class name
    true,                     // TLS enabled
    "letsencrypt-prod"        // cert-manager cluster issuer
);
```

### Wait for Deployment Readiness

```java
boolean ready = k8sPort.waitForDeploymentReady(
    "my-namespace", 
    "nginx",
    Duration.ofMinutes(5)
);

if (ready) {
    System.out.println("Deployment is ready!");
} else {
    System.out.println("Deployment did not become ready in time");
}
```

### Get Deployment Status

```java
KubernetesPort.DeploymentStatus status = k8sPort.getDeploymentStatus("my-namespace", "nginx");

System.out.println("Replicas: " + status.getReplicas());
System.out.println("Ready: " + status.getReadyReplicas());
System.out.println("Available: " + status.isAvailable());
System.out.println("Message: " + status.getStatusMessage());
```

### Delete Resources

```java
k8sPort.deleteResource("my-namespace", "Deployment", "nginx");
k8sPort.deleteResource("my-namespace", "Service", "nginx-service");
k8sPort.deleteResource("my-namespace", "Ingress", "nginx-ingress");
k8sPort.deleteNamespace("my-namespace");
```

### Cleanup

```java
// Always close the adapter when done
if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
    adapter.close();
}
```

## Configuration

The adapter supports two configuration modes:

### In-Cluster Configuration

When running inside a Kubernetes cluster, the adapter automatically uses the service account credentials:

```java
KubernetesPort k8sPort = new Fabric8KubernetesAdapter(); // defaults to in-cluster
```

### Out-of-Cluster Configuration (Development)

For local development, you can specify a kubeconfig file:

```java
KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, "/home/user/.kube/config");
```

Or use auto-configuration (tries kubeconfig from `$KUBECONFIG` or `~/.kube/config`):

```java
KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
```

## Features

### Server-Side Apply

All resource operations use Kubernetes server-side apply (SSA) for idempotent operations and conflict resolution.

### Automatic Labels

All applied resources automatically get labeled with:
- `app=shipkit`
- `managed-by=shipkit`

### TLS with Cert-Manager

When creating an Ingress with TLS enabled, the adapter automatically:
- Adds the `cert-manager.io/cluster-issuer` annotation
- Configures TLS with the specified host
- Creates a TLS secret with name `<ingress-name>-tls`

### Readiness Checks

The `waitForDeploymentReady` method uses exponential backoff:
- Starts with 1 second delay
- Doubles each iteration
- Max backoff of 10 seconds
- Checks deployment "Available" condition

## Exception Handling

The adapter throws domain-specific exceptions:

- `KubernetesOperationException`: Base exception for all Kubernetes operations
- `ResourceNotFoundException`: When a requested resource doesn't exist
- `ReadinessTimeoutException`: When waiting for readiness is interrupted

Example:

```java
try {
    k8sPort.createNamespace("my-namespace");
    k8sPort.applyManifest("my-namespace", manifest);
    k8sPort.waitForDeploymentReady("my-namespace", "nginx", Duration.ofMinutes(5));
} catch (KubernetesOperationException e) {
    log.error("Kubernetes operation failed", e);
}
```

## Testing

The module includes unit tests for:
- TimeProvider implementation
- IdGenerator implementation
- DeploymentStatus builder

Run tests with:
```bash
./gradlew :shipkit-k8s-adapter:test
```

## Integration Testing

For integration testing against a real Kubernetes cluster, you can use:
- **kind** (Kubernetes in Docker)
- **minikube** 
- **k3s**

Example integration test setup:

```java
@Test
void testFullDeploymentFlow() {
    KubernetesPort k8sPort = new Fabric8KubernetesAdapter(false, null);
    
    try {
        // Create namespace
        k8sPort.createNamespace("test-ns");
        
        // Apply deployment
        String manifest = // ... deployment YAML
        k8sPort.applyManifest("test-ns", manifest);
        
        // Create service
        k8sPort.createOrUpdateService("test-ns", "nginx-svc", 
            Map.of("app", "nginx"), 80, 80);
        
        // Wait for ready
        boolean ready = k8sPort.waitForDeploymentReady("test-ns", "nginx", 
            Duration.ofMinutes(2));
        
        assertTrue(ready);
        
        // Check status
        var status = k8sPort.getDeploymentStatus("test-ns", "nginx");
        assertEquals(1, status.getReadyReplicas());
        
    } finally {
        k8sPort.deleteNamespace("test-ns");
        if (k8sPort instanceof Fabric8KubernetesAdapter adapter) {
            adapter.close();
        }
    }
}
```

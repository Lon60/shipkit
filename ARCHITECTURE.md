# Hexagonal Architecture - Kubernetes Adapter Implementation

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER (shipkit-api)                  │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                         PORTS (Interfaces)                    │ │
│  │                                                                │ │
│  │  ┌──────────────────┐  ┌──────────────┐  ┌─────────────────┐│ │
│  │  │ KubernetesPort   │  │ TimeProvider │  │  IdGenerator    ││ │
│  │  ├──────────────────┤  ├──────────────┤  ├─────────────────┤│ │
│  │  │ • createNS()     │  │ • now()      │  │ • generateId()  ││ │
│  │  │ • deleteNS()     │  │ • millis()   │  │ • generateUuid()││ │
│  │  │ • applyManifest()│  └──────────────┘  └─────────────────┘│ │
│  │  │ • deleteResource()                                        │ │
│  │  │ • createService()                                         │ │
│  │  │ • createIngress()                                         │ │
│  │  │ • waitForReady()                                          │ │
│  │  │ • getStatus()                                             │ │
│  │  └──────────────────┘                                         │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    DOMAIN EXCEPTIONS                          │ │
│  │                                                                │ │
│  │  ShipkitException (base)                                      │ │
│  │    ├── KubernetesOperationException                           │ │
│  │    │     └── ReadinessTimeoutException                        │ │
│  │    └── ResourceNotFoundException                              │ │
│  └──────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ implements
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│              INFRASTRUCTURE LAYER (shipkit-k8s-adapter)             │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    ADAPTERS (Implementations)                 │ │
│  │                                                                │ │
│  │  ┌──────────────────────────────────────────────────────────┐│ │
│  │  │         Fabric8KubernetesAdapter                          ││ │
│  │  │  ┌─────────────────────────────────────────────────────┐ ││ │
│  │  │  │ • Uses Fabric8 Kubernetes Client 6.13.4           │ ││ │
│  │  │  │ • Server-side apply (SSA)                          │ ││ │
│  │  │  │ • Auto-labeling (app=shipkit, managed-by=shipkit) │ ││ │
│  │  │  │ • In-cluster / Out-of-cluster config              │ ││ │
│  │  │  │ • TLS via cert-manager annotations                │ ││ │
│  │  │  │ • Exponential backoff for readiness               │ ││ │
│  │  │  └─────────────────────────────────────────────────────┘ ││ │
│  │  └──────────────────────────────────────────────────────────┘│ │
│  │                                                                │ │
│  │  ┌──────────────────┐  ┌──────────────┐  ┌─────────────────┐│ │
│  │  │ SystemTimeProvider│  │UuidIdGenerator│ │DeploymentStatus ││ │
│  │  │                   │  │               │  │     Impl        ││ │
│  │  └──────────────────┘  └──────────────┘  └─────────────────┘│ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                     │
│                           ▼ connects to                            │
│                                                                     │
│                 ┌─────────────────────────────┐                    │
│                 │   Kubernetes API Server     │                    │
│                 │  (kind/minikube/k3s/etc)    │                    │
│                 └─────────────────────────────┘                    │
└─────────────────────────────────────────────────────────────────────┘
```

## Data Flow Example

### Deploying an Application

```
Application Code
       │
       │ 1. Call port method
       ▼
┌─────────────────────────┐
│   KubernetesPort        │
│   (interface)           │
│                         │
│   applyManifest()       │
└─────────────────────────┘
       │
       │ 2. Implementation delegates
       ▼
┌──────────────────────────────────┐
│  Fabric8KubernetesAdapter        │
│                                  │
│  1. Load YAML                    │
│  2. Add labels                   │
│  3. Set namespace                │
│  4. Server-side apply            │
│  5. Map exceptions               │
└──────────────────────────────────┘
       │
       │ 3. Kubernetes API call
       ▼
┌──────────────────────────────────┐
│     Kubernetes Cluster           │
│                                  │
│  • Namespace created             │
│  • Deployment applied            │
│  • Resources labeled             │
└──────────────────────────────────┘
```

## Key Features

### 1. Server-Side Apply (SSA)
- Idempotent operations
- Conflict resolution
- Field ownership tracking
- Uses `shipkit` as field manager

### 2. Automatic Labeling
All resources get:
```yaml
labels:
  app: shipkit
  managed-by: shipkit
```

### 3. TLS Configuration
Ingress with cert-manager:
```yaml
annotations:
  cert-manager.io/cluster-issuer: <issuer-name>
spec:
  tls:
  - hosts: [<fqdn>]
    secretName: <ingress-name>-tls
```

### 4. Readiness Checks
- Exponential backoff: 1s → 2s → 4s → 8s → 10s (max)
- Checks "Available" condition
- Timeout configurable via Duration

### 5. Error Mapping
```
Fabric8 Exception → Domain Exception
─────────────────────────────────────
KubernetesClientException → KubernetesOperationException
ResourceNotFound → ResourceNotFoundException
Timeout → ReadinessTimeoutException
```

## Usage Flow

### Basic Deployment Flow

```java
// 1. Initialize adapter
KubernetesPort k8s = new Fabric8KubernetesAdapter();

// 2. Create namespace
k8s.createNamespace("my-app");

// 3. Apply deployment
k8s.applyManifest("my-app", deploymentYaml);

// 4. Create service
k8s.createOrUpdateService("my-app", "svc", 
    Map.of("app", "nginx"), 80, 80);

// 5. Create ingress with TLS
k8s.createOrUpdateIngress("my-app", "ingress",
    "app.example.com", "svc", 80, "nginx",
    true, "letsencrypt-prod");

// 6. Wait for ready
boolean ready = k8s.waitForDeploymentReady(
    "my-app", "nginx", Duration.ofMinutes(5));

// 7. Check status
DeploymentStatus status = k8s.getDeploymentStatus(
    "my-app", "nginx");
```

## Testing Strategy

### Unit Tests (7 tests ✅)
- `SystemTimeProviderTest` - Time operations
- `UuidIdGeneratorTest` - ID generation & uniqueness
- `DeploymentStatusImplTest` - Status model

### Integration Tests (8 tests, disabled by default)
- `Fabric8KubernetesAdapterIntegrationTest`
  - Namespace lifecycle
  - Deployment operations
  - Service creation
  - Ingress with/without TLS
  - Readiness waiting
  - Multi-document YAML
  - Idempotent operations
  - Error handling

### Example Code
- `BasicUsageExample` - Complete deployment flow

## Configuration

| Mode | Constructor | Use Case |
|------|-------------|----------|
| In-cluster | `new Fabric8KubernetesAdapter()` | Production (Pod) |
| Out-of-cluster | `new Fabric8KubernetesAdapter(false, null)` | Development (auto-detect) |
| Custom config | `new Fabric8KubernetesAdapter(false, "/path/to/config")` | Custom kubeconfig |

## Files Structure

```
shipkit-api/
  src/main/java/com/shipkit/api/
    ports/
      ├── KubernetesPort.java          (Main port interface)
      ├── TimeProvider.java             (Time abstraction)
      └── IdGenerator.java              (ID generation)
    exception/
      ├── ShipkitException.java         (Base exception)
      ├── KubernetesOperationException.java
      ├── ResourceNotFoundException.java
      └── ReadinessTimeoutException.java

shipkit-k8s-adapter/
  src/main/java/com/shipkit/k8s/adapter/
    ├── Fabric8KubernetesAdapter.java  (Main implementation)
    ├── SystemTimeProvider.java        (Time impl)
    ├── UuidIdGenerator.java           (ID impl)
    └── DeploymentStatusImpl.java      (Status model)
  src/test/java/com/shipkit/k8s/adapter/
    ├── DeploymentStatusImplTest.java
    ├── SystemTimeProviderTest.java
    ├── UuidIdGeneratorTest.java
    ├── Fabric8KubernetesAdapterIntegrationTest.java
    └── examples/
        └── BasicUsageExample.java
  README.md                             (Usage documentation)
```

## Dependencies

```gradle
implementation("io.fabric8:kubernetes-client:6.13.4")
```

The Fabric8 client provides:
- Kubernetes API client
- Resource builders
- Server-side apply support
- Type-safe operations
- Connection management

## Clean Architecture Benefits

✅ **Testability**: Easy to mock ports for testing  
✅ **Flexibility**: Swap implementations without changing domain  
✅ **Maintainability**: Clear separation of concerns  
✅ **Independence**: Domain doesn't depend on infrastructure  
✅ **Scalability**: Easy to add new adapters (e.g., kubectl, official client)

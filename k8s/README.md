# Kubernetes Deployment Guide for Shipkit

This directory will contain the Kubernetes manifests (or Helm chart) that let you run Shipkit on a local **k3d** cluster or on any K3s-compatible environment.

> **Status:** early draft – only Traefik bootstrap is supplied for now. The actual Shipkit workloads will be added once the `k3s-control` micro-service is merged.

---

## 1. Requirements

* Docker (for k3d)
* `k3d` ≥ v5.6
* `kubectl` ≥ v1.25

If you used `scripts/start_k3s_dev.sh` these tools are installed automatically.

## 2. Create the cluster

```bash
./scripts/start_k3s_dev.sh        # creates a single-node K3s cluster via k3d
kubectl get nodes                 # sanity-check
```

## 3. Bootstrap Traefik

Traefik acts as the global ingress & load-balancer for Shipkit. The manifest below installs it **without** the default dashboard or ACME; you can tweak the `ConfigMap` later.

```bash
kubectl apply -f bootstrap/traefik.yaml
```

Verify:

```bash
kubectl -n traefik get pods,svc,ingressroutes
```

## 4. Shipkit components (coming soon)

Once `k3s-control` and the Kubernetes translation in `gateway-api` are ready, we’ll add:

* Namespace `shipkit-system` containing:
  * `gateway-api` Deployment + Service
  * `frontend` Deployment + Service
  * `k3s-control` Deployment + Service (gRPC)
  * Config CRDs (`PlatformSetting`, etc.)
* Sample `IngressRoute`s so the UI is reachable at `http://localhost` from your host.

Until then you can keep developing the backend locally (e.g., `./gradlew bootRun`) while the cluster only hosts Traefik.

## 5. Tear down

```bash
k3d cluster delete shipkit
```

This removes the entire cluster along with all workloads and volumes. 
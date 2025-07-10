# Kubernetes Deployment Guide for Shipkit

This directory now uses **Kustomize**. `k8s/base` holds common resources, while `k8s/overlays/dev` tweaks the images for local development (built via `scripts/start_k3s_dev.sh`).

Production installs apply `k8s/base` (or a future `overlays/prod`) through the new installer script:

```bash
curl -sSL https://raw.githubusercontent.com/lon60/shipkit/main/install.sh | bash
```

Local dev (k3d): `./scripts/start_k3s_dev.sh` – this script now calls `kubectl apply -k k8s/overlays/dev` under the hood.

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
## 3. Shipkit components

Once `k3s-control` and the Kubernetes translation in `gateway-api` are ready, we’ll add:

* Namespace `shipkit-system` containing:
  * `gateway-api` Deployment + Service
  * `frontend` Deployment + Service
  * `k3s-control` Deployment + Service (gRPC)
  * Config CRDs (`PlatformSetting`, etc.)
* Sample `IngressRoute`s so the UI is reachable at `http://localhost` from your host.

Until then you can keep developing the backend locally (e.g., `./gradlew bootRun`) while the cluster only hosts Traefik.

## 4. Tear down

```bash
k3d cluster delete shipkit
```

This removes the entire cluster along with all workloads and volumes. 
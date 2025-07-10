#!/usr/bin/env bash
# scripts/start_k3s_dev.sh
#
# Spin up a single-node K3s cluster inside Docker using k3d so that the full
# "Kubernetes version" of Shipkit can be tested locally on any machine which
# already has Docker installed (no additional system-level dependencies).
#
# After the cluster is created the script prints instructions on how to deploy
# the Shipkit components.
#
# Prerequisites: Docker must be running. Everything else (k3d, kubectl) will be
# installed automatically if missing.
#
# Usage:
#   ./scripts/start_k3s_dev.sh            # creates a cluster called "shipkit"
#   CLUSTER_NAME=mycluster ./scripts/start_k3s_dev.sh  # custom name
set -euo pipefail

CLUSTER_NAME=${CLUSTER_NAME:-shipkit}
DEV_DOMAIN=${DEV_DOMAIN:-localhost}
K3D_VERSION="v5.6.0"
K8S_VERSION="v1.27.3"

## Parse CLI flags
FORCE_BUILD=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -b|--build)
      shift
      while [[ $# -gt 0 && $1 != -* ]]; do
        IFS=',' read -ra PARTS <<< "$1"
        for p in "${PARTS[@]}"; do
          FORCE_BUILD+=("$p")
        done
        shift
      done
      ;;
    *)
      echo "Unknown option: $1" >&2; exit 1;;
  esac
done

# Helper to decide rebuild
should_rebuild() {
  local img="$1"
  for i in "${FORCE_BUILD[@]}"; do
    if [[ "$i" == "$img" ]]; then return 0; fi
  done
  return 1
}

# -------------------------------------------------------------------------------------------------
# Helpers
# -------------------------------------------------------------------------------------------------
command_exists() {
  command -v "$1" >/dev/null 2>&1
}

install_k3d() {
  echo "[+] Installing k3d ${K3D_VERSION} ..."
  curl -sSL "https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh" | bash
}

install_kubectl() {
  echo "[+] Installing kubectl ${K8S_VERSION} ..."
  curl -sSL -o kubectl "https://dl.k8s.io/release/${K8S_VERSION}/bin/linux/amd64/kubectl"
  chmod +x kubectl
  sudo mv kubectl /usr/local/bin/
}

# -------------------------------------------------------------------------------------------------
# Install missing dependencies
# -------------------------------------------------------------------------------------------------
if ! command_exists k3d; then
  install_k3d
fi

if ! command_exists kubectl; then
  install_kubectl
fi

# -------------------------------------------------------------------------------------------------
# Create cluster (if it doesn't already exist)
# -------------------------------------------------------------------------------------------------
if k3d cluster list | grep -q "^${CLUSTER_NAME}\b"; then
  echo "[!] k3d cluster '${CLUSTER_NAME}' already exists – skipping creation."
else
  echo "[+] Creating k3d cluster '${CLUSTER_NAME}' ..."
  k3d cluster create "${CLUSTER_NAME}" \
    --k3s-arg "--disable=traefik@server:0" \
    --servers 1 --agents 0 \
    --api-port "6550" \
    --port "80:80@loadbalancer" \

  echo "[+] k3d cluster '${CLUSTER_NAME}' created successfully."
fi

# -------------------------------------------------------------------------------------------------
# Kubeconfig helper
# -------------------------------------------------------------------------------------------------
KUBECONFIG_FILE=$(k3d kubeconfig write "${CLUSTER_NAME}")
export KUBECONFIG="${KUBECONFIG_FILE}"

echo "[i] KUBECONFIG written to ${KUBECONFIG_FILE} and exported for this shell session."

AUTO_DEPLOY=${AUTO_DEPLOY:-true}

# ---------------------------------------------------------------------------------------------
# Build dev images if missing
# ---------------------------------------------------------------------------------------------
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

build_gateway() {
  echo "[+] Building gateway-api:dev image via Gradle ..."
  (cd "$PROJECT_ROOT/apps/gateway-api" && ./gradlew bootBuildImage --imageName=gateway-api:dev)
}

build_frontend() {
  echo "[+] Building frontend:dev image via docker ..."
  (cd "$PROJECT_ROOT/apps/frontend" && docker build -f Dockerfile -t frontend:dev .)
}

build_k3s_control() {
  echo "[+] Building k3s-control:dev image via docker ..."
  (cd "$PROJECT_ROOT/apps/k3s-control" && docker build -f Dockerfile -t k3s-control:dev .)
}

if should_rebuild gateway-api; then
  build_gateway
elif ! docker image inspect gateway-api:dev >/dev/null 2>&1; then
  build_gateway
fi

if should_rebuild frontend; then
  build_frontend
elif ! docker image inspect frontend:dev >/dev/null 2>&1; then
  build_frontend
fi

if should_rebuild k3s-control; then
  build_k3s_control
elif ! docker image inspect k3s-control:dev >/dev/null 2>&1; then
  build_k3s_control
fi

# -------------------------------------------------------------------------------------------------
# Import images into cluster
# -------------------------------------------------------------------------------------------------
for IMG in gateway-api:dev frontend:dev; do
  if docker image inspect "$IMG" >/dev/null 2>&1; then
    echo "[+] Importing $IMG into k3d cluster"
    k3d image import -c "${CLUSTER_NAME}" "$IMG" || true
  fi
done

# Import k3s-control image
if docker image inspect k3s-control:dev >/dev/null 2>&1; then
  echo "[+] Importing k3s-control:dev into k3d cluster"
  k3d image import -c "${CLUSTER_NAME}" k3s-control:dev || true
fi

# -------------------------------------------------------------------------------------------------
# Deploy full Shipkit stack (Postgres, Gateway, Frontend)
# -------------------------------------------------------------------------------------------------
# -------------------------------------------------------------
# Install Traefik via Helm (includes CRDs)
# -------------------------------------------------------------

# Ensure helm present
if ! command -v helm >/dev/null 2>&1; then
  echo "[+] Installing Helm (v3) ..."
  curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
fi

# Add repo (idempotent)
helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
helm repo update traefik >/dev/null 2>&1

echo "[+] Installing Traefik via Helm chart"
helm upgrade --install traefik traefik/traefik \
  --namespace traefik --create-namespace \
  --set service.type=LoadBalancer \
  --set ports.web.port=80 \
  --set ports.websecure.port=443 \
  --set installCRDs=true \
  --set ingressRoute.dashboard.enabled=false \
  --wait

# Wait for IngressRoute CRD
echo -n "[i] Waiting for Traefik CRDs to register"
until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1; do
  echo -n "."; sleep 2;
done
echo " ✔"

echo "[+] Applying dev manifests via Kustomize overlay"
# Create / update a ConfigMap with environment variables from .env (if present)
if [ -f "$PROJECT_ROOT/.env" ]; then
  echo "[+] Creating/updating 'shipkit-env' ConfigMap from .env file"
  # Ensure target namespace exists (created by postgres manifest, but create first for idempotency)
  kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -
  kubectl -n shipkit-system delete configmap shipkit-env 2>/dev/null || true
  kubectl -n shipkit-system create configmap shipkit-env --from-env-file="$PROJECT_ROOT/.env"
else
  echo "[!] No .env file found at $PROJECT_ROOT/.env – skipping ConfigMap creation"
fi
kubectl apply -k "$PROJECT_ROOT/k8s/overlays/dev"

# k3s-control is included in the overlay resources

# -------------------------------------------------------------------------------------------------
# Create default Ingress for local development
# -------------------------------------------------------------------------------------------------

echo "[+] Creating default Ingress for http://${DEV_DOMAIN} → / (frontend) and /api (gateway)"
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: shipkit-${DEV_DOMAIN//./-}
  namespace: shipkit-system
  labels:
    app.kubernetes.io/managed-by: shipkit-dev-script
spec:
  ingressClassName: traefik
  rules:
  - host: ${DEV_DOMAIN}
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: gateway-api
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: shipkit-frontend
            port:
              number: 3000
EOF

echo "[✓] Shipkit dev stack is now running. Access UI at: http://${DEV_DOMAIN}"

# -------------------------------------------------------------------------------------------------
# Final instructions
# ------------------------------------------------------------------------------------------------- 
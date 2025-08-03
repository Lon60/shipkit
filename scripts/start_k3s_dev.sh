#!/usr/bin/env bash
# scripts/start_k3s_dev.sh
#
# Spin up a single-node K3s cluster inside Docker using k3d for local development.
# All components (gateway-api, frontend, k3s-control) are built as Docker images
# and imported into the k3d cluster.
#
# Prerequisites: Docker must be running. Everything else (k3d, kubectl) will be
# installed automatically if missing.
#
# Usage:
#   ./scripts/start_k3s_dev.sh                    # creates a cluster called "shipkit"
#   CLUSTER_NAME=mycluster ./scripts/start_k3s_dev.sh  # custom name
#   ./scripts/start_k3s_dev.sh --build gateway-api    # force rebuild specific image
set -euo pipefail

# Check if Docker is installed and running
if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] Docker is not installed. Please install Docker first."
  exit 1
fi

# Check if Docker daemon is running by testing the socket
if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] Docker daemon is not running."
  exit 1
fi

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

# Helper functions
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

# Install missing dependencies
if ! command_exists k3d; then
  install_k3d
fi

if ! command_exists kubectl; then
  install_kubectl
fi

# Create cluster if it doesn't already exist
if k3d cluster list | grep -q "^${CLUSTER_NAME}\b"; then
  echo "[!] k3d cluster '${CLUSTER_NAME}' already exists – skipping creation."
else
  echo "[+] Creating k3d cluster '${CLUSTER_NAME}' ..."
  k3d cluster create "${CLUSTER_NAME}" \
    --k3s-arg "--disable=traefik@server:0" \
    --servers 1 --agents 0 \
    --api-port "6550" \
    --port "80:80@loadbalancer" \
    --port "443:443@loadbalancer" \

  echo "[+] k3d cluster '${CLUSTER_NAME}' created successfully."
fi

# Setup kubeconfig
KUBECONFIG_FILE=$(k3d kubeconfig write "${CLUSTER_NAME}")
export KUBECONFIG="${KUBECONFIG_FILE}"

echo "[i] KUBECONFIG written to ${KUBECONFIG_FILE} and exported for this shell session."

AUTO_DEPLOY=${AUTO_DEPLOY:-true}

# Build dev images if missing
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

# Import images into the k3d cluster
for IMG in gateway-api:dev frontend:dev; do
  if docker image inspect "$IMG" >/dev/null 2>&1; then
    echo "[+] Importing $IMG into k3d cluster"
    k3d image import -c "${CLUSTER_NAME}" "$IMG" || true
  fi
done

if docker image inspect k3s-control:dev >/dev/null 2>&1; then
  echo "[+] Importing k3s-control:dev into k3d cluster"
  k3d image import -c "${CLUSTER_NAME}" k3s-control:dev || true
fi

# Deploy full Shipkit stack (Postgres, Gateway, Frontend, Traefik)

# Ensure helm present
if ! command -v helm >/dev/null 2>&1; then
  echo "[+] Installing Helm (v3) ..."
  curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
fi

# Add repo (idempotent)
helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
helm repo update traefik >/dev/null 2>&1

# Create .env file if missing, or validate/update JWT_SECRET
ENV_FILE="$PROJECT_ROOT/.env"
ENV_EXAMPLE="$PROJECT_ROOT/.env.example"

if [ ! -f "$ENV_FILE" ] && [ -f "$ENV_EXAMPLE" ]; then
  echo "[+] Creating .env file from .env.example"
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  # Generate JWT_SECRET
  JWT_SECRET=$(openssl rand -base64 32)
  sed -i -E "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
fi

# Validate JWT_SECRET if .env exists
if [ -f "$ENV_FILE" ]; then
  # Check if JWT_SECRET exists and is valid
  JWT_SECRET_LINE=$(grep "^JWT_SECRET=" "$ENV_FILE" 2>/dev/null || echo "")
  if [ -z "$JWT_SECRET_LINE" ] || [[ "$JWT_SECRET_LINE" == "JWT_SECRET=" ]] || [[ "$JWT_SECRET_LINE" =~ JWT_SECRET=[[:space:]]*$ ]]; then
    echo "[!] JWT_SECRET missing or empty in .env - generating a new one"
    JWT_SECRET=$(openssl rand -base64 32)
    # Remove existing line if any
    grep -v "^JWT_SECRET=" "$ENV_FILE" > "${ENV_FILE}.tmp" 2>/dev/null || touch "${ENV_FILE}.tmp"
    # Add new JWT_SECRET
    echo "JWT_SECRET=$JWT_SECRET" >> "${ENV_FILE}.tmp"
    mv "${ENV_FILE}.tmp" "$ENV_FILE"
    echo "[+] Generated new JWT_SECRET in .env file"
  fi

  echo "[+] Creating/updating 'shipkit-env' ConfigMap from .env file"
  # Ensure target namespace exists
  kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -
  kubectl -n shipkit-system delete configmap shipkit-env 2>/dev/null || true
  kubectl -n shipkit-system create configmap shipkit-env --from-env-file="$ENV_FILE"
else
  echo "[!] No .env file found at $PROJECT_ROOT/.env – skipping ConfigMap creation"
fi

# First, create the traefik namespace
echo "[+] Creating traefik namespace"
kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -

# Install Traefik with our configuration - this installs CRDs first
echo "[+] Installing Traefik via Helm chart (this installs CRDs)"
helm upgrade --install traefik traefik/traefik \
  --namespace traefik \
  --create-namespace \
  --version "v25.0.0" \
  -f "$PROJECT_ROOT/k8s/base/traefik/helm-values.yaml" \
  --set installCRDs=true \
  --wait

# Force install the CRDs if for some reason they weren't installed by Helm
echo "[+] Ensuring Traefik CRDs are installed"
kubectl get crd | grep -q "middlewares.traefik.io" || {
  echo "[!] Traefik CRDs not found, installing manually"
  TEMP_DIR=$(mktemp -d)
  curl -s -o "$TEMP_DIR/crds.yaml" https://raw.githubusercontent.com/traefik/traefik/v2.10/docs/content/reference/dynamic-configuration/kubernetes-crd-definition-v1.yml
  kubectl apply -f "$TEMP_DIR/crds.yaml"
  rm -rf "$TEMP_DIR"
}

# Wait for Traefik CRDs to be registered
echo -n "[i] Waiting for Traefik CRDs to register"
until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1; do
  echo -n "."; sleep 2;
done
echo " ✔"

# Apply Traefik configuration (requires CRDs to be installed first)
echo "[+] Applying Traefik configuration from local k8s directory"
kubectl apply -k "$PROJECT_ROOT/k8s/base/traefik"

# Apply the full development overlay with all resources
echo "[+] Applying development overlay via Kustomize"
kubectl apply -k "$PROJECT_ROOT/k8s/overlays/development"

echo "[✓] Shipkit dev stack is now running. Access UI at: http://${DEV_DOMAIN}"
echo "[i] If you encounter 404 errors:"
echo "    1. Make sure you use http://${DEV_DOMAIN} "
echo "    2. Allow a minute for services to initialize"
echo "    3. Run: kubectl get ingress -n shipkit-system"
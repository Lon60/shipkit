#!/usr/bin/env bash
# Shipkit – one-liner installer
#
# Usage:
#   curl -sSL https://raw.githubusercontent.com/lon60/shipkit/main/install.sh | bash
#
# What it does:
#   1. Installs Docker (if missing) and K3s (lightweight Kubernetes) on Ubuntu/Debian.
#   2. Installs kubectl + Helm if not present.
#   3. Installs Traefik Ingress controller via Helm.
#   4. Deploys the latest Shipkit release (Kustomize base) from the GitHub repo.
#
# -------------------------------------------------------------------------------------------------
set -euo pipefail

INFO()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
WARN()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
ERROR() { echo -e "\033[1;31m[ERR ]\033[0m  $*"; }

# ------------------------------------------------------------
# 0. Privilege check
# ------------------------------------------------------------
if [[ "$EUID" -ne 0 ]]; then
  ERROR "This script must be run as root (or via sudo)."
  exit 1
fi

# ------------------------------------------------------------
# 1. Docker & K3s
# ------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  INFO "Installing Docker (CE) …"
  apt-get update -qq
  apt-get install -yq ca-certificates curl gnupg lsb-release >/dev/null
  mkdir -p /etc/apt/keyrings
  curl -fsSL "https://download.docker.com/linux/$(. /etc/os-release && echo "$ID")/gpg" \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/$(. /etc/os-release && echo "$ID") \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list >/dev/null
  apt-get update -qq && apt-get install -yq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin >/dev/null
  systemctl enable --now docker
else
  INFO "Docker already installed – skipping."
fi

if [[ "${SKIP_K3S:-0}" != "1" ]]; then
  if ! command -v kubectl >/dev/null 2>&1; then
    INFO "Installing single-node K3s …"
    curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik" sh -
  else
    INFO "kubectl present – assuming an existing cluster; skipping K3s install."
  fi
fi

if [[ -f /etc/rancher/k3s/k3s.yaml ]]; then
  export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
fi

# ------------------------------------------------------------
# 2. kubectl & Helm (if we didn’t install via K3s bundle)
# ------------------------------------------------------------
if ! command -v kubectl >/dev/null 2>&1; then
  INFO "Installing kubectl …"
  curl -sSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/$(curl -sSL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" \
    && chmod +x /usr/local/bin/kubectl
fi

if ! command -v helm >/dev/null 2>&1; then
  INFO "Installing Helm …"
  curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
fi

# ------------------------------------------------------------
# 3. Generate .env and ConfigMap
# ------------------------------------------------------------

SHIPKIT_HOME="$HOME/shipkit"
mkdir -p "$SHIPKIT_HOME"
ENV_FILE="$SHIPKIT_HOME/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  INFO "Generating .env file at $ENV_FILE"
  curl -sSL "https://raw.githubusercontent.com/Lon60/shipkit/refs/heads/main/.env.example" -o "$ENV_FILE"
  JWT_SECRET=$(openssl rand -base64 32)
  sed -i -E "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$ENV_FILE"
fi

# Ensure namespace exists before creating ConfigMap
kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -

# Create or update ConfigMap from .env
kubectl -n shipkit-system create configmap shipkit-env --from-env-file="$ENV_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

# ------------------------------------------------------------
# 4. Deploy Shipkit with Kustomize (production overlay)
# ------------------------------------------------------------
INFO "Adding Traefik Helm repo"
helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
helm repo update traefik >/dev/null 2>&1

# Create traefik namespace
kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -

# Apply Traefik and core Shipkit components with Kustomize
INFO "Deploying Traefik using values from Kustomize configs"
REMOTE_HELM_VALUES="github.com/lon60/shipkit//k8s/base/traefik/helm-values.yaml?ref=main"

# Install Traefik using the helm values from our repo
helm upgrade --install traefik traefik/traefik \
  --namespace traefik \
  --create-namespace \
  --version "v25.0.0" \
  -f <(curl -sSL "https://raw.githubusercontent.com/$REMOTE_HELM_VALUES") \
  --set installCRDs=true \
  --wait

# Wait for Traefik CRDs to be registered
echo -n "[i] Waiting for Traefik CRDs to register";
until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1 && kubectl get crd middlewares.traefik.io >/dev/null 2>&1; do
  echo -n "."; sleep 2;
done
echo " ✔"

INFO "Applying Traefik base configuration (Middlewares, etc.)"
TRAEFIK_BASE="github.com/lon60/shipkit//k8s/base/traefik?ref=main"
kubectl apply -k "$TRAEFIK_BASE"

INFO "Deploying Shipkit components using Kustomize (production overlay)"
REMOTE="github.com/lon60/shipkit//k8s/overlays/production?ref=main"

# Apply the production overlay
kubectl apply -k "$REMOTE"


# Ensure pods pull latest images
INFO "Rolling Deployments to ensure newest images are used"
kubectl -n shipkit-system rollout restart deployment gateway-api shipkit-frontend k3s-control postgres || true

PUBLIC_IP=$(curl -s https://api.ipify.org || echo "<server-ip>")
INFO "Shipkit installation complete! Access the UI at: http://$PUBLIC_IP or http://localhost"
INFO "Run: kubectl -n shipkit-system get pods,svc to watch status." 
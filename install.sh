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
  curl -fsSL https://download.docker.com/linux/$(. /etc/os-release && echo "$ID")/gpg | \
    gpg --dearmor -o /etc/apt/keyrings/docker.gpg
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
    export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
  else
    INFO "kubectl present – assuming an existing cluster; skipping K3s install."
  fi
else
  WARN "SKIP_K3S=1 → assuming an existing cluster."
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
# 3. Traefik Ingress controller
# ------------------------------------------------------------
helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true
helm repo update traefik >/dev/null 2>&1

INFO "Deploying Traefik"
helm upgrade --install traefik traefik/traefik \
  --namespace traefik --create-namespace \
  --set service.type=LoadBalancer \
  --set installCRDs=true \
  --wait

# ------------------------------------------------------------
# 4. Deploy Shipkit workload (latest main branch)
# ------------------------------------------------------------
INFO "Deploying Shipkit workloads (namespace: shipkit-system)"
REMOTE="github.com/lon60/shipkit//k8s/base?ref=main"

kubectl apply -k "$REMOTE"

INFO "Rolling Deployments to ensure newest images are used"
kubectl -n shipkit-system rollout restart deployment gateway-api shipkit-frontend k3s-control || true

INFO "Shipkit installation complete! Run:\n  kubectl -n shipkit-system get pods,svc" 
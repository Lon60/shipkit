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
K3D_VERSION="v5.6.0"
K8S_VERSION="v1.27.3"

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
    --port "443:443@loadbalancer" \
    --port "9998:9998@loadbalancer" # gRPC port example for k3s-control

  echo "[+] k3d cluster '${CLUSTER_NAME}' created successfully."
fi

# -------------------------------------------------------------------------------------------------
# Kubeconfig helper
# -------------------------------------------------------------------------------------------------
KUBECONFIG_FILE=$(k3d kubeconfig write "${CLUSTER_NAME}")
export KUBECONFIG="${KUBECONFIG_FILE}"

echo "[i] KUBECONFIG written to ${KUBECONFIG_FILE} and exported for this shell session."

echo "\nNext steps:"
echo "1. Verify cluster is up: kubectl get nodes -o wide"
echo "2. Deploy Traefik & Shipkit components – for now you can run:"
echo "     kubectl apply -f k8s/bootstrap/ (manifests to be added)"
echo "3. Access Shipkit UI on http://localhost (ports 80/443 are forwarded)."
echo "\nTo destroy the cluster run: k3d cluster delete ${CLUSTER_NAME}" 
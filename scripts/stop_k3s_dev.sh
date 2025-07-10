#!/usr/bin/env bash
# scripts/stop_k3s_dev.sh
#
# Remove the local k3d-based K3s dev cluster that was created by
#   scripts/start_k3s_dev.sh
#
# By default the cluster is named "shipkit".  Override with
#   CLUSTER_NAME=mycluster ./scripts/stop_k3s_dev.sh
#
# This script is idempotent: if the cluster does not exist it exits successfully.
set -euo pipefail

CLUSTER_NAME=${CLUSTER_NAME:-shipkit}

if ! command -v k3d &>/dev/null; then
  echo "k3d is not installed – nothing to delete." >&2
  exit 0
fi

echo "[+] Deleting k3d cluster '${CLUSTER_NAME}' (if it exists) ..."
if k3d cluster list | grep -q "^${CLUSTER_NAME}\b"; then
  k3d cluster delete "${CLUSTER_NAME}"
else
  echo "[i] Cluster '${CLUSTER_NAME}' not found, skipping delete."
fi

# Remove kubeconfig file (k3d stores one per cluster)
KUBECONFIG_FILE="$HOME/.config/k3d/kubeconfig-${CLUSTER_NAME}.yaml"
if [[ -f "${KUBECONFIG_FILE}" ]]; then
  echo "[+] Removing kubeconfig ${KUBECONFIG_FILE}"
  rm -f "${KUBECONFIG_FILE}"
fi

# Unset KUBECONFIG for current shell (if set)
if [[ "${KUBECONFIG:-}" != "" ]]; then
  unset KUBECONFIG
  echo "[+] Unset KUBECONFIG environment variable for this shell session."
fi

echo "[✓] Cluster cleanup complete." 
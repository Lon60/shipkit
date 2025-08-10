#!/usr/bin/env bash
set -euo pipefail
U="${SUDO_USER:-$USER}"; D="$(eval echo ~"$U")/Downloads"; mkdir -p "$D"
M=$(uname -m)
case "$M" in
  x86_64|amd64) A=amd64 ;;
  aarch64|arm64) A=arm64 ;;
  *) echo "Unsupported architecture: $M" >&2; exit 1 ;;
esac
URL="https://github.com/lon60/shipkit/releases/latest/download/shipkit-install-linux-$A"
curl -fsSL -o "$D/shipkit-install" "$URL"
chmod +x "$D/shipkit-install"
exec "$D/shipkit-install"

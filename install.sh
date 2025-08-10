#!/usr/bin/env bash
set -euo pipefail
U="${SUDO_USER:-$USER}"; D="$(eval echo ~"$U")/Downloads"; mkdir -p "$D"
curl -fsSL -o "$D/shipkit-install" https://github.com/lon60/shipkit/releases/latest/download/shipkit-install
chmod +x "$D/shipkit-install"
exec "$D/shipkit-install"

#!/usr/bin/env bash
set -euo pipefail

# scripts/e2e/run_e2e.sh
# Local E2E test harness for Shipkit using k3d-based dev stack
# - Spins up the dev stack (Traefik, Postgres, gateway-api, frontend)
# - Registers the first admin via GraphQL
# - Configures the domain via GraphQL (skip DNS validation for local)
# - Verifies HTTP->HTTPS redirects and service routing
#
# Usage:
#   scripts/e2e/run_e2e.sh                 # default domain shipkit.local
#   DOMAIN=example.local scripts/e2e/run_e2e.sh
#   DOMAIN=<public-domain> ACME_EMAIL=<email> ACME_STAGING=1 scripts/e2e/run_e2e.sh  # optional ACME staging test
#
# Notes:
# - For ACME tests you need a public domain pointed to your host and ports 80/443 reachable from the Internet.
# - By default we use a local mkcert-generated cert (if available) or Traefik's fallback cert.

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJECT_ROOT"

DOMAIN=${DOMAIN:-shipkit.local}
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@${DOMAIN}}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-password123}
SKIP_VALIDATION=${SKIP_VALIDATION:-true}
SSL_ENABLED=${SSL_ENABLED:-true}
FORCE_SSL=${FORCE_SSL:-true}

# Optional ACME config (staging recommended for tests)
ACME_EMAIL=${ACME_EMAIL:-}
ACME_STAGING=${ACME_STAGING:-}

export DEV_DOMAIN="${DOMAIN}"

echo "[+] Starting dev stack for domain '${DOMAIN}'"
"$PROJECT_ROOT/scripts/start_k3s_dev.sh"

# If ACME is requested, ensure Traefik has email and optional staging CA set
if [[ -n "${ACME_EMAIL}" ]]; then
  echo "[+] Configuring Traefik ACME email (${ACME_EMAIL})";
  set +e
  EXTRA_SET=()
  EXTRA_SET+=("--set" "certificatesResolvers.letsencrypt.acme.email=${ACME_EMAIL}")
  if [[ -n "${ACME_STAGING}" ]]; then
    echo "[i] Using Let's Encrypt STAGING CA for tests"
    EXTRA_SET+=("--set" "certificatesResolvers.letsencrypt.acme.caServer=https://acme-staging-v02.api.letsencrypt.org/directory")
  fi
  set -e
  helm upgrade --install traefik traefik/traefik \
    --namespace traefik \
    -f "$PROJECT_ROOT/k8s/base/traefik/helm-values.yaml" \
    "${EXTRA_SET[@]}" \
    --wait
fi

# Run Python e2e driver
EXPECT_ISSUER_SUBSTR=""
if [[ -n "${ACME_EMAIL}" ]]; then
  if [[ -n "${ACME_STAGING}" ]]; then
    EXPECT_ISSUER_SUBSTR="Fake LE"
  else
    EXPECT_ISSUER_SUBSTR="Let's Encrypt"
  fi
fi

python3 "$PROJECT_ROOT/e2e/e2e.py" \
  --domain "$DOMAIN" \
  --admin-email "$ADMIN_EMAIL" \
  --admin-password "$ADMIN_PASSWORD" \
  --skip-validation "$SKIP_VALIDATION" \
  --ssl-enabled "$SSL_ENABLED" \
  --force-ssl "$FORCE_SSL" \
  --graphql-http "http://localhost/api/graphql" \
  --graphql-https "https://${DOMAIN}/api/graphql" \
  ${EXPECT_ISSUER_SUBSTR:+--expect-issuer-substr "$EXPECT_ISSUER_SUBSTR"}

echo "[✓] E2E passed for domain ${DOMAIN}"
### Shipkit E2E Tests (local)

This harness spins up the dev k3d stack and verifies the platform end-to-end:
- Builds/imports dev images and deploys Traefik + Shipkit components
- Registers the first admin account via GraphQL
- Configures the platform domain via GraphQL
- Checks routing: `/` → frontend, `/api` → gateway-api
- Validates HTTPS (mkcert if present or Traefik fallback); optionally exercises ACME in staging

#### Prerequisites
- Docker running
- Python 3.10+

Install Python deps:

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r e2e/requirements.txt
```

#### Run

```bash
scripts/e2e/run_e2e.sh
```

Environment variables:
- `DOMAIN` (default: `shipkit.local`)
- `ADMIN_EMAIL` (default: `admin@<DOMAIN>`)
- `ADMIN_PASSWORD` (default: `password123`)
- `SKIP_VALIDATION` (default: `true`) – skip DNS A-record check for local
- `SSL_ENABLED` (default: `true`)
- `FORCE_SSL` (default: `true`) – HTTP→HTTPS redirect
- `ACME_EMAIL` – set to test Traefik ACME (requires public domain + reachable 80/443)
- `ACME_STAGING=1` – use Let’s Encrypt staging directory to avoid rate limits

#### What it checks
- Gateway reachable at `http://localhost/api/graphql`
- Admin registration/login OK
- Domain setup mutation applies Traefik IngressRoutes
- HTTP routing works; if `FORCE_SSL=true`, HTTP redirects to HTTPS
- HTTPS for domain works; GraphQL `status` query returns healthy and adminInitialized
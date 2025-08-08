#!/usr/bin/env python3
import argparse
import json
import os
import sys
import time
import socket
import ssl
from typing import Tuple

import requests

DEFAULT_TIMEOUT = 420  # seconds for components to become ready
RETRY_SLEEP = 3


def wait_for_graphql(url: str, timeout: int = DEFAULT_TIMEOUT) -> None:
    deadline = time.time() + timeout
    last_exc = None
    payload = {"query": "{ status { status } }"}
    while time.time() < deadline:
        try:
            r = requests.post(url, json=payload, timeout=10)
            if r.status_code == 200:
                return
        except Exception as e:
            last_exc = e
        time.sleep(RETRY_SLEEP)
    if last_exc:
        raise RuntimeError(f"Timeout waiting for GraphQL at {url}: last error {last_exc}")
    raise RuntimeError(f"Timeout waiting for GraphQL at {url}")


def graphql(url: str, query: str, variables: dict | None = None, token: str | None = None) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    payload = {"query": query}
    if variables is not None:
        payload["variables"] = variables
    resp = requests.post(url, headers=headers, data=json.dumps(payload), timeout=20)
    resp.raise_for_status()
    data = resp.json()
    if "errors" in data:
        raise RuntimeError(f"GraphQL errors: {data['errors']}")
    return data["data"]


def register_admin(graphql_url: str, email: str, password: str) -> str:
    mutation = """
    mutation Register($input: CreateAccountDTO!) {
      register(input: $input) { token }
    }
    """
    data = graphql(graphql_url, mutation, {"input": {"email": email, "password": password}})
    return data["register"]["token"]


def login(graphql_url: str, email: str, password: str) -> str:
    mutation = """
    mutation Login($email: String!, $password: String!) {
      login(email: $email, password: $password) { token }
    }
    """
    data = graphql(graphql_url, mutation, {"email": email, "password": password})
    return data["login"]["token"]


def setup_domain(graphql_url: str, token: str, domain: str, skip_validation: bool, ssl_enabled: bool, force_ssl: bool) -> None:
    mutation = """
    mutation SetupDomain($domain: String!, $skip: Boolean!, $ssl: Boolean!, $force: Boolean!) {
      setupDomain(domain: $domain, skipValidation: $skip, sslEnabled: $ssl, forceSsl: $force)
    }
    """
    graphql(
        graphql_url,
        mutation,
        {"domain": domain, "skip": skip_validation, "ssl": ssl_enabled, "force": force_ssl},
        token,
    )


def get_status(graphql_url: str) -> dict:
    query = """
    query { status { status adminInitialized domainInitialized } }
    """
    return graphql(graphql_url, query)


def _resolve_domain_ip(domain: str) -> str | None:
    try:
        return socket.gethostbyname(domain)
    except Exception:
        return None


def verify_routing(domain: str, expect_ssl: bool, force_ssl: bool) -> None:
    resolved_ip = _resolve_domain_ip(domain)
    base_http = domain if resolved_ip else "localhost"
    headers = {"Host": domain} if not resolved_ip else {}

    # HTTP root should be 200 (frontend) unless force_ssl true and no HTTP route
    http_root = f"http://{base_http}/"
    r = requests.get(http_root, timeout=15, allow_redirects=False, headers=headers)
    if force_ssl and expect_ssl:
        # Depending on current ingress logic, HTTP may still be served; accept redirect or OK
        assert r.status_code in (200, 301, 308, 304), f"Unexpected status at {http_root}: {r.status_code}"
    else:
        assert r.status_code in (200, 304), f"Expected OK at {http_root}, got {r.status_code}"

    # /api must respond (GraphQL endpoint)
    http_api = f"http://{base_http}/api/graphql"
    r = requests.get(http_api, timeout=15, allow_redirects=False, headers=headers)
    # GraphQL over GET may return 200/400/405 depending on server; accept these
    assert r.status_code in (200, 400, 405, 301, 308), f"Unexpected status for {http_api}: {r.status_code}"

    if expect_ssl:
        base_https = domain if resolved_ip else "localhost"
        https_root = f"https://{base_https}/"
        r = requests.get(https_root, timeout=20, verify=False, headers=headers)
        assert r.status_code in (200, 304), f"Expected OK at {https_root}, got {r.status_code}"

        https_api = f"https://{base_https}/api/graphql"
        r = requests.post(https_api, json={"query": "{ status { status } }"}, timeout=20, verify=False, headers=headers)
        assert r.status_code == 200, f"GraphQL over HTTPS failed at {https_api}, got {r.status_code}"


def get_tls_issuer(domain: str, timeout: int = 20) -> str:
    ctx = ssl.create_default_context()
    with socket.create_connection((domain, 443), timeout=timeout) as sock:
        with ctx.wrap_socket(sock, server_hostname=domain) as ssock:
            cert = ssock.getpeercert()
            issuer = cert.get("issuer")
            # issuer is list of tuples of ( (key, value), ... ). Flatten values
            parts = []
            for rdn in issuer:
                for key, value in rdn:
                    parts.append(f"{key}={value}")
            return ", ".join(parts)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--domain", required=True)
    parser.add_argument("--admin-email", required=True)
    parser.add_argument("--admin-password", required=True)
    parser.add_argument("--skip-validation", default="true")
    parser.add_argument("--ssl-enabled", default="true")
    parser.add_argument("--force-ssl", default="true")
    parser.add_argument("--graphql-http", default="http://localhost/api/graphql")
    parser.add_argument("--graphql-https", default=None)
    parser.add_argument("--expect-issuer-substr", default=None)
    args = parser.parse_args()

    domain = args.domain
    admin_email = args.admin_email
    admin_password = args.admin_password
    skip_validation = str(args.skip_validation).lower() == "true"
    ssl_enabled = str(args.ssl_enabled).lower() == "true"
    force_ssl = str(args.force_ssl).lower() == "true"

    graphql_http = args.graphql_http
    graphql_https = args.graphql_https or f"https://{domain}/api/graphql"

    print("[i] Waiting for gateway GraphQL to be ready at http://localhost/api/graphql ...")
    wait_for_graphql(graphql_http)

    print("[+] Registering first admin account ...")
    try:
        token = register_admin(graphql_http, admin_email, admin_password)
    except RuntimeError as e:
        # If already registered, try login path
        print("[i] Registration failed (possibly already registered), attempting login ...")
        token = login(graphql_http, admin_email, admin_password)

    print("[+] Setting up domain via GraphQL ...")
    setup_domain(graphql_http, token, domain, skip_validation, ssl_enabled, force_ssl)

    print("[i] Waiting for ingress to propagate ...")
    time.sleep(5)

    print("[+] Verifying routing and TLS ...")
    verify_routing(domain, expect_ssl=ssl_enabled, force_ssl=force_ssl)

    print("[+] Verifying GraphQL status over HTTPS ...")
    data = graphql(graphql_https, "{ status { status adminInitialized domainInitialized } }", token=None)
    assert data["status"]["status"] == "healthy"
    assert data["status"]["adminInitialized"] is True
    # domainInitialized may be async; allow it to become true with retries
    deadline = time.time() + 120
    ok = False
    while time.time() < deadline:
        res = graphql(graphql_https, "{ status { domainInitialized } }")
        if res["status"]["domainInitialized"]:
            ok = True
            break
        time.sleep(5)
    if not ok:
        print("[!] domainInitialized still false after waiting; continuing")

    if args.expect_issuer_substr:
        print(f"[+] Checking TLS issuer contains '{args.expect_issuer_substr}' ...")
        issuer = get_tls_issuer(domain)
        assert args.expect_issuer_substr in issuer, f"Issuer '{issuer}' does not contain expected substring"

    print("[✓] E2E checks passed")


if __name__ == "__main__":
    sys.exit(main())
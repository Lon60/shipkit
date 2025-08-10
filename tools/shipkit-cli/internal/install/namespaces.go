package install

import "os/exec"

func EnsureNamespacesAndSecrets(useMkcert bool) error {
	if err := exec.Command("bash", "-lc", "kubectl get namespace traefik >/dev/null 2>&1 || kubectl create namespace traefik").Run(); err != nil {
		return err
	}
	if err := exec.Command("bash", "-lc", "kubectl -n traefik get secret traefik-acme >/dev/null 2>&1 || kubectl -n traefik create secret generic traefik-acme --from-literal=dummy=1").Run(); err != nil {
		return err
	}
	if err := exec.Command("bash", "-lc", "kubectl get namespace shipkit-system >/dev/null 2>&1 || kubectl create namespace shipkit-system").Run(); err != nil {
		return err
	}
	if useMkcert {
		_ = exec.Command("bash", "-lc", "mkcert -install >/dev/null 2>&1 || true").Run()
		_ = exec.Command("bash", "-lc", "TMP=$(mktemp -d); mkcert -cert-file $TMP/shipkit.crt -key-file $TMP/shipkit.key shipkit.local *.shipkit.local && kubectl -n traefik create secret tls shipkit-dev-tls --cert=$TMP/shipkit.crt --key=$TMP/shipkit.key --dry-run=client -o yaml | kubectl apply -f - && rm -rf $TMP").Run()
	}
	return nil
}

package install

import (
	"os/exec"
	"path/filepath"
)

func InstallTraefik(baseValuesPath string, additionalValues []string, chartVersion string) error {
	if err := exec.Command("bash", "-lc", "helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true").Run(); err != nil {
		return err
	}
	if err := exec.Command("helm", "repo", "update", "traefik").Run(); err != nil {
		return err
	}
	args := []string{
		"upgrade", "--install", "traefik", "traefik/traefik",
		"--namespace", "traefik",
		"--create-namespace",
		"--version", chartVersion,
		"--reset-values",
		"-f", filepath.Clean(baseValuesPath),
	}
	for _, v := range additionalValues {
		if v != "" {
			args = append(args, "-f", filepath.Clean(v))
		}
	}
	args = append(args, "--set", "installCRDs=true")
	if err := exec.Command("helm", args...).Run(); err != nil {
		return err
	}
	// ensure CRDs present
	_ = exec.Command("bash", "-lc", "kubectl get crd | grep -q 'middlewares.traefik.io' || (curl -s -o /tmp/traefik-crds.yaml https://raw.githubusercontent.com/traefik/traefik/v2.10/docs/content/reference/dynamic-configuration/kubernetes-crd-definition-v1.yml && kubectl apply -f /tmp/traefik-crds.yaml)").Run()
	return exec.Command("bash", "-lc", "until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1; do sleep 2; done").Run()
}

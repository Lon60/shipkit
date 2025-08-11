package install

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
)

func ensureBinary(name string, hint string) error {
	if _, err := exec.LookPath(name); err != nil {
		return fmt.Errorf("%s not found. %s", name, hint)
	}
	return nil
}

func EnsureKubectl() error {
	return ensureBinary("kubectl", "Install kubectl")
}

func EnsureKubectlInstalled() error {
	if _, err := exec.LookPath("kubectl"); err == nil {
		return nil
	}
	return exec.Command("bash", "-lc", `curl -sSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/$(curl -sSL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && chmod +x /usr/local/bin/kubectl`).Run()
}

func EnsureHelm() error {
	if _, err := exec.LookPath("helm"); err == nil {
		return nil
	}
	return errors.New("helm not installed")
}

func EnsureContainerEngineInstalled() error {
	if _, err := exec.LookPath("podman"); err == nil {
		return nil
	}
	cmd := exec.Command("bash", "-lc", `
set -euo pipefail
if command -v apt-get >/dev/null 2>&1; then
  . /etc/os-release
  sudo apt-get update -qq
  sudo apt-get install -yq ca-certificates curl gnupg lsb-release >/dev/null
  sudo apt-get -yq install podman >/dev/null
fi`)
	return cmd.Run()
}

func EnsureK3sInstalled() error {
	if _, err := os.Stat("/etc/rancher/k3s/k3s.yaml"); err == nil {
		return nil
	}
	return exec.Command("bash", "-lc", `curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik" sh -`).Run()
}

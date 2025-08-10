package install

import (
	"os/exec"
)

// EnsureHelmInstalled installs Helm v3 if it is not present.
func EnsureHelmInstalled() error {
	if _, err := exec.LookPath("helm"); err == nil {
		return nil
	}
	return exec.Command("bash", "-lc", "curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash").Run()
}

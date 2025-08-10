package install

import (
	"fmt"
	"os/exec"
)

func ApplyOverlay(overlayPath string) error {
	return exec.Command("kubectl", "apply", "-k", overlayPath).Run()
}

func WaitForDeployment(ns, name string, timeoutSeconds int) error {
	check := exec.Command("bash", "-lc", fmt.Sprintf("kubectl -n %s get deploy %s >/dev/null 2>&1", ns, name))
	for i := 0; i < timeoutSeconds; i++ {
		if check.Run() == nil {
			break
		}
		_ = exec.Command("bash", "-lc", "sleep 1").Run()
		if i == timeoutSeconds-1 {
			return fmt.Errorf("deployment %s/%s not found", ns, name)
		}
	}
	return exec.Command("kubectl", "-n", ns, "rollout", "status", fmt.Sprintf("deployment/%s", name), fmt.Sprintf("--timeout=%ds", timeoutSeconds)).Run()
}

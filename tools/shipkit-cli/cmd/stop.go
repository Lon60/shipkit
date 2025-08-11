package cmd

import (
	"fmt"
	"os/exec"

	"github.com/spf13/cobra"
)

var stopCmd = &cobra.Command{
	Use:   "stop",
	Short: "Tear down the kind dev cluster",
	RunE: func(cmd *cobra.Command, args []string) error {
		if _, err := exec.LookPath("telepresence"); err == nil {
			_ = exec.Command("telepresence", "quit", "-s").Run()
		}
		if _, err := exec.LookPath("helm"); err == nil {
			_ = exec.Command("bash", "-lc", "helm uninstall traefik -n traefik >/dev/null 2>&1 || true").Run()
		}
		_ = exec.Command("bash", "-lc", "kubectl delete namespace traefik --ignore-not-found=true").Run()
		_ = exec.Command("bash", "-lc", "kubectl delete namespace shipkit-system --ignore-not-found=true").Run()

		if _, err := exec.LookPath("kind"); err == nil {
			fmt.Println("[+] Deleting kind cluster 'shipkit' (if it exists) …")
			_ = exec.Command("bash", "-lc", "KIND_EXPERIMENTAL_PROVIDER=podman kind delete cluster --name shipkit").Run()
		}
		return nil
	},
}

func init() {
	rootCmd.AddCommand(stopCmd)
}

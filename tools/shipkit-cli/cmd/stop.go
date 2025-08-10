package cmd

import (
	"fmt"
	"os"
	"os/exec"

	"github.com/spf13/cobra"
)

var stopCmd = &cobra.Command{
	Use:   "stop",
	Short: "Tear down the k3d dev cluster",
	RunE: func(cmd *cobra.Command, args []string) error {
		if _, err := exec.LookPath("telepresence"); err == nil {
			_ = exec.Command("telepresence", "quit", "-s").Run()
		}
		if _, err := exec.LookPath("helm"); err == nil {
			_ = exec.Command("bash", "-lc", "helm uninstall traefik -n traefik >/dev/null 2>&1 || true").Run()
		}
		_ = exec.Command("bash", "-lc", "kubectl delete namespace traefik --ignore-not-found=true").Run()

		if _, err := exec.LookPath("k3d"); err != nil {
			fmt.Println("k3d not installed – nothing to stop.")
			return nil
		}
		fmt.Println("[+] Deleting k3d cluster 'shipkit' (if it exists) …")
		_ = exec.Command("k3d", "cluster", "delete", "shipkit").Run()

		_ = exec.Command("docker", "network", "rm", "k3d-shipkit").Run()
		_ = exec.Command("docker", "volume", "rm", "k3d-shipkit-images").Run()
		_ = os.Remove(fmt.Sprintf("%s/.config/k3d/kubeconfig-shipkit.yaml", os.Getenv("HOME")))
		return nil
	},
}

func init() {
	rootCmd.AddCommand(stopCmd)
}

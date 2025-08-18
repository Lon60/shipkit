package cmd

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	inst "shipkit-cli/internal/install"

	"github.com/spf13/cobra"
)

var (
	flagBuild []string
	flagLocal string
	flagPort  string
)

func execCmd(name string, args ...string) error {
	cmd := exec.Command(name, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin
	return cmd.Run()
}

func ensureBinary(name, installHint string) error {
	if _, err := exec.LookPath(name); err != nil {
		return fmt.Errorf("%s not found. %s", name, installHint)
	}
	return nil
}

func repoRoot() (string, error) {
	wd, err := os.Getwd()
	if err != nil {
		return "", err
	}
	dir := wd
	for i := 0; i < 6; i++ {
		if _, err := os.Stat(filepath.Join(dir, "k8s")); err == nil {
			return dir, nil
		}
		dir = filepath.Dir(dir)
	}
	return wd, nil
}

func kindClusterExists(name string) bool {
	out, err := exec.Command("bash", "-lc", "kind get clusters").Output()
	if err != nil {
		return false
	}
	return strings.Contains(string(out), name)
}

func createOrUseCluster(clusterName string) error {
	if err := ensureBinary("podman", "Install Podman: https://podman.io/docs/installation"); err != nil {
		return err
	}
	ensurePodmanUserSocket()
	if err := ensureBinary("kind", "Install kind: https://kind.sigs.k8s.io/docs/user/quick-start/#installation"); err != nil {
		return err
	}
	if err := ensureBinary("kubectl", "Install kubectl"); err != nil {
		return err
	}
	if kindClusterExists(clusterName) {
		fmt.Printf("[!] kind cluster '%s' already exists – skipping creation.\n", clusterName)
		return nil
	}
	cfg := "" +
		"kind: Cluster\n" +
		"apiVersion: kind.x-k8s.io/v1alpha4\n" +
		"networking:\n" +
		"  apiServerAddress: \"127.0.0.1\"\n" +
		"  apiServerPort: 6550\n" +
		"nodes:\n" +
		"- role: control-plane\n" +
		"  extraPortMappings:\n" +
		"  - containerPort: 30080\n" +
		"    hostPort: 80\n" +
		"    protocol: TCP\n" +
		"  - containerPort: 30443\n" +
		"    hostPort: 443\n" +
		"    protocol: TCP\n"
	tmp := filepath.Join(os.TempDir(), "shipkit-kind-config.yaml")
	if err := os.WriteFile(tmp, []byte(cfg), 0o600); err != nil {
		return err
	}
	fmt.Printf("[+] Creating kind cluster '%s' ...\n", clusterName)
	cmd := exec.Command("bash", "-lc", fmt.Sprintf("KIND_EXPERIMENTAL_PROVIDER=podman kind create cluster --name %s --config %s", clusterName, tmp))
	cmd.Stdout, cmd.Stderr, cmd.Stdin = os.Stdout, os.Stderr, os.Stdin
	return cmd.Run()
}

func buildImages(projectRoot string, rebuild []string) error {
	should := func(img string) bool {
		for _, r := range rebuild {
			if r == img {
				return true
			}
		}
		return false
	}
	fmt.Println("[+] Building container images with Podman …")
	if should("gateway-api") || !imageExists("gateway-api:dev") {
		fmt.Println("    - Building gateway-api:dev …")
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/gateway-api && podman build -f Dockerfile -t gateway-api:dev .", projectRoot)); err != nil {
			return err
		}
	}
	if should("frontend") || !imageExists("frontend:dev") {
		fmt.Println("    - Building frontend:dev …")
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/frontend && podman build -f Dockerfile -t frontend:dev .", projectRoot)); err != nil {
			return err
		}
	}
	if should("k3s-control") || !imageExists("k3s-control:dev") {
		fmt.Println("    - Building k3s-control:dev …")
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/k3s-control && podman build -f Dockerfile -t k3s-control:dev .", projectRoot)); err != nil {
			return err
		}
	}
	return nil
}

func imageExists(name string) bool {
	cmd := exec.Command("bash", "-lc", fmt.Sprintf("podman image exists %s", name))
	return cmd.Run() == nil
}

func importImages(cluster string) error {
	fmt.Println("[+] Importing images into kind …")
	for _, img := range []string{"gateway-api:dev", "frontend:dev", "k3s-control:dev"} {
		if imageExists(img) {
			tar := filepath.Join(os.TempDir(), strings.ReplaceAll(img, ":", "_")+".tar")
			_ = os.Remove(tar)
			fmt.Printf("    - Saving %s and loading into kind …\n", img)
			if err := execCmd("bash", "-lc", fmt.Sprintf("podman image save -o %s %s", tar, img)); err != nil {
				_ = os.Remove(tar)
				if err2 := execCmd("bash", "-lc", fmt.Sprintf("podman image save -o %s %s", tar, img)); err2 != nil {
					return err
				}
			}
			if err := execCmd("bash", "-lc", fmt.Sprintf("KIND_EXPERIMENTAL_PROVIDER=podman kind load image-archive --name %s %s", cluster, tar)); err != nil {
				return err
			}
			_ = os.Remove(tar)
		}
	}
	return nil
}

func ensurePodmanUserSocket() {
	_ = exec.Command("bash", "-lc", "systemctl --user enable --now podman.socket >/dev/null 2>&1 || true").Run()
}

func connectTelepresence() error {
	if _, err := exec.LookPath("telepresence"); err != nil {
		fmt.Println("[+] Installing Telepresence CLI …")
		tmp := filepath.Join(os.TempDir(), "telepresence")
		if err := execCmd("bash", "-lc", fmt.Sprintf("curl -fL https://app.getambassador.io/download/tel2/linux/amd64/latest/telepresence -o %s && chmod +x %s && sudo mv %s /usr/local/bin/telepresence", tmp, tmp, tmp)); err != nil {
			return err
		}
	}
	_ = execCmd("telepresence", "status")
	return execCmd("telepresence", "connect")
}

func runLocalService(projectRoot, svc, localPort string) error {
	const ns = "shipkit-system"
	switch svc {
	case "gateway-api":
		lp := localPort
		if lp == "" {
			lp = "8080"
		}
		_ = execCmd("telepresence", "leave", svc, "--namespace", ns)
		if err := execCmd("telepresence", "intercept", svc, "--namespace", ns, "--port", fmt.Sprintf("%s:8080", lp)); err != nil {
			return err
		}
		os.Setenv("DATABASE_URL", "jdbc:postgresql://postgres.shipkit-system.svc.cluster.local:5432/shipkit")
		os.Setenv("DATABASE_USERNAME", "postgres")
		os.Setenv("DATABASE_PASSWORD", "postgres")
		os.Setenv("JWT_SECRET", "dev-secret-change-me")
		os.Setenv("JWT_EXPIRATION_MS", "86400000")
		os.Setenv("CORS_ALLOWED_ORIGINS", "http://localhost,http://127.0.0.1")
		os.Setenv("K3S_CONTROL_HOST", "k3s-control.shipkit-system.svc.cluster.local")
		os.Setenv("K3S_CONTROL_PORT", "9998")
		cmd := exec.Command("bash", "-lc", fmt.Sprintf("cd %s && cd apps/gateway-api && ./gradlew bootRun", projectRoot))
		cmd.Stdout, cmd.Stderr, cmd.Stdin = os.Stdout, os.Stderr, os.Stdin
		return cmd.Run()
	case "frontend":
		lp := localPort
		if lp == "" {
			lp = "3000"
		}
		_ = execCmd("telepresence", "leave", "shipkit-frontend", "--namespace", ns)
		if err := execCmd("telepresence", "intercept", "shipkit-frontend", "--namespace", ns, "--port", fmt.Sprintf("%s:3000", lp)); err != nil {
			return err
		}
		os.Setenv("API_BASE_URL", "/api")
		cmd := exec.Command("bash", "-lc", fmt.Sprintf("cd %s && cd apps/frontend && if command -v bun >/dev/null 2>&1; then bun install && bun run dev; else npm install && npm run dev; fi", projectRoot))
		cmd.Stdout, cmd.Stderr, cmd.Stdin = os.Stdout, os.Stderr, os.Stdin
		return cmd.Run()
	case "k3s-control":
		lp := localPort
		if lp == "" {
			lp = "9998"
		}
		_ = execCmd("telepresence", "leave", svc, "--namespace", ns)
		if err := execCmd("telepresence", "intercept", svc, "--namespace", ns, "--port", fmt.Sprintf("%s:9998", lp)); err != nil {
			return err
		}
		cmd := exec.Command("bash", "-lc", fmt.Sprintf("cd %s && cd apps/k3s-control && go run ./cmd/server", projectRoot))
		cmd.Stdout, cmd.Stderr, cmd.Stdin = os.Stdout, os.Stderr, os.Stdin
		return cmd.Run()
	default:
		return fmt.Errorf("unknown service for --local: %s", svc)
	}
}

var devCmd = &cobra.Command{
	Use:   "start",
	Short: "Start the Shipkit kind dev environment",
	RunE: func(cmd *cobra.Command, args []string) error {
		if flagPort != "" && flagLocal == "" {
			return fmt.Errorf("--local-port requires --local <service>")
		}
		if flagLocal != "" {
			switch flagLocal {
			case "gateway-api", "frontend", "k3s-control":
			default:
				return fmt.Errorf("invalid --local value: %s (valid: gateway-api, frontend, k3s-control)", flagLocal)
			}
		}
		root, err := repoRoot()
		if err != nil {
			return err
		}
		if err := createOrUseCluster("shipkit"); err != nil {
			return err
		}
		if err := buildImages(root, flagBuild); err != nil {
			return err
		}
		if err := importImages("shipkit"); err != nil {
			return err
		}
		if err := inst.EnsureKubectl(); err != nil {
			return err
		}
		if err := inst.EnsureHelmInstalled(); err != nil {
			return err
		}
		useMkcert := (exec.Command("bash", "-lc", "command -v mkcert >/dev/null 2>&1").Run() == nil)
		if err := inst.EnsureNamespacesAndSecrets(useMkcert); err != nil {
			return err
		}
		if err := inst.EnsureShipkitEnvFromFileOrDefaults(root, ""); err != nil {
			return err
		}
		baseValues := filepath.Join(root, "k8s/base/traefik/helm-values.yaml")
		values := []string{filepath.Join(root, "k8s/overlays/development/traefik-kind-port-patch.yaml")}
		if useMkcert {
			values = append(values, filepath.Join(root, "k8s/overlays/development/traefik-staging-ca-patch.yaml"))
		}
		fmt.Println("[+] Installing Traefik via Helm …")
		if err := inst.InstallTraefik(baseValues, values, "v37.0.0"); err != nil {
			return err
		}
		fmt.Println("[+] Applying Shipkit development overlay …")
		if err := inst.ApplyOverlay(filepath.Join(root, "k8s/overlays/development")); err != nil {
			return err
		}
		_ = execCmd("bash", "-lc", "kubectl -n shipkit-system rollout restart deployment gateway-api k3s-control shipkit-frontend || true")
		fmt.Println("[+] Waiting for core components to become ready …")
		_ = inst.WaitForDeployment("shipkit-system", "postgres", 180)
		_ = inst.WaitForDeployment("shipkit-system", "gateway-api", 180)
		_ = inst.WaitForDeployment("shipkit-system", "k3s-control", 180)
		_ = inst.WaitForDeployment("shipkit-system", "shipkit-frontend", 180)
		fmt.Println("[✓] Shipkit dev stack is now running at: http://localhost")

		if flagLocal != "" {
			if err := connectTelepresence(); err != nil {
				return err
			}
			return runLocalService(root, flagLocal, flagPort)
		}
		return nil
	},
}

func init() {
	rootCmd.AddCommand(devCmd)
	devCmd.Flags().StringSliceVarP(&flagBuild, "build", "b", nil, "Rebuild image(s): gateway-api, frontend, k3s-control")
	devCmd.Flags().StringVar(&flagLocal, "local", "", "Run service locally: gateway-api|frontend|k3s-control")
	devCmd.Flags().StringVar(&flagPort, "local-port", "", "Local port override for --local")

	_ = devCmd.RegisterFlagCompletionFunc("build", func(cmd *cobra.Command, args []string, toComplete string) ([]string, cobra.ShellCompDirective) {
		return []string{"gateway-api", "frontend", "k3s-control"}, cobra.ShellCompDirectiveNoFileComp
	})
	_ = devCmd.RegisterFlagCompletionFunc("local", func(cmd *cobra.Command, args []string, toComplete string) ([]string, cobra.ShellCompDirective) {
		return []string{"gateway-api", "frontend", "k3s-control"}, cobra.ShellCompDirectiveNoFileComp
	})
	_ = devCmd.RegisterFlagCompletionFunc("local-port", func(cmd *cobra.Command, args []string, toComplete string) ([]string, cobra.ShellCompDirective) {
		return []string{}, cobra.ShellCompDirectiveNoFileComp
	})
}

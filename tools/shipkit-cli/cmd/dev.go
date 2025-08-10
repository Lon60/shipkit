package cmd

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

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

func k3dClusterExists(name string) bool {
	cmd := exec.Command("k3d", "cluster", "list")
	out, err := cmd.Output()
	if err != nil {
		return false
	}
	return strings.Contains(string(out), name)
}

func createOrUseCluster(clusterName string) error {
	if err := ensureBinary("docker", "Install Docker and ensure daemon is running"); err != nil {
		return err
	}
	if err := exec.Command("docker", "info").Run(); err != nil {
		return fmt.Errorf("docker daemon is not running. please start docker and retry")
	}
	if err := ensureBinary("k3d", "Install k3d: https://k3d.io/#installation"); err != nil {
		return err
	}
	if err := ensureBinary("kubectl", "Install kubectl"); err != nil {
		return err
	}

	if k3dClusterExists(clusterName) {
		fmt.Printf("[!] k3d cluster '%s' already exists – skipping creation.\n", clusterName)
	} else {
		fmt.Printf("[+] Creating k3d cluster '%s' ...\n", clusterName)
		if err := execCmd("k3d", "cluster", "create", clusterName,
			"--k3s-arg", "--disable=traefik@server:0",
			"--servers", "1", "--agents", "0",
			"--api-port", "6550",
			"--port", "80:80@loadbalancer",
			"--port", "443:443@loadbalancer",
		); err != nil {
			return err
		}
	}

	return nil
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
	if should("gateway-api") || !imageExists("gateway-api:dev") {
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/gateway-api && ./gradlew bootBuildImage --imageName=gateway-api:dev", projectRoot)); err != nil {
			return err
		}
	}
	if should("frontend") || !imageExists("frontend:dev") {
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/frontend && docker build -f Dockerfile -t frontend:dev .", projectRoot)); err != nil {
			return err
		}
	}
	if should("k3s-control") || !imageExists("k3s-control:dev") {
		if err := execCmd("bash", "-lc", fmt.Sprintf("cd %s && cd apps/k3s-control && docker build -f Dockerfile -t k3s-control:dev .", projectRoot)); err != nil {
			return err
		}
	}
	return nil
}

func imageExists(name string) bool {
	cmd := exec.Command("bash", "-lc", fmt.Sprintf("docker image inspect %s >/dev/null 2>&1", name))
	return cmd.Run() == nil
}

func importImages(cluster string) error {
	for _, img := range []string{"gateway-api:dev", "frontend:dev", "k3s-control:dev"} {
		if imageExists(img) {
			if err := execCmd("k3d", "image", "import", "-c", cluster, img); err != nil {
				return err
			}
		}
	}
	return nil
}

func ensureHelm() error {
	if _, err := exec.LookPath("helm"); err == nil {
		return nil
	}
	fmt.Println("[+] Installing Helm (v3) …")
	return execCmd("bash", "-lc", "curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash")
}

func applyKustomize(projectRoot string) error {
	if err := ensureHelm(); err != nil {
		return err
	}
	if err := execCmd("bash", "-lc", "helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true"); err != nil {
		return err
	}
	if err := execCmd("helm", "repo", "update", "traefik"); err != nil {
		return err
	}
	if err := execCmd("bash", "-lc", "kubectl get namespace traefik >/dev/null 2>&1 || kubectl create namespace traefik"); err != nil {
		return err
	}
	if err := execCmd("bash", "-lc", "kubectl -n traefik get secret traefik-acme >/dev/null 2>&1 || kubectl -n traefik create secret generic traefik-acme --from-literal=dummy=1"); err != nil {
		return err
	}
	if err := execCmd("bash", "-lc", "kubectl get namespace shipkit-system >/dev/null 2>&1 || kubectl create namespace shipkit-system"); err != nil {
		return err
	}

	if err := ensureShipkitEnv(projectRoot); err != nil {
		return err
	}

	_ = ensureLocalTLSTrust()

	helmArgs := []string{
		"upgrade", "--install", "traefik", "traefik/traefik",
		"--namespace", "traefik",
		"--create-namespace",
		"--version", "v37.0.0",
		"--reset-values",
		"-f", filepath.Join(projectRoot, "k8s/base/traefik/helm-values.yaml"),
	}
	if _, err := exec.LookPath("mkcert"); err == nil {
		helmArgs = append(helmArgs, "-f", filepath.Join(projectRoot, "k8s/overlays/development/traefik-staging-ca-patch.yaml"))
	}
	helmArgs = append(helmArgs, "--set", "installCRDs=true", "--wait")
	if err := execCmd("helm", helmArgs...); err != nil {
		return err
	}
	_ = execCmd("bash", "-lc", "kubectl get crd | grep -q 'middlewares.traefik.io' || (curl -s -o /tmp/traefik-crds.yaml https://raw.githubusercontent.com/traefik/traefik/v2.10/docs/content/reference/dynamic-configuration/kubernetes-crd-definition-v1.yml && kubectl apply -f /tmp/traefik-crds.yaml)")
	if err := execCmd("bash", "-lc", "until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1; do sleep 2; done"); err != nil {
		return err
	}

	return execCmd("kubectl", "apply", "-k", filepath.Join(projectRoot, "k8s/overlays/development"))
}

func waitForDeployment(namespace, name string, timeoutSeconds int) error {
	check := exec.Command("bash", "-lc", fmt.Sprintf("kubectl -n %s get deploy %s >/dev/null 2>&1", namespace, name))
	for i := range timeoutSeconds {
		if check.Run() == nil {
			break
		}
		exec.Command("bash", "-lc", "sleep 1").Run()
		if i == timeoutSeconds-1 {
			return fmt.Errorf("deployment %s/%s not found", namespace, name)
		}
	}
	return execCmd("kubectl", "-n", namespace, "rollout", "status", fmt.Sprintf("deployment/%s", name), fmt.Sprintf("--timeout=%ds", timeoutSeconds))
}

func generateJWTSecret() (string, error) {
	buf := make([]byte, 32)
	if _, err := rand.Read(buf); err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(buf), nil
}

func ensureShipkitEnv(projectRoot string) error {
	envFile := filepath.Join(projectRoot, ".env")
	envExample := filepath.Join(projectRoot, ".env.example")

	if _, err := os.Stat(envFile); os.IsNotExist(err) {
		if _, err2 := os.Stat(envExample); err2 == nil {
			data, err := os.ReadFile(envExample)
			if err != nil {
				return err
			}
			content := string(data)
			sec, _ := generateJWTSecret()
			lines := strings.Split(content, "\n")
			replaced := false
			for i, line := range lines {
				if strings.HasPrefix(line, "JWT_SECRET=") {
					lines[i] = "JWT_SECRET=" + sec
					replaced = true
					break
				}
			}
			if !replaced {
				lines = append(lines, "JWT_SECRET="+sec)
			}
			final := strings.Join(lines, "\n")
			if err := os.WriteFile(envFile, []byte(final), 0o600); err != nil {
				return err
			}
		}
	}

	var fromFile string
	if _, err := os.Stat(envFile); err == nil {
		data, err := os.ReadFile(envFile)
		if err != nil {
			return err
		}
		lines := strings.Split(string(data), "\n")
		hasJWT := false
		for _, line := range lines {
			if strings.HasPrefix(line, "JWT_SECRET=") && len(strings.TrimSpace(strings.TrimPrefix(line, "JWT_SECRET="))) > 0 {
				hasJWT = true
				break
			}
		}
		if !hasJWT {
			sec, _ := generateJWTSecret()
			lines = append(lines, "JWT_SECRET="+sec)
			if err := os.WriteFile(envFile, []byte(strings.Join(lines, "\n")), 0o600); err != nil {
				return err
			}
		}
		fromFile = envFile
	} else {
		tmp := filepath.Join(os.TempDir(), "shipkit-env-fallback")
		sec, _ := generateJWTSecret()
		content := strings.Join([]string{
			"JWT_SECRET=" + sec,
			"JWT_EXPIRATION_MS=86400000",
			"DATABASE_URL=jdbc:postgresql://postgres.shipkit-system.svc.cluster.local:5432/shipkit",
			"DATABASE_USERNAME=postgres",
			"DATABASE_PASSWORD=postgres",
			"CORS_ALLOWED_ORIGINS=http://localhost,http://127.0.0.1",
			"API_BASE_URL=/api",
		}, "\n") + "\n"
		if err := os.WriteFile(tmp, []byte(content), 0o600); err != nil {
			return err
		}
		fromFile = tmp
		defer os.Remove(tmp)
	}

	_ = execCmd("bash", "-lc", "kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -")
	_ = execCmd("bash", "-lc", "kubectl -n shipkit-system delete configmap shipkit-env --ignore-not-found=true")
	return execCmd("bash", "-lc", fmt.Sprintf("kubectl -n shipkit-system create configmap shipkit-env --from-env-file=%s", fromFile))
}

func ensureLocalTLSTrust() error {
	if _, err := exec.LookPath("mkcert"); err != nil {
		return nil
	}
	_ = execCmd("bash", "-lc", "mkcert -install >/dev/null 2>&1 || true")
	tmpDir := filepath.Join(os.TempDir(), "skdev-cert")
	_ = execCmd("bash", "-lc", fmt.Sprintf("rm -rf %s && mkdir -p %s", tmpDir, tmpDir))
	if err := execCmd("bash", "-lc", fmt.Sprintf("mkcert -cert-file %s/shipkit.crt -key-file %s/shipkit.key shipkit.local *.shipkit.local", tmpDir, tmpDir)); err != nil {
		return err
	}
	defer execCmd("bash", "-lc", fmt.Sprintf("rm -rf %s", tmpDir))
	return execCmd("bash", "-lc", fmt.Sprintf("kubectl -n traefik create secret tls shipkit-dev-tls --cert=%s/shipkit.crt --key=%s/shipkit.key --dry-run=client -o yaml | kubectl apply -f -", tmpDir, tmpDir))
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
		// defaults for Spring app
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
	Short: "Start the Shipkit k3d dev environment",
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
		if err := applyKustomize(root); err != nil {
			return err
		}
		// Wait for Postgres to be ready before gateway starts hitting it
		_ = waitForDeployment("shipkit-system", "postgres", 120)
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

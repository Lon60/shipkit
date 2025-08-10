package main

import (
	"fmt"
	"os"
	"os/exec"
)

func run(cmd string) error {
	c := exec.Command("bash", "-lc", cmd)
	c.Stdout = os.Stdout
	c.Stderr = os.Stderr
	return c.Run()
}

func main() {
	if os.Geteuid() != 0 {
		fmt.Println("This installer must be run as root (or via sudo).")
		os.Exit(1)
	}

	_ = run(`if ! command -v docker >/dev/null 2>&1; then
  apt-get update -qq
  apt-get install -yq ca-certificates curl gnupg lsb-release >/dev/null
  mkdir -p /etc/apt/keyrings
  curl -fsSL "https://download.docker.com/linux/$(. /etc/os-release && echo "$ID")/gpg" | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/$(. /etc/os-release && echo "$ID") $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list >/dev/null
  apt-get update -qq && apt-get install -yq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin >/dev/null
  systemctl enable --now docker || true
fi`)

	if os.Getenv("SKIP_K3S") != "1" {
		if err := run(`command -v kubectl >/dev/null 2>&1 || (curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik" sh -)`); err != nil {
			fmt.Println(err)
			os.Exit(1)
		}
	}
	if _, err := os.Stat("/etc/rancher/k3s/k3s.yaml"); err == nil {
		_ = os.Setenv("KUBECONFIG", "/etc/rancher/k3s/k3s.yaml")
	}

	_ = run(`command -v kubectl >/dev/null 2>&1 || (curl -sSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/$(curl -sSL https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && chmod +x /usr/local/bin/kubectl)`)
	if err := run(`command -v helm >/dev/null 2>&1 || curl -sSL https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash`); err != nil {
		fmt.Println("helm install failed:", err)
		os.Exit(1)
	}

	_ = run(`kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -`)
	_ = run(`mkdir -p "$HOME/shipkit"`)
	_ = run(`if [[ ! -f "$HOME/shipkit/.env" ]]; then curl -sSL "https://raw.githubusercontent.com/Lon60/shipkit/refs/heads/main/.env.example" -o "$HOME/shipkit/.env"; JWT_SECRET=$(openssl rand -base64 32); sed -i -E "s|^JWT_SECRET=.*|JWT_SECRET=$JWT_SECRET|" "$HOME/shipkit/.env"; fi`)
	if err := run(`kubectl -n shipkit-system create configmap shipkit-env --from-env-file="$HOME/shipkit/.env" --dry-run=client -o yaml | kubectl apply -f -`); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	_ = run(`helm repo add traefik https://traefik.github.io/charts >/dev/null 2>&1 || true`)
	_ = run(`helm repo update traefik >/dev/null 2>&1`)
	_ = run(`kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -`)
	_ = run(`kubectl -n traefik create secret generic traefik-acme --dry-run=client -o yaml | kubectl apply -f -`)

	if err := run(`helm upgrade --install traefik traefik/traefik \
  --namespace traefik \
  --create-namespace \
  --version "v37.0.0" \
  --reset-values \
  -f "https://raw.githubusercontent.com/Lon60/shipkit/main/k8s/base/traefik/helm-values.yaml" \
  --set installCRDs=true \
  --wait`); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	if err := run(`until kubectl get crd ingressroutes.traefik.io >/dev/null 2>&1 && kubectl get crd middlewares.traefik.io >/dev/null 2>&1; do sleep 2; done`); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	if err := run(`kubectl apply -k "github.com/lon60/shipkit//k8s/base/traefik?ref=main"`); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
	if err := run(`kubectl apply -k "github.com/lon60/shipkit//k8s/overlays/production?ref=main"`); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	_ = run(`kubectl -n shipkit-system rollout restart deployment gateway-api shipkit-frontend k3s-control postgres || true`)

	_ = run(`PUBLIC_IP=$(curl -s https://api.ipify.org || echo "<server-ip>"); echo "Shipkit installation complete! Access the UI at: http://$PUBLIC_IP or http://localhost"; echo "Run: kubectl -n shipkit-system get pods,svc to watch status."`)
}

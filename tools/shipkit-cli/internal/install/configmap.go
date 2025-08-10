package install

import (
	"crypto/rand"
	"encoding/base64"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

func ensureJWTSecret(lines []string) []string {
	for _, l := range lines {
		if strings.HasPrefix(l, "JWT_SECRET=") && len(strings.TrimSpace(strings.TrimPrefix(l, "JWT_SECRET="))) > 0 {
			return lines
		}
	}
	buf := make([]byte, 32)
	_, _ = rand.Read(buf)
	sec := base64.StdEncoding.EncodeToString(buf)
	return append(lines, "JWT_SECRET="+sec)
}

func EnsureShipkitEnvFromFileOrDefaults(projectRoot, envFilePath string) error {
	envFile := envFilePath
	if envFile == "" {
		envFile = filepath.Join(projectRoot, ".env")
	}

	var fromFile string
	if _, err := os.Stat(envFile); err == nil {
		data, err := os.ReadFile(envFile)
		if err != nil {
			return err
		}
		lines := ensureJWTSecret(strings.Split(string(data), "\n"))
		if err := os.WriteFile(envFile, []byte(strings.Join(lines, "\n")), 0o600); err != nil {
			return err
		}
		fromFile = envFile
	} else {
		tmp := filepath.Join(os.TempDir(), "shipkit-env-fallback")
		lines := []string{
			"JWT_EXPIRATION_MS=86400000",
			"DATABASE_URL=jdbc:postgresql://postgres.shipkit-system.svc.cluster.local:5432/shipkit",
			"DATABASE_USERNAME=postgres",
			"DATABASE_PASSWORD=postgres",
			"CORS_ALLOWED_ORIGINS=http://localhost,http://127.0.0.1",
			"API_BASE_URL=/api",
		}
		lines = ensureJWTSecret(lines)
		if err := os.WriteFile(tmp, []byte(strings.Join(lines, "\n")+"\n"), 0o600); err != nil {
			return err
		}
		fromFile = tmp
		defer os.Remove(tmp)
	}
	_ = exec.Command("bash", "-lc", "kubectl create namespace shipkit-system --dry-run=client -o yaml | kubectl apply -f -").Run()
	_ = exec.Command("bash", "-lc", "kubectl -n shipkit-system delete configmap shipkit-env --ignore-not-found=true").Run()
	return exec.Command("bash", "-lc", "kubectl -n shipkit-system create configmap shipkit-env --from-env-file='"+fromFile+"'").Run()
}

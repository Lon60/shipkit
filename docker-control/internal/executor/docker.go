package executor

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strings"

	pb "github.com/shipkit/docker-control/proto"
)

func buildDockerEnv() []string {
	env := os.Environ()
	if os.Getenv("DOCKER_HOST") == "" {
		env = append(env, "DOCKER_HOST=unix:///var/run/docker.sock")
	}
	return env
}

type DockerComposeExecutor struct{}

func NewDockerComposeExecutor() *DockerComposeExecutor {
	return &DockerComposeExecutor{}
}

func (e *DockerComposeExecutor) ComposeUp(ctx context.Context, project string, yaml string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "-f", "-", "up", "-d")
	cmd.Env = buildDockerEnv()
	cmd.Stdin = bytes.NewBufferString(yaml)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose up failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) ComposeDown(ctx context.Context, project string) error {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "down", "--remove-orphans")
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("docker compose down failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) ComposeStatus(ctx context.Context, project string) (*ComposeStatus, error) {
	cmd := exec.CommandContext(ctx, "docker", "compose", "-p", project, "ps", "--format", "json")
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("docker compose ps failed: %w - output: %s", err, string(output))
	}
	trimmed := strings.TrimSpace(string(output))

	var services []ServiceStatus

	var arr []map[string]any
	if json.Unmarshal([]byte(trimmed), &arr) == nil {
		for _, elem := range arr {
			services = append(services, mapToService(elem))
		}
	} else {
		for _, line := range strings.Split(trimmed, "\n") {
			line = strings.TrimSpace(line)
			if line == "" {
				continue
			}
			var m map[string]any
			if json.Unmarshal([]byte(line), &m) != nil {
				continue
			}
			services = append(services, mapToService(m))
		}
	}

	return &ComposeStatus{Services: services}, nil
}

func (e *DockerComposeExecutor) ReloadNginx(ctx context.Context, containerName string) error {
	cmd := exec.CommandContext(ctx, "docker", "exec", containerName, "nginx", "-s", "reload")
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("nginx reload failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) IssueCertificate(ctx context.Context, domain string) error {
	cmdArgs := []string{
		"run", "--rm",
		"-v", "certbot_certs:/etc/letsencrypt",
		"-v", "certbot_www:/var/www/certbot",
		"certbot/certbot",
		"certonly", "--webroot", "-w", "/var/www/certbot",
		"--email", "admin@" + domain, //TODO: make email configurable
		"-d", domain,
		"--rsa-key-size", "4096",
		"--agree-tos",
		"--non-interactive",
	}
	cmd := exec.CommandContext(ctx, "docker", cmdArgs...)
	cmd.Env = buildDockerEnv()
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("certbot failed: %w - output: %s", err, string(output))
	}
	return nil
}

func (e *DockerComposeExecutor) GetStatus(ctx context.Context, projectName string) (*pb.AppStatus, error) {
	status, err := e.ComposeStatus(ctx, projectName)
	if err != nil {
		return nil, err
	}

	var containerStatuses []*pb.ContainerStatus
	for _, s := range status.Services {
		containerStatuses = append(containerStatuses, &pb.ContainerStatus{
			Name:   s.Name,
			State:  s.State,
			Health: s.Health,
			Ports:  s.Ports,
		})
	}

	return &pb.AppStatus{
		Uuid:       projectName,
		Status:     0, // Assuming 0 is OK
		Message:    "Status retrieved successfully",
		Containers: containerStatuses,
	}, nil
}

func mapToService(elem map[string]any) ServiceStatus {
	var name, state, health string
	var ports []string

	if n, ok := elem["Name"].(string); ok {
		name = n
	}
	if s, ok := elem["State"].(string); ok {
		state = s
	}
	if h, ok := elem["Health"].(string); ok {
		health = h
	}

	if pubs, ok := elem["Publishers"].([]any); ok {
		for _, p := range pubs {
			if mp, ok := p.(map[string]any); ok {
				if pubPort, ok := mp["PublishedPort"].(float64); ok {
					if tgtPort, ok := mp["TargetPort"].(float64); ok {
						if proto, ok := mp["Protocol"].(string); ok {
							ports = append(ports, fmt.Sprintf("%.0f:%.0f/%s", pubPort, tgtPort, proto))
						}
					}
				}
			}
		}
	}
	return ServiceStatus{Name: name, State: state, Health: health, Ports: ports}
}

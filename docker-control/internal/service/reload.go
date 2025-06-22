package service

import (
	"context"
	"os/exec"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

// ReloadNginx triggers nginx -s reload in the specified container using docker exec
func (s *DockerControlService) ReloadNginx(ctx context.Context, req *pb.ReloadNginxRequest) (*pb.ActionResult, error) {
	container := req.GetContainerName()
	if container == "" {
		return &pb.ActionResult{
			Status:  1,
			Message: "container_name is required",
			Details: "",
		}, nil
	}

	s.logger.Info("Reloading NGINX", zap.String("container", container))

	cmd := exec.CommandContext(ctx, "docker", "exec", container, "nginx", "-s", "reload")
	output, err := cmd.CombinedOutput()
	if err != nil {
		s.logger.Error("Failed to reload nginx", zap.String("container", container), zap.Error(err))
		return &pb.ActionResult{
			Status:  1,
			Message: "Failed to reload nginx",
			Details: string(output),
		}, nil
	}

	return &pb.ActionResult{
		Status:  0,
		Message: "NGINX reloaded successfully",
		Details: string(output),
	}, nil
}

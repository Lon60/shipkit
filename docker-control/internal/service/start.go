package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) StartCompose(ctx context.Context, req *pb.StartComposeRequest) (*pb.ActionResult, error) {
	deploymentDir, err := s.deploymentPath(req.Uuid)
	if err != nil {
		return &pb.ActionResult{
			Success:   false,
			Message:   err.Error(),
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	if req.ComposeYaml == "" {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Compose YAML is required",
			ErrorCode: "INVALID_COMPOSE_YAML",
		}, nil
	}

	s.logger.Info("Starting compose", zap.String("uuid", req.Uuid))

	if err := os.MkdirAll(deploymentDir, 0755); err != nil {
		s.logger.Error("Failed to create deployment directory",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to create deployment directory",
			ErrorCode: "DIRECTORY_CREATE_FAILED",
		}, nil
	}

	composeFile := filepath.Join(deploymentDir, "docker-compose.yml")
	if err := os.WriteFile(composeFile, []byte(req.ComposeYaml), 0644); err != nil {
		s.logger.Error("Failed to write compose file",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to write compose file",
			ErrorCode: "FILE_WRITE_FAILED",
		}, nil
	}

	if err := s.executor.ComposeUp(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to start compose",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to start compose: %v", err),
			ErrorCode: "COMPOSE_UP_FAILED",
		}, nil
	}

	s.logger.Info("Successfully started compose", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "Compose started successfully",
	}, nil
}

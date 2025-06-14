package service

import (
	"context"
	"fmt"
	"os"
	"path/filepath"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) UpdateCompose(ctx context.Context, req *pb.UpdateComposeRequest) (*pb.ActionResult, error) {
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

	s.logger.Info("Updating compose", zap.String("uuid", req.Uuid))

	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Deployment not found",
			ErrorCode: "DEPLOYMENT_NOT_FOUND",
		}, nil
	}

	composeFile := filepath.Join(deploymentDir, "docker-compose.yml")
	if err := os.WriteFile(composeFile, []byte(req.ComposeYaml), 0644); err != nil {
		s.logger.Error("Failed to update compose file",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   "Failed to update compose file",
			ErrorCode: "FILE_WRITE_FAILED",
		}, nil
	}

	if err := s.executor.ComposeDown(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to stop app during update",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to stop app during update: %v", err),
			ErrorCode: "COMPOSE_DOWN_FAILED",
		}, nil
	}

	if err := s.executor.ComposeUp(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to start app after update",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to start app after update: %v", err),
			ErrorCode: "COMPOSE_UP_FAILED",
		}, nil
	}

	s.logger.Info("Successfully updated compose", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "Compose updated successfully",
	}, nil
}

package service

import (
	"context"
	"fmt"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) StartCompose(ctx context.Context, req *pb.StartComposeRequest) (*pb.ActionResult, error) {
	_, err := s.deploymentPath(req.Uuid)
	if err != nil {
		return &pb.ActionResult{
			Status:  1,
			Message: err.Error(),
			Details: "UUID validation failed",
		}, nil
	}

	if req.ComposeYaml == "" {
		return &pb.ActionResult{
			Status:  1,
			Message: "Compose YAML is required",
			Details: "Empty compose_yaml field",
		}, nil
	}

	s.logger.Info("Starting compose", zap.String("uuid", req.Uuid))

	if err := s.executor.ComposeUp(ctx, req.Uuid, req.ComposeYaml); err != nil {
		s.logger.Error("Failed to start compose",
			zap.String("uuid", req.Uuid),
			zap.Error(err))

		// Cleanup failed deployment resources
		s.logger.Info("Cleaning up failed deployment", zap.String("uuid", req.Uuid))
		if cleanupErr := s.executor.ComposeDown(ctx, req.Uuid); cleanupErr != nil {
			s.logger.Warn("Failed to cleanup failed deployment",
				zap.String("uuid", req.Uuid),
				zap.Error(cleanupErr))
		} else {
			s.logger.Info("Successfully cleaned up failed deployment", zap.String("uuid", req.Uuid))
		}

		return &pb.ActionResult{
			Status:  1,
			Message: "Failed to start compose",
			Details: err.Error(),
		}, nil
	}

	s.logger.Info("Successfully started compose", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Status:  0,
		Message: "Compose started successfully",
		Details: fmt.Sprintf("Project %s is now running", req.Uuid),
	}, nil
}

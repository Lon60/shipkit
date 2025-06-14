package service

import (
	"context"
	"fmt"
	"os"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) StopApp(ctx context.Context, req *pb.StopAppRequest) (*pb.ActionResult, error) {
	deploymentDir, err := s.deploymentPath(req.Uuid)
	if err != nil {
		return &pb.ActionResult{
			Success:   false,
			Message:   err.Error(),
			ErrorCode: "INVALID_UUID",
		}, nil
	}

	s.logger.Info("Stopping app", zap.String("uuid", req.Uuid))

	if _, err := os.Stat(deploymentDir); os.IsNotExist(err) {
		return &pb.ActionResult{
			Success:   false,
			Message:   "Deployment not found",
			ErrorCode: "DEPLOYMENT_NOT_FOUND",
		}, nil
	}

	if err := s.executor.ComposeDown(ctx, deploymentDir); err != nil {
		s.logger.Error("Failed to stop app",
			zap.String("uuid", req.Uuid),
			zap.Error(err))
		return &pb.ActionResult{
			Success:   false,
			Message:   fmt.Sprintf("Failed to stop app: %v", err),
			ErrorCode: "COMPOSE_DOWN_FAILED",
		}, nil
	}

	s.logger.Info("Successfully stopped app", zap.String("uuid", req.Uuid))
	return &pb.ActionResult{
		Success: true,
		Message: "App stopped successfully",
	}, nil
}

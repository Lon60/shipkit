package service

import (
	"context"
	"fmt"

	pb "github.com/shipkit/docker-control/proto"
	"go.uber.org/zap"
)

func (s *DockerControlService) IssueCertificate(ctx context.Context, req *pb.IssueCertificateRequest) (*pb.ActionResult, error) {
	s.logger.Info("Issuing certificate", zap.String("domain", req.Domain))

	if err := s.executor.IssueCertificate(ctx, req.Domain); err != nil {
		s.logger.Error("Failed to issue certificate",
			zap.String("domain", req.Domain),
			zap.Error(err))

		return &pb.ActionResult{
			Status:  1,
			Message: "Failed to issue certificate",
			Details: err.Error(),
		}, nil
	}

	s.logger.Info("Successfully issued certificate", zap.String("domain", req.Domain))
	return &pb.ActionResult{
		Status:  0,
		Message: "Certificate issued successfully",
		Details: fmt.Sprintf("Certificate for %s is now available", req.Domain),
	}, nil
}

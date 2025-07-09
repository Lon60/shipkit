package service

import (
	"context"

	pb "github.com/shipkit/k3s-control/proto"
)

// Ensure implementation satisfies interface
var _ pb.K3sControlServiceServer = (*Service)(nil)

// Service is a stub implementation that will be extended to apply manifests via the Kubernetes API.
type Service struct {
	pb.UnimplementedK3sControlServiceServer
}

func New() *Service {
	return &Service{}
}

func (s *Service) ApplyDeployment(ctx context.Context, req *pb.ApplyRequest) (*pb.ActionResult, error) {
	// TODO: call k8s client to apply manifest
	return &pb.ActionResult{Status: 0, Message: "Stub apply success"}, nil
}

func (s *Service) DeleteDeployment(ctx context.Context, req *pb.DeleteRequest) (*pb.ActionResult, error) {
	// TODO: delete namespace or resources
	return &pb.ActionResult{Status: 0, Message: "Stub delete success"}, nil
}

func (s *Service) GetStatus(ctx context.Context, req *pb.StatusRequest) (*pb.AppStatus, error) {
	// TODO: query pod/containers status
	return &pb.AppStatus{Uuid: req.Uuid, Status: 0, Message: "Stub status"}, nil
}

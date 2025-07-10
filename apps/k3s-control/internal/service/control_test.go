package service

import (
	"context"
	"testing"

	pb "github.com/shipkit/k3s-control/proto"
)

func TestStubService(t *testing.T) {
	svc := &Service{noop: true}
	ctx := context.Background()

	applyRes, err := svc.ApplyDeployment(ctx, &pb.ApplyRequest{Uuid: "123", ManifestYaml: "apiVersion: v1"})
	if err != nil {
		t.Fatalf("ApplyDeployment returned error: %v", err)
	}
	if applyRes.Status != 0 {
		t.Errorf("expected status 0, got %d", applyRes.Status)
	}

	deleteRes, err := svc.DeleteDeployment(ctx, &pb.DeleteRequest{Uuid: "123"})
	if err != nil {
		t.Fatalf("DeleteDeployment returned error: %v", err)
	}
	if deleteRes.Status != 0 {
		t.Errorf("expected status 0, got %d", deleteRes.Status)
	}

	statusRes, err := svc.GetStatus(ctx, &pb.StatusRequest{Uuid: "123"})
	if err != nil {
		t.Fatalf("GetStatus returned error: %v", err)
	}
	if statusRes.Status != 0 {
		t.Errorf("expected status 0, got %d", statusRes.Status)
	}
}

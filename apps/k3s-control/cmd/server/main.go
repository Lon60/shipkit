//go:build ignore
// +build ignore

// Temporary skeleton for k3s-control gRPC server. Will be compiled once protobuf generation is in place.

package main

import (
	"flag"
	"log"
	"net"

	"github.com/shipkit/k3s-control/internal/service"
	pb "github.com/shipkit/k3s-control/proto"
	"google.golang.org/grpc"
)

func main() {
	addr := flag.String("addr", ":9998", "gRPC listen address")
	flag.Parse()

	lis, err := net.Listen("tcp", *addr)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterK3sControlServiceServer(grpcServer, service.New())

	log.Printf("k3s-control gRPC server listening on %s", *addr)
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}

// Temporary skeleton for k3s-control gRPC server. Build with `make build`.

package main

import (
	"flag"
	"log"
	"net"

	"github.com/shipkit/k3s-control/internal/service"
	pb "github.com/shipkit/k3s-control/proto"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
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
	reflection.Register(grpcServer)

	log.Printf("k3s-control gRPC server listening on %s", *addr)
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}

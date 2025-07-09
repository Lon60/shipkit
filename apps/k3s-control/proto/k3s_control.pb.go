package proto

import (
	"context"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// Auto-generated stubs – simplified for development until real protoc generation is added.
// These definitions allow the service package to compile and run integration tests.
// DO NOT edit business logic here – only generated types & interfaces should live in this file.

// AppState mirrors the enum in k3s_control.proto.
type AppState int32

const (
	AppState_UNKNOWN  AppState = 0
	AppState_RUNNING  AppState = 1
	AppState_STOPPED  AppState = 2
	AppState_STARTING AppState = 3
	AppState_STOPPING AppState = 4
	AppState_ERROR    AppState = 5
)

const (
	K3sControlService_ApplyDeployment_FullMethodName  = "/k3s_control.K3sControlService/ApplyDeployment"
	K3sControlService_DeleteDeployment_FullMethodName = "/k3s_control.K3sControlService/DeleteDeployment"
	K3sControlService_GetStatus_FullMethodName        = "/k3s_control.K3sControlService/GetStatus"
)

func _K3sControlService_ApplyDeployment_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(ApplyRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(K3sControlServiceServer).ApplyDeployment(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: K3sControlService_ApplyDeployment_FullMethodName,
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(K3sControlServiceServer).ApplyDeployment(ctx, req.(*ApplyRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _K3sControlService_DeleteDeployment_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(DeleteRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(K3sControlServiceServer).DeleteDeployment(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: K3sControlService_DeleteDeployment_FullMethodName,
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(K3sControlServiceServer).DeleteDeployment(ctx, req.(*DeleteRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _K3sControlService_GetStatus_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(StatusRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(K3sControlServiceServer).GetStatus(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: K3sControlService_GetStatus_FullMethodName,
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(K3sControlServiceServer).GetStatus(ctx, req.(*StatusRequest))
	}
	return interceptor(ctx, in, info, handler)
}

// ApplyRequest represents an incoming apply deployment call.
type ApplyRequest struct {
	Uuid         string
	ManifestYaml string
}

type DeleteRequest struct {
	Uuid string
}

type StatusRequest struct {
	Uuid string
}

type ActionResult struct {
	Status  int32
	Message string
	Details string
}

// ContainerStatus provides per-container state and port info.
type ContainerStatus struct {
	Name      string
	State     string
	Readiness string
	Ports     []string
}

type AppStatus struct {
	Uuid       string
	State      AppState
	Containers []*ContainerStatus
	Message    string
	Status     int32
}

// K3sControlServiceServer is the server API for K3sControlService service.
// All implementations must embed UnimplementedK3sControlServiceServer
// for forward compatibility.
type K3sControlServiceServer interface {
	ApplyDeployment(context.Context, *ApplyRequest) (*ActionResult, error)
	DeleteDeployment(context.Context, *DeleteRequest) (*ActionResult, error)
	GetStatus(context.Context, *StatusRequest) (*AppStatus, error)
	mustEmbedUnimplementedK3sControlServiceServer()
}

// UnimplementedK3sControlServiceServer must be embedded to have forward compatible implementations.
// NOTE: this should be embedded by value to avoid nil pointer deref.
type UnimplementedK3sControlServiceServer struct{}

func (UnimplementedK3sControlServiceServer) ApplyDeployment(context.Context, *ApplyRequest) (*ActionResult, error) {
	return nil, status.Errorf(codes.Unimplemented, "method ApplyDeployment not implemented")
}
func (UnimplementedK3sControlServiceServer) DeleteDeployment(context.Context, *DeleteRequest) (*ActionResult, error) {
	return nil, status.Errorf(codes.Unimplemented, "method DeleteDeployment not implemented")
}
func (UnimplementedK3sControlServiceServer) GetStatus(context.Context, *StatusRequest) (*AppStatus, error) {
	return nil, status.Errorf(codes.Unimplemented, "method GetStatus not implemented")
}
func (UnimplementedK3sControlServiceServer) mustEmbedUnimplementedK3sControlServiceServer() {}

// UnsafeK3sControlServiceServer may be embedded to opt-out of forward compatibility.
type UnsafeK3sControlServiceServer interface {
	mustEmbedUnimplementedK3sControlServiceServer()
}

// RegisterK3sControlServiceServer registers the service implementation with the gRPC server.
func RegisterK3sControlServiceServer(s grpc.ServiceRegistrar, srv K3sControlServiceServer) {
	// Nothing fancy – just register the service description.
	s.RegisterService(&K3sControlService_ServiceDesc, srv)
}

// Update service descriptor with method handlers.
var K3sControlService_ServiceDesc = grpc.ServiceDesc{
	ServiceName: "k3s_control.K3sControlService",
	HandlerType: (*K3sControlServiceServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "ApplyDeployment",
			Handler:    _K3sControlService_ApplyDeployment_Handler,
		},
		{
			MethodName: "DeleteDeployment",
			Handler:    _K3sControlService_DeleteDeployment_Handler,
		},
		{
			MethodName: "GetStatus",
			Handler:    _K3sControlService_GetStatus_Handler,
		},
	},
	Streams: []grpc.StreamDesc{},
}

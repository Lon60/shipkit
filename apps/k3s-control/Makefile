PROTO_SRC := proto/k3s_control.proto

.PHONY: proto build run clean

## proto: Generate Go gRPC stubs from proto/k3s_control.proto
proto:
	protoc \
		--go_out=. --go_opt=paths=source_relative \
		--go-grpc_out=. --go-grpc_opt=paths=source_relative \
		$(PROTO_SRC)

## build: Compile the k3s-control binary (requires generated code)
build:
	go build -o bin/k3s-control ./cmd/server

## run: Build and run the server locally on :9998
run: build
	./bin/k3s-control -addr :9998

## clean: Remove generated stubs and build artefacts
clean:
	rm -rf proto/*.pb.go bin 

## test: run unit tests
.PHONY: test

test:
	go mod tidy
	go test ./... 

## docker-build: Build container image
.PHONY: docker-build

docker-build:
	docker build -t k3s-control:dev . 
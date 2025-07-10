# syntax=docker/dockerfile:1

# --- Builder stage ---
FROM golang:1.24-alpine AS builder

RUN apk add --no-cache git ca-certificates tzdata protobuf gcc musl-dev
WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download

# Install protoc Go plugins
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.34.1 \
    && go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.4.0

COPY . .

# Generate protobuf code
RUN protoc --go_out=paths=source_relative:. --go-grpc_out=paths=source_relative:. proto/k3s_control.proto
RUN go mod tidy

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags='-w -s' \
    -o k3s-control-server ./cmd/server

# --- Runtime stage ---
FROM gcr.io/distroless/static AS runtime
COPY --from=builder /src/k3s-control-server /usr/bin/k3s-control-server

EXPOSE 9998

ENTRYPOINT ["/usr/bin/k3s-control-server"] 
FROM golang:1.24-alpine AS builder

RUN apk add --no-cache git ca-certificates tzdata

WORKDIR /app

COPY go.mod go.sum ./

RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags='-w -s -extldflags "-static"' \
    -a -installsuffix cgo \
    -o docker-control-server \
    ./cmd/server

FROM docker:24.0.9-cli AS runtime

COPY --from=builder /app/docker-control-server /usr/local/bin/docker-control-server

RUN mkdir -p /deployments && chown root:root /deployments

EXPOSE 50051

ENV DOCKER_CONTROL_PORT=50051 \
    DOCKER_CONTROL_LOG_LEVEL=info \
    DOCKER_CONTROL_DEPLOYMENTS_DIR=/deployments

ENTRYPOINT ["docker-control-server"] 
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

FROM scratch

COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=builder /usr/share/zoneinfo /usr/share/zoneinfo
COPY --from=builder /etc/passwd /etc/passwd
COPY --from=builder /etc/group /etc/group

COPY --from=builder /app/docker-control-server /docker-control-server

COPY --from=builder --chown=65534:65534 /tmp /deployments

USER 65534:65534

EXPOSE 50051

ENV DOCKER_CONTROL_PORT=50051
ENV DOCKER_CONTROL_LOG_LEVEL=info
ENV DOCKER_CONTROL_DEPLOYMENTS_DIR=/deployments

ENTRYPOINT ["/docker-control-server"] 
package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DockerControlGrpcClient {

    private final ManagedChannel channel;
    private final DockerControlServiceGrpc.DockerControlServiceBlockingStub blockingStub;

    public DockerControlGrpcClient(
            @Value("${docker-control.host:localhost}") String host,
            @Value("${docker-control.port:50051}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = DockerControlServiceGrpc.newBlockingStub(channel);
        log.info("DockerControlGrpcClient connected to {}:{}", host, port);
    }

    public ActionResult startCompose(String uuid, String composeYaml) {
        StartComposeRequest req = StartComposeRequest.newBuilder()
                .setUuid(uuid)
                .setComposeYaml(composeYaml)
                .build();
        return blockingStub.startCompose(req);
    }

    public ActionResult stopApp(String uuid) {
        StopAppRequest req = StopAppRequest.newBuilder()
                .setUuid(uuid)
                .build();
        return blockingStub.stopApp(req);
    }

    public AppStatus getStatus(String uuid) {
        GetStatusRequest req = GetStatusRequest.newBuilder()
                .setUuid(uuid)
                .build();
        return blockingStub.getStatus(req);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
    }
} 
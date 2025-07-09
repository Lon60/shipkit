package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sActionResult;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sAppStatus;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
// Always enabled since Shipkit now runs exclusively on K3s
import org.springframework.stereotype.Component;

import k3s_control.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class K3sControlGrpcClient {

    private final ManagedChannel channel;
    private final K3sControlServiceGrpc.K3sControlServiceBlockingStub blockingStub;

    public K3sControlGrpcClient(
            @Value("${k3s-control.host:localhost}") String host,
            @Value("${k3s-control.port:9998}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = K3sControlServiceGrpc.newBlockingStub(channel);
        log.info("K3sControlGrpcClient connected to {}:{}", host, port);
    }

    public K3sActionResult applyDeployment(String uuid, String manifestYaml) {
        ApplyRequest req = ApplyRequest.newBuilder()
                .setUuid(uuid)
                .setManifestYaml(manifestYaml)
                .build();
        ActionResult res = blockingStub.applyDeployment(req);
        return toActionResult(res);
    }

    public K3sActionResult deleteDeployment(String uuid) {
        DeleteRequest req = DeleteRequest.newBuilder()
                .setUuid(uuid)
                .build();
        ActionResult res = blockingStub.deleteDeployment(req);
        return toActionResult(res);
    }

    public K3sAppStatus getStatus(String uuid) {
        StatusRequest req = StatusRequest.newBuilder()
                .setUuid(uuid)
                .build();
        AppStatus res = blockingStub.getStatus(req);
        return toAppStatus(res);
    }

    private K3sActionResult toActionResult(ActionResult res) {
        return K3sActionResult.builder()
                .status(res.getStatus())
                .message(res.getMessage())
                .details(res.getDetails())
                .build();
    }

    private K3sAppStatus toAppStatus(AppStatus res) {
        List<K3sAppStatus.ContainerStatus> containers = res.getContainersList().stream()
                .map(c -> K3sAppStatus.ContainerStatus.builder()
                        .name(c.getName())
                        .state(c.getState())
                        .readiness(c.getReadiness())
                        .ports(c.getPortsList())
                        .build())
                .collect(Collectors.toList());

        return K3sAppStatus.builder()
                .uuid(res.getUuid())
                .status(res.getStatus())
                .message(res.getMessage())
                .state(res.getState().name())
                .containers(containers)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
    }
} 
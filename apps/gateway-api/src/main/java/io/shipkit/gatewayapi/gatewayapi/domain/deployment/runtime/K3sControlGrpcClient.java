package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sAppStatus;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sActionResult;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Temporarily stub out gRPC generated code until proto integration
class DummyStub {
    K3sActionResult applyDeployment(String u, String yaml) { return K3sActionResult.builder().status(0).message("ok").build(); }
    K3sActionResult deleteDeployment(String u) { return K3sActionResult.builder().status(0).message("ok").build(); }
    K3sAppStatus getStatus(String u) { return K3sAppStatus.builder().uuid(u).status(0).message("ok").build(); }
}

@Slf4j
@Component
@ConditionalOnProperty(name = "shipkit.runtime", havingValue = "k8s")
public class K3sControlGrpcClient {

    private final ManagedChannel channel;
    private final DummyStub stub;

    public K3sControlGrpcClient(
            @Value("${k3s-control.host:localhost}") String host,
            @Value("${k3s-control.port:9998}") int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = new DummyStub();
        log.info("K3sControlGrpcClient connected to {}:{}", host, port);
    }

    public K3sActionResult applyDeployment(String uuid, String manifestYaml) {
        return stub.applyDeployment(uuid, manifestYaml);
    }

    public K3sActionResult deleteDeployment(String uuid) {
        return stub.deleteDeployment(uuid);
    }

    public K3sAppStatus getStatus(String uuid) {
        return stub.getStatus(uuid);
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdownNow();
    }
} 
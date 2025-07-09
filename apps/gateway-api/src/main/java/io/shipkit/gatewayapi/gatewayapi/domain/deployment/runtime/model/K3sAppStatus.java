package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K3sAppStatus {
    private String uuid;
    private int status;
    private String message;
    private String state;
    private List<ContainerStatus> containers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerStatus {
        private String name;
        private String state;
        private String readiness;
        private List<String> ports;
    }
} 
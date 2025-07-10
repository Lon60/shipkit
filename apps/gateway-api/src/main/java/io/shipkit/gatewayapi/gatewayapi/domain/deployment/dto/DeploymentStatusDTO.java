package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sAppStatus;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sAppStatus.ContainerStatus;

import java.util.List;
import java.util.stream.Collectors;

public record DeploymentStatusDTO(String uuid,
                                  String state,
                                  String message,
                                  int status,
                                  List<ContainerStatusDTO> containers) {

    public static DeploymentStatusDTO from(K3sAppStatus appStatus) {
        List<ContainerStatusDTO> containers = appStatus.getContainers().stream()
                .map(DeploymentStatusDTO::mapContainer)
                .collect(Collectors.toList());
        return new DeploymentStatusDTO(
                appStatus.getUuid(),
                appStatus.getState(),
                appStatus.getMessage(),
                appStatus.getStatus(),
                containers);
    }

    private static ContainerStatusDTO mapContainer(ContainerStatus cs) {
        return new ContainerStatusDTO(
                cs.getName(),
                cs.getState(),
                cs.getReadiness(),
                cs.getPorts());
    }
} 
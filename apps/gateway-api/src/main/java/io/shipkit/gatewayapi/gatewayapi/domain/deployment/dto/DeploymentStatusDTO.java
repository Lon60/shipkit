package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import docker_control.AppStatus;
import docker_control.ContainerStatus;

import java.util.List;
import java.util.stream.Collectors;

public record DeploymentStatusDTO(String uuid,
                                  String state,
                                  String message,
                                  int status,
                                  List<ContainerStatusDTO> containers) {

    public static DeploymentStatusDTO from(AppStatus appStatus) {
        List<ContainerStatusDTO> containers = appStatus.getContainersList().stream()
                .map(DeploymentStatusDTO::mapContainer)
                .collect(Collectors.toList());
        return new DeploymentStatusDTO(
                appStatus.getUuid(),
                appStatus.getState().name(),
                appStatus.getMessage(),
                appStatus.getStatus(),
                containers);
    }

    private static ContainerStatusDTO mapContainer(ContainerStatus cs) {
        return new ContainerStatusDTO(
                cs.getName(),
                cs.getState(),
                cs.getHealth(),
                cs.getPortsList());
    }
} 
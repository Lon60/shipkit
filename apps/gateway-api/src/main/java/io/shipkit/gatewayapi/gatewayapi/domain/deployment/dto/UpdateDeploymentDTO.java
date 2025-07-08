package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

public record UpdateDeploymentDTO(
        String name,
        String composeYaml
) {} 
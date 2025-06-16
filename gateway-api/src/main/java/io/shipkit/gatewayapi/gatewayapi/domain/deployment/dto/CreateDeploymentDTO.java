package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeploymentDTO(
        @NotBlank String name,
        @NotBlank String composeYaml
) {} 
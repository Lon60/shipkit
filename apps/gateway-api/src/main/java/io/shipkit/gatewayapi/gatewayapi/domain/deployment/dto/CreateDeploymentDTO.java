package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateDeploymentDTO(
        @NotBlank String name,
        @NotBlank String manifestYaml,
        List<ServiceDefinitionDTO> services
) {} 
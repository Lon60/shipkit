package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import java.util.List;

public record UpdateDeploymentDTO(
        String name,
        List<ServiceDefinitionDTO> services
) {} 
package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import java.util.List;

public record UpdateDeploymentDTO(
        String name,
        String manifestYaml,
        List<ServiceDefinitionDTO> services
) {} 
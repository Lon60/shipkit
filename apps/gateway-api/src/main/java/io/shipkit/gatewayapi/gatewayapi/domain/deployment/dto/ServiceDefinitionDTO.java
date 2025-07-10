package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceDefinitionDTO(
        @NotBlank String serviceName,
        @NotBlank String image,
        Integer internalPort,
        String subDomain,
        @NotNull Boolean sslEnabled
) {} 
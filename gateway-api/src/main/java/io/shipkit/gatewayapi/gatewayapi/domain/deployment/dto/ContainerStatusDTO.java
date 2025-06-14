package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import java.util.List;

public record ContainerStatusDTO(String name, String state, String health, List<String> ports) {} 
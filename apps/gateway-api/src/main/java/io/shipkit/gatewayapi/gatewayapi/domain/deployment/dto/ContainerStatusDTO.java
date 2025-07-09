package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import java.util.List;

public record ContainerStatusDTO(String name, String state, String readiness, List<String> ports) {} 
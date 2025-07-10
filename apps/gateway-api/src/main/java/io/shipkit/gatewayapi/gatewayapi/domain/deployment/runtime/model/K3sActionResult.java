package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K3sActionResult {
    private int status;
    private String message;
    private String details;
} 
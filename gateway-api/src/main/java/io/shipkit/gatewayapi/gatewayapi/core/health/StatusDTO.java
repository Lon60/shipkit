package io.shipkit.gatewayapi.gatewayapi.core.health;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatusDTO {
    private String status;
    private boolean adminInitialized;
    private boolean domainInitialized;
} 
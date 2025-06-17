package io.shipkit.gatewayapi.gatewayapi.core.health;

import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class HealthGraphQLController {

    private final HealthService healthService;

    @QueryMapping
    @PreAuthorize("permitAll()")
    public StatusDTO status() {
        return healthService.getStatus();
    }
} 
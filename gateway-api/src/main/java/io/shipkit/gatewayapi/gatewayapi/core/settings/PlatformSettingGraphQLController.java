package io.shipkit.gatewayapi.gatewayapi.core.settings;

import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class PlatformSettingGraphQLController {

    private final DomainSetupService domainSetupService;
    private final PlatformSettingRepository repository;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean setupDomain(@Argument String domain, @Argument Boolean skipValidation, @Argument Boolean sslEnabled, @Argument Boolean forceSsl) {
        domainSetupService.configureDomain(domain, skipValidation != null && skipValidation, sslEnabled != null && sslEnabled, forceSsl != null && forceSsl);
        return true;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PlatformSetting platformSettings() {
        return repository.findTopByOrderByCreatedAtDesc().orElse(null);
    }
} 
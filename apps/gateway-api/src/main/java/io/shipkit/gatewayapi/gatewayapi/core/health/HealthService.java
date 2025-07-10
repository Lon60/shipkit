package io.shipkit.gatewayapi.gatewayapi.core.health;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.AccountRepository;
import io.shipkit.gatewayapi.gatewayapi.core.settings.PlatformSettingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class HealthService {
    
    private final AccountRepository accountRepository;
    private final PlatformSettingRepository platformSettingRepository;

    @Transactional(readOnly = true)
    public StatusDTO getStatus() {
        boolean adminInitialized = accountRepository.count() > 0;
        boolean domainInitialized = platformSettingRepository.count() > 0;
        return new StatusDTO("healthy", adminInitialized, domainInitialized);
    }
} 
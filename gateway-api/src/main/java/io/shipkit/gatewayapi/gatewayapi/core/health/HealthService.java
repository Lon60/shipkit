package io.shipkit.gatewayapi.gatewayapi.core.health;

import io.shipkit.gatewayapi.gatewayapi.core.security.account.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class HealthService {
    
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public StatusDTO getStatus() {
        boolean adminInitialized = accountRepository.count() > 0;
        return new StatusDTO("healthy", adminInitialized);
    }
} 
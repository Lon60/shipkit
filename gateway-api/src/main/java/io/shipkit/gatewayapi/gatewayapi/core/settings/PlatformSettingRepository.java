package io.shipkit.gatewayapi.gatewayapi.core.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, UUID> {
    Optional<PlatformSetting> findTopByOrderByCreatedAtDesc();
    boolean existsByFqdn(String fqdn);
    Optional<PlatformSetting> findByFqdn(String fqdn);
} 
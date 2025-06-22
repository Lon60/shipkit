package io.shipkit.gatewayapi.gatewayapi.core.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    Optional<PlatformSetting> findTopByOrderByCreatedAtDesc();
    boolean existsByFqdn(String fqdn);
} 
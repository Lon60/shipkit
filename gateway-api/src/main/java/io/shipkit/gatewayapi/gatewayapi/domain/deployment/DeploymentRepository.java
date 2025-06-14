package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
} 
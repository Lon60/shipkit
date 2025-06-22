package io.shipkit.gatewayapi.gatewayapi.core.settings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fqdn", nullable = false, unique = true)
    private String fqdn;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
} 
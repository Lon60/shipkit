package io.shipkit.gatewayapi.gatewayapi.core.settings;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String fqdn;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    private boolean sslEnabled;

    private boolean forceSsl;
}
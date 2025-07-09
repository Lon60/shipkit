package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {
    @Id
    @GeneratedValue
    private UUID id;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String manifestYaml;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant createdAt;

    public static Deployment create(String name, String manifestYaml) {
        return Deployment.builder()
                .name(name)
                .manifestYaml(manifestYaml)
                .createdAt(Instant.now())
                .build();
    }
} 
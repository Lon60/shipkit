package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.KubernetesPort;
import lombok.Builder;

/**
 * Implementation of {@link KubernetesPort.DeploymentStatus}.
 */
@Builder
public record DeploymentStatusImpl(int replicas,
                                   int readyReplicas,
                                   int availableReplicas,
                                   boolean available,
                                   String statusMessage
) implements KubernetesPort.DeploymentStatus {
}

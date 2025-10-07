package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.KubernetesPort;
import lombok.Builder;
import lombok.Getter;

/**
 * Implementation of {@link KubernetesPort.DeploymentStatus}.
 */
@Getter
@Builder
public class DeploymentStatusImpl implements KubernetesPort.DeploymentStatus {
    
    private final int replicas;
    private final int readyReplicas;
    private final int availableReplicas;
    private final boolean available;
    private final String statusMessage;
}

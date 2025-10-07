package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.KubernetesPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentStatusImplTest {

    @Test
    void shouldBuildDeploymentStatus() {
        KubernetesPort.DeploymentStatus status = DeploymentStatusImpl.builder()
            .replicas(3)
            .readyReplicas(3)
            .availableReplicas(3)
            .available(true)
            .statusMessage("Deployment is available")
            .build();
        
        assertEquals(3, status.getReplicas());
        assertEquals(3, status.getReadyReplicas());
        assertEquals(3, status.getAvailableReplicas());
        assertTrue(status.isAvailable());
        assertEquals("Deployment is available", status.getStatusMessage());
    }

    @Test
    void shouldHandleUnavailableDeployment() {
        KubernetesPort.DeploymentStatus status = DeploymentStatusImpl.builder()
            .replicas(3)
            .readyReplicas(1)
            .availableReplicas(1)
            .available(false)
            .statusMessage("Waiting for replicas")
            .build();
        
        assertEquals(3, status.getReplicas());
        assertEquals(1, status.getReadyReplicas());
        assertFalse(status.isAvailable());
    }
}

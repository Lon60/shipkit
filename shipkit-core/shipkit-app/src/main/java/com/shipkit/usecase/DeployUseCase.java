package com.shipkit.usecase;

import com.shipkit.api.domain.Deployment;
import com.shipkit.api.repositories.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeployUseCase {

    private final DeploymentRepository deploymentRepository;

    public void execute(Deployment deployment) {
        deploymentRepository.save(deployment);
    }
}

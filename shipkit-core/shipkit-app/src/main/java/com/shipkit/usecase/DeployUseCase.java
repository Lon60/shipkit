package com.shipkit.usecase;

import com.shipkit.api.domain.Deployment;
import com.shipkit.api.repositories.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeployUseCase {

    private final DeploymentRepository deploymentRepository;

    public void execute(Deployment deployment) {
        log.info("Deploying... : {}", deployment);
        deploymentRepository.save(deployment);
    }
}

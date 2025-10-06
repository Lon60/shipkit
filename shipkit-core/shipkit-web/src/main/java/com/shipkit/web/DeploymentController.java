package com.shipkit.web;

import com.shipkit.api.domain.Deployment;
import com.shipkit.usecase.DeployUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/deployment")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeployUseCase deploy;

    @PostMapping
    public void deploy(@RequestBody Deployment deployment) {
        deploy.execute(deployment);
    }
}

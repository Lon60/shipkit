package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.AppStatus;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.DeploymentStatusDTO;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID; 

@Controller
@AllArgsConstructor
public class DeploymentGraphQLController {

    private final DeploymentService deploymentService;

    @MutationMapping
    public Deployment startDeployment(@Argument String composeYaml) {
        return deploymentService.startDeployment(composeYaml);
    }

    @MutationMapping
    public boolean stopDeployment(@Argument UUID id) {
        deploymentService.stopDeployment(id);
        return true;
    }

    @QueryMapping
    public DeploymentStatusDTO deploymentStatus(@Argument UUID id) {
        AppStatus status = deploymentService.getStatus(id);
        return DeploymentStatusDTO.from(status);
    }

    @QueryMapping
    public List<Deployment> deployments() {
        return deploymentService.listDeployments();
    }
} 
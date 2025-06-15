package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.AppStatus;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.DeploymentStatusDTO;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID; 

@Controller
@AllArgsConstructor
public class DeploymentGraphQLController {

    private final DeploymentService deploymentService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Deployment createDeployment(@Argument String composeYaml) {
        return deploymentService.createDeployment(composeYaml);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Deployment updateDeployment(@Argument UUID id, @Argument String composeYaml) {
        return deploymentService.updateDeployment(id, composeYaml);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteDeployment(@Argument UUID id) {
        deploymentService.deleteDeployment(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean stopDeployment(@Argument UUID id) {
        deploymentService.stopDeployment(id);
        return true;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public DeploymentStatusDTO deploymentStatus(@Argument UUID id) {
        AppStatus status = deploymentService.getStatus(id);
        return DeploymentStatusDTO.from(status);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Deployment> deployments() {
        return deploymentService.listDeployments();
    }
} 
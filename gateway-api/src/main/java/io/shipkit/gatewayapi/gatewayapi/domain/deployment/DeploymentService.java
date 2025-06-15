package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.ActionResult;
import docker_control.AppStatus;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DockerControlGrpcClient grpcClient;

    @Transactional
    public Deployment createDeployment(String composeYaml) {
        Deployment deployment = Deployment.create(composeYaml);
        deployment = deploymentRepository.save(deployment);

        String uuid = deployment.getId().toString();
        ActionResult result = grpcClient.startCompose(uuid, composeYaml);
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to start compose: " + result.getMessage());
        }
        return deployment;
    }

    @Transactional
    public Deployment updateDeployment(UUID id, String composeYaml) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));
        
        ActionResult stopResult = grpcClient.stopApp(id.toString());
        if (stopResult.getStatus() != 0) {
            throw new BadRequestException("Failed to stop existing deployment: " + stopResult.getMessage());
        }
        
        deployment.setComposeYaml(composeYaml);
        deployment = deploymentRepository.save(deployment);
        
        ActionResult startResult = grpcClient.startCompose(id.toString(), composeYaml);
        if (startResult.getStatus() != 0) {
            throw new BadRequestException("Failed to start updated compose: " + startResult.getMessage());
        }
        
        return deployment;
    }

    @Transactional
    public void deleteDeployment(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        
        ActionResult result = grpcClient.stopApp(id.toString());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to stop deployment before deletion: " + result.getMessage());
        }
        
        deploymentRepository.deleteById(id);
    }

    @Transactional
    public void stopDeployment(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        ActionResult result = grpcClient.stopApp(id.toString());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to stop compose: " + result.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AppStatus getStatus(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        return grpcClient.getStatus(id.toString());
    }

    @Transactional(readOnly = true)
    public List<Deployment> listDeployments() {
        return deploymentRepository.findAll();
    }

    @Transactional
    public Deployment startDeployment(UUID id) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));

        ActionResult result = grpcClient.startCompose(id.toString(), deployment.getComposeYaml());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to start compose: " + result.getMessage());
        }
        return deployment;
    }
} 
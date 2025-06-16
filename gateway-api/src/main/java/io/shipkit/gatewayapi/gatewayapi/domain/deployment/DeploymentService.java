package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.ActionResult;
import docker_control.AppStatus;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.ResourceNotFoundException;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.UpdateDeploymentDTO;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.CreateDeploymentDTO;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.DeploymentMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DockerControlGrpcClient grpcClient;
    private final DeploymentMapper deploymentMapper;

    @Transactional
    public Deployment createDeployment(CreateDeploymentDTO createDTO) {
        Deployment deployment = deploymentMapper.toEntity(createDTO);
        deployment.setCreatedAt(Instant.now());
        deployment = deploymentRepository.save(deployment);
        
        ActionResult result = grpcClient.startCompose(deployment.getId().toString(), deployment.getComposeYaml());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to start deployment: " + result.getMessage());
        }
        
        return deployment;
    }

    @Transactional
    public Deployment updateDeployment(UUID id, UpdateDeploymentDTO updateDTO) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));

        String originalCompose = deployment.getComposeYaml();
        
        deploymentMapper.updateEntity(deployment, updateDTO);
        
        boolean composeChanged = updateDTO.composeYaml() != null && 
                                !updateDTO.composeYaml().equals(originalCompose);
        
        if (composeChanged) {
            ActionResult stopResult = grpcClient.stopApp(id.toString());
            if (stopResult.getStatus() != 0) {
                throw new BadRequestException("Failed to stop existing deployment: " + stopResult.getMessage());
            }
            
            ActionResult startResult = grpcClient.startCompose(id.toString(), updateDTO.composeYaml());
            if (startResult.getStatus() != 0) {
                throw new BadRequestException("Failed to start updated deployment: " + startResult.getMessage());
            }
        }
        
        return deploymentRepository.save(deployment);
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

    @Transactional(readOnly = true)
    public Deployment findById(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));
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
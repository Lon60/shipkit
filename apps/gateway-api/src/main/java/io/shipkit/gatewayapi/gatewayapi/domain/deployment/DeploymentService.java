package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

import docker_control.AppStatus;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.BadRequestException;
import io.shipkit.gatewayapi.gatewayapi.core.exceptions.ResourceNotFoundException;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.UpdateDeploymentDTO;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.CreateDeploymentDTO;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.DeploymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.ManifestBuilder;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.K3sControlGrpcClient;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sActionResult;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.ServiceDefinitionDTO;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinition;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinitionRepository;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DockerControlGrpcClient grpcClient;
    private final DeploymentServiceDefinitionRepository svcRepo;
    private final ManifestBuilder manifestBuilder;
    private final ObjectProvider<K3sControlGrpcClient> k3sClientProvider;
    @Value("${shipkit.runtime:compose}")
    private String runtimeMode;
    private final DeploymentMapper deploymentMapper;

    @Transactional
    public Deployment createDeployment(CreateDeploymentDTO createDTO) {
        Deployment deployment = deploymentMapper.toEntity(createDTO);
        deployment.setCreatedAt(Instant.now());
        deployment = deploymentRepository.save(deployment);

        if ("k8s".equalsIgnoreCase(runtimeMode)) {
            K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
            if (k3sClient == null) {
                throw new IllegalStateException("K3s runtime requested but K3sControlGrpcClient bean not present");
            }
            // persist service definitions if provided
            if (createDTO.services() != null) {
                for (ServiceDefinitionDTO dto : createDTO.services()) {
                    DeploymentServiceDefinition def = DeploymentServiceDefinition.builder()
                            .deployment(deployment)
                            .serviceName(dto.serviceName())
                            .image(dto.image())
                            .internalPort(dto.internalPort())
                            .subDomain(dto.subDomain())
                            .expose(Boolean.TRUE.equals(dto.expose()))
                            .sslEnabled(Boolean.TRUE.equals(dto.sslEnabled()))
                            .build();
                    svcRepo.save(def);
                }
            }

            String manifest = manifestBuilder.build(deployment, svcRepo.findAll());
            K3sActionResult res = k3sClient.applyDeployment(deployment.getId().toString(), manifest);
            if (res.getStatus() != 0) {
                throw new BadRequestException("Failed to apply deployment: " + res.getMessage());
            }
        } else {
            docker_control.ActionResult result = grpcClient.startCompose(deployment.getId().toString(), deployment.getComposeYaml());
            if (result.getStatus() != 0) {
                throw new BadRequestException("Failed to start deployment: " + result.getMessage());
            }
        }
        return deployment;
    }

    private void dockerPathStart(Deployment deployment) {
        docker_control.ActionResult result = grpcClient.startCompose(deployment.getId().toString(), deployment.getComposeYaml());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to start deployment: " + result.getMessage());
        }
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
            docker_control.ActionResult stopResult = grpcClient.stopApp(id.toString());
            if (stopResult.getStatus() != 0 && !stopResult.getMessage().toLowerCase().contains("app not found")) {
                throw new BadRequestException("Failed to stop existing deployment: " + stopResult.getMessage());
            }
            
            docker_control.ActionResult startResult = grpcClient.startCompose(id.toString(), updateDTO.composeYaml());
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
        
        docker_control.ActionResult result = grpcClient.stopApp(id.toString());
        if (result.getStatus() != 0 && !result.getMessage().toLowerCase().contains("app not found")) {
            throw new BadRequestException("Failed to stop deployment before deletion: " + result.getMessage());
        }
        
        deploymentRepository.deleteById(id);
    }

    @Transactional
    public void stopDeployment(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        docker_control.ActionResult result = grpcClient.stopApp(id.toString());
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

        docker_control.ActionResult result = grpcClient.startCompose(id.toString(), deployment.getComposeYaml());
        if (result.getStatus() != 0) {
            throw new BadRequestException("Failed to start compose: " + result.getMessage());
        }
        return deployment;
    }
} 
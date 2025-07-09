package io.shipkit.gatewayapi.gatewayapi.domain.deployment;

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
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime.model.K3sAppStatus;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto.ServiceDefinitionDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinition;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinitionRepository;

@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final DeploymentServiceDefinitionRepository svcRepo;
    private final ManifestBuilder manifestBuilder;
    private final ObjectProvider<K3sControlGrpcClient> k3sClientProvider;
    private final DeploymentMapper deploymentMapper;

    @Transactional
    public Deployment createDeployment(CreateDeploymentDTO createDTO) {
        Deployment deployment = deploymentMapper.toEntity(createDTO);
        deployment.setCreatedAt(Instant.now());
        deployment = deploymentRepository.save(deployment);

        K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
        if (k3sClient == null) {
            throw new IllegalStateException("K3sControlGrpcClient bean not present");
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
        return deployment;
    }

    @Transactional
    public Deployment updateDeployment(UUID id, UpdateDeploymentDTO updateDTO) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment not found: " + id));

        String originalManifest = deployment.getManifestYaml();
        
        deploymentMapper.updateEntity(deployment, updateDTO);
        
        boolean manifestChanged = updateDTO.manifestYaml() != null && 
                                !updateDTO.manifestYaml().equals(originalManifest);
        
        if (manifestChanged) {
            K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
            if (k3sClient == null) {
                throw new IllegalStateException("K3sControlGrpcClient bean not present");
            }

            // Update manifest definitions if provided (simplified: delete all & re-insert)
            if (updateDTO.services() != null) {
                svcRepo.deleteAll();
                for (ServiceDefinitionDTO dto : updateDTO.services()) {
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
            K3sActionResult res = k3sClient.applyDeployment(id.toString(), manifest);
            if (res.getStatus() != 0) {
                throw new BadRequestException("Failed to apply deployment: " + res.getMessage());
            }
        }
        
        return deploymentRepository.save(deployment);
    }

    @Transactional
    public void deleteDeployment(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        
        K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
        if (k3sClient == null) {
            throw new IllegalStateException("K3sControlGrpcClient bean not present");
        }

        K3sActionResult res = k3sClient.deleteDeployment(id.toString());
        if (res.getStatus() != 0 && (res.getMessage() == null || !res.getMessage().toLowerCase().contains("not found"))) {
            throw new BadRequestException("Failed to delete deployment: " + res.getMessage());
        }
        
        deploymentRepository.deleteById(id);
    }

    @Transactional
    public void stopDeployment(UUID id) {
        // For K3s runtime stop == delete namespace
        deleteDeployment(id);
    }

    @Transactional(readOnly = true)
    public K3sAppStatus getStatus(UUID id) {
        if (!deploymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deployment not found: " + id);
        }
        K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
        if (k3sClient == null) {
            throw new IllegalStateException("K3sControlGrpcClient bean not present");
        }
        return k3sClient.getStatus(id.toString());
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

        // For K3s runtime "start" simply reapplies the manifest
        K3sControlGrpcClient k3sClient = k3sClientProvider.getIfAvailable();
        if (k3sClient == null) {
            throw new IllegalStateException("K3sControlGrpcClient bean not present");
        }
        String manifest = deployment.getManifestYaml();
        if (manifest == null || manifest.isBlank()) {
            manifest = manifestBuilder.build(deployment, svcRepo.findAll());
        }
        K3sActionResult res = k3sClient.applyDeployment(id.toString(), manifest);
        if (res.getStatus() != 0) {
            throw new BadRequestException("Failed to start deployment: " + res.getMessage());
        }
        return deployment;
    }

    @Transactional(readOnly = true)
    public String previewDeployment(CreateDeploymentDTO dto) {
        Deployment dummy = Deployment.builder()
                .id(UUID.randomUUID())
                .name(dto.name())
                .createdAt(Instant.now())
                .build();

        List<DeploymentServiceDefinition> defs = dto.services() == null ? List.of() : dto.services().stream()
                .map(s -> DeploymentServiceDefinition.builder()
                        .deployment(dummy)
                        .serviceName(s.serviceName())
                        .image(s.image())
                        .internalPort(s.internalPort())
                        .subDomain(s.subDomain())
                        .expose(Boolean.TRUE.equals(s.expose()))
                        .sslEnabled(Boolean.TRUE.equals(s.sslEnabled()))
                        .build())
                .toList();

        return manifestBuilder.build(dummy, defs);
    }
} 
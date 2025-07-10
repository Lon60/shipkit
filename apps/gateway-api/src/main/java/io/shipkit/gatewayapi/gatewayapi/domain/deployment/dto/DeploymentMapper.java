package io.shipkit.gatewayapi.gatewayapi.domain.deployment.dto;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.Deployment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DeploymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "manifestYaml", constant = "")
    Deployment toEntity(CreateDeploymentDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "name", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "manifestYaml", ignore = true)
    void updateEntity(@MappingTarget Deployment entity, UpdateDeploymentDTO dto);
} 
package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.Deployment;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ManifestBuilder {

    public String build(Deployment deployment, List<DeploymentServiceDefinition> services) {
        // Very simplified YAML generator – placeholder until full implementation
        StringBuilder sb = new StringBuilder();
        String ns = "deploy-" + deployment.getId();
        sb.append("apiVersion: v1\nkind: Namespace\nmetadata:\n  name: ").append(ns).append("\n---\n");
        for (DeploymentServiceDefinition svc : services) {
            String appName = svc.getServiceName();
            sb.append("apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: ").append(appName).append("\n  namespace: ").append(ns).append("\n---\n");
        }
        return sb.toString();
    }
} 
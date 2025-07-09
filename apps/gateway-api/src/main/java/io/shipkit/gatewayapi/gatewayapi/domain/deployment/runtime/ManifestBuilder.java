package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime;

import io.shipkit.gatewayapi.gatewayapi.domain.deployment.Deployment;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ManifestBuilder {

    public String build(Deployment deployment, List<DeploymentServiceDefinition> services) {
        StringBuilder sb = new StringBuilder();
        String ns = "deploy-" + deployment.getId();

        // Namespace
        sb.append("apiVersion: v1\nkind: Namespace\nmetadata:\n  name: ").append(ns).append("\n---\n");

        for (DeploymentServiceDefinition svc : services) {
            String app = svc.getServiceName();
            int port   = svc.getInternalPort() != null ? svc.getInternalPort() : 80;

            // Deployment
            sb.append("apiVersion: apps/v1\n")
              .append("kind: Deployment\n")
              .append("metadata:\n  name: ").append(app).append("\n  namespace: ").append(ns).append("\n")
              .append("spec:\n  replicas: 1\n  selector:\n    matchLabels:\n      app: ").append(app).append("\n  template:\n    metadata:\n      labels:\n        app: ").append(app).append("\n    spec:\n      containers:\n      - name: ").append(app).append("\n        image: ").append(svc.getImage()).append("\n        ports:\n        - containerPort: ").append(port).append("\n")
              .append("---\n");

            // Service
            sb.append("apiVersion: v1\nkind: Service\nmetadata:\n  name: ").append(app).append("\n  namespace: ").append(ns).append("\n")
              .append("spec:\n  selector:\n    app: ").append(app).append("\n  ports:\n  - port: ").append(port).append("\n    targetPort: ").append(port).append("\n    protocol: TCP\n---\n");

            if (svc.isExpose()) {
                String host = (svc.getSubDomain() != null && !svc.getSubDomain().isBlank()) ? svc.getSubDomain() + ".example.com" : app + ".example.com";
                sb.append("apiVersion: traefik.containo.us/v1alpha1\nkind: IngressRoute\nmetadata:\n  name: ").append(app).append("\n  namespace: ").append(ns).append("\n")
                  .append("spec:\n  entryPoints:\n  - web\n");
                if (svc.isSslEnabled()) {
                    sb.append("  - websecure\n");
                }
                sb.append("  routes:\n  - match: Host(`").append(host).append("`)\n    kind: Rule\n    services:\n    - name: ").append(app).append("\n      namespace: ").append(ns).append("\n      port: ").append(port).append("\n")
                  .append("---\n");
            }
        }

        return sb.toString();
    }
} 
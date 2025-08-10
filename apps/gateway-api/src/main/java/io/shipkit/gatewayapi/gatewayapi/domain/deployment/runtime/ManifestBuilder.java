package io.shipkit.gatewayapi.gatewayapi.domain.deployment.runtime;

import io.shipkit.gatewayapi.gatewayapi.core.config.K8sTemplateRenderer;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.Deployment;
import io.shipkit.gatewayapi.gatewayapi.domain.deployment.DeploymentServiceDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ManifestBuilder {

    private final K8sTemplateRenderer renderer;

    public String build(Deployment deployment, List<DeploymentServiceDefinition> services, String fqdn) {
        StringBuilder sb = new StringBuilder();
        String ns = "deploy-" + deployment.getId();

        Map<String, Object> nsModel = Map.of("namespace", ns);
        sb.append(renderer.render("namespace.ftl.yaml", nsModel)).append("\n---\n");

        for (DeploymentServiceDefinition svc : services) {
            String app = svc.getServiceName().toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("^-+", "")
                    .replaceAll("-+$", "");

            if (app.isBlank()) {
                throw new IllegalArgumentException("Service name '" + svc.getServiceName() + "' is not valid after sanitisation");
            }

            int port = svc.getInternalPort() != null ? svc.getInternalPort() : 80;

            Map<String, Object> depModel = new HashMap<>();
            depModel.put("appName", app);
            depModel.put("namespace", ns);
            depModel.put("image", svc.getImage());
            depModel.put("containerPort", port);
            sb.append(renderer.render("deployment.ftl.yaml", depModel)).append("\n---\n");

            Map<String, Object> svcModel = new HashMap<>();
            svcModel.put("appName", app);
            svcModel.put("namespace", ns);
            svcModel.put("servicePort", port);
            svcModel.put("targetPort", port);
            sb.append(renderer.render("service.ftl.yaml", svcModel)).append("\n---\n");

            if (svc.getSubDomain() != null && !svc.getSubDomain().isBlank()) {
                String host = svc.getSubDomain() + "." + fqdn;
                Map<String, Object> irModel = new HashMap<>();
                irModel.put("name", app);
                irModel.put("namespace", ns);
                irModel.put("sslEnabled", svc.isSslEnabled());
                if (svc.isSslEnabled()) {
                    irModel.put("certResolver", "letsencrypt");
                }
                irModel.put("match", "Host(`" + host + "`)");
                irModel.put("serviceName", app);
                irModel.put("serviceNamespace", ns);
                irModel.put("servicePort", port);
                sb.append(renderer.render("ingressroute.ftl.yaml", irModel)).append("\n---\n");
            }
        }

        return sb.toString();
    }
}
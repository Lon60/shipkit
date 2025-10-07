package com.shipkit.k8s.adapter;

import com.shipkit.api.exception.KubernetesOperationException;
import com.shipkit.api.exception.ReadinessTimeoutException;
import com.shipkit.api.exception.ResourceNotFoundException;
import com.shipkit.api.ports.KubernetesPort;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fabric8-based implementation of {@link KubernetesPort}.
 * <p>
 * This adapter uses the Fabric8 Kubernetes client to interact with Kubernetes clusters.
 * It supports both in-cluster and out-of-cluster configurations.
 * </p>
 */
@Slf4j
public class Fabric8KubernetesAdapter implements KubernetesPort {

    private final KubernetesClient client;

    /**
     * Creates a new Fabric8KubernetesAdapter with in-cluster configuration.
     */
    public Fabric8KubernetesAdapter() {
        this(true, null);
    }

    /**
     * Creates a new Fabric8KubernetesAdapter with custom configuration.
     *
     * @param inCluster whether to use in-cluster configuration
     * @param kubeConfigPath optional path to kubeconfig file (used when inCluster is false)
     */
    public Fabric8KubernetesAdapter(boolean inCluster, String kubeConfigPath) {
        try {
            Config config;
            
            if (inCluster) {
                log.info("Initializing Kubernetes client with in-cluster config");
                config = Config.autoConfigure(null);
            } else {
                log.info("Initializing Kubernetes client with out-of-cluster config");
                if (kubeConfigPath != null && !kubeConfigPath.isEmpty()) {
                    config = Config.fromKubeconfig(kubeConfigPath);
                } else {
                    config = Config.autoConfigure(null);
                }
            }
            
            this.client = new KubernetesClientBuilder().withConfig(config).build();
            log.info("Kubernetes client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Kubernetes client", e);
            throw new KubernetesOperationException("Failed to initialize Kubernetes client", e);
        }
    }

    /**
     * Closes the Kubernetes client and releases resources.
     */
    public void close() {
        if (client != null) {
            client.close();
            log.info("Kubernetes client closed");
        }
    }

    @Override
    public void createNamespace(String namespaceName) {
        try {
            Namespace existing = client.namespaces().withName(namespaceName).get();
            if (existing != null) {
                log.debug("Namespace {} already exists", namespaceName);
                return;
            }

            Namespace namespace = new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespaceName)
                    .addToLabels("app", "shipkit")
                    .addToLabels("managed-by", "shipkit")
                .endMetadata()
                .build();

            client.namespaces().resource(namespace).create();
            log.info("Created namespace: {}", namespaceName);
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException("Failed to create namespace: " + namespaceName, e);
        }
    }

    @Override
    public void deleteNamespace(String namespaceName) {
        try {
            Namespace namespace = client.namespaces().withName(namespaceName).get();
            if (namespace == null) {
                log.debug("Namespace {} does not exist", namespaceName);
                return;
            }

            client.namespaces().withName(namespaceName).delete();
            log.info("Deleted namespace: {}", namespaceName);
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException("Failed to delete namespace: " + namespaceName, e);
        }
    }

    @Override
    public Map<String, String> applyManifest(String namespace, String manifestYaml) {
        Map<String, String> appliedResources = new HashMap<>();
        
        try {
            List<HasMetadata> resources = client.load(new ByteArrayInputStream(manifestYaml.getBytes())).items();
            
            for (HasMetadata resource : resources) {
                // Add shipkit labels
                Map<String, String> labels = resource.getMetadata().getLabels();
                if (labels == null) {
                    labels = new HashMap<>();
                }
                labels.put("app", "shipkit");
                labels.put("managed-by", "shipkit");
                resource.getMetadata().setLabels(labels);
                
                // Set namespace if not already set
                if (resource.getMetadata().getNamespace() == null) {
                    resource.getMetadata().setNamespace(namespace);
                }
                
                // Apply using server-side apply
                PatchContext patchContext = new PatchContext.Builder()
                    .withPatchType(PatchType.SERVER_SIDE_APPLY)
                    .withFieldManager("shipkit")
                    .withForce(true)
                    .build();
                
                HasMetadata applied = client.resource(resource)
                    .inNamespace(namespace)
                    .patch(patchContext);
                
                String kind = applied.getKind();
                String name = applied.getMetadata().getName();
                appliedResources.put(kind, name);
                
                log.info("Applied {} {} in namespace {}", kind, name, namespace);
            }
            
            return appliedResources;
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException("Failed to apply manifest in namespace: " + namespace, e);
        }
    }

    @Override
    public void deleteResource(String namespace, String kind, String name) {
        try {
            // For common resources, use direct APIs
            switch (kind) {
                case "Deployment":
                    client.apps().deployments().inNamespace(namespace).withName(name).delete();
                    log.info("Deleted {} {} in namespace {}", kind, name, namespace);
                    break;
                case "Service":
                    client.services().inNamespace(namespace).withName(name).delete();
                    log.info("Deleted {} {} in namespace {}", kind, name, namespace);
                    break;
                case "Ingress":
                    client.network().v1().ingresses().inNamespace(namespace).withName(name).delete();
                    log.info("Deleted {} {} in namespace {}", kind, name, namespace);
                    break;
                default:
                    log.warn("Delete for resource kind {} not implemented, skipping", kind);
                    break;
            }
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException(
                String.format("Failed to delete %s %s in namespace %s", kind, name, namespace), e);
        }
    }

    @Override
    public void createOrUpdateService(String namespace, String serviceName, Map<String, String> selector, 
                                      int port, int targetPort) {
        try {
            Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .withNamespace(namespace)
                    .addToLabels("app", "shipkit")
                    .addToLabels("managed-by", "shipkit")
                .endMetadata()
                .withNewSpec()
                    .withSelector(selector)
                    .addNewPort()
                        .withPort(port)
                        .withNewTargetPort(targetPort)
                        .withProtocol("TCP")
                    .endPort()
                    .withType("ClusterIP")
                .endSpec()
                .build();

            PatchContext patchContext = new PatchContext.Builder()
                .withPatchType(PatchType.SERVER_SIDE_APPLY)
                .withFieldManager("shipkit")
                .withForce(true)
                .build();

            client.services()
                .inNamespace(namespace)
                .resource(service)
                .patch(patchContext);
            
            log.info("Created/Updated service {} in namespace {}", serviceName, namespace);
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException(
                String.format("Failed to create/update service %s in namespace %s", serviceName, namespace), e);
        }
    }

    @Override
    public void createOrUpdateIngress(String namespace, String ingressName, String host, String serviceName,
                                      int servicePort, String ingressClassName, boolean tlsEnabled, String clusterIssuer) {
        try {
            IngressBuilder ingressBuilder = new IngressBuilder()
                .withNewMetadata()
                    .withName(ingressName)
                    .withNamespace(namespace)
                    .addToLabels("app", "shipkit")
                    .addToLabels("managed-by", "shipkit")
                .endMetadata()
                .withNewSpec()
                    .withIngressClassName(ingressClassName)
                    .addNewRule()
                        .withHost(host)
                        .withNewHttp()
                            .addNewPath()
                                .withPath("/")
                                .withPathType("Prefix")
                                .withNewBackend()
                                    .withNewService()
                                        .withName(serviceName)
                                        .withNewPort()
                                            .withNumber(servicePort)
                                        .endPort()
                                    .endService()
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec();

            if (tlsEnabled) {
                if (clusterIssuer == null || clusterIssuer.isEmpty()) {
                    throw new KubernetesOperationException("Cluster issuer is required when TLS is enabled");
                }
                
                ingressBuilder
                    .editMetadata()
                        .addToAnnotations("cert-manager.io/cluster-issuer", clusterIssuer)
                    .endMetadata()
                    .editSpec()
                        .addNewTl()
                            .addToHosts(host)
                            .withSecretName(ingressName + "-tls")
                        .endTl()
                    .endSpec();
            }

            Ingress ingress = ingressBuilder.build();

            PatchContext patchContext = new PatchContext.Builder()
                .withPatchType(PatchType.SERVER_SIDE_APPLY)
                .withFieldManager("shipkit")
                .withForce(true)
                .build();

            client.network().v1().ingresses()
                .inNamespace(namespace)
                .resource(ingress)
                .patch(patchContext);
            
            log.info("Created/Updated ingress {} in namespace {} with TLS: {}", ingressName, namespace, tlsEnabled);
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException(
                String.format("Failed to create/update ingress %s in namespace %s", ingressName, namespace), e);
        }
    }

    @Override
    public boolean waitForDeploymentReady(String namespace, String deploymentName, Duration timeout) {
        try {
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeout.toMillis();
            long backoffMillis = 1000; // Start with 1 second
            long maxBackoffMillis = 10000; // Max 10 seconds
            
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                DeploymentStatus status = getDeploymentStatus(namespace, deploymentName);
                
                if (status.isAvailable() && status.getReadyReplicas() == status.getReplicas()) {
                    log.info("Deployment {} in namespace {} is ready", deploymentName, namespace);
                    return true;
                }
                
                log.debug("Waiting for deployment {} in namespace {}: {}/{} replicas ready",
                    deploymentName, namespace, status.getReadyReplicas(), status.getReplicas());
                
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ReadinessTimeoutException("Interrupted while waiting for deployment readiness", e);
                }
                
                // Exponential backoff
                backoffMillis = Math.min(backoffMillis * 2, maxBackoffMillis);
            }
            
            log.warn("Deployment {} in namespace {} did not become ready within timeout", deploymentName, namespace);
            return false;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (KubernetesOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new KubernetesOperationException(
                String.format("Error waiting for deployment %s in namespace %s", deploymentName, namespace), e);
        }
    }

    @Override
    public DeploymentStatus getDeploymentStatus(String namespace, String deploymentName) {
        try {
            Deployment deployment = client.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .get();
            
            if (deployment == null) {
                throw new ResourceNotFoundException(
                    String.format("Deployment %s not found in namespace %s", deploymentName, namespace));
            }
            
            Integer replicas = deployment.getSpec().getReplicas();
            Integer readyReplicas = deployment.getStatus().getReadyReplicas();
            Integer availableReplicas = deployment.getStatus().getAvailableReplicas();
            
            boolean available = false;
            String statusMessage = "Unknown";
            
            List<DeploymentCondition> conditions = deployment.getStatus().getConditions();
            if (conditions != null) {
                for (DeploymentCondition condition : conditions) {
                    if ("Available".equals(condition.getType())) {
                        available = "True".equals(condition.getStatus());
                        statusMessage = condition.getMessage();
                        break;
                    }
                }
            }
            
            return DeploymentStatusImpl.builder()
                .replicas(replicas != null ? replicas : 0)
                .readyReplicas(readyReplicas != null ? readyReplicas : 0)
                .availableReplicas(availableReplicas != null ? availableReplicas : 0)
                .available(available)
                .statusMessage(statusMessage)
                .build();
        } catch (KubernetesClientException e) {
            throw new KubernetesOperationException(
                String.format("Failed to get status for deployment %s in namespace %s", deploymentName, namespace), e);
        }
    }
}

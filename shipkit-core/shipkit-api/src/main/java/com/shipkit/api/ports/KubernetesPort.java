package com.shipkit.api.ports;

import java.time.Duration;
import java.util.Map;

/**
 * Port interface for Kubernetes operations following hexagonal architecture.
 * <p>
 * This port defines the contract for Kubernetes resource management without
 * exposing any external dependencies or implementation details.
 * </p>
 */
public interface KubernetesPort {

    /**
     * Creates a new Kubernetes namespace.
     *
     * @param namespaceName the name of the namespace to create
     * @throws com.shipkit.api.exception.KubernetesOperationException if namespace creation fails
     */
    void createNamespace(String namespaceName);

    /**
     * Deletes a Kubernetes namespace and all resources within it.
     *
     * @param namespaceName the name of the namespace to delete
     * @throws com.shipkit.api.exception.KubernetesOperationException if namespace deletion fails
     */
    void deleteNamespace(String namespaceName);

    /**
     * Applies or updates Kubernetes resources from YAML manifest using server-side apply.
     * <p>
     * Resources are created if they don't exist, or updated if they do.
     * The operation is idempotent and uses server-side apply for conflict resolution.
     * </p>
     * <p>
     * Applied resources will have the following labels automatically added:
     * <ul>
     *   <li>app=shipkit</li>
     *   <li>managed-by=shipkit</li>
     * </ul>
     * </p>
     *
     * @param namespace the target namespace for the resources
     * @param manifestYaml the YAML manifest containing the resource definitions
     * @return map of resource kind to resource name for applied resources
     * @throws com.shipkit.api.exception.KubernetesOperationException if apply operation fails
     */
    Map<String, String> applyManifest(String namespace, String manifestYaml);

    /**
     * Deletes a Kubernetes resource.
     *
     * @param namespace the namespace containing the resource
     * @param kind the kind of resource (e.g., "Deployment", "Service")
     * @param name the name of the resource
     * @throws com.shipkit.api.exception.KubernetesOperationException if deletion fails
     */
    void deleteResource(String namespace, String kind, String name);

    /**
     * Creates or updates a Kubernetes Service.
     *
     * @param namespace the target namespace
     * @param serviceName the name of the service
     * @param selector label selector for pods
     * @param port the service port
     * @param targetPort the target port on pods
     * @throws com.shipkit.api.exception.KubernetesOperationException if service creation fails
     */
    void createOrUpdateService(String namespace, String serviceName, Map<String, String> selector, int port, int targetPort);

    /**
     * Creates or updates a Kubernetes Ingress with optional TLS configuration.
     * <p>
     * The Ingress will be configured with:
     * <ul>
     *   <li>Specified ingress class name</li>
     *   <li>Host-based routing to the specified FQDN</li>
     *   <li>TLS configuration with cert-manager annotations if enabled</li>
     * </ul>
     * </p>
     *
     * @param namespace the target namespace
     * @param ingressName the name of the ingress
     * @param host the fully qualified domain name (FQDN)
     * @param serviceName the backend service name
     * @param servicePort the backend service port
     * @param ingressClassName the ingress class to use
     * @param tlsEnabled whether to enable TLS
     * @param clusterIssuer the cert-manager cluster issuer name (required if tlsEnabled is true)
     * @throws com.shipkit.api.exception.KubernetesOperationException if ingress creation fails
     */
    void createOrUpdateIngress(String namespace, String ingressName, String host, String serviceName, 
                                int servicePort, String ingressClassName, boolean tlsEnabled, String clusterIssuer);

    /**
     * Waits for a Deployment to become ready.
     * <p>
     * Monitors the Deployment's "Available" condition and waits until all replicas are ready.
     * Uses exponential backoff for polling.
     * </p>
     *
     * @param namespace the namespace containing the deployment
     * @param deploymentName the name of the deployment
     * @param timeout maximum time to wait for readiness
     * @return true if deployment became ready within timeout, false otherwise
     * @throws com.shipkit.api.exception.KubernetesOperationException if readiness check fails
     */
    boolean waitForDeploymentReady(String namespace, String deploymentName, Duration timeout);

    /**
     * Queries the status of a deployment.
     *
     * @param namespace the namespace containing the deployment
     * @param deploymentName the name of the deployment
     * @return deployment status information including ready replicas and conditions
     * @throws com.shipkit.api.exception.KubernetesOperationException if status query fails
     */
    DeploymentStatus getDeploymentStatus(String namespace, String deploymentName);

    /**
     * Represents the status of a Kubernetes Deployment.
     */
    interface DeploymentStatus {
        int replicas();
        int readyReplicas();
        int availableReplicas();
        boolean available();
        String statusMessage();
    }
}

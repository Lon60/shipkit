package com.shipkit.api.exception;

/**
 * Exception thrown when a Kubernetes operation fails.
 * <p>
 * This exception is thrown by implementations of {@link com.shipkit.api.ports.KubernetesPort}
 * when operations such as resource creation, updates, or deletions fail.
 * </p>
 */
public class KubernetesOperationException extends ShipkitException {

    public KubernetesOperationException(String message) {
        super(message);
    }

    public KubernetesOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

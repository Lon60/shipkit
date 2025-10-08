package com.shipkit.api.exception;

/**
 * Exception thrown when a readiness timeout occurs.
 */
public class ReadinessTimeoutException extends KubernetesOperationException {

    public ReadinessTimeoutException(String message) {
        super(message);
    }

    public ReadinessTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.shipkit.api.exception;

/**
 * Exception thrown when a resource is not found.
 */
public class ResourceNotFoundException extends ShipkitException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

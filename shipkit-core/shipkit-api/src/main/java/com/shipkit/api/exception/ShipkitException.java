package com.shipkit.api.exception;

/**
 * Base exception for all domain exceptions in the shipkit API.
 */
public class ShipkitException extends RuntimeException {

    public ShipkitException(String message) {
        super(message);
    }

    public ShipkitException(String message, Throwable cause) {
        super(message, cause);
    }
}

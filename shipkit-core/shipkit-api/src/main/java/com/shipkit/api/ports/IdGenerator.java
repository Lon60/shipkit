package com.shipkit.api.ports;

/**
 * Port interface for ID generation following hexagonal architecture.
 * <p>
 * This abstraction provides a consistent way to generate unique identifiers
 * across the application without coupling to a specific implementation.
 * </p>
 */
public interface IdGenerator {

    /**
     * Generates a new unique identifier.
     *
     * @return a unique identifier string
     */
    String generateId();

    /**
     * Generates a new UUID (Universally Unique Identifier).
     *
     * @return a UUID string in standard format
     */
    String generateUuid();
}

package com.shipkit.api.ports;

import java.time.Instant;

/**
 * Port interface for time operations following hexagonal architecture.
 * <p>
 * This abstraction allows for controlled time manipulation in tests
 * and provides a consistent way to access current time across the application.
 * </p>
 */
public interface TimeProvider {

    /**
     * Returns the current instant in UTC.
     *
     * @return the current instant
     */
    Instant now();

    /**
     * Returns the current time in milliseconds since epoch.
     *
     * @return current time in milliseconds
     */
    long currentTimeMillis();
}

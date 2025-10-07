package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.TimeProvider;

import java.time.Instant;

/**
 * Default implementation of {@link TimeProvider} using system time.
 */
public class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}

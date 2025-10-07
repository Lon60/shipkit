package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.IdGenerator;

import java.util.UUID;

/**
 * Default implementation of {@link IdGenerator} using UUID.
 */
public class UuidIdGenerator implements IdGenerator {

    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }
}

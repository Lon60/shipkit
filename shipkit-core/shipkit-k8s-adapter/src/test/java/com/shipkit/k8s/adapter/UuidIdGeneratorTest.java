package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.IdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidIdGeneratorTest {

    @Test
    void shouldGenerateValidUuid() {
        IdGenerator generator = new UuidIdGenerator();
        String id = generator.generateId();
        
        assertNotNull(id);
        assertFalse(id.isEmpty());
        
        // Verify it's a valid UUID format
        assertDoesNotThrow(() -> UUID.fromString(id));
    }

    @Test
    void shouldGenerateUniqueIds() {
        IdGenerator generator = new UuidIdGenerator();
        Set<String> ids = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String id = generator.generateId();
            assertTrue(ids.add(id), "Generated duplicate ID: " + id);
        }
    }

    @Test
    void shouldGenerateValidUuidString() {
        IdGenerator generator = new UuidIdGenerator();
        String uuid = generator.generateUuid();
        
        assertNotNull(uuid);
        assertFalse(uuid.isEmpty());
        
        // Verify it's a valid UUID format
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }
}

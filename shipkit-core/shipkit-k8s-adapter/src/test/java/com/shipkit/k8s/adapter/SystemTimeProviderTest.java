package com.shipkit.k8s.adapter;

import com.shipkit.api.ports.TimeProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SystemTimeProviderTest {

    @Test
    void shouldReturnCurrentTime() {
        TimeProvider provider = new SystemTimeProvider();
        
        Instant before = Instant.now();
        Instant actual = provider.now();
        Instant after = Instant.now();
        
        assertTrue(actual.equals(before) || actual.isAfter(before));
        assertTrue(actual.equals(after) || actual.isBefore(after));
    }

    @Test
    void shouldReturnCurrentTimeMillis() {
        TimeProvider provider = new SystemTimeProvider();
        
        long before = System.currentTimeMillis();
        long actual = provider.currentTimeMillis();
        long after = System.currentTimeMillis();
        
        assertTrue(actual >= before);
        assertTrue(actual <= after);
    }
}

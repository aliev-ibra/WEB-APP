package com.vizja.lab3.lib.http;


import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    @Getter
    private final String id;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final long maxInactiveIntervalSeconds;
    private Instant lastAccessed;

    public Session(long maxInactiveIntervalSeconds) {
        this.id = UUID.randomUUID().toString();
        this.lastAccessed = Instant.now();
        this.maxInactiveIntervalSeconds = maxInactiveIntervalSeconds;
    }

    public Object getAttribute(String name) {
        access();
        return data.get(name);
    }

    public void setAttribute(String name, Object value) {
        access();
        data.put(name, value);
    }

    public void removeAttribute(String name) {
        access();
        data.remove(name);
    }

    /**
     * Checks if the session has expired based on the last access time.
     * @return true if the session is expired, false otherwise.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(lastAccessed.plusSeconds(maxInactiveIntervalSeconds));
    }

    /**
     * Updates the last accessed timestamp to the current time.
     * This should be called whenever the session is retrieved or modified.
     */
    private void access() {
        this.lastAccessed = Instant.now();
    }
}
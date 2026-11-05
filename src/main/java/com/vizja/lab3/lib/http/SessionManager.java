package com.vizja.lab3.lib.http;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of all server sessions, including creation,
 * retrieval, and cleanup of expired sessions.
 */
public final class SessionManager {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static final long DEFAULT_SESSION_TIMEOUT_SECONDS = 1800; // 30 minutes
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();


    private SessionManager() {
    }

    /**
     * Creates a new session and stores it.
     * @return The newly created Session object.
     */
    public static Session createSession() {
        final Session session = new Session(DEFAULT_SESSION_TIMEOUT_SECONDS);
        SESSIONS.put(session.getId(), session);
        log.info("Created new session with ID: {}", session.getId());
        return session;
    }

    /**
     * Retrieves an existing session by its ID, if it exists and has not expired.
     * @param sessionId The ID of the session to retrieve.
     * @return An Optional containing the Session, or an empty Optional if not found or expired.
     */
    public static Optional<Session> getSession(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }
        Session session = SESSIONS.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                SESSIONS.remove(sessionId, session);
                log.info("Removed expired session during get: {}", sessionId);
                return Optional.empty();
            }
            return Optional.of(session);
        }
        return Optional.empty();
    }


}
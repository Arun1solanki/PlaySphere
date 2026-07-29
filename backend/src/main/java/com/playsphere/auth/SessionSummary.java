package com.playsphere.auth;

import java.time.Instant;

public record SessionSummary(
        String id,
        boolean current,
        String device,
        String ipAddress,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt
) {
    static SessionSummary from(RefreshToken session, String currentSessionId) {
        return new SessionSummary(
                session.getId(),
                session.getId().equals(currentSessionId),
                session.deviceLabel(),
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt()
        );
    }
}

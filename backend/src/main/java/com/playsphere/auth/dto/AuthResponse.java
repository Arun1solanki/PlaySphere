package com.playsphere.auth.dto;

public record AuthResponse(
        String accessToken,
        long accessExpiresInSeconds,
        String sessionId,
        UserSummary user
) {}

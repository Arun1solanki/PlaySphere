package com.playsphere.auth;

import jakarta.servlet.http.HttpServletRequest;

public record SessionRequestMetadata(String userAgent, String ipAddress) {
    public static SessionRequestMetadata from(HttpServletRequest request) {
        return new SessionRequestMetadata(
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        );
    }
}

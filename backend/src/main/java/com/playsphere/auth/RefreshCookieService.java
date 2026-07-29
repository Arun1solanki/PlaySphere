package com.playsphere.auth;

import com.playsphere.common.BusinessException;
import com.playsphere.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Stores refresh tokens in session-specific HttpOnly cookies.
 *
 * Each browser tab keeps only a non-secret session id in sessionStorage and sends
 * it using X-PlaySphere-Session. The server then selects the matching HttpOnly
 * cookie. This allows different accounts to remain signed in in different tabs
 * without exposing refresh tokens to JavaScript.
 */
@Component
public class RefreshCookieService {
    public static final String SESSION_HEADER = "X-PlaySphere-Session";
    private static final String COOKIE_PATH = "/api/auth";

    private final AppProperties properties;

    public RefreshCookieService(AppProperties properties) {
        this.properties = properties;
    }

    public String requireSessionId(String sessionId) {
        if (sessionId == null || !sessionId.matches("[0-9a-fA-F-]{36}")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No active browser-tab session");
        }
        return sessionId.toLowerCase(Locale.ROOT);
    }

    public String read(HttpServletRequest request, String sessionId) {
        String normalizedSessionId = requireSessionId(sessionId);
        if (request.getCookies() == null) return null;
        String expectedName = cookieName(normalizedSessionId);
        return Arrays.stream(request.getCookies())
                .filter(cookie -> expectedName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void write(HttpServletResponse response, String sessionId, String value, Duration maxAge) {
        String normalizedSessionId = requireSessionId(sessionId);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieName(normalizedSessionId), value)
                .httpOnly(true)
                .secure(properties.session().secureCookie())
                .sameSite(properties.session().sameSite())
                .path(COOKIE_PATH);
        if (maxAge != null) builder.maxAge(maxAge);
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clear(HttpServletResponse response, String sessionId) {
        if (sessionId == null || !sessionId.matches("[0-9a-fA-F-]{36}")) return;
        expire(response, cookieName(sessionId.toLowerCase(Locale.ROOT)));
    }

    public void clearAll(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null) return;
        String prefix = cookiePrefix();
        Arrays.stream(request.getCookies())
                .map(Cookie::getName)
                .filter(name -> name.startsWith(prefix))
                .distinct()
                .forEach(name -> expire(response, name));
    }

    private void expire(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie
                .from(name, "")
                .httpOnly(true)
                .secure(properties.session().secureCookie())
                .sameSite(properties.session().sameSite())
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String cookieName(String sessionId) {
        return cookiePrefix() + sessionId.replace("-", "");
    }

    private String cookiePrefix() {
        return properties.session().cookieName() + "_";
    }
}

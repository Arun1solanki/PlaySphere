package com.playsphere.auth;

import com.playsphere.common.BusinessException;
import com.playsphere.common.TokenHasher;
import com.playsphere.config.AppProperties;
import com.playsphere.user.AppUser;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(60);

    private final RefreshTokenRepository sessions;
    private final TokenHasher tokenHasher;
    private final SessionPolicy policy;
    private final AppProperties properties;

    public SessionService(
            RefreshTokenRepository sessions,
            TokenHasher tokenHasher,
            SessionPolicy policy,
            AppProperties properties
    ) {
        this.sessions = sessions;
        this.tokenHasher = tokenHasher;
        this.policy = policy;
        this.properties = properties;
    }

    @Transactional
    public CreatedSession create(AppUser user, SessionRequestMetadata metadata, boolean rememberMe) {
        String rawToken = tokenHasher.newRawToken();
        Instant expiresAt = rememberMe
                ? Instant.now().plus(properties.jwt().refreshDays(), ChronoUnit.DAYS)
                : Instant.now().plus(properties.session().regularLoginHours(), ChronoUnit.HOURS);
        RefreshToken session = sessions.save(new RefreshToken(
                user,
                tokenHasher.sha256(rawToken),
                expiresAt,
                metadata.userAgent(),
                metadata.ipAddress(),
                rememberMe
        ));
        Duration cookieMaxAge = rememberMe ? Duration.ofDays(properties.jwt().refreshDays()) : null;
        return new CreatedSession(session, rawToken, cookieMaxAge);
    }

    @Transactional
    public RefreshToken requireByRawToken(String rawToken, SessionRequestMetadata metadata) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "No active session");
        }
        RefreshToken session = sessions.findByTokenHash(tokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid session"));
        Instant now = Instant.now();
        if (!session.isUsable(now, policy.idleTimeout(session.getUser()))) {
            session.revoke();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Session expired");
        }
        session.touch(now, metadata.userAgent(), metadata.ipAddress());
        return session;
    }

    @Transactional
    public boolean validateAccess(String sessionId, String userId, SessionRequestMetadata metadata) {
        RefreshToken session = sessions.findByIdAndUser_Id(sessionId, userId).orElse(null);
        if (session == null) return false;
        Instant now = Instant.now();
        if (!session.isUsable(now, policy.idleTimeout(session.getUser()))) {
            session.revoke();
            return false;
        }
        if (Duration.between(session.getLastUsedAt(), now).compareTo(TOUCH_INTERVAL) >= 0) {
            session.touch(now, metadata.userAgent(), metadata.ipAddress());
        }
        return true;
    }


    @Transactional
    public boolean validateAccess(String sessionId, String userId) {
        RefreshToken session = sessions.findByIdAndUser_Id(sessionId, userId).orElse(null);
        if (session == null) return false;
        Instant now = Instant.now();
        if (!session.isUsable(now, policy.idleTimeout(session.getUser()))) {
            session.revoke();
            return false;
        }
        if (Duration.between(session.getLastUsedAt(), now).compareTo(TOUCH_INTERVAL) >= 0) {
            session.touch(now, session.getUserAgent(), session.getIpAddress());
        }
        return true;
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHash(tokenHasher.sha256(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revoke(String userId, String sessionId) {
        RefreshToken session = sessions.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Session not found"));
        session.revoke();
    }

    @Transactional
    public void revokeAll(String userId) {
        sessions.findAllByUser_IdAndRevokedAtIsNullOrderByLastUsedAtDesc(userId)
                .forEach(RefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> list(String userId, String currentSessionId) {
        return sessions.findAllByUser_IdAndRevokedAtIsNullOrderByLastUsedAtDesc(userId).stream()
                .filter(session -> session.isUsable(Instant.now(), policy.idleTimeout(session.getUser())))
                .map(session -> SessionSummary.from(session, currentSessionId))
                .toList();
    }

    public record CreatedSession(RefreshToken session, String rawToken, Duration cookieMaxAge) {}
}

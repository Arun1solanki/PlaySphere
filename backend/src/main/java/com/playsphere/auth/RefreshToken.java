package com.playsphere.auth;

import com.playsphere.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "persistent_login", nullable = false)
    private boolean persistentLogin;

    protected RefreshToken() {}

    public RefreshToken(
            AppUser user,
            String tokenHash,
            Instant expiresAt,
            String userAgent,
            String ipAddress,
            boolean persistentLogin
    ) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.userAgent = truncate(userAgent, 255);
        this.ipAddress = truncate(ipAddress, 64);
        this.persistentLogin = persistentLogin;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = now;
        lastUsedAt = now;
    }

    public String getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }
    public boolean isPersistentLogin() { return persistentLogin; }

    public boolean isUsable(Instant now, Duration idleTimeout) {
        return revokedAt == null
                && expiresAt.isAfter(now)
                && lastUsedAt.plus(idleTimeout).isAfter(now);
    }

    public void touch(Instant now, String userAgent, String ipAddress) {
        this.lastUsedAt = now;
        this.userAgent = truncate(userAgent, 255);
        this.ipAddress = truncate(ipAddress, 64);
    }

    public void revoke() {
        if (revokedAt == null) revokedAt = Instant.now();
    }

    public String deviceLabel() {
        if (userAgent == null || userAgent.isBlank()) return "Unknown browser";
        String browser = userAgent.contains("Edg/") ? "Microsoft Edge"
                : userAgent.contains("Chrome/") ? "Google Chrome"
                : userAgent.contains("Firefox/") ? "Mozilla Firefox"
                : userAgent.contains("Safari/") ? "Safari"
                : "Web browser";
        String os = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS"
                : userAgent.contains("Mac OS") ? "macOS"
                : userAgent.contains("Linux") ? "Linux"
                : "Unknown device";
        return browser + " on " + os;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}

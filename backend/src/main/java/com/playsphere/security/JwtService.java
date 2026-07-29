package com.playsphere.security;

import com.playsphere.config.AppProperties;
import com.playsphere.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessMinutes;

    public JwtService(AppProperties properties) {
        byte[] secret = Decoders.BASE64.decode(properties.jwt().secretBase64());
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessMinutes = properties.jwt().accessMinutes();
    }

    public String createAccessToken(AppUser user, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(accessMinutes, ChronoUnit.MINUTES);
        List<String> roles = user.getRoles().stream().map(Enum::name).sorted().toList();

        return Jwts.builder()
                .subject(user.getId())
                .claim("sid", sessionId)
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractSessionId(String token) {
        return parseClaims(token).get("sid", String.class);
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date())
                    && claims.getSubject() != null
                    && claims.get("sid", String.class) != null;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public long accessExpiresInSeconds() {
        return accessMinutes * 60;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

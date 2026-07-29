package com.playsphere.auth;

import com.playsphere.auth.dto.AuthResponse;
import com.playsphere.auth.dto.LoginRequest;
import com.playsphere.auth.dto.RegisterRequest;
import com.playsphere.auth.dto.UserSummary;
import com.playsphere.common.BusinessException;
import com.playsphere.common.TokenHasher;
import com.playsphere.config.AppProperties;
import com.playsphere.integration.email.TransactionalEmailSender;
import com.playsphere.security.JwtService;
import com.playsphere.user.AccountStatus;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import com.playsphere.user.PlatformRole;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final TransactionalEmailSender emailSender;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final AppProperties properties;

    public AuthService(
            AppUserRepository users,
            EmailVerificationTokenRepository verificationTokens,
            PasswordResetTokenRepository passwordResetTokens,
            PasswordEncoder passwordEncoder,
            TokenHasher tokenHasher,
            TransactionalEmailSender emailSender,
            JwtService jwtService,
            SessionService sessionService,
            AppProperties properties
    ) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.passwordResetTokens = passwordResetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.emailSender = emailSender;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    @Transactional
    public void register(RegisterRequest request) {
        PlatformRole role = request.role();
        if (role == PlatformRole.ADMIN || role == PlatformRole.SUPER_ADMIN) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Admin accounts cannot be created through public registration");
        }

        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        AppUser user = new AppUser(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                role
        );
        users.save(user);
        issueAndSendVerification(user);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = verificationTokens.findByTokenHash(tokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Invalid verification link"));

        if (token.getUsedAt() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "This verification link has already been used");
        }
        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Verification link has expired");
        }

        token.getUser().verifyEmail();
        token.markUsed();
    }

    @Transactional
    public void resendVerification(String emailValue) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(emailValue)).orElse(null);
        if (user == null || user.isEmailVerified()) return;

        verificationTokens.findTopByUser_IdOrderByCreatedAtDesc(user.getId()).ifPresent(latest -> {
            long elapsed = Duration.between(latest.getCreatedAt(), Instant.now()).getSeconds();
            if (elapsed < properties.email().resendCooldownSeconds()) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another verification email");
            }
        });

        issueAndSendVerification(user);
    }

    @Transactional
    public void requestPasswordReset(String emailValue) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(emailValue)).orElse(null);
        if (user == null || !user.isEmailVerified() || user.getStatus() == AccountStatus.DELETED) return;

        passwordResetTokens.findTopByUser_IdOrderByCreatedAtDesc(user.getId()).ifPresent(latest -> {
            long elapsed = Duration.between(latest.getCreatedAt(), Instant.now()).getSeconds();
            if (elapsed < properties.email().resendCooldownSeconds()) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another password-reset email");
            }
        });

        passwordResetTokens.deleteAllByUser_Id(user.getId());
        String rawToken = tokenHasher.newRawToken();
        Instant expiresAt = Instant.now().plus(properties.email().passwordResetMinutes(), ChronoUnit.MINUTES);
        passwordResetTokens.save(new PasswordResetToken(user, tokenHasher.sha256(rawToken), expiresAt));

        String resetUrl = properties.frontendUrl() + "/reset-password?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailSender.sendPasswordResetEmail(user.getEmail(), user.getDisplayName(), resetUrl);
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokens.findByTokenHash(tokenHasher.sha256(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Invalid password-reset link"));
        if (token.getUsedAt() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "This password-reset link has already been used");
        }
        if (!token.getExpiresAt().isAfter(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Password-reset link has expired");
        }

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed();
        sessionService.revokeAll(user.getId());
    }

    @Transactional
    public AuthSessionResult login(LoginRequest request, SessionRequestMetadata metadata) {
        AppUser user = users.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        ensureEligible(user);

        SessionService.CreatedSession created = sessionService.create(user, metadata, request.rememberMe());
        return result(user, created.session(), created.rawToken(), created.cookieMaxAge());
    }

    @Transactional
    public AuthSessionResult refresh(
            String rawRefreshToken,
            SessionRequestMetadata metadata,
            String expectedSessionId
    ) {
        RefreshToken session = sessionService.requireByRawToken(rawRefreshToken, metadata);
        if (!session.getId().equals(expectedSessionId)) {
            session.revoke();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Session mismatch");
        }
        AppUser user = session.getUser();
        ensureEligible(user);
        return result(user, session, rawRefreshToken, session.isPersistentLogin()
                ? Duration.ofDays(properties.jwt().refreshDays())
                : null);
    }

    private AuthSessionResult result(
            AppUser user,
            RefreshToken session,
            String rawRefreshToken,
            Duration cookieMaxAge
    ) {
        AuthResponse response = new AuthResponse(
                jwtService.createAccessToken(user, session.getId()),
                jwtService.accessExpiresInSeconds(),
                session.getId(),
                UserSummary.from(user)
        );
        return new AuthSessionResult(response, rawRefreshToken, cookieMaxAge);
    }

    private void ensureEligible(AppUser user) {
        if (!user.isEmailVerified()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "This account is not active");
        }
    }

    private void issueAndSendVerification(AppUser user) {
        verificationTokens.deleteAllByUser_Id(user.getId());
        String rawToken = tokenHasher.newRawToken();
        Instant expiresAt = Instant.now().plus(properties.email().verificationMinutes(), ChronoUnit.MINUTES);
        verificationTokens.save(new EmailVerificationToken(user, tokenHasher.sha256(rawToken), expiresAt));

        String verificationUrl = properties.frontendUrl() + "/verify-email?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailSender.sendVerificationEmail(user.getEmail(), user.getDisplayName(), verificationUrl);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package com.playsphere.auth;

import com.playsphere.auth.dto.AuthResponse;
import com.playsphere.auth.dto.LoginRequest;
import com.playsphere.auth.dto.PasswordResetConfirmRequest;
import com.playsphere.auth.dto.PasswordResetRequest;
import com.playsphere.auth.dto.RegisterRequest;
import com.playsphere.auth.dto.ResendVerificationRequest;
import com.playsphere.auth.dto.UserSummary;
import com.playsphere.auth.dto.VerifyEmailRequest;
import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionService sessionService;
    private final RefreshCookieService cookies;
    private final CurrentUserService currentUser;

    public AuthController(
            AuthService authService,
            SessionService sessionService,
            RefreshCookieService cookies,
            CurrentUserService currentUser
    ) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.cookies = cookies;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. Check your email for the verification link."));
    }

    @PostMapping("/verify-email")
    ApiResponse<Void> verify(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ApiResponse.ok("Email verified successfully");
    }

    @PostMapping("/resend-verification")
    ApiResponse<Void> resend(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return ApiResponse.ok("If the account is eligible, a new verification email has been sent");
    }

    @PostMapping("/password-reset/request")
    ApiResponse<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        return ApiResponse.ok("If the address belongs to an eligible account, a password-reset email has been sent");
    }

    @PostMapping("/password-reset/confirm")
    ApiResponse<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        authService.confirmPasswordReset(request.token(), request.newPassword());
        cookies.clearAll(httpRequest, response);
        return ApiResponse.ok("Password changed successfully. Please log in again.");
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthSessionResult result = authService.login(request, SessionRequestMetadata.from(httpRequest));
        cookies.write(
                httpResponse,
                result.response().sessionId(),
                result.refreshToken(),
                result.cookieMaxAge()
        );
        return ApiResponse.ok("Login successful", result.response());
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(
            @RequestHeader(name = RefreshCookieService.SESSION_HEADER, required = false) String sessionId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String normalizedSessionId = cookies.requireSessionId(sessionId);
        String rawRefreshToken = cookies.read(request, normalizedSessionId);
        AuthSessionResult result = authService.refresh(
                rawRefreshToken,
                SessionRequestMetadata.from(request),
                normalizedSessionId
        );
        cookies.write(response, normalizedSessionId, result.refreshToken(), result.cookieMaxAge());
        return ApiResponse.ok("Session refreshed", result.response());
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @RequestHeader(name = RefreshCookieService.SESSION_HEADER, required = false) String sessionId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (sessionId != null && sessionId.matches("[0-9a-fA-F-]{36}")) {
            sessionService.revokeByRawToken(cookies.read(request, sessionId));
            cookies.clear(response, sessionId);
        }
        return ApiResponse.ok("Logged out successfully");
    }

    @PostMapping("/logout-all")
    ApiResponse<Void> logoutAll(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        sessionService.revokeAll(currentUser.require(authentication).getId());
        cookies.clearAll(request, response);
        return ApiResponse.ok("Logged out from every device");
    }

    @GetMapping("/me")
    ApiResponse<UserSummary> me(Authentication authentication) {
        return ApiResponse.ok("Current user", UserSummary.from(currentUser.require(authentication)));
    }
}

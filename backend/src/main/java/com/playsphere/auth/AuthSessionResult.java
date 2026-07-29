package com.playsphere.auth;

import com.playsphere.auth.dto.AuthResponse;
import java.time.Duration;

public record AuthSessionResult(
        AuthResponse response,
        String refreshToken,
        Duration cookieMaxAge
) {}

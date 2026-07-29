package com.playsphere.auth.dto;

import com.playsphere.user.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 80) String displayName,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                message = "must include uppercase, lowercase, number, and special character")
        String password,
        @NotNull PlatformRole role
) {}

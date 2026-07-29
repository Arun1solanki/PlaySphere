package com.playsphere.auth.dto;

import com.playsphere.user.AppUser;
import com.playsphere.user.PlatformRole;
import java.util.Set;

public record UserSummary(
        String id,
        String email,
        String displayName,
        Set<PlatformRole> roles,
        boolean emailVerified,
        boolean profileCompleted
) {
    public static UserSummary from(AppUser user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                Set.copyOf(user.getRoles()),
                user.isEmailVerified(),
                user.isProfileCompleted()
        );
    }
}

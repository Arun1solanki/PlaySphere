package com.playsphere.user;

import com.playsphere.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AppUser require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return users.findById(authentication.getName())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}

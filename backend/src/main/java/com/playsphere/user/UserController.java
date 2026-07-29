package com.playsphere.user;

import com.playsphere.auth.dto.UserSummary;
import com.playsphere.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserService currentUser;

    public UserController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    ApiResponse<UserSummary> me(Authentication authentication) {
        return ApiResponse.ok("Current user", UserSummary.from(currentUser.require(authentication)));
    }
}

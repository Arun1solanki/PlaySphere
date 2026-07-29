package com.playsphere.profile;

import com.playsphere.common.ApiResponse;
import com.playsphere.profile.dto.PlayerDiscoveryView;
import com.playsphere.profile.dto.ProfileResponse;
import com.playsphere.profile.dto.ProfileUpsertRequest;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final CurrentUserService currentUser;
    private final ProfileService profiles;

    public ProfileController(CurrentUserService currentUser, ProfileService profiles) {
        this.currentUser = currentUser;
        this.profiles = profiles;
    }

    @GetMapping("/players")
    ApiResponse<List<PlayerDiscoveryView>> players(
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String sport
    ) {
        return ApiResponse.ok("Players", profiles.discoverPlayers(query, city, sport));
    }

    @GetMapping
    ApiResponse<ProfileResponse> get(Authentication authentication) {
        return ApiResponse.ok("Profile", profiles.get(currentUser.require(authentication)));
    }

    @PutMapping
    ApiResponse<ProfileResponse> upsert(
            Authentication authentication,
            @Valid @RequestBody ProfileUpsertRequest request
    ) {
        return ApiResponse.ok("Profile saved", profiles.upsert(currentUser.require(authentication), request));
    }
}

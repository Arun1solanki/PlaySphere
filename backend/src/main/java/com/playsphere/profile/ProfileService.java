package com.playsphere.profile;

import com.playsphere.common.BusinessException;
import com.playsphere.media.MediaOwnershipService;
import com.playsphere.profile.dto.PlayerDiscoveryView;
import com.playsphere.profile.dto.ProfileResponse;
import com.playsphere.profile.dto.ProfileUpsertRequest;
import com.playsphere.user.AppUser;
import com.playsphere.user.PlatformRole;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserProfileRepository profiles;
    private final MediaOwnershipService mediaOwnership;

    public ProfileService(
            UserProfileRepository profiles,
            MediaOwnershipService mediaOwnership
    ) {
        this.profiles = profiles;
        this.mediaOwnership = mediaOwnership;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(AppUser user) {
        return profiles.findByUser_Id(user.getId())
                .map(ProfileResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Profile has not been created"));
    }

    @Transactional(readOnly = true)
    public List<PlayerDiscoveryView> discoverPlayers(String query, String city, String sport) {
        String normalizedQuery = normalize(query);
        String normalizedCity = normalize(city);
        String normalizedSport = normalize(sport);
        return profiles.findByDiscoverableTrueOrderByFullNameAsc().stream()
                .filter(profile -> profile.getUser().getStatus() == com.playsphere.user.AccountStatus.ACTIVE)
                .filter(profile -> profile.getUser().getRoles().contains(PlatformRole.PLAYER))
                .filter(profile -> normalizedCity == null || contains(profile.getCity(), normalizedCity))
                .filter(profile -> normalizedSport == null || contains(profile.getPreferredSports(), normalizedSport))
                .filter(profile -> normalizedQuery == null
                        || contains(profile.getFullName(), normalizedQuery)
                        || contains(profile.getCity(), normalizedQuery)
                        || contains(profile.getLocality(), normalizedQuery)
                        || contains(profile.getPreferredSports(), normalizedQuery)
                        || contains(profile.getPlayingPosition(), normalizedQuery))
                .limit(100)
                .map(PlayerDiscoveryView::from)
                .toList();
    }

    @Transactional
    public ProfileResponse upsert(AppUser user, ProfileUpsertRequest request) {
        validateRoleSpecificFields(user.getRoles(), request);

        UserProfile profile = profiles.findByUser_Id(user.getId())
                .orElseGet(() -> new UserProfile(user));

        String profileImageUrl = request.profileImageUrl() == null || request.profileImageUrl().isBlank()
                ? profile.getProfileImageUrl()
                : mediaOwnership.requireOwnedPurpose(user.getId(), request.profileImageUrl(), "profiles");

        profile.update(
                request.fullName().trim(),
                request.phoneNumber().trim(),
                request.city().trim(),
                request.locality().trim(),
                trimToNull(request.locationDescription()),
                trimToNull(request.bio()),
                profileImageUrl,
                trimToNull(request.preferredSports()),
                trimToNull(request.skillLevel()),
                trimToNull(request.playingPosition()),
                trimToNull(request.availabilitySummary()),
                trimToNull(request.organizationName()),
                trimToNull(request.businessName()),
                request.discoverable()
        );
        profiles.save(profile);
        user.markProfileCompleted(request.fullName().trim());
        return ProfileResponse.from(profile);
    }

    private void validateRoleSpecificFields(Set<PlatformRole> roles, ProfileUpsertRequest request) {
        if (roles.contains(PlatformRole.PLAYER)) {
            requireText(request.preferredSports(), "Preferred sports are required for a Player profile");
            requireText(request.skillLevel(), "Skill level is required for a Player profile");
        }
        if (roles.contains(PlatformRole.ORGANIZER)) {
            requireText(request.organizationName(), "Organization name is required for an Organizer profile");
        }
        if (roles.contains(PlatformRole.TURF_OWNER)) {
            requireText(request.businessName(), "Business name is required for a Turf Owner profile");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}

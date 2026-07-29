package com.playsphere.profile.dto;

import com.playsphere.profile.UserProfile;

public record PlayerDiscoveryView(
        String userId,
        String fullName,
        String profileImageUrl,
        String city,
        String locality,
        String preferredSports,
        String skillLevel,
        String playingPosition,
        String availabilitySummary,
        String bio
) {
    public static PlayerDiscoveryView from(UserProfile profile) {
        return new PlayerDiscoveryView(
                profile.getUser().getId(),
                profile.getFullName(),
                profile.getProfileImageUrl(),
                profile.getCity(),
                profile.getLocality(),
                profile.getPreferredSports(),
                profile.getSkillLevel(),
                profile.getPlayingPosition(),
                profile.getAvailabilitySummary(),
                profile.getBio()
        );
    }
}

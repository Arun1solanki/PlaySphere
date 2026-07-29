package com.playsphere.profile.dto;

import com.playsphere.profile.UserProfile;

public record ProfileResponse(
        String id,
        String fullName,
        String phoneNumber,
        String city,
        String locality,
        String locationDescription,
        String bio,
        String profileImageUrl,
        String preferredSports,
        String skillLevel,
        String playingPosition,
        String availabilitySummary,
        String organizationName,
        String businessName,
        boolean discoverable
) {
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.getId(), profile.getFullName(), profile.getPhoneNumber(),
                profile.getCity(), profile.getLocality(), profile.getLocationDescription(),
                profile.getBio(), profile.getProfileImageUrl(), profile.getPreferredSports(),
                profile.getSkillLevel(), profile.getPlayingPosition(), profile.getAvailabilitySummary(),
                profile.getOrganizationName(), profile.getBusinessName(), profile.isDiscoverable()
        );
    }
}

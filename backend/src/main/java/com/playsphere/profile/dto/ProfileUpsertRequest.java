package com.playsphere.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpsertRequest(
        @NotBlank @Size(min = 2, max = 80) String fullName,
        @NotBlank
        @Pattern(regexp = "^\\+91[6-9]\\d{9}$", message = "must start with +91 and contain a valid 10-digit Indian mobile number")
        String phoneNumber,
        @NotBlank @Size(min = 2, max = 80) String city,
        @NotBlank @Size(min = 2, max = 80) String locality,
        @Size(max = 180) String locationDescription,
        @Size(max = 500) String bio,
        @Size(max = 500) String profileImageUrl,
        @Size(max = 300) String preferredSports,
        @Size(max = 32) String skillLevel,
        @Size(max = 80) String playingPosition,
        @Size(max = 180) String availabilitySummary,
        @Size(max = 140) String organizationName,
        @Size(max = 140) String businessName,
        boolean discoverable
) {}

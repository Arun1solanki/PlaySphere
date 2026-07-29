package com.playsphere.team;

public record TeamPlayerSummary(
        String id,
        String displayName,
        String profileImageUrl,
        String city,
        String locality,
        String skillLevel,
        String playingPosition
) {}

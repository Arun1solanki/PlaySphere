package com.playsphere.team;

import java.time.Instant;
import java.util.List;

public record TeamView(
        String id,
        String captainUserId,
        String name,
        String sport,
        String city,
        String locality,
        String skillLevel,
        String description,
        String logoUrl,
        int maxMembers,
        String visibility,
        String joinMode,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long memberCount,
        TeamPlayerSummary captain,
        List<TeamMemberView> members
) {}

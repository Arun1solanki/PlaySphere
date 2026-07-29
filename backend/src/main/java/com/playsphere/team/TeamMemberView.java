package com.playsphere.team;

import java.time.Instant;

public record TeamMemberView(
        String id,
        String userId,
        String memberRole,
        Instant joinedAt,
        TeamPlayerSummary user
) {}

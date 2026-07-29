package com.playsphere.team;

import java.time.Instant;

public record TeamJoinRequestView(
        String id,
        String teamId,
        String applicantUserId,
        String message,
        String status,
        String decidedByUserId,
        Instant createdAt,
        Instant decidedAt,
        TeamPlayerSummary applicant
) {}

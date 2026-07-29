package com.playsphere.team;

import java.time.Instant;

public record RecruitmentApplicationView(
        String id,
        String postId,
        String applicantUserId,
        String message,
        String status,
        Instant createdAt,
        Instant decidedAt,
        TeamPlayerSummary applicant
) {}

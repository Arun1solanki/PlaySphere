package com.playsphere.dashboard;

import java.util.List;

public record DashboardResponse(
        String role,
        String title,
        String subtitle,
        String accent,
        List<DashboardCard> cards,
        List<String> primaryActions
) {}

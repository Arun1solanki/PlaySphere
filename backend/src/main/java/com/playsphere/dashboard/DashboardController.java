package com.playsphere.dashboard;

import com.playsphere.common.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/player")
    @PreAuthorize("hasAnyRole('PLAYER','ADMIN','SUPER_ADMIN')")
    ApiResponse<DashboardResponse> player() {
        return ApiResponse.ok("Player dashboard", new DashboardResponse(
                "PLAYER", "Ready for your next game?", "Discover, join, book, and build your team.", "cyan",
                List.of(
                        new DashboardCard("Upcoming bookings", "0", "Turf reservations"),
                        new DashboardCard("Team requests", "0", "Waiting for action"),
                        new DashboardCard("Events joined", "0", "This month")
                ),
                List.of("Find a turf", "Explore teams", "Create team", "Need players")
        ));
    }

    @GetMapping("/organizer")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN','SUPER_ADMIN')")
    ApiResponse<DashboardResponse> organizer() {
        return ApiResponse.ok("Organizer dashboard", new DashboardResponse(
                "ORGANIZER", "Turn plans into packed arenas", "Manage events, participants, fixtures, and results.", "violet",
                List.of(
                        new DashboardCard("Active events", "0", "Published now"),
                        new DashboardCard("Pending registrations", "0", "Need review"),
                        new DashboardCard("Event revenue", "₹0", "Current month")
                ),
                List.of("Create event", "Manage registrations", "Book venue", "Publish results")
        ));
    }

    @GetMapping("/turf-owner")
    @PreAuthorize("hasAnyRole('TURF_OWNER','ADMIN','SUPER_ADMIN')")
    ApiResponse<DashboardResponse> turfOwner() {
        return ApiResponse.ok("Turf Owner dashboard", new DashboardResponse(
                "TURF_OWNER", "Your venues, always in play", "Control availability, bookings, pricing, and check-ins.", "emerald",
                List.of(
                        new DashboardCard("Today's bookings", "0", "Across all venues"),
                        new DashboardCard("Available slots", "0", "Today"),
                        new DashboardCard("Monthly earnings", "₹0", "Verified payments")
                ),
                List.of("Add turf", "Create slots", "Block time", "Verify check-in")
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    ApiResponse<DashboardResponse> admin() {
        return ApiResponse.ok("Admin dashboard", new DashboardResponse(
                "ADMIN", "Platform control center", "Moderate, approve, investigate, and protect the community.", "amber",
                List.of(
                        new DashboardCard("Pending approvals", "0", "Owners and turfs"),
                        new DashboardCard("Open reports", "0", "Need review"),
                        new DashboardCard("Refund queue", "0", "Awaiting action")
                ),
                List.of("Review approvals", "Handle reports", "Process refunds", "Audit activity")
        ));
    }
}

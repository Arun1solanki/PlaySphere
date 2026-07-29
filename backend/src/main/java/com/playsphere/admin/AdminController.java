package com.playsphere.admin;

import com.playsphere.audit.AuditLog;
import com.playsphere.audit.AuditService;
import com.playsphere.common.ApiResponse;
import com.playsphere.common.BusinessException;
import com.playsphere.event.SportsEvent;
import com.playsphere.event.SportsEventRepository;
import com.playsphere.payment.Payment;
import com.playsphere.payment.PaymentRepository;
import com.playsphere.review.Review;
import com.playsphere.review.ReviewRepository;
import com.playsphere.team.Team;
import com.playsphere.team.TeamRepository;
import com.playsphere.turf.Booking;
import com.playsphere.turf.BookingRepository;
import com.playsphere.turf.Turf;
import com.playsphere.turf.TurfRepository;
import com.playsphere.user.AccountStatus;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {
    private final AppUserRepository users;
    private final TeamRepository teams;
    private final TurfRepository turfs;
    private final SportsEventRepository events;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final ReviewRepository reviews;
    private final AuditService audit;
    private final CurrentUserService current;

    public AdminController(
            AppUserRepository users,
            TeamRepository teams,
            TurfRepository turfs,
            SportsEventRepository events,
            BookingRepository bookings,
            PaymentRepository payments,
            ReviewRepository reviews,
            AuditService audit,
            CurrentUserService current
    ) {
        this.users = users;
        this.teams = teams;
        this.turfs = turfs;
        this.events = events;
        this.bookings = bookings;
        this.payments = payments;
        this.reviews = reviews;
        this.audit = audit;
        this.current = current;
    }

    public record StatusRequest(
            @Pattern(regexp = "ACTIVE|SUSPENDED|BLOCKED") String status
    ) {}

    @GetMapping("/users")
    public ApiResponse<List<AppUser>> users() {
        return ApiResponse.ok("Users", users.findAll());
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<AppUser> status(
            @PathVariable String id,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication
    ) {
        AppUser actor = current.require(authentication);
        if (actor.getId().equals(id) && !"ACTIVE".equals(request.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "You cannot suspend your own account");
        }
        AppUser user = users.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        user.setStatus(AccountStatus.valueOf(request.status()));
        users.save(user);
        audit.record(actor.getId(), "USER_STATUS_CHANGED", "USER", id, request.status());
        return ApiResponse.ok("User status updated", user);
    }

    @GetMapping("/teams")
    public ApiResponse<List<Team>> teams() {
        return ApiResponse.ok("Teams", teams.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/turfs")
    public ApiResponse<List<Turf>> turfs() {
        return ApiResponse.ok("Turfs", turfs.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/events")
    public ApiResponse<List<SportsEvent>> events() {
        return ApiResponse.ok("Events", events.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/bookings")
    public ApiResponse<List<Booking>> bookings() {
        return ApiResponse.ok("Bookings", bookings.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/payments")
    public ApiResponse<List<Payment>> payments() {
        return ApiResponse.ok("Payments", payments.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/reviews")
    public ApiResponse<List<Review>> reviews() {
        return ApiResponse.ok("Reviews", reviews.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<AuditLog>> logs() {
        return ApiResponse.ok("Audit logs", audit.recent());
    }
}

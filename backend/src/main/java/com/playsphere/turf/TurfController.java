package com.playsphere.turf;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TurfController {
    private final TurfService service;
    private final CurrentUserService current;

    public TurfController(TurfService service, CurrentUserService current) {
        this.service = service;
        this.current = current;
    }

    public record CreateTurfRequest(
            @NotBlank @Size(max = 140) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 220) String addressLine,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String locality,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
            @NotBlank @Size(max = 300) String sports,
            @Size(max = 700) String amenities,
            @NotNull @DecimalMin("0.00") BigDecimal basePrice,
            @Size(max = 700) String coverImageUrl
    ) {}

    public record CreateSlotRequest(
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            @NotNull @DecimalMin("0.00") BigDecimal price
    ) {}

    public record GenerateSlotsRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull LocalTime openingTime,
            @NotNull LocalTime closingTime,
            @Min(30) @Max(240) int slotMinutes,
            @NotNull @DecimalMin("0.00") BigDecimal price
    ) {}

    public record CancelRequest(@NotBlank @Size(max = 400) String reason) {}
    public record CheckInRequest(@NotBlank String bookingCode, @NotBlank String qrToken) {}
    public record CreateEquipmentRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @Min(0) int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerBooking
    ) {}
    public record UpdateEquipmentRequest(
            @Min(0) int quantity,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerBooking,
            boolean active
    ) {}
    public record SlotStatusRequest(@Pattern(regexp = "AVAILABLE|BLOCKED") String status) {}
    public record DecisionRequest(boolean approve, @Size(max = 500) String reason) {}

    @GetMapping("/turfs")
    public ApiResponse<List<Turf>> discover() {
        return ApiResponse.ok("Approved turfs", service.discover());
    }

    @GetMapping("/turfs/nearby")
    public ApiResponse<List<NearbyTurfView>> nearby(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "15") @DecimalMin("1") @DecimalMax("100") double radiusKm
    ) {
        return ApiResponse.ok("Nearby turfs", service.nearby(latitude, longitude, radiusKm));
    }

    @GetMapping("/turfs/mine")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<List<Turf>> mine(Authentication authentication) {
        return ApiResponse.ok("My turfs", service.mine(current.require(authentication).getId()));
    }

    @PostMapping("/turfs")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<Turf> create(
            @Valid @RequestBody CreateTurfRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Turf submitted for approval. You will receive a notification after Admin review.",
                service.create(current.require(authentication).getId(), request)
        );
    }

    @GetMapping("/turfs/{id}/slots")
    public ApiResponse<List<TurfSlot>> slots(@PathVariable String id) {
        return ApiResponse.ok("Available slots", service.availableSlots(id));
    }

    @PostMapping("/turfs/{id}/slots")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<TurfSlot> createSlot(
            @PathVariable String id,
            @Valid @RequestBody CreateSlotRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Slot created",
                service.createSlot(current.require(authentication).getId(), id, request)
        );
    }

    @PostMapping("/turfs/{id}/slots/generate")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<List<TurfSlot>> generateSlots(
            @PathVariable String id,
            @Valid @RequestBody GenerateSlotsRequest request,
            Authentication authentication
    ) {
        List<TurfSlot> created = service.generateSlots(
                current.require(authentication).getId(),
                id,
                request
        );
        return ApiResponse.ok(created.size() + " availability slots created", created);
    }

    @GetMapping("/turfs/{id}/slots/manage")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<List<TurfSlot>> manageSlots(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Managed slots",
                service.ownerSlots(current.require(authentication).getId(), id)
        );
    }

    @PatchMapping("/turfs/{turfId}/slots/{slotId}/status")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<TurfSlot> slotStatus(
            @PathVariable String turfId,
            @PathVariable String slotId,
            @Valid @RequestBody SlotStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Slot status updated",
                service.slotStatus(
                        current.require(authentication).getId(),
                        turfId,
                        slotId,
                        request.status()
                )
        );
    }

    @GetMapping("/turfs/{id}/equipment")
    public ApiResponse<List<TurfEquipment>> equipment(@PathVariable String id) {
        return ApiResponse.ok("Turf equipment", service.equipment(id, false, null));
    }

    @GetMapping("/turfs/{id}/equipment/manage")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<List<TurfEquipment>> manageEquipment(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Managed equipment",
                service.equipment(id, true, current.require(authentication).getId())
        );
    }

    @PostMapping("/turfs/{id}/equipment")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<TurfEquipment> addEquipment(
            @PathVariable String id,
            @Valid @RequestBody CreateEquipmentRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Equipment created",
                service.addEquipment(current.require(authentication).getId(), id, request)
        );
    }

    @PatchMapping("/turfs/{turfId}/equipment/{equipmentId}")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<TurfEquipment> updateEquipment(
            @PathVariable String turfId,
            @PathVariable String equipmentId,
            @Valid @RequestBody UpdateEquipmentRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Equipment updated",
                service.updateEquipment(
                        current.require(authentication).getId(),
                        turfId,
                        equipmentId,
                        request
                )
        );
    }

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Booking> book(
            @RequestParam String slotId,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Booking created. Complete payment to confirm.",
                service.book(current.require(authentication).getId(), slotId)
        );
    }

    @GetMapping("/bookings/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<Booking>> myBookings(Authentication authentication) {
        return ApiResponse.ok(
                "My bookings",
                service.playerBookings(current.require(authentication).getId())
        );
    }

    @GetMapping("/turfs/{id}/bookings")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<List<Booking>> ownerBookings(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Turf bookings",
                service.ownerBookings(current.require(authentication).getId(), id)
        );
    }

    @PostMapping("/bookings/{id}/qr")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Booking> qr(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "QR token generated",
                service.qr(current.require(authentication).getId(), id)
        );
    }

    @PatchMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelRequest request,
            Authentication authentication
    ) {
        service.cancel(current.require(authentication).getId(), id, request.reason());
        return ApiResponse.ok("Booking cancelled");
    }

    @PostMapping("/bookings/check-in")
    @PreAuthorize("hasRole('TURF_OWNER')")
    public ApiResponse<Booking> checkIn(
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Check-in complete",
                service.checkIn(
                        current.require(authentication).getId(),
                        request.bookingCode(),
                        request.qrToken()
                )
        );
    }

    @GetMapping("/admin/turfs/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<Turf>> pending() {
        return ApiResponse.ok("Pending turfs", service.pending());
    }

    @PatchMapping("/admin/turfs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Turf> decide(
            @PathVariable String id,
            @RequestBody DecisionRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Turf decision saved",
                service.decide(
                        current.require(authentication).getId(),
                        id,
                        request.approve(),
                        request.reason()
                )
        );
    }
}

package com.playsphere.turf;

import com.playsphere.audit.AuditService;
import com.playsphere.common.BusinessException;
import com.playsphere.common.Ids;
import com.playsphere.common.TokenHasher;
import com.playsphere.media.MediaOwnershipService;
import com.playsphere.notification.NotificationService;
import com.playsphere.user.PlatformRole;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TurfService {
    private final TurfRepository turfs;
    private final TurfSlotRepository slots;
    private final BookingRepository bookings;
    private final TurfEquipmentRepository equipment;
    private final TokenHasher hasher;
    private final NotificationService notifications;
    private final AuditService audit;
    private final MediaOwnershipService mediaOwnership;

    public TurfService(
            TurfRepository turfs,
            TurfSlotRepository slots,
            BookingRepository bookings,
            TurfEquipmentRepository equipment,
            TokenHasher hasher,
            NotificationService notifications,
            AuditService audit,
            MediaOwnershipService mediaOwnership
    ) {
        this.turfs = turfs;
        this.slots = slots;
        this.bookings = bookings;
        this.equipment = equipment;
        this.hasher = hasher;
        this.notifications = notifications;
        this.audit = audit;
        this.mediaOwnership = mediaOwnership;
    }

    @Transactional(readOnly = true)
    public List<Turf> discover() {
        return turfs.findByStatusOrderByCreatedAtDesc("APPROVED");
    }

    @Transactional(readOnly = true)
    public List<NearbyTurfView> nearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        double originLat = latitude.doubleValue();
        double originLon = longitude.doubleValue();
        double safeRadius = Math.max(1, Math.min(radiusKm, 100));

        return turfs.findByStatusOrderByCreatedAtDesc("APPROVED").stream()
                .filter(turf -> turf.getLatitude() != null && turf.getLongitude() != null)
                .map(turf -> new NearbyTurfView(
                        turf,
                        roundDistance(haversineKm(
                                originLat,
                                originLon,
                                turf.getLatitude().doubleValue(),
                                turf.getLongitude().doubleValue()
                        ))
                ))
                .filter(view -> view.distanceKm() <= safeRadius)
                .sorted(Comparator.comparingDouble(NearbyTurfView::distanceKm))
                .limit(100)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Turf> mine(String ownerId) {
        return turfs.findByOwnerUserIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Turf require(String id) {
        return turfs.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Turf not found"));
    }

    @Transactional
    public Turf create(String ownerId, TurfController.CreateTurfRequest request) {
        if (request.latitude() == null || request.longitude() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Choose the turf location using the map");
        }

        String coverImageUrl = mediaOwnership.requireOwnedPurpose(
                ownerId,
                request.coverImageUrl(),
                "turfs"
        );
        Turf turf = turfs.save(new Turf(ownerId, request, coverImageUrl));

        notifications.send(
                ownerId,
                "TURF_SUBMITTED",
                "Turf submitted for approval",
                turf.getName() + " was submitted successfully. You will be notified after Admin review.",
                "/app/turf-owner/turfs"
        );
        notifications.sendToRoles(
                Set.of(PlatformRole.ADMIN, PlatformRole.SUPER_ADMIN),
                "TURF_APPROVAL_REQUIRED",
                "New turf requires approval",
                turf.getName() + " is waiting for review.",
                "/app/admin/turfs"
        );
        audit.record(ownerId, "TURF_SUBMITTED", "TURF", turf.getId(), turf.getName());
        return turf;
    }

    @Transactional(readOnly = true)
    public List<TurfSlot> availableSlots(String turfId) {
        require(turfId);
        return slots.findByTurfIdAndStatusAndStartAtAfterOrderByStartAt(
                turfId,
                "AVAILABLE",
                Instant.now()
        );
    }

    @Transactional
    public TurfSlot createSlot(String ownerId, String turfId, TurfController.CreateSlotRequest request) {
        Turf turf = requireOwner(ownerId, turfId);
        validateApprovedTurf(turf);
        validateSlotWindow(request.startAt(), request.endAt());
        ensureNoOverlap(turfId, request.startAt(), request.endAt());
        return slots.save(new TurfSlot(turfId, request.startAt(), request.endAt(), request.price()));
    }

    @Transactional
    public List<TurfSlot> generateSlots(
            String ownerId,
            String turfId,
            TurfController.GenerateSlotsRequest request
    ) {
        Turf turf = requireOwner(ownerId, turfId);
        validateApprovedTurf(turf);

        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        if (request.startDate().plusDays(31).isBefore(request.endDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Generate at most 31 days at a time");
        }
        if (!request.closingTime().isAfter(request.openingTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Closing time must be after opening time");
        }

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        List<TurfSlot> created = new ArrayList<>();
        LocalDate date = request.startDate();
        while (!date.isAfter(request.endDate())) {
            LocalTime cursor = request.openingTime();
            while (!cursor.plusMinutes(request.slotMinutes()).isAfter(request.closingTime())) {
                Instant start = date.atTime(cursor).atZone(zone).toInstant();
                Instant end = date.atTime(cursor.plusMinutes(request.slotMinutes())).atZone(zone).toInstant();
                if (start.isAfter(Instant.now())
                        && !slots.existsByTurfIdAndStartAtLessThanAndEndAtGreaterThan(turfId, end, start)) {
                    created.add(slots.save(new TurfSlot(turfId, start, end, request.price())));
                }
                cursor = cursor.plusMinutes(request.slotMinutes());
            }
            date = date.plusDays(1);
        }

        if (created.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "No new slots were created. The selected times may be in the past or overlap existing slots."
            );
        }
        notifications.send(
                ownerId,
                "TURF_SLOTS_CREATED",
                "Availability published",
                created.size() + " slots were created for " + turf.getName() + ".",
                "/app/turf-owner/availability"
        );
        audit.record(ownerId, "TURF_SLOTS_GENERATED", "TURF", turfId, String.valueOf(created.size()));
        return created;
    }

    @Transactional(readOnly = true)
    public List<TurfSlot> ownerSlots(String ownerId, String turfId) {
        requireOwner(ownerId, turfId);
        return slots.findByTurfIdOrderByStartAt(turfId);
    }

    @Transactional
    public TurfSlot slotStatus(String ownerId, String turfId, String slotId, String status) {
        requireOwner(ownerId, turfId);
        TurfSlot slot = slots.findById(slotId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!slot.getTurfId().equals(turfId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Slot does not belong to this turf");
        }
        if ("BOOKED".equals(slot.getStatus()) || "EVENT_RESERVED".equals(slot.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "A reserved slot cannot be blocked or reopened");
        }
        if (slot.getStartAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Past slots cannot be changed");
        }
        if ("BLOCKED".equals(status)) {
            slot.block();
        } else {
            slot.makeAvailable();
        }
        audit.record(ownerId, "TURF_SLOT_" + status, "TURF_SLOT", slotId, turfId);
        return slot;
    }

    @Transactional(readOnly = true)
    public List<TurfEquipment> equipment(String turfId, boolean ownerView, String ownerId) {
        if (ownerView) {
            requireOwner(ownerId, turfId);
            return equipment.findByTurfIdOrderByName(turfId);
        }
        require(turfId);
        return equipment.findByTurfIdAndActiveTrueOrderByName(turfId);
    }

    @Transactional
    public TurfEquipment addEquipment(
            String ownerId,
            String turfId,
            TurfController.CreateEquipmentRequest request
    ) {
        requireOwner(ownerId, turfId);
        if (equipment.existsByTurfIdAndNameIgnoreCase(turfId, request.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Equipment with this name already exists");
        }
        TurfEquipment item = equipment.save(new TurfEquipment(
                turfId,
                request.name(),
                request.description(),
                request.quantity(),
                request.pricePerBooking()
        ));
        audit.record(ownerId, "TURF_EQUIPMENT_CREATED", "TURF_EQUIPMENT", item.getId(), item.getName());
        return item;
    }

    @Transactional
    public TurfEquipment updateEquipment(
            String ownerId,
            String turfId,
            String equipmentId,
            TurfController.UpdateEquipmentRequest request
    ) {
        requireOwner(ownerId, turfId);
        TurfEquipment item = equipment.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Equipment not found"));
        if (!item.getTurfId().equals(turfId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Equipment does not belong to this turf");
        }
        item.update(request.quantity(), request.pricePerBooking(), request.active());
        audit.record(ownerId, "TURF_EQUIPMENT_UPDATED", "TURF_EQUIPMENT", item.getId(), item.getName());
        return item;
    }

    @Transactional
    public Booking book(String playerId, String slotId) {
        TurfSlot slot = slots.findByIdForUpdate(slotId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Slot not found"));
        if (!"AVAILABLE".equals(slot.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Slot is not available");
        }
        if (slot.getStartAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "This slot has already started");
        }

        slot.book();
        String rawToken = Ids.token();
        Booking booking = bookings.save(new Booking(
                playerId,
                slot,
                hasher.sha256(rawToken),
                rawToken
        ));
        notifications.send(
                playerId,
                "BOOKING_CREATED",
                "Complete your booking payment",
                "Booking " + booking.getBookingCode() + " is awaiting payment.",
                "/app/player/bookings"
        );
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> playerBookings(String playerId) {
        return bookings.findByPlayerUserIdOrderByCreatedAtDesc(playerId);
    }

    @Transactional
    public Booking qr(String playerId, String bookingId) {
        Booking booking = requireBooking(bookingId);
        if (!booking.getPlayerUserId().equals(playerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Cancelled booking has no active QR code");
        }
        String rawToken = Ids.token();
        booking.rotateQr(hasher.sha256(rawToken), rawToken);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> ownerBookings(String ownerId, String turfId) {
        requireOwner(ownerId, turfId);
        return bookings.findByTurfIdOrderByCreatedAtDesc(turfId);
    }

    @Transactional
    public void cancel(String playerId, String bookingId, String reason) {
        Booking booking = requireBooking(bookingId);
        if (!booking.getPlayerUserId().equals(playerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not your booking");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Booking is already cancelled");
        }
        if ("CHECKED_IN".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Checked-in booking cannot be cancelled");
        }
        booking.cancel(reason);
        slots.findById(booking.getSlotId()).ifPresent(TurfSlot::release);
        notifications.send(
                playerId,
                "BOOKING_CANCELLED",
                "Booking cancelled",
                "Booking " + booking.getBookingCode() + " was cancelled.",
                "/app/player/bookings"
        );
        audit.record(playerId, "BOOKING_CANCELLED", "BOOKING", bookingId, reason);
    }

    @Transactional
    public Booking checkIn(String ownerId, String bookingCode, String rawToken) {
        Booking booking = bookings.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Booking not found"));
        requireOwner(ownerId, booking.getTurfId());
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Booking is not ready for check-in");
        }
        if (!hasher.sha256(rawToken).equals(booking.getQrTokenHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid QR token");
        }
        booking.checkIn();
        notifications.send(
                booking.getPlayerUserId(),
                "CHECK_IN",
                "Check-in completed",
                "You checked in for booking " + bookingCode + ".",
                "/app/player/bookings"
        );
        audit.record(ownerId, "BOOKING_CHECKED_IN", "BOOKING", booking.getId(), bookingCode);
        return booking;
    }

    @Transactional
    public Turf decide(String adminId, String turfId, boolean approve, String reason) {
        Turf turf = require(turfId);
        turf.decide(approve, reason);
        notifications.send(
                turf.getOwnerUserId(),
                "TURF_APPROVAL",
                approve ? "Turf approved" : "Turf needs changes",
                approve
                        ? turf.getName() + " is public. Publish availability slots so players and organizers can book it."
                        : (reason == null || reason.isBlank() ? "Please update the turf details and submit again." : reason),
                approve ? "/app/turf-owner/availability" : "/app/turf-owner/turfs"
        );
        audit.record(
                adminId,
                approve ? "TURF_APPROVED" : "TURF_REJECTED",
                "TURF",
                turf.getId(),
                reason
        );
        return turf;
    }

    @Transactional(readOnly = true)
    public List<Turf> pending() {
        return turfs.findByStatusOrderByCreatedAtDesc("PENDING_APPROVAL");
    }

    private void validateApprovedTurf(Turf turf) {
        if (!"APPROVED".equals(turf.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Turf must be approved before publishing slots");
        }
    }

    private void validateSlotWindow(Instant start, Instant end) {
        if (!end.isAfter(start) || start.isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid slot time");
        }
    }

    private void ensureNoOverlap(String turfId, Instant start, Instant end) {
        if (slots.existsByTurfIdAndStartAtLessThanAndEndAtGreaterThan(turfId, end, start)) {
            throw new BusinessException(HttpStatus.CONFLICT, "This slot overlaps an existing slot");
        }
    }

    private Booking requireBooking(String id) {
        return bookings.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private Turf requireOwner(String ownerId, String turfId) {
        Turf turf = require(turfId);
        if (!turf.getOwnerUserId().equals(ownerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You do not own this turf");
        }
        return turf;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double roundDistance(double distance) {
        return BigDecimal.valueOf(distance).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

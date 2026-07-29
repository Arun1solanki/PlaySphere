package com.playsphere.turf;

import com.playsphere.audit.AuditService;
import com.playsphere.common.BusinessException;
import com.playsphere.common.Ids;
import com.playsphere.common.TokenHasher;
import com.playsphere.notification.NotificationService;
import com.playsphere.media.MediaOwnershipService;
import java.time.Instant;
import java.util.List;
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

    public List<Turf> discover() {
        return turfs.findByStatusOrderByCreatedAtDesc("APPROVED");
    }

    public List<Turf> mine(String ownerId) {
        return turfs.findByOwnerUserIdOrderByCreatedAtDesc(ownerId);
    }

    public Turf require(String id) {
        return turfs.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Turf not found"));
    }

    @Transactional
    public Turf create(String ownerId, TurfController.CreateTurfRequest request) {
        String coverImageUrl = mediaOwnership.requireOwnedPurpose(
                ownerId,
                request.coverImageUrl(),
                "turfs"
        );
        Turf turf = turfs.save(new Turf(ownerId, request, coverImageUrl));
        audit.record(ownerId, "TURF_SUBMITTED", "TURF", turf.getId(), turf.getName());
        return turf;
    }

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
        if (!"APPROVED".equals(turf.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Turf must be approved before publishing slots");
        }
        if (!request.endAt().isAfter(request.startAt()) || request.startAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid slot time");
        }
        if (slots.existsByTurfIdAndStartAtLessThanAndEndAtGreaterThan(
                turfId,
                request.endAt(),
                request.startAt()
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "This slot overlaps an existing slot");
        }
        return slots.save(new TurfSlot(turfId, request.startAt(), request.endAt(), request.price()));
    }

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
        if ("BOOKED".equals(slot.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "A booked slot cannot be blocked or reopened");
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

        // Locking the slot inside this transaction prevents two players from booking it simultaneously.
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
                approve ? turf.getName() + " is now public." : reason,
                "/app/turf-owner/turfs"
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

    public List<Turf> pending() {
        return turfs.findByStatusOrderByCreatedAtDesc("PENDING_APPROVAL");
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
}

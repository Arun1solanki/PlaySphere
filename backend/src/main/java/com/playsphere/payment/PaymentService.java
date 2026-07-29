package com.playsphere.payment;

import com.playsphere.audit.AuditService;
import com.playsphere.common.BusinessException;
import com.playsphere.event.EventRegistration;
import com.playsphere.event.EventRegistrationRepository;
import com.playsphere.event.SportsEventRepository;
import com.playsphere.notification.NotificationService;
import com.playsphere.turf.Booking;
import com.playsphere.turf.BookingRepository;
import com.playsphere.turf.TurfRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final List<String> ACTIVE_PAYMENT_STATUSES = List.of("CREATED", "PENDING", "SUCCESS");

    private final PaymentRepository payments;
    private final RefundRequestRepository refunds;
    private final PaymentGateway gateway;
    private final BookingRepository bookings;
    private final EventRegistrationRepository registrations;
    private final SportsEventRepository events;
    private final TurfRepository turfs;
    private final NotificationService notifications;
    private final AuditService audit;

    public PaymentService(
            PaymentRepository payments,
            RefundRequestRepository refunds,
            PaymentGateway gateway,
            BookingRepository bookings,
            EventRegistrationRepository registrations,
            SportsEventRepository events,
            TurfRepository turfs,
            NotificationService notifications,
            AuditService audit
    ) {
        this.payments = payments;
        this.refunds = refunds;
        this.gateway = gateway;
        this.bookings = bookings;
        this.registrations = registrations;
        this.events = events;
        this.turfs = turfs;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public Payment create(String userId, PaymentController.CreatePaymentRequest request) {
        BigDecimal amount = resolveAmountAndVerifyOwner(userId, request.purpose(), request.referenceId());
        if (payments.existsByPurposeAndReferenceIdAndStatusIn(
                request.purpose(),
                request.referenceId(),
                ACTIVE_PAYMENT_STATUSES
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "An active payment already exists for this item");
        }

        Payment payment = payments.save(new Payment(
                userId,
                request.purpose(),
                request.referenceId(),
                "PENDING",
                amount
        ));
        PaymentGateway.GatewayOrder order = gateway.createOrder(payment);
        payment.order(order.provider(), order.orderId());
        if (order.autoPaid()) {
            payment.success(order.paymentId());
            completeReference(payment);
        }
        return payment;
    }

    public List<Payment> mine(String userId) {
        return payments.findByUserIdOrderByCreatedAtDesc(userId);
    }


    public List<Payment> organizerEarnings(String organizerId) {
        return payments.findAllByOrderByCreatedAtDesc().stream()
                .filter(payment -> "SUCCESS".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus()))
                .filter(payment -> "EVENT_REGISTRATION".equals(payment.getPurpose()))
                .filter(payment -> registrations.findById(payment.getReferenceId())
                        .flatMap(registration -> events.findById(registration.getEventId()))
                        .map(event -> event.getOrganizerUserId().equals(organizerId))
                        .orElse(false))
                .toList();
    }

    public List<Payment> turfOwnerEarnings(String ownerId) {
        return payments.findAllByOrderByCreatedAtDesc().stream()
                .filter(payment -> "SUCCESS".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus()))
                .filter(payment -> "BOOKING".equals(payment.getPurpose()))
                .filter(payment -> bookings.findById(payment.getReferenceId())
                        .flatMap(booking -> turfs.findById(booking.getTurfId()))
                        .map(turf -> turf.getOwnerUserId().equals(ownerId))
                        .orElse(false))
                .toList();
    }

    @Transactional
    public RefundRequest requestRefund(
            String userId,
            String paymentId,
            PaymentController.RefundRequestInput request
    ) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!payment.getUserId().equals(userId) || !"SUCCESS".equals(payment.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Payment is not refundable");
        }
        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Refund amount cannot exceed the payment amount");
        }
        if (refunds.existsByPaymentIdAndStatus(paymentId, "REQUESTED")) {
            throw new BusinessException(HttpStatus.CONFLICT, "A refund request is already pending");
        }
        RefundRequest refund = refunds.save(new RefundRequest(
                paymentId,
                userId,
                request.reason(),
                request.amount()
        ));
        audit.record(userId, "REFUND_REQUESTED", "PAYMENT", paymentId, request.reason());
        return refund;
    }

    public List<RefundRequest> myRefunds(String userId) {
        return refunds.findByRequestedByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<RefundRequest> pendingRefunds() {
        return refunds.findByStatusOrderByCreatedAtAsc("REQUESTED");
    }

    @Transactional
    public RefundRequest decideRefund(
            String adminId,
            String refundId,
            PaymentController.RefundDecision request
    ) {
        RefundRequest refund = refunds.findById(refundId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Refund not found"));
        if (!"REQUESTED".equals(refund.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Refund already decided");
        }
        Payment payment = payments.findById(refund.getPaymentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (request.approve()) {
            gateway.refund(payment, refund.getRequestedAmount());
            payment.refunded();
        }
        refund.decide(request.approve(), adminId, request.note());
        notifications.send(
                refund.getRequestedByUserId(),
                "REFUND_DECISION",
                request.approve() ? "Refund approved" : "Refund rejected",
                request.note() == null ? "Your refund request was reviewed." : request.note(),
                "/app/player/payments"
        );
        audit.record(
                adminId,
                request.approve() ? "REFUND_APPROVED" : "REFUND_REJECTED",
                "REFUND",
                refund.getId(),
                request.note()
        );
        return refund;
    }

    @Transactional
    public Payment confirmGatewayPayment(String providerOrderId, String providerPaymentId) {
        Payment payment = findPaymentByProviderOrder(providerOrderId);
        return markGatewayPaymentSuccessful(payment, providerPaymentId);
    }

    @Transactional
    public Payment confirmGatewayPaymentForUser(
            String userId,
            String providerOrderId,
            String providerPaymentId
    ) {
        Payment payment = findPaymentByProviderOrder(providerOrderId);
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Payment order does not belong to this account");
        }
        return markGatewayPaymentSuccessful(payment, providerPaymentId);
    }

    private Payment findPaymentByProviderOrder(String providerOrderId) {
        return payments.findByProviderOrderId(providerOrderId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Payment order not found"));
    }

    private Payment markGatewayPaymentSuccessful(Payment payment, String providerPaymentId) {
        if ("SUCCESS".equals(payment.getStatus())) {
            return payment;
        }
        if (!"RAZORPAY".equals(payment.getProvider())) {
            throw new BusinessException(HttpStatus.CONFLICT, "Only Razorpay orders can be confirmed here");
        }
        payment.success(providerPaymentId);
        completeReference(payment);
        return payment;
    }

    private BigDecimal resolveAmountAndVerifyOwner(String userId, String purpose, String referenceId) {
        return switch (purpose) {
            case "BOOKING" -> {
                Booking booking = bookings.findById(referenceId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Booking not found"));
                if (!booking.getPlayerUserId().equals(userId)) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "Not your booking");
                }
                if ("CANCELLED".equals(booking.getStatus())) {
                    throw new BusinessException(HttpStatus.CONFLICT, "Cancelled booking cannot be paid");
                }
                yield booking.getAmount();
            }
            case "EVENT_REGISTRATION" -> {
                EventRegistration registration = registrations.findById(referenceId)
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Registration not found"));
                if (!registration.getUserId().equals(userId)) {
                    throw new BusinessException(HttpStatus.FORBIDDEN, "Not your registration");
                }
                yield events.findById(registration.getEventId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Event not found"))
                        .getEntryFee();
            }
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported payment purpose");
        };
    }

    private void completeReference(Payment payment) {
        if ("BOOKING".equals(payment.getPurpose())) {
            bookings.findById(payment.getReferenceId()).ifPresent(Booking::paid);
        }
        if ("EVENT_REGISTRATION".equals(payment.getPurpose())) {
            registrations.findById(payment.getReferenceId()).ifPresent(EventRegistration::paid);
        }
        notifications.send(
                payment.getUserId(),
                "PAYMENT_SUCCESS",
                "Payment successful",
                "Payment " + payment.getId() + " completed.",
                "/app/player/payments"
        );
    }
}

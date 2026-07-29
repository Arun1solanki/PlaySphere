package com.playsphere.review;

import com.playsphere.common.BusinessException;
import com.playsphere.event.EventRegistrationRepository;
import com.playsphere.event.SportsEventRepository;
import com.playsphere.turf.BookingRepository;
import com.playsphere.turf.TurfRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final EventRegistrationRepository registrations;
    private final TurfRepository turfs;
    private final SportsEventRepository events;

    public ReviewService(
            ReviewRepository reviews,
            BookingRepository bookings,
            EventRegistrationRepository registrations,
            TurfRepository turfs,
            SportsEventRepository events
    ) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.registrations = registrations;
        this.turfs = turfs;
        this.events = events;
    }

    public List<Review> list(String targetType, String targetId) {
        return reviews.findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
                targetType,
                targetId,
                "PUBLISHED"
        );
    }

    @Transactional
    public Review create(String userId, ReviewController.CreateReview request) {
        if (reviews.existsByAuthorUserIdAndTargetTypeAndTargetId(
                userId,
                request.targetType(),
                request.targetId()
        )) {
            throw new BusinessException(HttpStatus.CONFLICT, "You already reviewed this item");
        }
        verifyParticipation(userId, request.targetType(), request.targetId());
        return reviews.save(new Review(
                userId,
                request.targetType(),
                request.targetId(),
                request.rating(),
                request.comment()
        ));
    }

    @Transactional
    public Review moderate(String id, String status) {
        if (!List.of("PUBLISHED", "HIDDEN", "REMOVED", "UNDER_REVIEW").contains(status)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Invalid review status");
        }
        Review review = reviews.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Review not found"));
        review.moderate(status);
        return review;
    }

    private void verifyParticipation(String userId, String targetType, String targetId) {
        switch (targetType) {
            case "TURF" -> {
                if (!turfs.existsById(targetId)) {
                    throw new BusinessException(HttpStatus.NOT_FOUND, "Turf not found");
                }
                boolean eligible = bookings.existsByPlayerUserIdAndTurfIdAndStatusIn(
                        userId,
                        targetId,
                        List.of("CHECKED_IN", "COMPLETED")
                );
                if (!eligible) {
                    throw new BusinessException(
                            HttpStatus.FORBIDDEN,
                            "Complete a verified turf booking before reviewing it"
                    );
                }
            }
            case "EVENT" -> {
                if (!events.existsById(targetId)) {
                    throw new BusinessException(HttpStatus.NOT_FOUND, "Event not found");
                }
                boolean eligible = registrations.existsByEventIdAndUserIdAndStatusIn(
                        targetId,
                        userId,
                        List.of("APPROVED", "COMPLETED")
                );
                if (!eligible) {
                    throw new BusinessException(
                            HttpStatus.FORBIDDEN,
                            "Join the event before reviewing it"
                    );
                }
            }
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported review target");
        }
    }
}

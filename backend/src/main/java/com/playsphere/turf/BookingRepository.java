package com.playsphere.turf;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByPlayerUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByTurfIdOrderByCreatedAtDesc(String turfId);
    List<Booking> findAllByOrderByCreatedAtDesc();
    Optional<Booking> findByBookingCode(String code);
    boolean existsByPlayerUserIdAndTurfIdAndStatusIn(
            String playerUserId,
            String turfId,
            List<String> statuses
    );
}

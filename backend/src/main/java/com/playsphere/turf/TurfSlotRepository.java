package com.playsphere.turf;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TurfSlotRepository extends JpaRepository<TurfSlot, String> {
    List<TurfSlot> findByTurfIdAndStatusAndStartAtAfterOrderByStartAt(
            String turfId,
            String status,
            Instant after
    );

    List<TurfSlot> findByTurfIdOrderByStartAt(String turfId);

    boolean existsByTurfIdAndStartAtLessThanAndEndAtGreaterThan(
            String turfId,
            Instant requestedEnd,
            Instant requestedStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from TurfSlot slot where slot.id = :id")
    Optional<TurfSlot> findByIdForUpdate(@Param("id") String id);
}

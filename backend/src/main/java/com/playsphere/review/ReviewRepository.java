package com.playsphere.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
            String targetType,
            String targetId,
            String status
    );
    List<Review> findAllByOrderByCreatedAtDesc();
    boolean existsByAuthorUserIdAndTargetTypeAndTargetId(
            String authorUserId,
            String targetType,
            String targetId
    );
}

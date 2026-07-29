package com.playsphere.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
    boolean existsByEventIdAndUserId(String eventId, String userId);
    boolean existsByEventIdAndUserIdAndStatusIn(String eventId, String userId, List<String> statuses);
    boolean existsByEventIdAndTeamIdAndStatusIn(String eventId, String teamId, List<String> statuses);
    long countByEventIdAndStatus(String eventId, String status);
    List<EventRegistration> findByEventIdOrderByCreatedAtDesc(String eventId);
    List<EventRegistration> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<EventRegistration> findByEventIdAndUserId(String eventId, String userId);
}

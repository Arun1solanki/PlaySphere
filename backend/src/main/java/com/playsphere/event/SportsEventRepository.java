package com.playsphere.event;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportsEventRepository extends JpaRepository<SportsEvent, String> {
    List<SportsEvent> findByStatusOrderByStartAtAsc(String status);
    List<SportsEvent> findByOrganizerUserIdOrderByCreatedAtDesc(String userId);
    List<SportsEvent> findAllByOrderByCreatedAtDesc();
}

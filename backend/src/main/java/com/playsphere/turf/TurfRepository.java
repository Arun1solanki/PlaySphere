package com.playsphere.turf;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurfRepository extends JpaRepository<Turf, String> {
    List<Turf> findByStatusOrderByCreatedAtDesc(String status);
    List<Turf> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);
    List<Turf> findAllByOrderByCreatedAtDesc();
}

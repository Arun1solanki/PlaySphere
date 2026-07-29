package com.playsphere.team;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, String> {
    List<Team> findByStatusAndVisibilityOrderByCreatedAtDesc(String status, String visibility);
    List<Team> findByStatusAndVisibilityAndSportIgnoreCaseOrderByCreatedAtDesc(
            String status,
            String visibility,
            String sport
    );
    List<Team> findByCaptainUserIdOrderByCreatedAtDesc(String captainUserId);
    List<Team> findAllByOrderByCreatedAtDesc();
    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);
}

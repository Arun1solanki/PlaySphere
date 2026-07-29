package com.playsphere.turf;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TurfEquipmentRepository extends JpaRepository<TurfEquipment, String> {
    List<TurfEquipment> findByTurfIdOrderByName(String turfId);
    List<TurfEquipment> findByTurfIdAndActiveTrueOrderByName(String turfId);
    boolean existsByTurfIdAndNameIgnoreCase(String turfId, String name);
}

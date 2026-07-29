package com.playsphere.profile;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByUser_Id(String userId);
    List<UserProfile> findByDiscoverableTrueOrderByFullNameAsc();
}

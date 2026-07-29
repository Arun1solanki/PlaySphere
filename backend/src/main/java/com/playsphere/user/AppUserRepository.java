package com.playsphere.user;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select distinct u
            from AppUser u
            join u.roles role
            where role in :roles and u.status = :status
            """)
    List<AppUser> findByAnyRoleAndStatus(
            @Param("roles") Set<PlatformRole> roles,
            @Param("status") AccountStatus status
    );
}

package com.playsphere.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, String> {
    boolean existsByTeamIdAndUserId(String teamId, String userId);
    long countByTeamId(String teamId);
    List<TeamMember> findByTeamIdOrderByJoinedAtAsc(String teamId);
    List<TeamMember> findByUserId(String userId);
    Optional<TeamMember> findByTeamIdAndUserId(String teamId, String userId);
    void deleteByTeamIdAndUserId(String teamId, String userId);
}

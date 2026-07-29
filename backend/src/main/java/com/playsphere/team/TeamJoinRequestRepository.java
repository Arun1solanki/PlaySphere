package com.playsphere.team;
import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest,String>{boolean existsByTeamIdAndApplicantUserIdAndStatus(String teamId,String userId,String status);List<TeamJoinRequest> findByTeamIdOrderByCreatedAtDesc(String teamId);}

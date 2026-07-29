package com.playsphere.team;
import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
public interface RecruitmentPostRepository extends JpaRepository<RecruitmentPost,String>{List<RecruitmentPost> findByStatusOrderByCreatedAtDesc(String status);List<RecruitmentPost> findByTeamIdOrderByCreatedAtDesc(String teamId);}

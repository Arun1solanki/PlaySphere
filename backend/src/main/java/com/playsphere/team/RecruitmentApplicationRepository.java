package com.playsphere.team;
import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication,String>{boolean existsByPostIdAndApplicantUserId(String postId,String userId);List<RecruitmentApplication> findByPostIdOrderByCreatedAtDesc(String postId);}

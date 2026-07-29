package com.playsphere.team;
import com.playsphere.common.Ids;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="recruitment_applications")
public class RecruitmentApplication{
 @Id @Column(length=36) private String id;@Column(name="post_id",nullable=false,length=36) private String postId;@Column(name="applicant_user_id",nullable=false,length=36) private String applicantUserId;@Column(length=400) private String message;@Column(nullable=false,length=24) private String status;@Column(name="created_at",nullable=false) private Instant createdAt;@Column(name="decided_at") private Instant decidedAt;
 protected RecruitmentApplication(){}public RecruitmentApplication(String post,String user,String message){id=Ids.uuid();postId=post;applicantUserId=user;this.message=message;status="PENDING";createdAt=Instant.now();}
 public String getId(){return id;}public String getPostId(){return postId;}public String getApplicantUserId(){return applicantUserId;}public String getMessage(){return message;}public String getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}public Instant getDecidedAt(){return decidedAt;}public void decide(String next){status=next;decidedAt=Instant.now();}
}

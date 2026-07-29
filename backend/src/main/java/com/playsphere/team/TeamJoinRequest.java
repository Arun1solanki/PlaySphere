package com.playsphere.team;
import com.playsphere.common.Ids;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="team_join_requests")
public class TeamJoinRequest{
 @Id @Column(length=36) private String id;@Column(name="team_id",nullable=false,length=36) private String teamId;@Column(name="applicant_user_id",nullable=false,length=36) private String applicantUserId;@Column(length=400) private String message;@Column(nullable=false,length=24) private String status;@Column(name="decided_by_user_id",length=36) private String decidedByUserId;@Column(name="created_at",nullable=false) private Instant createdAt;@Column(name="decided_at") private Instant decidedAt;
 protected TeamJoinRequest(){}public TeamJoinRequest(String team,String applicant,String message){id=Ids.uuid();teamId=team;applicantUserId=applicant;this.message=message;status="PENDING";createdAt=Instant.now();}
 public String getId(){return id;}public String getTeamId(){return teamId;}public String getApplicantUserId(){return applicantUserId;}public String getMessage(){return message;}public String getStatus(){return status;}public String getDecidedByUserId(){return decidedByUserId;}public Instant getCreatedAt(){return createdAt;}public Instant getDecidedAt(){return decidedAt;}
 public void decide(String status,String user){this.status=status;decidedByUserId=user;decidedAt=Instant.now();}
}

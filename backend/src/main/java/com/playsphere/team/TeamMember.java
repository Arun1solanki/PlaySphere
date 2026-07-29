package com.playsphere.team;
import com.playsphere.common.Ids;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="team_members")
public class TeamMember{
 @Id @Column(length=36) private String id;
 @Column(name="team_id",nullable=false,length=36) private String teamId;
 @Column(name="user_id",nullable=false,length=36) private String userId;
 @Column(name="member_role",nullable=false,length=24) private String memberRole;
 @Column(name="joined_at",nullable=false) private Instant joinedAt;
 protected TeamMember(){} public TeamMember(String teamId,String userId,String role){id=Ids.uuid();this.teamId=teamId;this.userId=userId;memberRole=role;joinedAt=Instant.now();}
 public String getId(){return id;}public String getTeamId(){return teamId;}public String getUserId(){return userId;}public String getMemberRole(){return memberRole;}public Instant getJoinedAt(){return joinedAt;}
 public void setMemberRole(String memberRole){this.memberRole=memberRole;}
}

package com.playsphere.audit;
import com.playsphere.common.Ids;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="audit_logs")
public class AuditLog{
 @Id @Column(length=36) private String id;
 @Column(name="actor_user_id",length=36) private String actorUserId;
 @Column(nullable=false,length=100) private String action;
 @Column(name="target_type",nullable=false,length=60) private String targetType;
 @Column(name="target_id",length=80) private String targetId;
 @Column(length=1500) private String details;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 protected AuditLog(){} public AuditLog(String actor,String action,String targetType,String targetId,String details){id=Ids.uuid();actorUserId=actor;this.action=action;this.targetType=targetType;this.targetId=targetId;this.details=details;createdAt=Instant.now();}
 public String getId(){return id;} public String getActorUserId(){return actorUserId;} public String getAction(){return action;} public String getTargetType(){return targetType;} public String getTargetId(){return targetId;} public String getDetails(){return details;} public Instant getCreatedAt(){return createdAt;}
}

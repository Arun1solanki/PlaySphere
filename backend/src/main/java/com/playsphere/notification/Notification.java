package com.playsphere.notification;

import com.playsphere.common.Ids;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="notifications")
public class Notification {
    @Id @Column(length=36) private String id;
    @Column(name="user_id", nullable=false, length=36) private String userId;
    @Column(nullable=false, length=40) private String type;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false, length=600) private String message;
    @Column(name="action_url", length=500) private String actionUrl;
    @Column(name="read_at") private Instant readAt;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    protected Notification() {}
    public Notification(String userId,String type,String title,String message,String actionUrl){this.id=Ids.uuid();this.userId=userId;this.type=type;this.title=title;this.message=message;this.actionUrl=actionUrl;this.createdAt=Instant.now();}
    public String getId(){return id;} public String getUserId(){return userId;} public String getType(){return type;} public String getTitle(){return title;} public String getMessage(){return message;} public String getActionUrl(){return actionUrl;} public Instant getReadAt(){return readAt;} public Instant getCreatedAt(){return createdAt;}
    public void markRead(){if(readAt==null) readAt=Instant.now();}
}

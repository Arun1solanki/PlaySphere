package com.playsphere.notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification,String>{List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(String userId);}

package com.playsphere.notification;
import com.playsphere.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationService{
 private final NotificationRepository repository;
 public NotificationService(NotificationRepository repository){this.repository=repository;}
 public Notification send(String userId,String type,String title,String message,String actionUrl){return repository.save(new Notification(userId,type,title,message,actionUrl));}
 public List<Notification> list(String userId){return repository.findTop100ByUserIdOrderByCreatedAtDesc(userId);}
 @Transactional public void markRead(String userId,String id){Notification n=repository.findById(id).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"Notification not found"));if(!n.getUserId().equals(userId))throw new BusinessException(HttpStatus.FORBIDDEN,"Not your notification");n.markRead();}
}

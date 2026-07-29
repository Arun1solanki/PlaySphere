package com.playsphere.notification;

import com.playsphere.common.BusinessException;
import com.playsphere.user.AccountStatus;
import com.playsphere.user.AppUserRepository;
import com.playsphere.user.PlatformRole;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final AppUserRepository users;

    public NotificationService(NotificationRepository repository, AppUserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    public Notification send(
            String userId,
            String type,
            String title,
            String message,
            String actionUrl
    ) {
        return repository.save(new Notification(userId, type, title, message, actionUrl));
    }

    @Transactional
    public void sendToRoles(
            Set<PlatformRole> roles,
            String type,
            String title,
            String message,
            String actionUrl
    ) {
        users.findByAnyRoleAndStatus(roles, AccountStatus.ACTIVE)
                .forEach(user -> repository.save(new Notification(
                        user.getId(),
                        type,
                        title,
                        message,
                        actionUrl
                )));
    }

    public List<Notification> list(String userId) {
        return repository.findTop100ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRead(String userId, String id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.markRead();
    }
}

package com.playsphere.notification;
import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/notifications")
public class NotificationController{
 private final NotificationService service; private final CurrentUserService current;
 public NotificationController(NotificationService service,CurrentUserService current){this.service=service;this.current=current;}
 @GetMapping public ApiResponse<List<Notification>> list(Authentication a){return ApiResponse.ok("Notifications",service.list(current.require(a).getId()));}
 @PatchMapping("/{id}/read") public ApiResponse<Void> read(@PathVariable String id,Authentication a){service.markRead(current.require(a).getId(),id);return ApiResponse.ok("Notification marked as read");}
}

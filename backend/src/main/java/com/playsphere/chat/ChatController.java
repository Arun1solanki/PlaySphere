package com.playsphere.chat;

import com.playsphere.common.ApiResponse;
import com.playsphere.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {
    private final ChatService service;
    private final CurrentUserService current;
    private final SimpMessagingTemplate messaging;

    public ChatController(
            ChatService service,
            CurrentUserService current,
            SimpMessagingTemplate messaging
    ) {
        this.service = service;
        this.current = current;
        this.messaging = messaging;
    }

    public record CreateConversationRequest(
            @NotBlank String type,
            String referenceId,
            @NotBlank @Size(max = 160) String title,
            @NotEmpty @Size(max = 9) List<@NotBlank String> memberUserIds
    ) {}

    public record SendMessageRequest(@NotBlank @Size(max = 1200) String body) {}

    @GetMapping
    public ApiResponse<List<Conversation>> mine(Authentication authentication) {
        return ApiResponse.ok(
                "Conversations",
                service.mine(current.require(authentication).getId())
        );
    }

    @PostMapping
    public ApiResponse<Conversation> create(
            @Valid @RequestBody CreateConversationRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Conversation created",
                service.create(current.require(authentication).getId(), request)
        );
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatMessageView>> history(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ApiResponse.ok(
                "Messages",
                service.history(current.require(authentication).getId(), id)
        );
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ChatMessageView> send(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication
    ) {
        ChatMessageView message = service.send(
                current.require(authentication).getId(),
                id,
                request.body()
        );
        messaging.convertAndSend("/topic/conversations/" + id, message);
        return ApiResponse.ok("Message sent", message);
    }
}

package com.playsphere.chat;

import com.playsphere.common.BusinessException;
import com.playsphere.profile.UserProfile;
import com.playsphere.profile.UserProfileRepository;
import com.playsphere.user.AppUser;
import com.playsphere.user.AppUserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final AppUserRepository users;
    private final UserProfileRepository profiles;

    public ChatService(
            ConversationRepository conversations,
            ConversationMemberRepository members,
            ChatMessageRepository messages,
            AppUserRepository users,
            UserProfileRepository profiles
    ) {
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional
    public Conversation create(String creator, ChatController.CreateConversationRequest request) {
        String type = request.type().trim().toUpperCase(Locale.ROOT);
        if (!"DIRECT".equals(type)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Team and event conversations are created automatically"
            );
        }

        Set<String> participantIds = new LinkedHashSet<>(request.memberUserIds());
        participantIds.remove(creator);
        if (participantIds.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Choose at least one conversation member");
        }
        if (participantIds.size() > 9) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Direct conversations support up to 10 members");
        }
        if (users.findAllById(participantIds).size() != participantIds.size()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "One or more conversation members do not exist");
        }

        Conversation conversation = conversations.save(new Conversation(
                type,
                null,
                request.title().trim(),
                creator
        ));
        members.save(new ConversationMember(conversation.getId(), creator));
        participantIds.forEach(userId -> members.save(new ConversationMember(conversation.getId(), userId)));
        return conversation;
    }

    @Transactional(readOnly = true)
    public List<Conversation> mine(String userId) {
        List<Conversation> result = new ArrayList<>();
        for (ConversationMember member : members.findByUserId(userId)) {
            conversations.findById(member.getConversationId()).ifPresent(result::add);
        }
        return result.stream()
                .sorted(Comparator.comparing(Conversation::getCreatedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> history(String userId, String conversationId) {
        requireMember(userId, conversationId);
        return messages.findTop100ByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public ChatMessageView send(String userId, String conversationId, String body) {
        requireMember(userId, conversationId);
        ChatMessage message = messages.save(new ChatMessage(conversationId, userId, body.trim()));
        return view(message);
    }

    private ChatMessageView view(ChatMessage message) {
        AppUser sender = users.findById(message.getSenderUserId()).orElse(null);
        UserProfile profile = profiles.findByUser_Id(message.getSenderUserId()).orElse(null);
        return new ChatMessageView(
                message.getId(),
                message.getConversationId(),
                message.getSenderUserId(),
                profile != null && profile.getFullName() != null
                        ? profile.getFullName()
                        : sender == null ? "Unknown player" : sender.getDisplayName(),
                profile == null ? null : profile.getProfileImageUrl(),
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private void requireMember(String userId, String conversationId) {
        if (!members.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You are not a conversation member");
        }
    }
}

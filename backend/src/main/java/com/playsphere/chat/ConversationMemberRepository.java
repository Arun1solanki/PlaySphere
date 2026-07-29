package com.playsphere.chat;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMember.Key> {
    boolean existsByConversationIdAndUserId(String conversationId, String userId);
    List<ConversationMember> findByUserId(String userId);
    void deleteByConversationIdAndUserId(String conversationId, String userId);
}

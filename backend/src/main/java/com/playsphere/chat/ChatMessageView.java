package com.playsphere.chat;

import java.time.Instant;

public record ChatMessageView(
        String id,
        String conversationId,
        String senderUserId,
        String senderDisplayName,
        String senderProfileImageUrl,
        String body,
        Instant createdAt
) {}

package com.ezdo.dto.ai;

import com.ezdo.entity.DecompositionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
    UUID sessionId,
    DecompositionStatus status,
    List<ConversationMessage> messages,
    boolean hasProposal,
    UUID confirmedGoalId,
    Instant createdAt,
    Instant updatedAt
) {
}

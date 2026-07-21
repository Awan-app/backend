package com.ezdo.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatReply(
    UUID sessionId,
    List<ContentBlock> blocks,
    boolean hasProposal,
    Instant timestamp
) {
}

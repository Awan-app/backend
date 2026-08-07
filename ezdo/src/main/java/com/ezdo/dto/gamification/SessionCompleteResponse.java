package com.ezdo.dto.gamification;

import com.ezdo.dto.SessionResponse;

/**
 * Response body of {@code POST /api/v1/sessions/{sessionId}/complete}: the
 * completed session plus the gamification reward (if any) it produced.
 */
public record SessionCompleteResponse(
    SessionResponse session,
    SessionCompletionReward reward
) {}

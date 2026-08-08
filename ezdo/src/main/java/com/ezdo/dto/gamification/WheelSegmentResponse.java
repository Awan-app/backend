package com.ezdo.dto.gamification;

import com.ezdo.config.PayoutType;

/**
 * A single wheel segment as exposed in the wheel config. Weights are
 * server-internal and intentionally not included.
 */
public record WheelSegmentResponse(
    String segmentId,
    int coins,
    PayoutType payoutType
) {}

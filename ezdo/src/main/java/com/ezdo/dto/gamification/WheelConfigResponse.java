package com.ezdo.dto.gamification;

import java.util.List;

/**
 * The wheel configuration plus the user's claim status, used to render the
 * wheel and decide whether the spin is available.
 */
public record WheelConfigResponse(
    List<WheelSegmentResponse> segments,
    boolean claimedToday,
    WheelClaimResponse lastClaim
) {}

package com.ezdo.dto.gamification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Snapshot of a past daily-gift claim. For item grants {@code coinsAwarded}
 * is 0 and {@code itemId}/{@code itemName} hold the won item; for coin grants
 * {@code itemId} and {@code itemName} are null.
 */
public record WheelClaimResponse(
    String segmentId,
    int coinsAwarded,
    UUID itemId,
    String itemName,
    LocalDate claimDate,
    Instant claimedAt
) {}

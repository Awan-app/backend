package com.ezdo.dto.gamification;

import com.ezdo.config.PayoutType;
import com.ezdo.dto.store.ItemResponse;

/**
 * Result of a daily-gift spin. For a {@link PayoutType#COINS} segment
 * {@code coinsAwarded} is the amount and {@code item} is null; for a
 * {@link PayoutType#ITEM} segment {@code coinsAwarded} is 0 and {@code item}
 * is the won item. {@code newBalance} is the wallet balance after the spin.
 */
public record WheelSpinResponse(
    String segmentId,
    PayoutType payoutType,
    int coinsAwarded,
    long newBalance,
    ItemResponse item
) {}

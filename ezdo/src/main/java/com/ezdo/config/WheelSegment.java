package com.ezdo.config;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hardcoded prize table for the daily gift wheel.
 *
 * <p>Each segment awards either a fixed number of coins ({@link PayoutType#COINS})
 * or a random unowned item from the store ({@link PayoutType#ITEM}). Weights are
 * arbitrary integers; a segment's probability is {@code weight / totalWeight()}.
 */
public enum WheelSegment {

    SEG_1(1, 40, PayoutType.COINS),
    SEG_2(5, 25, PayoutType.COINS),
    SEG_3(10, 15, PayoutType.COINS),
    SEG_4(20, 10, PayoutType.COINS),
    SEG_5(50, 7, PayoutType.COINS),
    SEG_6(100, 3, PayoutType.COINS),
    SEG_ITEM(0, 2, PayoutType.ITEM);

    private final int coins;
    private final int weight;
    private final PayoutType payoutType;

    WheelSegment(int coins, int weight, PayoutType payoutType) {
        this.coins = coins;
        this.weight = weight;
        this.payoutType = payoutType;
    }

    public int coins() {
        return coins;
    }

    public int weight() {
        return weight;
    }

    public PayoutType payoutType() {
        return payoutType;
    }

    public static int totalWeight() {
        int total = 0;
        for (WheelSegment segment : values()) {
            total += segment.weight;
        }
        return total;
    }

    /**
     * Weighted random selection. {@code rng} is injected so the pick is
     * deterministic under test; pass {@link ThreadLocalRandom#current()} in
     * production.
     */
    public static WheelSegment pick(Random rng) {
        int draw = rng.nextInt(totalWeight());
        int cumulative = 0;
        for (WheelSegment segment : values()) {
            cumulative += segment.weight;
            if (draw < cumulative) {
                return segment;
            }
        }
        return values()[values().length - 1];
    }
}

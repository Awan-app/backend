package com.ezdo.dto.gamification;

/**
 * Wallet point delta produced by a completion.
 *
 * @param awarded   true iff this completion actually credited points
 * @param amount    number of points credited (0 when {@code awarded} is false)
 * @param oldValue  wallet balance before the credit
 * @param newValue  wallet balance after the credit
 */
public record PointsDelta(
    boolean awarded,
    long amount,
    long oldValue,
    long newValue
) {}

package com.ezdo.dto.gamification;

/**
 * Streak delta produced by a completion.
 *
 * @param updated        true iff the user-visible effective streak changed
 * @param oldValue       effective streak before the activity
 * @param newValue       effective streak after the activity
 * @param maxStreakBroken true iff this activity set a new personal-best max streak
 * @param maxStreakOld   max streak before the activity
 * @param maxStreakNew   max streak after the activity
 */
public record StreakDelta(
    boolean updated,
    int oldValue,
    int newValue,
    boolean maxStreakBroken,
    int maxStreakOld,
    int maxStreakNew
) {}

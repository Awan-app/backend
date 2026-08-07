package com.ezdo.dto.gamification;

/**
 * The gamification outcome of a single session completion: the point and streak
 * deltas observed between the state before the completion and the state after.
 */
public record SessionCompletionReward(
    PointsDelta points,
    StreakDelta streak
) {}

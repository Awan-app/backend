package com.ezdo.dto.gamification;

/**
 * The gamification outcome of a completion — of one session, or of a task and
 * every session it closed: the point and streak deltas observed between the
 * state before and the state after.
 *
 * <p>For a task completion this is one aggregate delta for the whole call, not
 * one per session, so the streak is reported as having moved once.
 */
public record CompletionReward(
    PointsDelta points,
    StreakDelta streak
) {}

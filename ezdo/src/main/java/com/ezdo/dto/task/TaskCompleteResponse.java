package com.ezdo.dto.task;

import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.gamification.CompletionReward;
import com.ezdo.dto.goal.TaskInfoResponse;

import java.util.List;

/**
 * Response body of {@code POST /api/v1/tasks/{taskId}/complete}.
 *
 * @param task              the task in its new state ({@code status} is COMPLETED)
 * @param completedSessions only the sessions this call closed, so the client can
 *                          animate exactly those; empty when the task was already
 *                          completed
 * @param reward            one aggregate gamification delta for the whole call,
 *                          not one per session. Points credited are
 *                          {@code reward.points().amount()}.
 */
public record TaskCompleteResponse(
    TaskInfoResponse task,
    List<SessionResponse> completedSessions,
    CompletionReward reward
) {}

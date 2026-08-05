package com.ezdo.controller;

import com.ezdo.dto.ai.schedule.*;
import com.ezdo.service.AISchedulingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST surface for AI-based goal scheduling.
 *
 * <h3>Two-step workflow</h3>
 * <ol>
 *   <li>{@code POST /api/v1/ai/schedule} — asks the AI to plan sessions for
 *       all tasks in a goal and returns the full proposal without persisting
 *       anything.</li>
 *   <li>{@code POST /api/v1/ai/schedule/confirm} — the client sends back the
 *       sessions the user accepted; the server persists them and returns the
 *       saved records with their database IDs.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/ai/schedule")
@RequiredArgsConstructor
public class AISchedulingController {

    private final AISchedulingService schedulingService;

    /**
     * Produces an AI scheduling proposal for the requested goal.
     *
     * <p>The response contains three lists:
     * <ul>
     *   <li>{@code proposedSessions} — in-zone sessions ready to confirm.</li>
     *   <li>{@code suggestions} — no-zone or soft-overlap sessions for the
     *       user to review.</li>
     *   <li>{@code unscheduledTasks} — tasks the AI could not place anywhere.</li>
     * </ul>
     *
     * Nothing is written to the database by this call.
     */
    @PostMapping
    public ResponseEntity<AiGoalScheduleResponse> scheduleGoal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GoalScheduleRequest request
    ) {
        return ResponseEntity.ok(schedulingService.scheduleGoal(userId, request));
    }

    /**
     * Persists the sessions the user chose to accept from the scheduling
     * proposal.
     *
     * <p>The client may include any subset of {@code proposedSessions} and
     * accepted {@code suggestions} from the earlier scheduling response.
     * Each item is validated for ownership before being saved.
     *
     * @return the list of newly created sessions with their database IDs.
     */
    @PostMapping("/confirm")
    public ResponseEntity<List<ScheduledSessionResult>> confirmSchedule(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ConfirmScheduleRequest request
    ) {
        return ResponseEntity.ok(schedulingService.confirmSchedule(userId, request));
    }
}
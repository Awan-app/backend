package com.ezdo.service;

import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.goal.GoalInfoResponse;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.GoalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP-exposed tools that allow AI agents (Claude, Cursor, ChatGPT, VS Code)
 * to interact with the user's EZDO data.
 *
 * <p>Each {@code @Tool}-annotated method becomes a callable tool in the MCP
 * protocol. The method's Javadoc / {@code description} is shown to the AI
 * so it knows when and how to use each tool.
 *
 * <p><strong>Note:</strong> All methods require a {@code userId} parameter.
 * In a production MCP setup, this would be injected from the authenticated
 * session context. For now, the MCP client passes it explicitly (resolved
 * from the API key).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final GoalService goalService;
    private final TaskService taskService;
    private final SessionService sessionService;

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "List the user's goals. Returns goal titles, descriptions, statuses, and target dates. " +
                        "Use this when the user asks 'what are my goals?' or 'show me my goals'.")
    public List<GoalInfoResponse> listGoals(
            @ToolParam(description = "The user's UUID") String userId
    ) {
        log.info("[MCP] listGoals called for user {}", userId);
        return goalService.listGoals(
                UUID.fromString(userId),
                null,       // all statuses
                false,      // exclude inbox
                true,       // expand (include tasks)
                PageRequest.of(0, 50)
        ).getContent();
    }

    @Tool(description = "Get detailed info about a specific goal, including all its tasks. " +
                        "Use this when the user asks about a particular goal by name or ID.")
    public GoalInfoResponse getGoal(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "The goal's UUID") String goalId
    ) {
        log.info("[MCP] getGoal called for user {} goal {}", userId, goalId);
        return goalService.getGoal(
                UUID.fromString(userId),
                UUID.fromString(goalId),
                true    // expand tasks
        );
    }

    @Tool(description = "List all tasks belonging to a specific goal. " +
                        "Use when the user asks 'what tasks does goal X have?' or 'show me the tasks in my goal'.")
    public List<TaskInfoResponse> listTasksForGoal(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "The goal's UUID") String goalId
    ) {
        log.info("[MCP] listTasksForGoal called for user {} goal {}", userId, goalId);
        return taskService.listTasksForGoal(
                UUID.fromString(userId),
                UUID.fromString(goalId)
        );
    }

    @Tool(description = "Get the user's scheduled sessions for a specific date. " +
                        "Use when the user asks 'what's on my schedule today?' or 'what do I have on July 31?'. " +
                        "The date parameter should be in YYYY-MM-DD format.")
    public List<SessionResponse> getTodaySchedule(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "The date in YYYY-MM-DD format, e.g. 2026-07-30") String date
    ) {
        log.info("[MCP] getTodaySchedule called for user {} date {}", userId, date);
        return sessionService.getByDate(
                UUID.fromString(userId),
                LocalDate.parse(date)
        );
    }

    @Tool(description = "Get the user's schedule for a date range, grouped by day. " +
                        "Use when the user asks 'what's my schedule this week?' or 'show me next 3 days'. " +
                        "Dates should be in YYYY-MM-DD format.")
    public Map<LocalDate, List<SessionResponse>> getScheduleRange(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "Start date in YYYY-MM-DD format") String startDate,
            @ToolParam(description = "End date in YYYY-MM-DD format") String endDate
    ) {
        log.info("[MCP] getScheduleRange called for user {} from {} to {}", userId, startDate, endDate);
        return sessionService.getByDateRange(
                UUID.fromString(userId),
                LocalDate.parse(startDate),
                LocalDate.parse(endDate)
        );
    }

    @Tool(description = "Get the user's tasks for a specific date along with their sessions. " +
                        "Use when the user asks 'what tasks do I have today?' or needs task-level detail for a day.")
    public List<?> getTasksByDate(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "The date in YYYY-MM-DD format") String date
    ) {
        log.info("[MCP] getTasksByDate called for user {} date {}", userId, date);
        return taskService.getTasksByDate(
                UUID.fromString(userId),
                LocalDate.parse(date)
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "Update the status of a session. Valid statuses: SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED. " +
                        "Use when the user says 'mark my session as done' or 'I finished that task'.")
    public SessionResponse updateSessionStatus(
            @ToolParam(description = "The user's UUID") String userId,
            @ToolParam(description = "The session's UUID") String sessionId,
            @ToolParam(description = "New status: SCHEDULED, IN_PROGRESS, COMPLETED, or SKIPPED") String status
    ) {
        log.info("[MCP] updateSessionStatus called for user {} session {} status {}", userId, sessionId, status);
        return sessionService.updateStatus(
                UUID.fromString(userId),
                UUID.fromString(sessionId),
                com.ezdo.entity.SessionStatus.valueOf(status)
        );
    }
}

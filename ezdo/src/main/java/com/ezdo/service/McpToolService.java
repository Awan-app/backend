package com.ezdo.service;

import com.ezdo.dto.CategoryResponse;
import com.ezdo.dto.SessionRequest;
import com.ezdo.dto.SessionResponse;
import com.ezdo.dto.goal.*;
import com.ezdo.entity.GoalStatus;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.User;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MCP-exposed tools that allow AI agents (Claude, Cursor, ChatGPT, VS Code)
 * to interact with the user's EZDO data.
 *
 * <p>Each {@code @Tool}-annotated method becomes a callable tool in the MCP
 * protocol. The method's Javadoc / {@code description} is shown to the AI
 * so it knows when and how to use each tool.
 *
 * <p>The authenticated user's ID is automatically extracted from the
 * Spring Security context (set by the API-key or JWT filter). The AI
 * never needs to know or pass a user ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final GoalService goalService;
    private final TaskService taskService;
    private final SessionService sessionService;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
//    private final com.ezdo.service.AISchedulingService aiSchedulingService;
//    private final com.ezdo.service.ai.decompose.GoalDecompositionService decompositionService;

    // ─── Helper ──────────────────────────────────────────────────────────────

    private UUID currentUserId() {
        return UUID.fromString(
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Goals
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "List the user's goals. Returns goal titles, descriptions, statuses, and target dates. " +
                        "Use this when the user asks 'what are my goals?' or 'show me my goals'.")
    public List<GoalInfoResponse> listGoals() {
        UUID userId = currentUserId();
        log.info("[MCP] listGoals called for user {}", userId);
        return goalService.listGoals(
                userId,
                null,       // all statuses
                false,      // exclude inbox
                true,       // expand (include tasks)
                Pageable.unpaged()
        ).getContent();
    }

    @Tool(description = "Get detailed info about a specific goal, including all its tasks. " +
                        "Use this when the user asks about a particular goal by name or ID.")
    public GoalInfoResponse getGoal(
            @ToolParam(description = "The goal's UUID") String goalId
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getGoal called for user {} goal {}", userId, goalId);
        return goalService.getGoal(userId, UUID.fromString(goalId), true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Tasks
    // ═══════════════════════════════════════════════════════════════════════


    @Tool(description = "List all tasks belonging to a specific goal. " +
                        "Use when the user asks 'what tasks does goal X have?' or 'show me the tasks in my goal'.")
    public List<TaskInfoResponse> listTasksForGoal(
            @ToolParam(description = "The goal's UUID") String goalId
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] listTasksForGoal called for user {} goal {}", userId, goalId);
        return taskService.listTasksForGoal(userId, UUID.fromString(goalId));
    }

    @Tool(description = "Get the user's tasks for a specific date along with their sessions. " +
                        "Use when the user asks 'what tasks do I have today?' or needs task-level detail for a day.")
    public List<?> getTasksByDate(
            @ToolParam(description = "The date in YYYY-MM-DD format") String date
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getTasksByDate called for user {} date {}", userId, date);
        return taskService.getTasksByDate(userId, LocalDate.parse(date));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Schedule / Sessions
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "Get the user's scheduled sessions for a specific date. " +
                        "Use when the user asks 'what's on my schedule today?' or 'what do I have on July 31?'. " +
                        "The date parameter should be in YYYY-MM-DD format.")
    public List<SessionResponse> getTodaySchedule(
            @ToolParam(description = "The date in YYYY-MM-DD format, e.g. 2026-07-30") String date
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getTodaySchedule called for user {} date {}", userId, date);
        return sessionService.getByDate(userId, LocalDate.parse(date));
    }

    @Tool(description = "Get the user's schedule for a date range, grouped by day. " +
                        "Use when the user asks 'what's my schedule this week?' or 'show me next 3 days'. " +
                        "Dates should be in YYYY-MM-DD format.")
    public Map<LocalDate, List<SessionResponse>> getScheduleRange(
            @ToolParam(description = "Start date in YYYY-MM-DD format") String startDate,
            @ToolParam(description = "End date in YYYY-MM-DD format") String endDate
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getScheduleRange called for user {} from {} to {}", userId, startDate, endDate);
        return sessionService.getByDateRange(userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Context
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "List the user's categories (e.g. Work, Study, Exercise). " +
                        "Use when creating a task and you need to assign it to a category, " +
                        "or when the user asks 'what categories do I have?'.")
    public List<CategoryResponse> listCategories() {
        UUID userId = currentUserId();
        log.info("[MCP] listCategories called for user {}", userId);
        return categoryService.getAll(userId);
    }

    @Tool(description = "Get the user's scheduling preferences and context: timezone, wake/sleep times, " +
                        "preferred session duration, and buffer between sessions. " +
                        "Use this to understand the user's availability before creating or scheduling tasks.")
    public Map<String, Object> getUserContext() {
        UUID userId = currentUserId();
        log.info("[MCP] getUserContext called for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Preferences prefs = user.getPreferences();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userId", userId.toString());
        context.put("name", user.getFirstName() + " " + user.getLastName());
        context.put("email", user.getEmail());
        if (prefs != null) {
            context.put("timezone", prefs.getTimezone());
            context.put("wakeupTime", prefs.getWakeupTime() != null ? prefs.getWakeupTime().toString() : null);
            context.put("sleepTime", prefs.getSleepTime() != null ? prefs.getSleepTime().toString() : null);
            context.put("preferredSessionDurationMinutes", prefs.getPreferredSessionDuration());
            context.put("bufferBetweenSessionsMinutes", prefs.getBufferBetweenSessions());
            context.put("schedulingType", prefs.getSchedulingType() != null ? prefs.getSchedulingType().name() : null);
        }
        return context;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Goals
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "Create a new goal for the user. A goal is a high-level objective (e.g. 'Learn Spanish', 'Run a marathon'). " +
                        "Use when the user says 'I want to achieve X' or 'create a new goal'. " +
                        "The targetDate is optional and should be a future date in YYYY-MM-DD format.")
    public GoalInfoResponse createGoal(
            @ToolParam(description = "The goal's title, e.g. 'Learn Spanish'") String title,
            @ToolParam(description = "Optional description of the goal") String description,
            @ToolParam(description = "Optional target date in YYYY-MM-DD format, must be in the future") String targetDate
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] createGoal called for user {} title '{}'", userId, title);
        LocalDate target = (targetDate != null && !targetDate.isBlank())
                ? LocalDate.parse(targetDate) : null;
        GoalCreateRequest request = new GoalCreateRequest(title, description, target, List.of());
        return goalService.createGoal(userId, request);
    }

    @Tool(description = "Update an existing goal. You can change its title, description, status, or target date. " +
                        "Valid statuses: ACTIVE, ACHIEVED. " +
                        "Use when the user says 'rename my goal', 'I achieved my goal', or 'change the deadline'.")
    public GoalInfoResponse updateGoal(
            @ToolParam(description = "The goal's UUID") String goalId,
            @ToolParam(description = "New title, or null to keep current") String title,
            @ToolParam(description = "New description, or null to keep current") String description,
            @ToolParam(description = "New status: ACTIVE or ACHIEVED, or null to keep current") String status,
            @ToolParam(description = "New target date in YYYY-MM-DD format, or null to keep current") String targetDate
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] updateGoal called for user {} goal {}", userId, goalId);
        GoalStatus goalStatus = (status != null && !status.isBlank())
                ? GoalStatus.valueOf(status) : null;
        LocalDate target = (targetDate != null && !targetDate.isBlank())
                ? LocalDate.parse(targetDate) : null;
        GoalUpdateRequest request = new GoalUpdateRequest(
                (title != null && !title.isBlank()) ? title : null,
                (description != null && !description.isBlank()) ? description : null,
                goalStatus,
                target
        );
        return goalService.updateGoal(userId, UUID.fromString(goalId), request);
    }

    @Tool(description = "Delete a goal and all its tasks. This is permanent and cannot be undone. " +
                        "Use when the user explicitly asks to delete or remove a goal. " +
                        "The Inbox goal cannot be deleted.")
    public String deleteGoal(
            @ToolParam(description = "The goal's UUID") String goalId
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] deleteGoal called for user {} goal {}", userId, goalId);
        goalService.deleteGoal(userId, UUID.fromString(goalId));
        return "Goal deleted successfully.";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Tasks
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "Create a new task. If goalId is not provided, the task goes into the Inbox. " +
                        "Use when the user says 'add a task', 'remind me to...', or 'I need to do X'. " +
                        "estimatedDuration is in minutes (defaults to user's preferred session duration if not provided).")
    public TaskInfoResponse createTask(
            @ToolParam(description = "The task title, e.g. 'Buy groceries'") String title,
            @ToolParam(description = "Optional description of what needs to be done") String description,
            @ToolParam(description = "Optional UUID of the goal this task belongs to. Omit to add to Inbox.") String goalId,
            @ToolParam(description = "Optional estimated duration in minutes, e.g. 30 or 60") Integer estimatedDuration
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] createTask called for user {} title '{}'", userId, title);
        UUID goalUuid = (goalId != null && !goalId.isBlank()) ? UUID.fromString(goalId) : null;
        TaskCreateRequest request = new TaskCreateRequest(
                title,
                description,
                estimatedDuration,
                null,   // mandatory — default
                null,   // estimatedPoints — default
                null,   // allowTaskSplitting — default
                goalUuid,
                null    // categoryId — can be set via updateTask
        );
        return taskService.createTask(userId, request);
    }

    @Tool(description = "Update an existing task. You can change its title, description, estimated duration, or mark it as completed/cancelled. " +
                        "Valid statuses: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED. " +
                        "Use when the user says 'update my task', 'mark task as done', or 'change the task name'.")
    public TaskInfoResponse updateTask(
            @ToolParam(description = "The task's UUID") String taskId,
            @ToolParam(description = "New title, or null to keep current") String title,
            @ToolParam(description = "New description, or null to keep current") String description,
            @ToolParam(description = "New estimated duration in minutes, or null to keep current") Integer estimatedDuration,
            @ToolParam(description = "New status: SCHEDULED, IN_PROGRESS, COMPLETED, or CANCELLED. Null to keep current.") String status
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] updateTask called for user {} task {}", userId, taskId);
        com.ezdo.entity.TaskStatus taskStatus = (status != null && !status.isBlank())
                ? com.ezdo.entity.TaskStatus.valueOf(status) : null;
        TaskUpdateRequest request = new TaskUpdateRequest(
                (title != null && !title.isBlank()) ? title : null,
                (description != null && !description.isBlank()) ? description : null,
                estimatedDuration,
                taskStatus,
                null, // mandatory
                null, // estimatedPoints
                null, // allowTaskSplitting
                null  // categoryId
        );
        return taskService.updateTask(userId, UUID.fromString(taskId), request);
    }

    @Tool(description = "Delete a task permanently. This cannot be undone. " +
                        "Use when the user explicitly asks to delete or remove a task.")
    public String deleteTask(
            @ToolParam(description = "The task's UUID") String taskId
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] deleteTask called for user {} task {}", userId, taskId);
        taskService.deleteTask(userId, UUID.fromString(taskId), true);
        return "Task deleted successfully.";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Sessions
    // ═══════════════════════════════════════════════════════════════════════

    @Tool(description = "Update the status of a session. Valid statuses: SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED. " +
                        "Use when the user says 'mark my session as done' or 'I finished that task'.")
    public SessionResponse updateSessionStatus(
            @ToolParam(description = "The session's UUID") String sessionId,
            @ToolParam(description = "New status: SCHEDULED, IN_PROGRESS, COMPLETED, or SKIPPED") String status
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] updateSessionStatus called for user {} session {} status {}", userId, sessionId, status);
        return sessionService.updateStatus(
                userId,
                UUID.fromString(sessionId),
                com.ezdo.entity.SessionStatus.valueOf(status)
        );
    }

    @Tool(description = "Reschedule a session to a new time. Use when the user says 'move my session to 3 PM' or 'reschedule it to tomorrow'. " +
                        "Both start and end must be provided in ISO date-time format: YYYY-MM-DDTHH:mm (e.g. 2026-08-05T14:00).")
    public SessionResponse rescheduleSession(
            @ToolParam(description = "The session's UUID") String sessionId,
            @ToolParam(description = "New start time in ISO format, e.g. 2026-08-05T14:00") String start,
            @ToolParam(description = "New end time in ISO format, e.g. 2026-08-05T15:00") String end
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] rescheduleSession called for user {} session {} to {}-{}", userId, sessionId, start, end);
        SessionRequest request = new SessionRequest(
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                null // keep current status
        );
        return sessionService.update(userId, UUID.fromString(sessionId), request);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — AI Delegation
    // ═══════════════════════════════════════════════════════════════════════

//    @Tool(description = "Ask the AI scheduling engine to automatically find time slots for all tasks in a goal. " +
//                        "This uses the user's templates, zones, and preferences to find the best available time slots. " +
//                        "This will ONLY return a PROPOSAL. It will NOT save anything to the database. " +
//                        "Present the proposal to the user, and if they like it, you can create the sessions manually using updateSessionStatus/createSession if available, or just tell them to review it in the app.")
//    public com.ezdo.dto.ai.schedule.AiGoalScheduleResponse autoScheduleGoal(
//            @ToolParam(description = "The goal's UUID to schedule") String goalId
//    ) {
//        UUID userId = currentUserId();
//        log.info("[MCP] autoScheduleGoal called for user {} goal {}", userId, goalId);
//        com.ezdo.dto.ai.schedule.GoalScheduleRequest request = new com.ezdo.dto.ai.schedule.GoalScheduleRequest(UUID.fromString(goalId));
//        return aiSchedulingService.scheduleGoal(userId, request);
//    }
//
//    @Tool(description = "Ask the AI backend engine to automatically break down a high-level goal into smaller tasks. " +
//                        "This creates a chat session that proposes a list of tasks. " +
//                        "Use when the user says 'help me break down this goal' or 'create a plan for X'. " +
//                        "You must pass a message to start the decomposition.")
//    public com.ezdo.dto.ai.decompose.ChatReply decomposeGoal(
//            @ToolParam(description = "The message to send to the AI, e.g. 'I want to learn Spanish'") String message
//    ) {
//        UUID userId = currentUserId();
//        log.info("[MCP] decomposeGoal called for user {} message '{}'", userId, message);
//        com.ezdo.dto.ai.decompose.ChatMessage request = new com.ezdo.dto.ai.decompose.ChatMessage(null, message); // null sessionId starts a new session
//        return decompositionService.processMessage(userId, request);
//    }
}

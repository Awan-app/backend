package com.ezdo.service;

import com.ezdo.dto.*;
import com.ezdo.dto.gamification.SessionCompleteResponse;
import com.ezdo.dto.goal.*;
import com.ezdo.dto.profile.UserProgressResponse;
import com.ezdo.dto.task.*;
import com.ezdo.entity.*;
import com.ezdo.exception.GoalNotFoundException;
import com.ezdo.exception.UserNotFoundException;
import com.ezdo.mapper.TaskMapper;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * MCP-exposed tools that allow AI agents (Claude, Cursor, ChatGPT, VS Code)
 * to interact with the user's EZDO data.
 *
 * <p>Each {@code @McpTool}-annotated method becomes a callable tool in the MCP
 * protocol. The method's description is shown to the AI so it knows when and
 * how to use each tool. The {@code title} attribute is a human-readable display
 * name (per the MCP spec) shown to end users in client UIs, separate from the
 * machine-facing {@code name} identifier the model actually calls. The
 * {@code annotations} attribute carries client hints (readOnlyHint,
 * destructiveHint, idempotentHint) so MCP clients like Claude can categorize
 * tools into read-only vs. write/delete buckets in their permissions UI.
 *
 * <p>The authenticated user's ID is automatically extracted from the
 * Spring Security context (set by the API-key or JWT filter). The AI
 * never needs to know or pass a user ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private static final int MAX_DATE_RANGE_DAYS = 90;

    private final GoalService goalService;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final SessionService sessionService;
    private final CategoryService categoryService;
    private final AvailabilityService availabilityService;
    private final GamificationService gamificationService;
    private final UserService userService;
    private final UserClockService userClockService;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("No authenticated user found in security context.");
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Authenticated principal name is not a valid UUID: " + auth.getName());
        }
    }

    private UUID parseUuid(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required but was empty.");
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName + ": '" + value + "' is not a valid UUID. " +
                            "Look up the correct ID first (e.g. via listGoals, listTasksForGoal, or getScheduleForDate) " +
                            "rather than guessing one.");
        }
    }

    /** Guards range-based tools against unbounded or inverted date ranges. */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "endDate (" + end + ") is before startDate (" + start + "). Swap the two dates.");
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Date range too large: " + days + " days requested, max is " + MAX_DATE_RANGE_DAYS +
                            ". Narrow the range and try again.");
        }
    }

    private LocalDate userToday(User user) {
        return userClockService.today(user);
    }

    private LocalDateTime userNow(User user) {
        ZoneId zone = userClockService.zoneOf(user);
        return LocalDateTime.now(zone);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Goals
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "listGoals",
            title = "View Goals",
            description = "List the user's goals. Returns goal titles, descriptions, statuses, and target dates. " +
                    "Use this when the user asks 'what are my goals?' or 'show me my goals'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    @Transactional(readOnly = true)
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

    @McpTool(
            name = "getGoal",
            title = "View Goal Details",
            description = "Get detailed info about a specific goal, including all its tasks. " +
                    "Use this when the user asks about a particular goal by name or ID.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    @Transactional(readOnly = true)
    public GoalInfoResponse getGoal(
            @McpToolParam(description = "The goal's UUID", required = true) String goalId
    ) {
        UUID userId = currentUserId();
        UUID goalUuid = parseUuid(goalId, "goalId");
        log.info("[MCP] getGoal called for user {} goal {}", userId, goalUuid);
        return goalService.getGoal(userId, goalUuid, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Tasks
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "listTasksForGoal",
            title = "View Goal's Tasks",
            description = "List all tasks belonging to a specific goal. " +
                    "Use when the user asks 'what tasks does goal X have?' or 'show me the tasks in my goal'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<TaskInfoResponse> listTasksForGoal(
            @McpToolParam(description = "The goal's UUID", required = true) String goalId
    ) {
        UUID userId = currentUserId();
        UUID goalUuid = parseUuid(goalId, "goalId");
        log.info("[MCP] listTasksForGoal called for user {} goal {}", userId, goalUuid);
        return taskService.listTasksForGoal(userId, goalUuid);
    }

    @McpTool(
            name = "getTasksByDate",
            title = "View Tasks for a Date",
            description = "Get the user's tasks for a specific date along with their sessions. " +
                    "Use when the user asks 'what tasks do I have today?' or needs task-level detail for a day.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<?> getTasksByDate(
            @McpToolParam(description = "The date in YYYY-MM-DD format", required = true) String date
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getTasksByDate called for user {} date {}", userId, date);
        return taskService.getTasksByDate(userId, LocalDate.parse(date));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Schedule / Sessions
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "getScheduleForDate",
            title = "View Schedule for a Date",
            description = "Get the user's scheduled sessions for a single specific date (any date, not just today). " +
                    "Use when the user asks 'what's on my schedule today?' or 'what do I have on July 31?'. " +
                    "The date parameter should be in YYYY-MM-DD format. For multiple days, use getScheduleRange instead.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<SessionResponse> getScheduleForDate(
            @McpToolParam(description = "The date in YYYY-MM-DD format, e.g. 2026-07-30", required = true) String date
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] getScheduleForDate called for user {} date {}", userId, date);
        return sessionService.getByDate(userId, LocalDate.parse(date), null);
    }

    @McpTool(
            name = "getScheduleRange",
            title = "View Schedule for a Date Range",
            description = "Get the user's schedule for a date range (max 90 days), grouped by day. " +
                    "Use when the user asks 'what's my schedule this week?' or 'show me next 3 days'. " +
                    "Dates should be in YYYY-MM-DD format.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public Map<LocalDate, List<SessionResponse>> getScheduleRange(
            @McpToolParam(description = "Start date in YYYY-MM-DD format", required = true) String startDate,
            @McpToolParam(description = "End date in YYYY-MM-DD format", required = true) String endDate
    ) {
        UUID userId = currentUserId();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        validateDateRange(start, end);
        log.info("[MCP] getScheduleRange called for user {} from {} to {}", userId, start, end);
        return sessionService.getByDateRange(userId, start, end, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Context
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "listCategories",
            title = "View Categories",
            description = "List the user's categories (e.g. Work, Study, Exercise) along with their IDs. " +
                    "Use when creating or updating a task and you need a categoryId to assign, " +
                    "or when the user asks 'what categories do I have?'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public List<CategoryResponse> listCategories() {
        UUID userId = currentUserId();
        log.info("[MCP] listCategories called for user {}", userId);
        return categoryService.getAll(userId);
    }

    @McpTool(
            name = "getUserContext",
            title = "View Scheduling Preferences",
            description = "Get the user's scheduling preferences and context: timezone, wake/sleep times, " +
                    "preferred session duration, and buffer between sessions. " +
                    "Use this to understand the user's availability before creating or scheduling tasks.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public Map<String, Object> getUserContext() {
        UUID userId = currentUserId();
        log.info("[MCP] getUserContext called for user {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Preferences prefs = user.getPreferences();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userId", userId.toString());
        context.put("name", user.getFirstName() + " " + user.getLastName());
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

    @McpTool(
            name = "createGoal",
            title = "Create Goal",
            description = "Create a new goal for the user. A goal is a high-level objective (e.g. 'Learn Spanish', 'Run a marathon'). " +
                    "Use when the user says 'I want to achieve X' or 'create a new goal'. " +
                    "The targetDate is optional and should be a future date in YYYY-MM-DD format.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = false
            )
    )
    public GoalInfoResponse createGoal(
            @McpToolParam(description = "The goal's title, e.g. 'Learn Spanish'", required = true) String title,
            @McpToolParam(description = "Optional description of the goal", required = false) String description,
            @McpToolParam(description = "Optional target date in YYYY-MM-DD format, must be in the future", required = false) String targetDate
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] createGoal called for user {} title '{}'", userId, title);
        LocalDate target = (targetDate != null && !targetDate.isBlank())
                ? LocalDate.parse(targetDate) : null;
        GoalCreateRequest request = new GoalCreateRequest(title, description, target, List.of());
        return goalService.createGoal(userId, request);
    }

    @McpTool(
            name = "updateGoal",
            title = "Edit Goal",
            description = "Update an existing goal. You can change its title, description, status, or target date. " +
                    "Valid statuses: ACTIVE, ACHIEVED. " +
                    "Use when the user says 'rename my goal', 'I achieved my goal', or 'change the deadline'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public GoalInfoResponse updateGoal(
            @McpToolParam(description = "The goal's UUID", required = true) String goalId,
            @McpToolParam(description = "New title, or null to keep current", required = false) String title,
            @McpToolParam(description = "New description, or null to keep current", required = false) String description,
            @McpToolParam(description = "New status: ACTIVE or ACHIEVED, or null to keep current", required = false) String status,
            @McpToolParam(description = "New target date in YYYY-MM-DD format, or null to keep current", required = false) String targetDate
    ) {
        UUID userId = currentUserId();
        UUID goalUuid = parseUuid(goalId, "goalId");
        log.info("[MCP] updateGoal called for user {} goal {}", userId, goalUuid);
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
        return goalService.updateGoal(userId, goalUuid, request);
    }

    @McpTool(
            name = "deleteGoal",
            title = "Delete Goal",
            description = "Delete a goal AND ALL OF ITS TASKS. This is permanent and cannot be undone. " +
                    "Use only when the user explicitly asks to delete or remove a goal — confirm with the user first " +
                    "if the goal has tasks in it, since those will be deleted too. " +
                    "The Inbox goal cannot be deleted.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = true,
                    idempotentHint = true
            )
    )
    public String deleteGoal(
            @McpToolParam(description = "The goal's UUID", required = true) String goalId
    ) {
        UUID userId = currentUserId();
        UUID goalUuid = parseUuid(goalId, "goalId");
        log.info("[MCP] deleteGoal called for user {} goal {}", userId, goalUuid);
        goalService.deleteGoal(userId, goalUuid);
        return "Goal deleted successfully.";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Tasks
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "createTask",
            title = "Create Task",
            description = "Create a new task. If goalId is not provided, the task goes into the Inbox. " +
                    "Use when the user says 'add a task', 'remind me to...', or 'I need to do X'. " +
                    "estimatedDuration is in minutes (defaults to user's preferred session duration if not provided). " +
                    "Use listCategories first if you need a categoryId.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = false
            )
    )
    public TaskInfoResponse createTask(
            @McpToolParam(description = "The task title, e.g. 'Buy groceries'", required = true) String title,
            @McpToolParam(description = "Optional description of what needs to be done", required = false) String description,
            @McpToolParam(description = "Optional UUID of the goal this task belongs to. Omit to add to Inbox.", required = false) String goalId,
            @McpToolParam(description = "Optional estimated duration in minutes, e.g. 30 or 60", required = false) Integer estimatedDuration,
            @McpToolParam(description = "Optional UUID of the category to assign. Use listCategories to find valid IDs.", required = false) String categoryId
    ) {
        UUID userId = currentUserId();
        log.info("[MCP] createTask called for user {} title '{}'", userId, title);
        UUID goalUuid = (goalId != null && !goalId.isBlank()) ? parseUuid(goalId, "goalId") : null;
        UUID categoryUuid = (categoryId != null && !categoryId.isBlank()) ? parseUuid(categoryId, "categoryId") : null;
        TaskCreateRequest request = new TaskCreateRequest(
                title,
                description,
                estimatedDuration,
                null,   // mandatory — default
                null,   // estimatedPoints — default
                null,   // allowTaskSplitting — default
                goalUuid,
                categoryUuid
        );
        return taskService.createTask(userId, request);
    }

    @McpTool(
            name = "updateTask",
            title = "Edit Task",
            description = "Update an existing task's title, description, estimated duration, or category. " +
                    "This cannot complete a task — use completeTask for that. " +
                    "This cannot move a task to a different goal — use moveTaskToGoal for that. " +
                    "Use when the user says 'update my task' or 'change the task name'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public TaskInfoResponse updateTask(
            @McpToolParam(description = "The task's UUID", required = true) String taskId,
            @McpToolParam(description = "New title, or null to keep current", required = false) String title,
            @McpToolParam(description = "New description, or null to keep current", required = false) String description,
            @McpToolParam(description = "New estimated duration in minutes, or null to keep current", required = false) Integer estimatedDuration,
            @McpToolParam(description = "New category UUID, or null to keep current. Use listCategories to find valid IDs.", required = false) String categoryId
    ) {
        UUID userId = currentUserId();
        UUID taskUuid = parseUuid(taskId, "taskId");
        log.info("[MCP] updateTask called for user {} task {}", userId, taskUuid);
        UUID categoryUuid = (categoryId != null && !categoryId.isBlank()) ? parseUuid(categoryId, "categoryId") : null;
        TaskUpdateRequest request = new TaskUpdateRequest(
                (title != null && !title.isBlank()) ? title : null,
                (description != null && !description.isBlank()) ? description : null,
                estimatedDuration,
                null, // mandatory
                null, // estimatedPoints
                null, // allowTaskSplitting
                categoryUuid
        );
        return taskService.updateTask(userId, taskUuid, request);
    }

    @McpTool(
            name = "completeTask",
            title = "Mark Task Complete",
            description = "Mark the ENTIRE task as done — including all of its still-scheduled sessions, whose points " +
                    "are awarded in the process. Afterwards the task's status is COMPLETED. " +
                    "Use when the user means the whole task is finished, e.g. 'mark this task as done' or " +
                    "'I'm done with this task' — NOT when only one of several scheduled sessions was completed " +
                    "(use completeSession for that instead).",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public TaskCompleteResponse completeTask(
            @McpToolParam(description = "The task's UUID", required = true) String taskId
    ) {
        UUID userId = currentUserId();
        UUID taskUuid = parseUuid(taskId, "taskId");
        log.info("[MCP] completeTask called for user {} task {}", userId, taskUuid);
        return taskService.completeTask(userId, taskUuid);
    }

    @McpTool(
            name = "deleteTask",
            title = "Delete Task",
            description = "Delete a task permanently, including its sessions. This cannot be undone. " +
                    "Use only when the user explicitly asks to delete or remove a task.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = true,
                    idempotentHint = true
            )
    )
    public String deleteTask(
            @McpToolParam(description = "The task's UUID", required = true) String taskId
    ) {
        UUID userId = currentUserId();
        UUID taskUuid = parseUuid(taskId, "taskId");
        log.info("[MCP] deleteTask called for user {} task {}", userId, taskUuid);
        taskService.deleteTask(userId, taskUuid, true);
        return "Task deleted successfully.";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Sessions
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(
            name = "completeSession",
            title = "Mark Session Complete",
            description = "Mark ONE specific scheduled session (time block) as completed — not the whole task. " +
                    "The user earns that session's points and their streak is updated (this happens once per " +
                    "session, ever — re-completing awards nothing). The response includes the points and streak " +
                    "reward deltas. Use when the user is completing a single block of a multi-session task, e.g. " +
                    "'I just finished my 3pm study block'. If the user means the whole task is done, use completeTask instead.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SessionCompleteResponse completeSession(
            @McpToolParam(description = "The session's UUID", required = true) String sessionId
    ) {
        UUID userId = currentUserId();
        UUID sessionUuid = parseUuid(sessionId, "sessionId");
        log.info("[MCP] completeSession called for user {} session {}", userId, sessionUuid);
        return sessionService.complete(userId, sessionUuid);
    }

    @McpTool(
            name = "uncompleteSession",
            title = "Undo Session Completion",
            description = "Revert a completed session back to scheduled (undo completion). " +
                    "No points or streak are revoked. " +
                    "Use when the user says 'undo completion' or 'I didn't actually finish that task'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SessionResponse uncompleteSession(
            @McpToolParam(description = "The session's UUID", required = true) String sessionId
    ) {
        UUID userId = currentUserId();
        UUID sessionUuid = parseUuid(sessionId, "sessionId");
        log.info("[MCP] uncompleteSession called for user {} session {}", userId, sessionUuid);
        return sessionService.uncomplete(userId, sessionUuid);
    }

    @McpTool(
            name = "cancelSession",
            title = "Cancel Session",
            description = "Cancel a session — it will not be executed or completed. " +
                    "Use when the user says 'cancel my session' or 'remove this from my schedule'.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SessionResponse cancelSession(
            @McpToolParam(description = "The session's UUID", required = true) String sessionId
    ) {
        UUID userId = currentUserId();
        UUID sessionUuid = parseUuid(sessionId, "sessionId");
        log.info("[MCP] cancelSession called for user {} session {}", userId, sessionUuid);
        return sessionService.cancel(userId, sessionUuid);
    }

    @McpTool(
            name = "rescheduleSession",
            title = "Reschedule Session",
            description = "Reschedule a session to a new time. Use when the user says 'move my session to 3 PM' or 'reschedule it to tomorrow'. " +
                    "Both start and end must be provided in ISO date-time format: YYYY-MM-DDTHH:mm (e.g. 2026-08-05T14:00). " +
                    "Consider calling getTaskDependencies first to avoid breaking dependency order.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = false,
                    destructiveHint = false,
                    idempotentHint = true
            )
    )
    public SessionResponse rescheduleSession(
            @McpToolParam(description = "The session's UUID", required = true) String sessionId,
            @McpToolParam(description = "New start time in ISO format, e.g. 2026-08-05T14:00", required = true) String start,
            @McpToolParam(description = "New end time in ISO format, e.g. 2026-08-05T15:00", required = true) String end
    ) {
        UUID userId = currentUserId();
        UUID sessionUuid = parseUuid(sessionId, "sessionId");
        log.info("[MCP] rescheduleSession called for user {} session {} to {}-{}", userId, sessionUuid, start, end);
        SessionRequest request = new SessionRequest(
                LocalDateTime.parse(start),
                LocalDateTime.parse(end),
                null // keep current status
        );
        return sessionService.update(userId, sessionUuid, request);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Intelligence
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "getDailyBriefing",
            title = "Get Daily Briefing",
            description = "Get a comprehensive daily briefing for the user. Returns: " +
                    "(1) today's scheduled sessions with task names, " +
                    "(2) missed sessions from yesterday that were never completed, " +
                    "(3) goals with upcoming deadlines in the next 7 days, " +
                    "(4) user preferences (timezone, wake/sleep times). " +
                    "Useful when the user wants a general overview of their day or asks to be caught up " +
                    "(e.g. 'what's on my plate today', 'catch me up'), or before making broad scheduling " +
                    "suggestions. Not needed for narrow questions unrelated to schedule or goals.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyBriefing() {
        UUID userId = currentUserId();
        log.info("[MCP] getDailyBriefing called for user {}", userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Preferences prefs = user.getPreferences();
        LocalDate today = userToday(user);
        LocalDate yesterday = today.minusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        List<SessionResponse> todaySessions = sessionService.getByDate(userId, today, null);

        List<Session> missedEntityList = sessionRepository.findMissed(userId, userNow(user), yesterday.atStartOfDay(), today.atStartOfDay());
        List<Map<String, Object>> missedYesterday = new ArrayList<>();
        for (Session s : missedEntityList) {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", s.getId());
            map.put("start", s.getStart().toString());
            map.put("end", s.getEnd().toString());
            map.put("taskId", s.getTask().getId());
            map.put("taskTitle", s.getTask().getTitle());
            map.put("goalTitle", s.getTask().getGoal().getTitle());
            missedYesterday.add(map);
        }

        List<Map<String, Object>> upcomingDeadlines = new ArrayList<>();
        goalRepository.findByUserIdAndInboxFalse(userId, Pageable.unpaged()).forEach(goal -> {
            if (goal.getTargetDate() != null && !goal.getTargetDate().isBefore(today) && !goal.getTargetDate().isAfter(nextWeek) && goal.getStatus() == GoalStatus.ACTIVE) {
                Map<String, Object> map = new HashMap<>();
                map.put("goalId", goal.getId());
                map.put("title", goal.getTitle());
                map.put("targetDate", goal.getTargetDate().toString());
                upcomingDeadlines.add(map);
            }
        });

        Map<String, Object> briefing = new HashMap<>();
        briefing.put("currentDate", today.toString());
        briefing.put("currentTime", userNow(user).toString());
        briefing.put("todaySessions", todaySessions);
        briefing.put("yesterdayMissed", missedYesterday);
        briefing.put("upcomingDeadlines", upcomingDeadlines);

        Map<String, Object> context = new HashMap<>();
        if (prefs != null) {
            context.put("timezone", prefs.getTimezone());
            context.put("wakeupTime", prefs.getWakeupTime() != null ? prefs.getWakeupTime().toString() : null);
            context.put("sleepTime", prefs.getSleepTime() != null ? prefs.getSleepTime().toString() : null);
        }
        briefing.put("preferences", context);

        return briefing;
    }

    @McpTool(name = "getMissedSessions",
            title = "View Missed Sessions",
            description = "Get sessions the user missed (were SCHEDULED but their time has passed without completion), " +
                    "within a date range of max 90 days. " +
                    "Returns enriched data: each missed session includes the task name, goal name, original time, and duration. " +
                    "Use this to help the user recover from missed work, then suggest rescheduling into available slots.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMissedSessions(
            @McpToolParam(description = "Start date in YYYY-MM-DD format", required = true) String startDate,
            @McpToolParam(description = "End date in YYYY-MM-DD format", required = true) String endDate) {
        UUID userId = currentUserId();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        validateDateRange(start, end);
        log.info("[MCP] getMissedSessions called for user {}", userId);

        List<Session> missedEntityList = sessionRepository.findMissed(userId, userNow(userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId))), start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        List<Map<String, Object>> missed = new ArrayList<>();
        for (Session s : missedEntityList) {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", s.getId());
            map.put("start", s.getStart().toString());
            map.put("end", s.getEnd().toString());
            map.put("taskId", s.getTask().getId());
            map.put("taskTitle", s.getTask().getTitle());
            map.put("goalTitle", s.getTask().getGoal().getTitle());
            missed.add(map);
        }
        return missed;
    }

    @McpTool(name = "getAvailableSlots",
            title = "Find Available Time Slots",
            description = "Find available time slots in the user's schedule for a date range (max 90 days). " +
                    "Each slot shows start/end time, which zone it falls in (if any), and the zone's category. " +
                    "Use this BEFORE creating or rescheduling sessions to find where to place them. " +
                    "Respects the user's wake/sleep times and existing sessions.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public Map<LocalDate, List<AvailableSlot>> getAvailableSlots(
            @McpToolParam(description = "Start date in YYYY-MM-DD format", required = true) String startDate,
            @McpToolParam(description = "End date in YYYY-MM-DD format", required = true) String endDate) {
        UUID userId = currentUserId();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        validateDateRange(start, end);
        log.info("[MCP] getAvailableSlots called for user {}", userId);
        return availabilityService.getAvailableSlotsForRange(userId, start, end);
    }

    @McpTool(name = "getTaskDependencies",
            title = "View Task Dependencies",
            description = "Get the dependency graph for a task: which tasks must be done BEFORE this one (prerequisites), " +
                    "and which tasks are BLOCKED until this one completes (dependents). " +
                    "Use this before rescheduling to avoid breaking dependency order.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    @Transactional(readOnly = true)
    public Map<String, Object> getTaskDependencies(
            @McpToolParam(description = "The task's UUID", required = true) String taskId) {
        UUID userId = currentUserId();
        UUID taskUuid = parseUuid(taskId, "taskId");
        log.info("[MCP] getTaskDependencies called for user {} task {}", userId, taskUuid);

        Map<String, Object> res = new HashMap<>();
        res.put("task", taskService.getTask(userId, taskUuid));
        res.put("dependsOn", taskService.listDependencies(userId, taskUuid));
        res.put("dependents", taskService.listDependents(userId, taskUuid));
        return res;
    }

    @McpTool(name = "getGoalProgress",
            title = "View Goal Progress",
            description = "Get detailed progress for a goal: total tasks, completed tasks, completion percentage, " +
                    "total estimated hours, completed hours, remaining hours, and days until the target deadline. " +
                    "Cancelled tasks are excluded from all totals since they represent work that won't happen. " +
                    "Use this to assess urgency ('you're only 30% done with 3 days left!') " +
                    "or to give the user a motivating progress report.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    @Transactional(readOnly = true)
    public Map<String, Object> getGoalProgress(
            @McpToolParam(description = "The goal's UUID", required = true) String goalId) {
        UUID userId = currentUserId();
        UUID goalUuid = parseUuid(goalId, "goalId");
        log.info("[MCP] getGoalProgress called for user {} goal {}", userId, goalUuid);

        Goal goal = goalRepository.findByIdAndUserId(goalUuid, userId)
                .orElseThrow(() -> new GoalNotFoundException(goalUuid));

        int totalTasks = 0;
        int completedTasks = 0;
        int cancelledTasks = 0;
        int totalDuration = 0;
        int completedDuration = 0;

        for (Task t : goal.getTasks()) {
            TaskStatus status = taskMapper.deriveStatus(t);

            if (status == TaskStatus.CANCELLED) {
                cancelledTasks++;
                continue; // excluded from totals — this work will never happen
            }

            int dur = t.getEstimatedDuration() != null ? t.getEstimatedDuration() : 0;
            totalTasks++;
            totalDuration += dur;

            if (status == TaskStatus.COMPLETED) {
                completedTasks++;
                completedDuration += dur;
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("goalTitle", goal.getTitle());
        res.put("targetDate", goal.getTargetDate() != null ? goal.getTargetDate().toString() : null);
        if (goal.getTargetDate() != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            res.put("daysRemaining", userToday(user).until(goal.getTargetDate()).getDays());
        }
        res.put("totalTasks", totalTasks);
        res.put("completedTasks", completedTasks);
        res.put("cancelledTasks", cancelledTasks);
        res.put("completionPercent", totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0);
        res.put("totalDurationMinutes", totalDuration);
        res.put("completedDurationMinutes", completedDuration);
        res.put("remainingDurationMinutes", totalDuration - completedDuration);
        res.put("status", goal.getStatus().name()); // Goal's own status (ACTIVE/ACHIEVED) — unrelated to Task status

        return res;
    }





    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Smart Actions
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "createTaskWithSessions",
            title = "Create & Schedule Task",
            description = "Create a new task AND immediately schedule it with specific time sessions in one step. " +
                    "Use this when you know exactly when the task should happen — " +
                    "e.g. the user says 'Schedule a 2-hour study session tomorrow at 10 AM'. " +
                    "Call getAvailableSlots first to find valid times. " +
                    "Sessions are provided as a JSON array of objects with 'start' and 'end' in ISO format.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, idempotentHint = false))
    public com.ezdo.dto.task.TaskWithSessionsResponse createTaskWithSessions(
            @McpToolParam(description = "Task title", required = true) String title,
            @McpToolParam(description = "Task description", required = false) String description,
            @McpToolParam(description = "Goal UUID, omit to add to Inbox", required = false) String goalId,
            @McpToolParam(description = "Estimated duration in minutes", required = false) Integer estimatedDuration,
            @McpToolParam(description = "JSON array of sessions, e.g. [{\"start\":\"2026-08-06T14:00\",\"end\":\"2026-08-06T15:00\"}]", required = true) String sessionsJson) {
        UUID userId = currentUserId();
        log.info("[MCP] createTaskWithSessions called for user {}", userId);

        List<Map<String, String>> sessionMaps;
        try {
            sessionMaps = objectMapper.readValue(sessionsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid sessionsJson: " + e.getOriginalMessage() +
                            ". Expected a JSON array like [{\"start\":\"2026-08-06T14:00\",\"end\":\"2026-08-06T15:00\"}].");
        }

        List<SessionDraftRequest> sessionDrafts;
        try {
            sessionDrafts = sessionMaps.stream()
                    .map(m -> new SessionDraftRequest(
                            m.containsKey("zoneId") ? UUID.fromString(m.get("zoneId")) : null,
                            LocalDateTime.parse(m.get("start")),
                            LocalDateTime.parse(m.get("end")),
                            null
                    )).toList();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid entry in sessionsJson: " + e.getMessage() +
                            ". Each entry needs 'start' and 'end' in ISO format (YYYY-MM-DDTHH:mm) and, if present, a valid 'zoneId' UUID.");
        }

        UUID goalUuid = (goalId != null && !goalId.isBlank()) ? parseUuid(goalId, "goalId") : null;
        TaskCreateRequest taskReq = new TaskCreateRequest(
                title, description, estimatedDuration, null, null, null, goalUuid, null
        );

        TaskWithSessionsRequest req = new TaskWithSessionsRequest(taskReq, sessionDrafts);
        return taskService.createTaskWithSessions(userId, req);
    }

    @McpTool(name = "addSessionToTask",
            title = "Add Session to Task",
            description = "Add a new scheduled time block (session) to an existing task. " +
                    "Use this when rescheduling: find a missed session, find an available slot via getAvailableSlots, " +
                    "then add a new session at that time. The original missed session can be cancelled via bulkUpdateSessions.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, idempotentHint = false))
    public List<SessionResponse> addSessionToTask(
            @McpToolParam(description = "The task's UUID", required = true) String taskId,
            @McpToolParam(description = "Start time in ISO format, e.g. 2026-08-06T14:00", required = true) String start,
            @McpToolParam(description = "End time in ISO format, e.g. 2026-08-06T15:00", required = true) String end) {
        UUID userId = currentUserId();
        UUID taskUuid = parseUuid(taskId, "taskId");
        log.info("[MCP] addSessionToTask called for user {} task {}", userId, taskUuid);

        AddSessionsRequest req = new AddSessionsRequest(List.of(
                new SessionDraftRequest(null, LocalDateTime.parse(start), LocalDateTime.parse(end), null)
        ));
        return taskService.addSessionsToTask(userId, taskUuid, req);
    }

    @McpTool(name = "deleteSession",
            title = "Delete Session",
            description = "Delete a session from the schedule permanently. " +
                    "Use this to clean up old missed sessions that won't be rescheduled. " +
                    "This only removes the time block — the task itself is preserved. " +
                    "Prefer bulkUpdateSessions with CANCELLED status if you want to keep history.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true))
    public String deleteSession(
            @McpToolParam(description = "The session's UUID", required = true) String sessionId) {
        UUID userId = currentUserId();
        UUID sessionUuid = parseUuid(sessionId, "sessionId");
        log.info("[MCP] deleteSession called for user {} session {}", userId, sessionUuid);
        sessionService.delete(userId, sessionUuid);
        return "Session deleted successfully.";
    }


    @McpTool(name = "bulkUpdateSessions",
            title = "Bulk Update Sessions",
            description = "Update multiple sessions at once — mark them COMPLETED or CANCELLED. " +
                    "Use this for recovery workflows: mark all yesterday's missed sessions as CANCELLED in one call, " +
                    "then create new ones via addSessionToTask. " +
                    "Pass a JSON array of objects, each with 'sessionId' and 'status' (COMPLETED or CANCELLED). " +
                    "Each item is applied independently — one failure does not affect the others. Max 100 items per call.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, idempotentHint = true))
    public List<Map<String, Object>> bulkUpdateSessions(
            @McpToolParam(description = "JSON array of updates, e.g. [{\"sessionId\":\"uuid\",\"status\":\"CANCELLED\"}]", required = true) String updatesJson) {
        UUID userId = currentUserId();
        log.info("[MCP] bulkUpdateSessions called for user {}", userId);

        List<Map<String, String>> updates;
        try {
            updates = objectMapper.readValue(updatesJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid updatesJson: " + e.getOriginalMessage() +
                            ". Expected a JSON array like [{\"sessionId\":\"uuid\",\"status\":\"CANCELLED\"}].");
        }

        if (updates.size() > 100) {
            throw new IllegalArgumentException("Too many updates in one batch (" + updates.size() + "), max is 100.");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, String> update : updates) {
            Map<String, Object> itemResult = new LinkedHashMap<>();
            String sessionIdStr = update.get("sessionId");
            String statusStr = update.get("status");
            itemResult.put("sessionId", sessionIdStr);

            try {
                UUID sessionId = UUID.fromString(sessionIdStr);
                SessionResponse response = switch (statusStr) {
                    case "COMPLETED" -> sessionService.complete(userId, sessionId).session();
                    case "CANCELLED" -> sessionService.cancel(userId, sessionId);
                    default -> throw new IllegalArgumentException(
                            "bulkUpdateSessions only supports COMPLETED or CANCELLED, got: " + statusStr);
                };
                itemResult.put("success", true);
                itemResult.put("session", response);
            } catch (Exception e) {
                log.warn("[MCP] bulkUpdateSessions failed for session {}: {}", sessionIdStr, e.getMessage());
                itemResult.put("success", false);
                itemResult.put("error", e.getMessage());
            }
            results.add(itemResult);
        }

        return results;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Gamification & Progress
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "getProgress",
            title = "View Points & Streak",
            description = "Get the user's gamification progress: current points, current streak, and max streak. " +
                    "Use when the user asks 'how am I doing?', 'how many points do I have?', " +
                    "'what's my streak?', or wants a motivational update.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true))
    @Transactional(readOnly = true)
    public UserProgressResponse getProgress() {
        UUID userId = currentUserId();
        log.info("[MCP] getProgress called for user {}", userId);
        return gamificationService.getProgress(userId);
    }

    @McpTool(name = "getActivityDates",
            title = "View Activity History",
            description = "Get the dates on which the user completed at least one session within a date range (max 90 days). " +
                    "Returns a list of dates — useful for showing consistency, e.g. " +
                    "'show me which days I was active this month' or 'how consistent have I been?'.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true))
    @Transactional(readOnly = true)
    public List<LocalDate> getActivityDates(
            @McpToolParam(description = "Start date in YYYY-MM-DD format", required = true) String startDate,
            @McpToolParam(description = "End date in YYYY-MM-DD format", required = true) String endDate) {
        UUID userId = currentUserId();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        validateDateRange(start, end);
        log.info("[MCP] getActivityDates called for user {} from {} to {}", userId, start, end);
        return gamificationService.getActivityDates(userId, start, end);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Categories
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "createCategory",
            title = "Create Category",
            description = "Create a new category for organizing tasks (e.g. 'Work', 'Study', 'Exercise'). " +
                    "Use when the user says 'create a Work category' or when you need a category that doesn't exist yet " +
                    "before assigning it to a task. Use listCategories first to check if it already exists.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, idempotentHint = false))
    public CategoryResponse createCategory(
            @McpToolParam(description = "Category name, e.g. 'Work' or 'Fitness'", required = true) String name) {
        UUID userId = currentUserId();
        log.info("[MCP] createCategory called for user {} name '{}'", userId, name);
        return categoryService.create(userId, new CategoryRequest(name));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Preferences
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "updatePreferences",
            title = "Update Preferences",
            description = "Update the user's scheduling preferences and notification settings. All parameters are optional — only provided values are changed. " +
                    "Use when the user says 'set my preferred session length to 45 minutes', " +
                    "'change my timezone to America/New_York', 'I now wake up at 7 AM', " +
                    "'set buffer between sessions to 15 minutes', 'turn off notifications', or " +
                    "'stop sending me the daily summary email'. " +
                    "Times should be in HH:mm format (e.g. '07:00', '23:30'). " +
                    "Timezone should be a valid IANA timezone (e.g. 'America/New_York', 'Europe/London', 'Asia/Tokyo').",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, idempotentHint = true))
    public Map<String, Object> updatePreferences(
            @McpToolParam(description = "IANA timezone, e.g. 'America/New_York'", required = false) String timezone,
            @McpToolParam(description = "Preferred session duration in minutes, e.g. 30 or 45", required = false) Integer preferredSessionDuration,
            @McpToolParam(description = "Buffer between sessions in minutes, e.g. 10 or 15", required = false) Integer bufferBetweenSessions,
            @McpToolParam(description = "Wakeup time in HH:mm format, e.g. '07:00'", required = false) String wakeupTime,
            @McpToolParam(description = "Sleep time in HH:mm format, e.g. '23:00'", required = false) String sleepTime,
            @McpToolParam(description = "Scheduling type: BALANCED, MORNING_HEAVY, or EVENING_HEAVY", required = false) String schedulingType,
            @McpToolParam(description = "Whether to send the daily summary email. true to enable, false to disable, omit to leave unchanged.", required = false) Boolean dailySummaryEnabled,
            @McpToolParam(description = "Whether push/general notifications are enabled. true to enable, false to disable, omit to leave unchanged.", required = false) Boolean notificationsEnabled) {
        UUID userId = currentUserId();
        log.info("[MCP] updatePreferences called for user {}", userId);

        LocalTime wakeup = (wakeupTime != null && !wakeupTime.isBlank()) ? LocalTime.parse(wakeupTime) : null;
        LocalTime sleep = (sleepTime != null && !sleepTime.isBlank()) ? LocalTime.parse(sleepTime) : null;
        SchedulingType schedType = (schedulingType != null && !schedulingType.isBlank())
                ? SchedulingType.valueOf(schedulingType) : null;

        UpdateProfileRequest request = new UpdateProfileRequest(
                null, // firstName
                null, // lastName
                null, // birthDate
                timezone,
                preferredSessionDuration,
                bufferBetweenSessions,
                wakeup,
                sleep,
                schedType,
                dailySummaryEnabled,
                notificationsEnabled
        );

        userService.updateProfile(userId, request);

        // Return the updated preferences for confirmation
        return getUserContext();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WRITE TOOLS — Task Lifecycle
    // ═══════════════════════════════════════════════════════════════════════




    // ═══════════════════════════════════════════════════════════════════════
    // READ TOOLS — Inbox
    // ═══════════════════════════════════════════════════════════════════════

    @McpTool(name = "getInbox",
            title = "View Inbox",
            description = "Get the user's Inbox — tasks that haven't been assigned to a specific goal. " +
                    "Use when the user asks 'what's in my inbox?' or 'show me unorganized tasks'. " +
                    "The Inbox is automatically created if it doesn't exist yet.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, idempotentHint = true))
    @Transactional(readOnly = true)
    public GoalInfoResponse getInbox() {
        UUID userId = currentUserId();
        log.info("[MCP] getInbox called for user {}", userId);
        return goalService.getOrCreateInboxResponse(userId);
    }
}

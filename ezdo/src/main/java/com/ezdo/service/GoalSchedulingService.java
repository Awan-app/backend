package com.ezdo.service;

import com.ezdo.dto.ai.*;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.entity.Zone;
import com.ezdo.exception.GoalNotFoundException;
import com.ezdo.exception.TaskNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.TaskRepository;
import com.ezdo.repository.UserRepository;
import com.ezdo.repository.ZoneRepository;
import com.ezdo.timefold.ScheduleSolution;
import com.ezdo.timefold.SchedulingPreprocessor;
import com.ezdo.timefold.SessionChunk;
import com.ezdo.timefold.TimefoldSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalSchedulingService {

    // Preserved for reference:
    // private final AiContextBuilder contextBuilder;
    // private final AiSchedulerClient schedulerClient;

    private final SchedulingPreprocessor schedulingPreprocessor;
    private final TimefoldSchedulerService timefoldSchedulerService;
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ZoneRepository zoneRepository;

    @Transactional
    public GoalScheduleResponse scheduleGoal(UUID userId, GoalScheduleRequest request) {

        Goal goal = goalRepository.findByIdAndUserId(request.goalId(), userId)
                .orElseThrow(() -> new GoalNotFoundException(request.goalId()));
        Preferences prefs = userRepository.findById(userId).orElseThrow().getPreferences();

        ScheduleSolution problem = schedulingPreprocessor.preprocess(goal, prefs, 14);
        ScheduleSolution solution = timefoldSchedulerService.schedule(problem);

        return buildResponse(request.goalId(), solution, userId);
    }

    @Transactional
    public TaskScheduleResponse scheduleTask(UUID userId, TaskScheduleRequest request) {
        Task task = taskRepository.findByIdAndGoalUserId(request.taskId(), userId)
                .orElseThrow(() -> new TaskNotFoundException(request.taskId()));
        Preferences prefs = userRepository.findById(userId).orElseThrow().getPreferences();

        int horizon = request.horizonDays() != null ? request.horizonDays() : 14;
        ScheduleSolution problem = schedulingPreprocessor.preprocessTasks(
                List.of(task), userId, task.getGoal().getTargetDate(), prefs, horizon);
        ScheduleSolution solution = timefoldSchedulerService.schedule(problem);

        GoalScheduleResponse response = buildResponse(task.getGoal().getId(), solution, userId);
        return new TaskScheduleResponse(request.taskId(), response.scheduledSessions(), response.unscheduledTasks());
    }

    private GoalScheduleResponse buildResponse(UUID contextId, ScheduleSolution solution, UUID userId) {
        List<ScheduledSessionResult> scheduledResults = new java.util.ArrayList<>();
        List<UnscheduledTaskResult> unscheduledResults = new java.util.ArrayList<>();

        var chunksByTask = solution.getChunks().stream().collect(Collectors.groupingBy(SessionChunk::getTaskId));

        for (var entry : chunksByTask.entrySet()) {
            UUID taskId = entry.getKey();
            List<SessionChunk> taskChunks = entry.getValue();

            boolean anyUnscheduled = taskChunks.stream().anyMatch(c -> c.getStartingGrain() == null);

            if (anyUnscheduled) {
                Task task = taskRepository.findById(taskId).orElse(null);
                unscheduledResults.add(new UnscheduledTaskResult(
                        taskId,
                        task != null ? task.getTitle() : "Unknown Task",
                        "NO_SUITABLE_ZONE",
                        "Timefold could not find a valid time slot for all chunks of this task."
                ));
            } else {
                for (SessionChunk chunk : taskChunks) {
                    Task task = taskRepository.findById(taskId)
                            .orElseThrow(() -> new TaskNotFoundException(taskId));

                    UUID zoneId = chunk.getStartingGrain().getZoneId();
                    Zone zone = zoneRepository.findByIdAndUserId(zoneId, userId)
                            .orElseThrow(() -> new ZoneNotFoundException(zoneId));

                    Session session = Session.builder()
                            .start(chunk.getStartingGrain().getDate().atTime(chunk.getStartingGrain().getStartTime()))
                            .end(chunk.getStartingGrain().getDate().atTime(chunk.getStartingGrain().getStartTime()).plusMinutes((long) chunk.getDurationInGrains() * 15))
                            .task(task)
                            .zone(zone)
                            .build();

                    sessionRepository.save(session);

                    scheduledResults.add(new ScheduledSessionResult(
                            session.getId(),
                            taskId,
                            zoneId,
                            session.getStart(),
                            session.getEnd()
                    ));
                }
            }
        }

        return new GoalScheduleResponse(contextId, scheduledResults, unscheduledResults);
    }
}

package com.ezdo.service;

import com.ezdo.dto.ai.*;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.entity.Zone;
import com.ezdo.exception.TaskNotFoundException;
import com.ezdo.exception.ZoneNotFoundException;
import com.ezdo.repository.SessionRepository;
import com.ezdo.repository.TaskRepository;
import com.ezdo.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalSchedulingService {

    private final AiContextBuilder contextBuilder;
    private final AiSchedulerClient schedulerClient;
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ZoneRepository zoneRepository;

    @Transactional
    public GoalScheduleResponse scheduleGoal(UUID userId, GoalScheduleRequest request) {
        AiSchedulingPayload payload = contextBuilder.buildPayload(userId, request.goalId(), 14);
        
        AiSchedulingResult result = schedulerClient.scheduleTasks(payload);
        System.out.println("Scheduling result: " + result);

        List<ScheduledSessionResult> scheduledResults = new java.util.ArrayList<>();
        
        for (ProposedSession ps : result.sessions()) {
            UUID taskId = UUID.fromString(ps.taskId());
            UUID zoneId = UUID.fromString(ps.zoneId());

            Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
                
            Zone zone = zoneRepository.findByIdAndUserId(zoneId, userId)
                .orElseThrow(() -> new ZoneNotFoundException(zoneId));

            Session session = Session.builder()
                .start(ps.date().atTime(ps.startTime()))
                .end(ps.date().atTime(ps.endTime()))
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
        
        List<UnscheduledTaskResult> unscheduledResults = result.failedTasks().stream()
            .map(f -> {
                UUID taskId = UUID.fromString(f.taskId());
                Task task = taskRepository.findById(taskId).orElse(null);
                return new UnscheduledTaskResult(
                    taskId,
                    task != null ? task.getTitle() : "Unknown Task",
                    f.reason().name(),
                    f.message()
                );
            })
            .toList();

        return new GoalScheduleResponse(request.goalId(), scheduledResults, unscheduledResults);
    }
}

package com.ezdo.timefold;

import com.ezdo.entity.Goal;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.dto.ZoneResponse;
import com.ezdo.repository.SessionRepository;
import com.ezdo.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulingPreprocessor {

    private final SessionRepository sessionRepository;
    private final ZoneService zoneService;

    public ScheduleSolution preprocess(Goal goal, Preferences prefs, int horizonDays) {
        return preprocessTasks(goal.getTasks(), goal.getUser().getId(), goal.getTargetDate(), prefs, horizonDays);
    }

    public ScheduleSolution preprocessTasks(List<Task> tasks, UUID userId, LocalDate targetDate, Preferences prefs, int horizonDays) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = targetDate;
        LocalDate maxEndDate = startDate.plusDays(horizonDays);
        if (endDate == null || endDate.isAfter(maxEndDate)) {
            endDate = maxEndDate;
        } else if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }

        int preferredSessionDuration = prefs.getPreferredSessionDuration() != null ? prefs.getPreferredSessionDuration() : 60;
        int bufferBetweenSessions = prefs.getBufferBetweenSessions() != null ? prefs.getBufferBetweenSessions() : 0;

        List<SessionChunk> chunks = new ArrayList<>();
        List<TimeGrain> grains = new ArrayList<>();

        // 1. Process Tasks into Chunks
        List<Task> sortedTasks = topologicalSort(tasks);
        int taskOrder = 0;
        for (Task task : sortedTasks) {
            taskOrder++;
            int remaining = task.getEstimatedDuration() != null ? task.getEstimatedDuration() : 60;
            boolean allowSplit = Boolean.TRUE.equals(task.getAllowTaskSplitting());
            UUID categoryId = task.getCategory() != null ? task.getCategory().getId() : null;
            Set<UUID> dependsOn = task.getDependsOn().stream().map(Task::getId).collect(Collectors.toSet());

            if (!allowSplit) {
                int durationGrains = (int) Math.ceil(remaining / 15.0);
                chunks.add(new SessionChunk(UUID.randomUUID(), task.getId(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.getMandatory()), dependsOn, bufferBetweenSessions));
            } else {
                while (remaining > preferredSessionDuration) {
                    int durationGrains = (int) Math.ceil(preferredSessionDuration / 15.0);
                    chunks.add(new SessionChunk(UUID.randomUUID(), task.getId(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.getMandatory()), dependsOn, bufferBetweenSessions));
                    remaining -= preferredSessionDuration;
                }
                if (remaining > 0) {
                    int durationGrains = (int) Math.ceil(remaining / 15.0);
                    chunks.add(new SessionChunk(UUID.randomUUID(), task.getId(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.getMandatory()), dependsOn, bufferBetweenSessions));
                }
            }
        }

        // 2. Process Calendar into Grains
        LocalDateTime absoluteStart = startDate.atStartOfDay();
        List<Session> existingSessions = sessionRepository.findByUserIdAndTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        long grainIdCounter = 1;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ZoneResponse> dailyZones = zoneService.getZonesByDate(userId, date);
            
            for (ZoneResponse z : dailyZones) {
                int zoneDurationMinutes = (int) ChronoUnit.MINUTES.between(z.startTime(), z.endTime());
                int zoneDurationGrains = zoneDurationMinutes / 15;
                
                for (int i = 0; i < zoneDurationGrains; i++) {
                    LocalTime grainTime = z.startTime().plusMinutes(i * 15L);
                    LocalDateTime grainDateTime = date.atTime(grainTime);
                    
                    // Check if overlaps with booked session
                    boolean isBooked = false;
                    for (Session booked : existingSessions) {
                        if (grainDateTime.isBefore(booked.getEnd()) && grainDateTime.plusMinutes(15).isAfter(booked.getStart())) {
                            isBooked = true;
                            break;
                        }
                    }
                    
                    if (!isBooked) {
                        int absMinute = (int) ChronoUnit.MINUTES.between(absoluteStart, grainDateTime);
                        grains.add(new TimeGrain(
                                grainIdCounter++, date, grainTime, i, z.id(), z.category() != null ? z.category().id():null, zoneDurationGrains, absMinute
                        ));
                    }
                }
            }
        }

        return new ScheduleSolution(tasks.isEmpty() ? null : tasks.getFirst().getGoal().getId(), grains, chunks);
    }

    private List<Task> topologicalSort(List<Task> tasks) {
        List<Task> sorted = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (Task t : tasks) {
            visit(t, visited, sorted);
        }
        return sorted;
    }

    private void visit(Task task, Set<UUID> visited, List<Task> sorted) {
        if (!visited.contains(task.getId())) {
            visited.add(task.getId());
            for (Task dep : task.getDependsOn()) {
                visit(dep, visited, sorted);
            }
            sorted.add(task);
        }
    }
}

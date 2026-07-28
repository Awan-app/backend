package com.ezdo.timefold;

import com.ezdo.dto.ZoneResponse;
import com.ezdo.dto.goal.TaskInfoResponse;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Preferences;
import com.ezdo.entity.Session;
import com.ezdo.entity.Task;
import com.ezdo.repository.SessionRepository;
import com.ezdo.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
        ZoneId userZone = (prefs.getTimezone() != null)
                ? ZoneId.of(prefs.getTimezone())
                : ZoneId.systemDefault(); // fallback
        LocalDate startDate = LocalDate.now(userZone);
        LocalDateTime now = LocalDateTime.now(userZone);
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

        // 1. Process Tasks into Chunks (unchanged)
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

        // 2. Process Calendar into Grains + booked intervals
        LocalDateTime absoluteStart = startDate.atStartOfDay();
        List<Session> existingSessions = sessionRepository.findByUserIdAndTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        // Build the hard-constraint-facing representation of "already booked" time,
        // independent of the grain list. This is what actually protects against
        // a new chunk spanning across an already-booked window.
        List<BookedInterval> bookedIntervals = existingSessions.stream()
                .map(s -> new BookedInterval(
                        s.getId(),
                        (int) ChronoUnit.MINUTES.between(absoluteStart, s.getStart()),
                        (int) ChronoUnit.MINUTES.between(absoluteStart, s.getEnd())
                ))
                .toList();

        long grainIdCounter = 1;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ZoneResponse> dailyZones = zoneService.getZonesByDate(userId, date);
            
            for (ZoneResponse z : dailyZones) {
                int zoneDurationMinutes = (int) ChronoUnit.MINUTES.between(z.startTime(), z.endTime());
                int zoneDurationGrains = zoneDurationMinutes / 15;
                
                for (int i = 0; i < zoneDurationGrains; i++) {
                    LocalTime grainTime = z.startTime().plusMinutes(i * 15L);
                    LocalDateTime grainDateTime = date.atTime(grainTime);

                    // Skip any grain slot that has already elapsed (only relevant for "today").
                    // A grain represents a 15-minute slot [grainDateTime, grainDateTime+15);
                    // if that slot has already ended, it can no longer be scheduled into.
//                    if (grainDateTime.plusMinutes(15).isBefore(now) || grainDateTime.plusMinutes(15).isEqual(now)) {
//                        continue;
//                    }
                    if (grainDateTime.isBefore(now)) {
                        continue;
                    }
                    // This filtering is now just an optimization (fewer candidate
                    // starting points for the solver to try) — it is NOT relied upon
                    // for correctness anymore. The respectBookedSessions hard constraint
                    // is the actual safety net, since it checks the full chunk span.
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
                                grainIdCounter++, date, grainTime, i, z.id(), z.category() != null ? z.category().id() : null, zoneDurationGrains, absMinute
                        ));
                    }
                }
            }
        }

        return new ScheduleSolution(
                tasks.isEmpty() ? null : tasks.getFirst().getGoal().getId(),
                grains,
                bookedIntervals,
                chunks
        );
    }

    public ScheduleSolution preprocessTaskResponses(List<TaskInfoResponse> tasks, UUID userId, LocalDate targetDate, Preferences prefs, int horizonDays) {
        ZoneId userZone = (prefs.getTimezone() != null)
                ? ZoneId.of(prefs.getTimezone())
                : ZoneId.systemDefault();
        LocalDate startDate = LocalDate.now(userZone);
        LocalDateTime now = LocalDateTime.now(userZone);
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

        // 1. Process TaskInfoResponse into Chunks
        List<TaskInfoResponse> sortedTasks = topologicalSortTaskResponses(tasks);
        int taskOrder = 0;
        for (TaskInfoResponse task : sortedTasks) {
            taskOrder++;
            int remaining = task.estimatedDuration() != null ? task.estimatedDuration() : 60;
            boolean allowSplit = Boolean.TRUE.equals(task.allowTaskSplitting());
            UUID categoryId = task.category() != null ? task.category().id() : null;
            Set<UUID> dependsOn = task.dependsOnTaskIds() != null ? task.dependsOnTaskIds() : Set.of();

            if (!allowSplit) {
                int durationGrains = (int) Math.ceil(remaining / 15.0);
                chunks.add(new SessionChunk(UUID.randomUUID(), task.id(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.mandatory()), dependsOn, bufferBetweenSessions));
            } else {
                while (remaining > preferredSessionDuration) {
                    int durationGrains = (int) Math.ceil(preferredSessionDuration / 15.0);
                    chunks.add(new SessionChunk(UUID.randomUUID(), task.id(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.mandatory()), dependsOn, bufferBetweenSessions));
                    remaining -= preferredSessionDuration;
                }
                if (remaining > 0) {
                    int durationGrains = (int) Math.ceil(remaining / 15.0);
                    chunks.add(new SessionChunk(UUID.randomUUID(), task.id(), categoryId, durationGrains, taskOrder, Boolean.TRUE.equals(task.mandatory()), dependsOn, bufferBetweenSessions));
                }
            }
        }

        // 2. Process Calendar into Grains + booked intervals (same as preprocessTasks)
        LocalDateTime absoluteStart = startDate.atStartOfDay();
        List<Session> existingSessions = sessionRepository.findByUserIdAndTimeRange(
                userId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        List<BookedInterval> bookedIntervals = existingSessions.stream()
                .map(s -> new BookedInterval(
                        s.getId(),
                        (int) ChronoUnit.MINUTES.between(absoluteStart, s.getStart()),
                        (int) ChronoUnit.MINUTES.between(absoluteStart, s.getEnd())
                ))
                .toList();

        long grainIdCounter = 1;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<ZoneResponse> dailyZones = zoneService.getZonesByDate(userId, date);

            for (ZoneResponse z : dailyZones) {
                int zoneDurationMinutes = (int) ChronoUnit.MINUTES.between(z.startTime(), z.endTime());
                int zoneDurationGrains = zoneDurationMinutes / 15;

                for (int i = 0; i < zoneDurationGrains; i++) {
                    LocalTime grainTime = z.startTime().plusMinutes(i * 15L);
                    LocalDateTime grainDateTime = date.atTime(grainTime);

                    if (grainDateTime.isBefore(now)) {
                        continue;
                    }

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
                                grainIdCounter++, date, grainTime, i, z.id(), z.category() != null ? z.category().id() : null, zoneDurationGrains, absMinute
                        ));
                    }
                }
            }
        }

        return new ScheduleSolution(
                null,
                grains,
                bookedIntervals,
                chunks
        );
    }

    private List<TaskInfoResponse> topologicalSortTaskResponses(List<TaskInfoResponse> tasks) {
        List<TaskInfoResponse> sorted = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Map<UUID, TaskInfoResponse> taskMap = tasks.stream()
                .collect(Collectors.toMap(TaskInfoResponse::id, t -> t));
        for (TaskInfoResponse t : tasks) {
            visitTaskResponse(t, taskMap, visited, sorted);
        }
        return sorted;
    }

    private void visitTaskResponse(TaskInfoResponse task, Map<UUID, TaskInfoResponse> taskMap,
                                    Set<UUID> visited, List<TaskInfoResponse> sorted) {
        if (!visited.contains(task.id())) {
            visited.add(task.id());
            if (task.dependsOnTaskIds() != null) {
                for (UUID depId : task.dependsOnTaskIds()) {
                    TaskInfoResponse dep = taskMap.get(depId);
                    if (dep != null) {
                        visitTaskResponse(dep, taskMap, visited, sorted);
                    }
                }
            }
            sorted.add(task);
        }
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

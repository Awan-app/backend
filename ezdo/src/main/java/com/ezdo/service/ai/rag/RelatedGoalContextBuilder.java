package com.ezdo.service.ai.rag;

import com.ezdo.dto.RelatedGoalMatch;
import com.ezdo.dto.ai.RelatedGoalContext;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Task;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedGoalContextBuilder {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;

    /**
     * {@code maxTasksPerGoal} is passed in rather than injected here so all three RAG
     * tuning values stay owned by {@link RelatedWorkService}.
     */
    @Transactional(readOnly = true)
    public List<RelatedGoalContext> build(UUID userId, List<RelatedGoalMatch> matches, int maxTasksPerGoal) {
        // The index can outlive the row it describes (a failed delete, a restored
        // backup), so re-check ownership and existence against the database.
        Map<UUID, Goal> goalsById = goalRepository.findAllById(
                matches.stream().map(RelatedGoalMatch::goalId).toList()).stream()
            .filter(g -> g.getUser().getId().equals(userId))
            .collect(Collectors.toMap(Goal::getId, g -> g));

        List<UUID> liveIds = matches.stream()
            .map(RelatedGoalMatch::goalId)
            .filter(goalsById::containsKey)
            .toList();
        if (liveIds.isEmpty()) {
            log.warn("Vector search returned {} match(es) for user {} but none exist in the database; "
                + "the index is stale", matches.size(), userId);
            return List.of();
        }

        Map<UUID, List<Task>> tasksByGoal = taskRepository.findAllByGoalIdIn(liveIds).stream()
            .collect(Collectors.groupingBy(t -> t.getGoal().getId()));

        List<RelatedGoalContext> contexts = new ArrayList<>();
        for (RelatedGoalMatch match : matches) {
            Goal goal = goalsById.get(match.goalId());
            if (goal == null) {
                continue;
            }
            List<Task> tasks = tasksByGoal.getOrDefault(goal.getId(), List.of());
            // Sort incomplete tasks first so they survive the maxTasksPerGoal cap —
            // those are the ones the AI must see to avoid re-proposing existing work.
            List<Task> ordered = tasks.stream()
                .sorted(Comparator.comparing(Task::isCompleted)
                    .thenComparing(Task::getTitle, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

            int omitted = Math.max(0, ordered.size() - maxTasksPerGoal);
            List<RelatedGoalContext.RelatedTask> rendered = ordered.stream()
                .limit(maxTasksPerGoal)
                .map(t -> new RelatedGoalContext.RelatedTask(
                    t.getTitle(), t.getEstimatedDuration(), t.isCompleted()))
                .toList();

            contexts.add(new RelatedGoalContext(
                goal.getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getTargetDate(),
                goal.getStatus() != null ? goal.getStatus().name() : null,
                match.score(),
                rendered,
                omitted));
        }
        return contexts;
    }
}

package com.ezdo.service.ai.rag;

import com.ezdo.dto.RelatedGoalMatch;
import com.ezdo.dto.ai.RelatedGoalContext;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Task;
import com.ezdo.repository.GoalRepository;
import com.ezdo.repository.TaskRepository;
import com.ezdo.service.GoalVectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The retrieval half of the decomposition RAG flow: match the user's message against
 * their past goals semantically, then expand each hit into its full task roster.
 * <p>
 * The two halves use deliberately different mechanisms. Matching is fuzzy ("finish
 * clean code" -> "Study Clean Code, chapters 1-5") and belongs to the vector store.
 * Expansion must be exhaustive — a partial roster is worse than none, because the
 * model would re-propose whatever was left out — so it goes through MySQL.
 */
@Slf4j
@Service
public class RelatedWorkService {

    private final GoalVectorService goalVectorService;
    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final int topK;
    private final double similarityThreshold;
    private final int maxTasksPerGoal;

    public RelatedWorkService(
        GoalVectorService goalVectorService,
        GoalRepository goalRepository,
        TaskRepository taskRepository,
        @Value("${ezdo.ai.rag.top-k}") int topK,
        @Value("${ezdo.ai.rag.similarity-threshold}") double similarityThreshold,
        @Value("${ezdo.ai.rag.max-tasks-per-goal}") int maxTasksPerGoal
    ) {
        this.goalVectorService = goalVectorService;
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.maxTasksPerGoal = maxTasksPerGoal;
    }

    @Transactional(readOnly = true)
    public List<RelatedGoalContext> findRelatedWork(UUID userId, String query) {
        List<RelatedGoalMatch> matches =
            goalVectorService.findSimilarGoals(query, userId, topK, similarityThreshold);
        if (matches.isEmpty()) {
            return List.of();
        }

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

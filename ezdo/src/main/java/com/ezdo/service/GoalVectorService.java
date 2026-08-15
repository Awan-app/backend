package com.ezdo.service;

import com.ezdo.dto.RelatedGoalMatch;
import com.ezdo.entity.Goal;
import com.ezdo.entity.Task;
import com.ezdo.repository.GoalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Goal-level index over the vector store: one document per goal, whose embedded text
 * covers the goal's title and description PLUS its task titles and descriptions.
 * <p>
 * The task text is there for recall, not for retrieval. A goal called "Q1 reading
 * plan" whose tasks name specific book chapters is unfindable from the goal's own
 * words alone, and a miss is a total failure — the model then re-proposes work the
 * user already has. What comes back is still only a goal id; the authoritative task
 * roster is read from MySQL afterwards, so a diluted vector costs ranking quality
 * but can never produce a partial task list.
 */
@Slf4j
@Service
public class GoalVectorService {

    /**
     * Embedding inputs are token-capped (~2048 for gemini-embedding-001), and past
     * that point extra task text only blurs the vector. Goal title and description
     * always survive this cap; tasks fill whatever is left.
     */
    private static final int MAX_EMBEDDED_CHARS = 6000;

    private final VectorStore vectorStore;
    private final GoalRepository goalRepository;
    private final boolean enabled;

    public GoalVectorService(
        VectorStore vectorStore,
        GoalRepository goalRepository,
        @Value("${ezdo.ai.rag.enabled}") boolean enabled
    ) {
        this.vectorStore = vectorStore;
        this.goalRepository = goalRepository;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Re-reads the goal and its tasks and (re)writes its document. Takes an id rather
     * than an entity because callers run this after transaction commit, where lazy
     * associations are no longer initialisable — this opens its own read transaction.
     */
    @Transactional(readOnly = true)
    public void reindexGoal(UUID goalId) {
        if (!enabled) {
            return;
        }
        Optional<Goal> found = goalRepository.findById(goalId);
        if (found.isEmpty()) {
            // Deleted between commit and re-index; drop any document left behind.
            deleteGoal(goalId);
            return;
        }
        Goal goal = found.get();
        if (Boolean.TRUE.equals(goal.getInbox())) {
            return;
        }

        UUID userId = goal.getUser().getId();
        try {
            // Delete-then-add rather than a bare add: the id is stable but the
            // embedded text changes whenever a task is added, renamed or removed.
            vectorStore.delete(List.of(goalId.toString()));
            vectorStore.add(List.of(toDocument(goal, userId)));
        } catch (Exception e) {
            log.warn("Failed to index goal {} for user {}; it will be missing from AI context",
                goalId, userId, e);
        }
    }

    public void deleteGoal(UUID goalId) {
        if (!enabled) {
            return;
        }
        try {
            vectorStore.delete(List.of(goalId.toString()));
        } catch (Exception e) {
            log.warn("Failed to remove goal {} from the vector store; it may resurface in AI context",
                goalId, e);
        }
    }

    /**
     * Semantic lookup over this user's goals. Returns an empty list — never throws —
     * when the store is unreachable or nothing clears the similarity threshold.
     */
    public List<RelatedGoalMatch> findSimilarGoals(String queryText, UUID userId, int topK, double threshold) {
        if (!enabled || queryText == null || queryText.isBlank()) {
            return List.of();
        }
        try {
            SearchRequest request = SearchRequest.builder()
                .query(queryText)
                .topK(topK)
                .similarityThreshold(threshold)
                .filterExpression("userId == '" + userId + "'")
                .build();

            List<Document> results = vectorStore.similaritySearch(request);
            if (results == null) {
                return List.of();
            }
            return results.stream()
                .map(doc -> new RelatedGoalMatch(
                    UUID.fromString(doc.getMetadata().get("goalId").toString()),
                    doc.getText(),
                    doc.getScore()))
                .toList();
        } catch (Exception e) {
            log.warn("Vector search failed for user {}; continuing without related-goal context", userId, e);
            return List.of();
        }
    }

    /** Exposed so the debug endpoint can show exactly what text was embedded. */
    @Transactional(readOnly = true)
    public String buildEmbeddedText(UUID goalId) {
        return goalRepository.findById(goalId)
            .map(this::embeddedText)
            .orElse("");
    }

    private Document toDocument(Goal goal, UUID userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("goalId", goal.getId().toString());
        metadata.put("userId", userId.toString());
        metadata.put("title", goal.getTitle());
        if (goal.getStatus() != null) {
            metadata.put("status", goal.getStatus().name());
        }
        return new Document(goal.getId().toString(), embeddedText(goal), metadata);
    }

    private String embeddedText(Goal goal) {
        StringBuilder sb = new StringBuilder(goal.getTitle() == null ? "" : goal.getTitle());
        if (goal.getDescription() != null && !goal.getDescription().isBlank()) {
            sb.append(". ").append(goal.getDescription());
        }

        List<Task> tasks = goal.getTasks();
        if (tasks != null && !tasks.isEmpty()) {
            sb.append("\nTasks:");
            for (Task task : tasks) {
                if (sb.length() >= MAX_EMBEDDED_CHARS) {
                    break;
                }
                sb.append("\n- ").append(task.getTitle());
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    sb.append(": ").append(task.getDescription());
                }
            }
        }

        String text = sb.toString();
        return text.length() > MAX_EMBEDDED_CHARS ? text.substring(0, MAX_EMBEDDED_CHARS) : text;
    }
}

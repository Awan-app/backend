package com.ezdo.service;

import com.ezdo.dto.RelatedGoalMatch;
import com.ezdo.service.ai.rag.GoalDocumentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class GoalVectorService {

    private final VectorStore vectorStore;
    private final GoalDocumentBuilder documentBuilder;
    private final boolean enabled;

    public GoalVectorService(
        VectorStore vectorStore,
        GoalDocumentBuilder documentBuilder,
        @Value("${ezdo.ai.rag.enabled}") boolean enabled
    ) {
        this.vectorStore = vectorStore;
        this.documentBuilder = documentBuilder;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Re-reads the goal and its tasks and (re)writes its document. Takes an id rather
     * than an entity because callers run this after transaction commit, where lazy
     * associations are no longer initialisable.
     */
    public void reindexGoal(UUID goalId) {
        if (!enabled) {
            return;
        }

        Optional<Document> document = documentBuilder.build(goalId);
        if (document.isEmpty()) {
            // Either deleted between commit and re-index, or an inbox goal, which is
            // never indexed. Both mean the same thing: no document should remain.
            deleteGoal(goalId);
            return;
        }

        try {
            // Delete-then-add rather than a bare add: the id is stable but the
            // embedded text changes whenever a task is added, renamed or removed.
            vectorStore.delete(List.of(goalId.toString()));
            vectorStore.add(List.of(document.get()));
        } catch (Exception e) {
            log.warn("Failed to index goal {}; it will be missing from AI context", goalId, e);
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
    public String buildEmbeddedText(UUID goalId) {
        return documentBuilder.buildEmbeddedText(goalId);
    }
}

package com.ezdo.service.ai.rag;

import com.ezdo.entity.Goal;
import com.ezdo.entity.Task;
import com.ezdo.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalDocumentBuilder {

    private static final int MAX_EMBEDDED_CHARS = 6000;

    private final GoalRepository goalRepository;

    /**
     * Empty means the goal should not be in the index at all — it no longer exists, or
     * it is the user's inbox. Callers treat both the same way: remove any document.
     */
    @Transactional(readOnly = true)
    public Optional<Document> build(UUID goalId) {
        return goalRepository.findById(goalId)
            .filter(goal -> !Boolean.TRUE.equals(goal.getInbox()))
            .map(this::toDocument);
    }

    /** Exposed so the debug endpoint can show exactly what text was embedded. */
    @Transactional(readOnly = true)
    public String buildEmbeddedText(UUID goalId) {
        return goalRepository.findById(goalId)
            .map(this::embeddedText)
            .orElse("");
    }

    private Document toDocument(Goal goal) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("goalId", goal.getId().toString());
        metadata.put("userId", goal.getUser().getId().toString());
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

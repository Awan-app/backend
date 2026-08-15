package com.ezdo.dto.ai;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A past goal the user has already planned, matched semantically against what they
 * are asking for now, together with its COMPLETE task roster. The roster is read
 * from MySQL rather than the vector store: the model has to see every task to know
 * what not to propose again, and a similarity search only ever returns a sample.
 */
public record RelatedGoalContext(
    UUID goalId,
    String title,
    String description,
    LocalDate targetDate,
    String status,
    Double score,
    List<RelatedTask> tasks,
    int omittedTaskCount
) {
    public record RelatedTask(
        String title,
        Integer estimatedDuration,
        boolean completed
    ) {}
}

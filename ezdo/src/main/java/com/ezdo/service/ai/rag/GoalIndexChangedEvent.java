package com.ezdo.service.ai.rag;

import java.util.UUID;

/**
 * Signals that a goal's indexed representation is out of date. Publishers only need
 * to know which goal changed — when the vector store is written, and whether that
 * write is even enabled, is entirely the listener's concern.
 *
 * @param goalId  the goal whose document should be rewritten or removed
 * @param deleted true if the goal itself is gone, false for any other change
 */
public record GoalIndexChangedEvent(UUID goalId, boolean deleted) {

    public static GoalIndexChangedEvent changed(UUID goalId) {
        return new GoalIndexChangedEvent(goalId, false);
    }

    public static GoalIndexChangedEvent deleted(UUID goalId) {
        return new GoalIndexChangedEvent(goalId, true);
    }
}

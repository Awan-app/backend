package com.ezdo.service.ai.rag;

import com.ezdo.service.GoalVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoalIndexEventListener {

    private final GoalVectorService goalVectorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onGoalIndexChanged(GoalIndexChangedEvent event) {
        try {
            if (event.deleted()) {
                goalVectorService.deleteGoal(event.goalId());
            } else {
                goalVectorService.reindexGoal(event.goalId());
            }
        } catch (Exception e) {
            log.warn("Goal index update failed for goal {}", event.goalId(), e);
        }
    }
}

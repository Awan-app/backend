package com.ezdo.service.ai.rag;

import com.ezdo.entity.Goal;
import com.ezdo.repository.GoalRepository;
import com.ezdo.service.GoalVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Rebuilds the goal index from the database. This is the repair path for index
 * drift, which happens when a transaction commits but its AFTER_COMMIT listener
 * never runs — a crash or a kill during deploy. Nothing detects that automatically,
 * so those changes stay invisible to retrieval until someone re-indexes.
 *
 * @see GoalIndexBackfillRunner for how it is triggered
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalIndexBackfillService {

    private static final int PAGE_SIZE = 100;

    private final GoalRepository goalRepository;
    private final GoalVectorService goalVectorService;

    public int backfillAll() {
        if (!goalVectorService.isEnabled()) {
            log.info("Goal index backfill requested but ezdo.ai.rag.enabled is false; skipping");
            return 0;
        }

        int indexed = 0;
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        Page<Goal> page;
        do {
            page = goalRepository.findAll(pageable);
            for (Goal goal : page.getContent()) {
                // Re-indexing is idempotent, so a partial run can simply be repeated.
                goalVectorService.reindexGoal(goal.getId());
                indexed++;
            }
            pageable = pageable.next();
        } while (page.hasNext());

        log.info("Goal index backfill complete: {} goal(s) submitted for indexing", indexed);
        return indexed;
    }
}

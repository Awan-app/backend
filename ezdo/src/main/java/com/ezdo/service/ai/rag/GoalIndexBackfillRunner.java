package com.ezdo.service.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Triggers a full goal-index rebuild at startup when
 * {@code ezdo.ai.rag.backfill-on-startup=true}. Runs asynchronously so that
 * it does not block application readiness or health-check probes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ezdo.ai.rag.backfill-on-startup", havingValue = "true")
public class GoalIndexBackfillRunner implements ApplicationRunner {

    private final GoalIndexBackfillService backfillService;

    @Async
    @Override
    public void run(ApplicationArguments args) {
        log.info("▶ Starting goal-vector backfill in background …");
        try {
            int count = backfillService.backfillAll();
            log.info("✔ Goal-vector backfill complete: {} goal(s) indexed", count);
        } catch (Exception e) {
            log.error("✘ Goal-vector backfill failed — index may be incomplete. "
                + "Re-run with ezdo.ai.rag.backfill-on-startup=true to retry.", e);
        }
    }
}

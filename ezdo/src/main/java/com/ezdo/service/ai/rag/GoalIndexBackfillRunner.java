package com.ezdo.service.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ezdo.ai.rag.backfill-on-startup", havingValue = "true")
public class GoalIndexBackfillRunner implements ApplicationRunner {

    private final GoalIndexBackfillService backfillService;

    @Override
    public void run(ApplicationArguments args) {
        backfillService.backfillAll();
    }
}

package com.ezdo.service.ai.rag;

import com.ezdo.dto.RelatedGoalMatch;
import com.ezdo.dto.ai.RelatedGoalContext;
import com.ezdo.service.GoalVectorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RelatedWorkService {

    private final GoalVectorService goalVectorService;
    private final RelatedGoalContextBuilder contextBuilder;
    private final int topK;
    private final double similarityThreshold;
    private final int maxTasksPerGoal;

    public RelatedWorkService(
        GoalVectorService goalVectorService,
        RelatedGoalContextBuilder contextBuilder,
        @Value("${ezdo.ai.rag.top-k}") int topK,
        @Value("${ezdo.ai.rag.similarity-threshold}") double similarityThreshold,
        @Value("${ezdo.ai.rag.max-tasks-per-goal}") int maxTasksPerGoal
    ) {
        this.goalVectorService = goalVectorService;
        this.contextBuilder = contextBuilder;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.maxTasksPerGoal = maxTasksPerGoal;
    }

    /** Do not annotate this {@code @Transactional} — see the class comment. */
    public List<RelatedGoalContext> findRelatedWork(UUID userId, String query) {
        List<RelatedGoalMatch> matches =
            goalVectorService.findSimilarGoals(query, userId, topK, similarityThreshold);
        if (matches.isEmpty()) {
            return List.of();
        }
        return contextBuilder.build(userId, matches, maxTasksPerGoal);
    }
}

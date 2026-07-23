package com.ezdo.timefold;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import java.util.List;
import java.util.UUID;

@PlanningSolution
public class ScheduleSolution {

    private UUID goalId;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<TimeGrain> grains;

    @PlanningEntityCollectionProperty
    private List<SessionChunk> chunks;

    @PlanningScore
    private HardSoftScore score;

    public ScheduleSolution() {}

    public ScheduleSolution(UUID goalId, List<TimeGrain> grains, List<SessionChunk> chunks) {
        this.goalId = goalId;
        this.grains = grains;
        this.chunks = chunks;
    }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }
    public List<TimeGrain> getGrains() { return grains; }
    public void setGrains(List<TimeGrain> grains) { this.grains = grains; }
    public List<SessionChunk> getChunks() { return chunks; }
    public void setChunks(List<SessionChunk> chunks) { this.chunks = chunks; }
    public HardSoftScore getScore() { return score; }
    public void setScore(HardSoftScore score) { this.score = score; }
}

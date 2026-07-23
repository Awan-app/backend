package com.ezdo.timefold;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import java.util.Set;
import java.util.UUID;

@PlanningEntity
public class SessionChunk {

    // Note: @PlanningId from domain.common is used in Timefold 2.x to uniquely identify entities.
    @PlanningId
    private UUID id;

    private UUID taskId;
    private UUID categoryId;
    private int durationInGrains;
    private int taskOrder;
    private boolean mandatory;
    private Set<UUID> dependsOnTaskIds;
    private int bufferMinutes;

    @PlanningVariable
    private TimeGrain startingGrain;

    public SessionChunk() {}

    public SessionChunk(UUID id, UUID taskId, UUID categoryId, int durationInGrains, int taskOrder,
                        boolean mandatory, Set<UUID> dependsOnTaskIds, int bufferMinutes) {
        this.id = id;
        this.taskId = taskId;
        this.categoryId = categoryId;
        this.durationInGrains = durationInGrains;
        this.taskOrder = taskOrder;
        this.mandatory = mandatory;
        this.dependsOnTaskIds = dependsOnTaskIds;
        this.bufferMinutes = bufferMinutes;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public int getDurationInGrains() { return durationInGrains; }
    public void setDurationInGrains(int durationInGrains) { this.durationInGrains = durationInGrains; }
    public int getTaskOrder() { return taskOrder; }
    public void setTaskOrder(int taskOrder) { this.taskOrder = taskOrder; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    public Set<UUID> getDependsOnTaskIds() { return dependsOnTaskIds; }
    public void setDependsOnTaskIds(Set<UUID> dependsOnTaskIds) { this.dependsOnTaskIds = dependsOnTaskIds; }
    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }

    public TimeGrain getStartingGrain() { return startingGrain; }
    public void setStartingGrain(TimeGrain startingGrain) { this.startingGrain = startingGrain; }

    @Override
    public String toString() {
        return "SessionChunk{taskId=" + taskId + ", duration=" + durationInGrains + ", startingGrain=" + startingGrain + '}';
    }
}

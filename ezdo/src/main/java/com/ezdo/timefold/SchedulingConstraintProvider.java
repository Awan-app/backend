package com.ezdo.timefold;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

import java.util.UUID;

public class SchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                categoryMatch(factory),
                stayWithinZone(factory),
                noOverlap(factory),
                respectBookedSessions(factory),
                respectBufferWithBookedSessions(factory),
                dependencyOrder(factory),
                respectBuffer(factory),
                minimizeFragmentation(factory),
                preferEarlier(factory)
                //number of sessions,
        };
    }

    // HARD: chunk placed in a zone whose category doesn't match the task's category
    Constraint categoryMatch(ConstraintFactory factory) {
        return factory.forEach(SessionChunk.class)
                .filter(chunk -> chunk.getStartingGrain() != null
                        && !categoryMatches(chunk))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Category mismatch");
    }

    private boolean categoryMatches(SessionChunk chunk) {
        UUID chunkCat = chunk.getCategoryId();
        UUID zoneCat = chunk.getStartingGrain().getCategoryId();
        if (chunkCat == null) {
            return false;
        }
        return chunkCat.equals(zoneCat);
    }

    // HARD: chunk overflows past the end of its containing zone
    Constraint stayWithinZone(ConstraintFactory factory) {
        return factory.forEach(SessionChunk.class)
                .filter(chunk -> chunk.getStartingGrain() != null
                        && chunk.getStartingGrain().getGrainIndex() + chunk.getDurationInGrains() > chunk.getStartingGrain().getZoneDurationGrains())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Overflows zone");
    }

    // HARD: two chunks overlap in time
    Constraint noOverlap(ConstraintFactory factory) {
        return factory.forEachUniquePair(SessionChunk.class)
                .filter((chunk1, chunk2) -> {
                    if (chunk1.getStartingGrain() == null || chunk2.getStartingGrain() == null) return false;
                    int s1 = chunk1.getStartingGrain().getAbsoluteStartMinute();
                    int e1 = s1 + (chunk1.getDurationInGrains() * 15);
                    int s2 = chunk2.getStartingGrain().getAbsoluteStartMinute();
                    int e2 = s2 + (chunk2.getDurationInGrains() * 15);
                    return s1 < e2 && s2 < e1;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Overlapping chunks");
    }

    // HARD: a dependent chunk starts before its prerequisite ends
    Constraint dependencyOrder(ConstraintFactory factory) {
        return factory.forEachUniquePair(SessionChunk.class)
                .filter((chunk1, chunk2) -> {
                    if (chunk1.getStartingGrain() == null || chunk2.getStartingGrain() == null) return false;

                    boolean c2DependsOnC1 = chunk2.getDependsOnTaskIds() != null && chunk2.getDependsOnTaskIds().contains(chunk1.getTaskId());
                    boolean c1DependsOnC2 = chunk1.getDependsOnTaskIds() != null && chunk1.getDependsOnTaskIds().contains(chunk2.getTaskId());

                    if (c2DependsOnC1) {
                        int e1 = chunk1.getStartingGrain().getAbsoluteStartMinute() + (chunk1.getDurationInGrains() * 15);
                        int s2 = chunk2.getStartingGrain().getAbsoluteStartMinute();
                        return e1 > s2;
                    } else if (c1DependsOnC2) {
                        int e2 = chunk2.getStartingGrain().getAbsoluteStartMinute() + (chunk2.getDurationInGrains() * 15);
                        int s1 = chunk1.getStartingGrain().getAbsoluteStartMinute();
                        return e2 > s1;
                    }
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Dependency order violated");
    }

    // HARD: two chunks are too close together (buffer not respected)
    Constraint respectBuffer(ConstraintFactory factory) {
        return factory.forEachUniquePair(SessionChunk.class)
                .filter((chunk1, chunk2) -> {
                    if (chunk1.getStartingGrain() == null || chunk2.getStartingGrain() == null) return false;
                    int end1 = chunk1.getStartingGrain().getAbsoluteStartMinute() + (chunk1.getDurationInGrains() * 15);
                    int start2 = chunk2.getStartingGrain().getAbsoluteStartMinute();

                    int end2 = chunk2.getStartingGrain().getAbsoluteStartMinute() + (chunk2.getDurationInGrains() * 15);
                    int start1 = chunk1.getStartingGrain().getAbsoluteStartMinute();

                    int buffer = Math.max(chunk1.getBufferMinutes(), chunk2.getBufferMinutes());

                    if (start2 >= end1 && start2 < end1 + buffer) return true;
                    if (start1 >= end2 && start1 < end2 + buffer) return true;
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Buffer violation");
    }

    // SOFT: penalise spreading the same task's chunks across multiple days
    Constraint minimizeFragmentation(ConstraintFactory factory) {
        return factory.forEachUniquePair(SessionChunk.class)
                .filter((chunk1, chunk2) ->
                        chunk1.getTaskId().equals(chunk2.getTaskId())
                        && chunk1.getStartingGrain() != null && chunk2.getStartingGrain() != null
                        && !chunk1.getStartingGrain().getDate().equals(chunk2.getStartingGrain().getDate()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Task fragmented across days");
    }

    // SOFT: prefer placing chunks as early as possible
    Constraint preferEarlier(ConstraintFactory factory) {
        return factory.forEach(SessionChunk.class)
                .filter(chunk -> chunk.getStartingGrain() != null)
                .penalize(HardSoftScore.ofSoft(1), chunk -> chunk.getStartingGrain().getAbsoluteStartMinute())
                .asConstraint("Prefer earlier grains");
    }
    Constraint respectBookedSessions(ConstraintFactory factory) {
        return factory.forEach(SessionChunk.class)
                .join(BookedInterval.class)
                .filter((chunk, booked) -> {
                    if (chunk.getStartingGrain() == null) return false;
                    int chunkStart = chunk.getStartingGrain().getAbsoluteStartMinute();
                    int chunkEnd = chunkStart + (chunk.getDurationInGrains() * 15);
                    return chunkStart < booked.getEndMinute() && booked.getStartMinute() < chunkEnd;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Overlaps booked session");
    }

    // HARD: chunk is too close to a booked session (buffer not respected)
    Constraint respectBufferWithBookedSessions(ConstraintFactory factory) {
        return factory.forEach(SessionChunk.class)
                .join(BookedInterval.class)
                .filter((chunk, booked) -> {
                    if (chunk.getStartingGrain() == null) return false;
                    int chunkStart = chunk.getStartingGrain().getAbsoluteStartMinute();
                    int chunkEnd = chunkStart + (chunk.getDurationInGrains() * 15);

                    int bookedStart = booked.getStartMinute();
                    int bookedEnd = booked.getEndMinute();

                    int buffer = chunk.getBufferMinutes();

                    if (bookedStart >= chunkEnd && bookedStart < chunkEnd + buffer) return true;
                    if (chunkStart >= bookedEnd && chunkStart < bookedEnd + buffer) return true;
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Buffer violation with booked session");
    }
}

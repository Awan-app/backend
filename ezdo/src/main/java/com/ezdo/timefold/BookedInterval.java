package com.ezdo.timefold;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an already-persisted, non-negotiable block of time
 * (e.g. a Session already committed to the DB from a prior solve).
 * Expressed in the same "absolute minute" coordinate space as TimeGrain,
 * so overlap checks are simple integer comparisons.
 */
public class BookedInterval {

    private UUID id;
    private int startMinute;
    private int endMinute;

    public BookedInterval() {}

    public BookedInterval(UUID id, int startMinute, int endMinute) {
        this.id = id;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getStartMinute() { return startMinute; }
    public void setStartMinute(int startMinute) { this.startMinute = startMinute; }
    public int getEndMinute() { return endMinute; }
    public void setEndMinute(int endMinute) { this.endMinute = endMinute; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookedInterval that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "BookedInterval{" + startMinute + "-" + endMinute + '}';
    }
}
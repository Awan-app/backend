package com.ezdo.timefold;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

public class TimeGrain {

    private long id;
    private LocalDate date;
    private LocalTime startTime;
    private int grainIndex;
    private UUID zoneId;
    private UUID categoryId;
    private long zoneDurationGrains;
    private int absoluteStartMinute;

    public TimeGrain() {}

    public TimeGrain(long id, LocalDate date, LocalTime startTime, int grainIndex, UUID zoneId, UUID categoryId, long zoneDurationGrains, int absoluteStartMinute) {
        this.id = id;
        this.date = date;
        this.startTime = startTime;
        this.grainIndex = grainIndex;
        this.zoneId = zoneId;
        this.categoryId = categoryId;
        this.zoneDurationGrains = zoneDurationGrains;
        this.absoluteStartMinute = absoluteStartMinute;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getGrainIndex() { return grainIndex; }
    public void setGrainIndex(int grainIndex) { this.grainIndex = grainIndex; }
    public UUID getZoneId() { return zoneId; }
    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public long getZoneDurationGrains() { return zoneDurationGrains; }
    public void setZoneDurationGrains(long zoneDurationGrains) { this.zoneDurationGrains = zoneDurationGrains; }
    public int getAbsoluteStartMinute() { return absoluteStartMinute; }
    public void setAbsoluteStartMinute(int absoluteStartMinute) { this.absoluteStartMinute = absoluteStartMinute; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeGrain timeGrain = (TimeGrain) o;
        return id == timeGrain.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TimeGrain{" +
                "date=" + date +
                ", startTime=" + startTime +
                ", zoneId=" + zoneId +
                '}';
    }
}

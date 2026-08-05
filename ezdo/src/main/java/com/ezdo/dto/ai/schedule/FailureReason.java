package com.ezdo.dto.ai.schedule;

//not used currently
public enum FailureReason {
    INSUFFICIENT_TIME,
    DEPENDENCY_CONFLICT,
    NO_SUITABLE_ZONE,
    EXCEEDS_DAILY_CAPACITY,
    /** The entire planning horizon contains no free time slots of any kind. */
    NO_FREE_TIME,
    /** Placement would require overlapping a locked (immovable) session. */
    LOCKED_COLLISION
}

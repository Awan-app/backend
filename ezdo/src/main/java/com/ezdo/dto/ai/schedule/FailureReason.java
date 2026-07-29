package com.ezdo.dto.ai.schedule;

public enum FailureReason {
    INSUFFICIENT_TIME,
    DEPENDENCY_CONFLICT,
    NO_SUITABLE_ZONE,
    EXCEEDS_DAILY_CAPACITY
}

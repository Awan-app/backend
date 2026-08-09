package com.ezdo.entity;

/**
 * The user-visible status of a task. Not persisted — it is derived in
 * {@code TaskMapper} from {@code Task.completedAt} plus the task's sessions.
 *
 * <p>{@link #COMPLETED} is reachable only by explicit declaration, never by
 * derivation: a task whose sessions are all done still reads {@link #ACTIVE}
 * until the user says it is finished.
 */
public enum TaskStatus {

    /** No sessions have been scheduled yet. */
    DRAFTED,

    /** Has at least one session that is not cancelled, and is not declared done. */
    ACTIVE,

    /** The user explicitly completed the task ({@code completedAt} is set). */
    COMPLETED,

    /** Every session was cancelled and the task was never declared done. */
    CANCELLED
}

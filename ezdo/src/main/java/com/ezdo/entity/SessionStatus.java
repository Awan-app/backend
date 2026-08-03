package com.ezdo.entity;

/**
 * The persisted lifecycle status of a {@code Session}.
 *
 * <p>Only terminal, explicitly-decided states are stored here. Time-based
 * states such as "active" (currently within its scheduled window) and
 * "missed" (window has elapsed with no resolution) are <b>not</b> stored —
 * they are derived at query time from {@link SessionStatus#SCHEDULED}
 * combined with the session's {@code startTime}/{@code endTime} versus the
 * current time. A missed session remains {@link #SCHEDULED} indefinitely
 * until a user retroactively marks it {@link #COMPLETED} or
 * {@link #CANCELLED}; there is no automatic expiry.
 */
public enum SessionStatus {

    /**
     * The session has been created and assigned a time window but has not
     * yet been resolved. This is the only non-terminal state.
     *
     * <p>Derived sub-states (not persisted, computed from the current time):
     * <ul>
     *   <li><b>active</b> — {@code now} falls within
     *       [{@code startTime}, {@code endTime}]</li>
     *   <li><b>missed</b> — {@code now} is after {@code endTime} and the
     *       session has still not been marked {@link #COMPLETED} or
     *       {@link #CANCELLED}</li>
     * </ul>
     */
    SCHEDULED,

    /**
     * The session was explicitly marked as done, either by the user or by
     * the executing task itself. Completion is never inferred from the
     * passage of time alone. Terminal state.
     */
    COMPLETED,

    /**
     * The session was explicitly cancelled and will not be executed or
     * retroactively completed. Terminal state.
     */
    CANCELLED
}

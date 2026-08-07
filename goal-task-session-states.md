# Task / Session — State Model

## Overview

Two entities, two layers of aggregation:

- **Task** — depends on other Tasks (DAG, dependency is informational only, not enforced), has zero or more Sessions
- **Session** — a scheduled execution of a Task in a time window

Only **Session** has a stored status. **Task** status is fully derived at read time from its Sessions — no status column, no sync bugs, no manual completion step.

---

## Session

The only entity with a persisted status field.

```java
public enum SessionStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
```

| Status      | Meaning                                                       |
| ----------- | ------------------------------------------------------------- |
| `SCHEDULED` | Created with a time window, not yet resolved. Non-terminal.   |
| `COMPLETED` | Explicitly marked done (by user or executing task). Terminal. |
| `CANCELLED` | Explicitly called off. Terminal.                              |

### Derived sub-states (not stored)

Computed from `SCHEDULED` + current time vs. `startTime`/`endTime`:

- **active** — `now` is within `[startTime, endTime]`
- **missed** — `now` is past `endTime`, still unresolved

A missed session stays `SCHEDULED` indefinitely. There is no auto-expiry — it waits for the user to retroactively mark it `COMPLETED` or `CANCELLED`.

### Transitions

```
SCHEDULED → COMPLETED
SCHEDULED → CANCELLED
```

Both are terminal. No transition out of `COMPLETED` or `CANCELLED` (fixing a mistake means creating a new session, not un-terminating an old one).

---

## Task

No stored status. Fully derived from its Sessions.

```java
public enum TaskStatus {
    DRAFTED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
```

### Derivation rule

```
if no sessions exist:                                DRAFTED
else if all sessions are CANCELLED:                   CANCELLED
else if every non-cancelled session is COMPLETED
        AND at least one session is COMPLETED:        COMPLETED
else:                                                  ACTIVE
```

| Sessions | Derived status |
|---|---|
| none | `DRAFTED` |
| all `CANCELLED` | `CANCELLED` |
| 2 `COMPLETED`, 1 `CANCELLED` | `COMPLETED` |
| 1 `SCHEDULED`, 2 `CANCELLED` | `ACTIVE` |
| 1 `COMPLETED`, 1 `SCHEDULED` | `ACTIVE` |

### Notes
- `COMPLETED` and `CANCELLED` are **not permanent** — attaching a new session to either automatically reopens the task to `ACTIVE` (or re-derives per the rule above), since there's nothing stored to hold it in place.
- "Cancel a task" = cancel all of its sessions. There's no separate task-level cancel action.
- Dependencies on other Tasks are **informational only** — shown in the UI (e.g. "usually comes after X"), not enforced. A Task can be started or completed regardless of its dependencies' status. The user has full control.

---

## Design principles behind this model

1. **Only Session has a stored status.** It's the only entity representing a real, irreversible fact (an actual scheduled slot of time). Task is an aggregate over that fact, so its status can always be recomputed.
2. **Dependency between Tasks is advisory, not a gate.** Nothing blocks a Task from starting or completing based on its dependencies.
3. **Nothing auto-expires.** Missed sessions sit open indefinitely; nothing silently cancels or completes based on time passing except the `active`/`missed` *display* derivation, which never touches stored state.
4. **Terminal states aren't truly terminal at the Task layer.** Because status is derived, `COMPLETED`/`CANCELLED` reflect a snapshot, not a lock — new activity reopens them automatically.

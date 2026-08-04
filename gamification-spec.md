# Gamification Spec — Streak & Daily Gift

## 1. Overview

Two independent gamification mechanics:

1. **Streak** — tracks consecutive days a user performs a *qualifying activity*. Currently, the only qualifying activity is completing a task. The mechanic is intentionally trigger-agnostic so future activities (habit check-ins, workouts, etc.) can extend it without redesigning the model.
2. **Daily Gift** — a manually-claimed, once-per-day coin reward. Independent of the streak; claiming the gift never affects the streak, and the streak never affects gift *eligibility* (only its reward amount, if we choose to scale it — open item, see §6).

These two systems have a **one-directional dependency**: the Daily Gift may *read* the Streak's current value to scale rewards, but the Streak has zero knowledge of the Daily Gift and is never written to by it.

---

## 2. Streak

### 2.1 Business rules

| Rule | Decision |
|---|---|
| Qualifying activity (v1) | Any task completion |
| Minimum activity per day | 1 |
| Day boundary | Fixed reference timezone — UTC |
| Missed-day behavior | **Hard reset** — streak drops to 0 |
| Reset mechanism | **Scheduled job**, not lazy/on-read computation |
| Reset schedule | Daily at **00:05 UTC** (5-minute buffer past midnight to absorb clock drift / in-flight writes) |
| Max streak | Historical high-water mark; only increases, never touched by the reset job |
| Streak type scope | Single streak per user (no discriminator/multi-type support in v1) |

### 2.2 Entity: `Streak`

One row per user (`@MapsId` 1:1 with `User`).

| Field | Type | Notes |
|---|---|---|
| `userId` | `UUID` | PK, shared with `User.id` |
| `user` | `User` | `@OneToOne` + `@MapsId` |
| `version` | `Long` | Optimistic locking |
| `currentStreak` | `int` | Consecutive qualifying days, default 0 |
| `maxStreak` | `int` | All-time high, default 0 |
| `lastActivityDate` | `LocalDate` | Last date the streak was extended |

The entity and its repository contain **no reference to "task"** — it only knows about generic activity dates.

### 2.3 Update logic (triggered by task completion today; other triggers later)

Called via `StreakService.recordQualifyingActivity(userId, today)`:

- `lastActivityDate == today` → no-op (already counted today).
- `lastActivityDate == today.minusDays(1)` → `currentStreak++`.
- Otherwise (gap, or first-ever activity) → `currentStreak = 1`.
- `maxStreak = max(maxStreak, currentStreak)` on every update.
- `lastActivityDate = today`.

The caller (task-completion service) does not need to know streak internals; the streak service does not need to know what triggered it.

### 2.4 Reset job

Runs once daily, independent of any request path.

- **Schedule**: `00:05 UTC`.
- **Logic**: bulk `UPDATE` — reset `currentStreak = 0` for every `Streak` where `lastActivityDate < yesterday` and `currentStreak > 0`.
- Bulk JPQL update (not load-modify-save loop) — no per-row entity loading, bypasses `@Version` (acceptable since this is a maintenance operation, not a user-driven write).
- Does **not** touch `maxStreak`.
- Single-instance deployment — no distributed lock needed today. If the app scales horizontally later, add a lock (e.g. ShedLock) before running multiple instances.

---

## 3. Daily Gift — Daily Wheel

The daily gift is a **spin-the-wheel** mechanic rather than a flat reward. The player spins once per day; the segment landed on determines the coin payout.

### 3.1 Business rules

| Rule | Decision |
|---|---|
| Claim trigger | **Manual** — user taps "spin" |
| Frequency | Once per calendar day (UTC) |
| Idempotency | Enforced at DB level via unique constraint on `(user_id, claim_date)` |
| Relationship to streak | None — the wheel does not read or write `Streak`. Streak-based scaling explicitly out of scope for now |
| Missed-day behavior on gift side | None — not spinning yesterday has no bearing on today's spin eligibility |
| Outcome authority | **Backend only.** The server determines the winning segment and persists the result before the client renders anything. The client never determines or submits the outcome — it only animates toward a result it's told in advance |
| Prize table source | **Hardcoded** (config class), not DB-backed, for now |
| Zero-value outcomes | **Not allowed.** Every spin awards ≥ 1 coin — no "nothing" segment |
| Segment weighting | Weighted random selection; weights are arbitrary integers, not required to sum to 100 |

### 3.2 Prize table (hardcoded, illustrative)

| Segment | Coins | Weight |
|---|---|---|
| SEG_1 | 1 | 40 |
| SEG_2 | 5 | 25 |
| SEG_3 | 10 | 15 |
| SEG_4 | 20 | 10 |
| SEG_5 | 50 | 7 |
| SEG_6 | 100 | 3 |

Selection probability for a segment = `weight / sum(all weights)`. Values are tunable in code; no deploy-free runtime tuning in v1 (would require migrating to a DB-backed table later).

### 3.3 Entity: `DailyGiftClaim`

Append-only audit/idempotency table — one row per user per day claimed. Stores a **snapshot** of the outcome, not a live reference to the prize table, so history stays accurate even if the table is later changed.

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `user` | `User` | `@ManyToOne` |
| `claimDate` | `LocalDate` | The day this claim covers |
| `segmentId` | `String` | Snapshot of which segment was won (e.g. `"SEG_4"`) — not a FK, since the prize table isn't a DB entity in v1 |
| `coinsAwarded` | `int` | Snapshot of the actual prize value at claim time |
| `claimedAt` | `Instant` | Actual timestamp of the spin |

Unique constraint: `(user_id, claim_date)`.

### 3.4 Spin flow

1. Pre-check: does a claim already exist for `(userId, today)`? If yes → reject (`409`-style "already claimed").
2. Backend runs weighted random selection over the hardcoded segment list → winning segment.
3. Insert `DailyGiftClaim` row (userId, today, segmentId, coinsAwarded); flush immediately so the unique constraint fires within the transaction.
   - On constraint violation → treat as "already claimed" (handles race between the pre-check and insert).
4. Credit `Wallet.coinBalance` by the segment's coin value.
5. Return `{ segmentId, coinsAwarded, newBalance }` to the client — this is what drives the spin animation. The client animates a spin that visually ends on the segment it was told, it does not decide the outcome itself.

### 3.5 Why outcome selection must be server-side

If the client selected the winning segment (or was trusted to report it), the mechanic would be trivially exploitable — since real currency is credited as a result, the server must be the sole source of truth for what was won, not just a passive recipient of a claimed prize.

---

## 4. Wallet

Unchanged from earlier design — one row per user, credited by the Daily Gift claim flow (and potentially other future sources of coins).

| Field | Type | Notes |
|---|---|---|
| `userId` | `UUID` | PK, shared with `User.id` |
| `user` | `User` | `@OneToOne` + `@MapsId` |
| `version` | `Long` | Optimistic locking |
| `coinBalance` | `long` | Current balance |

---

## 5. Service boundaries

```
TaskCompletionService ──▶ StreakService.recordQualifyingActivity(userId, today)
                                        │
                                        ▼
                                   Streak (entity)

DailyGiftService ──spins wheel (server-side)──▶ WheelSpinService ──▶ credits Wallet
                  ──writes──▶ DailyGiftClaim
```

Key invariant: **`Streak` and the Daily Wheel are fully independent.** `Streak` is never aware of `DailyGiftClaim` or `Wallet`, and the wheel does not read from `Streak`. This keeps both mechanics reusable/replaceable without cross-impact.

---

## 6. Open questions

- Whether `Wallet` auto-creates on user registration (event listener) or lazily on first credit.
- Whether additional qualifying activities will be added to the streak, and if/when a multi-streak-type model becomes necessary (not needed for v1).
- Whether the wheel prize table migrates to a DB-backed model later (needed only if runtime tuning / promotions become a requirement).

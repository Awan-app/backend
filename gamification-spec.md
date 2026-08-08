# Gamification Spec — Streak & Daily Gift

## 1. Overview

Two independent gamification mechanics:

1. **Streak** — tracks consecutive days a user performs a *qualifying activity*. Currently, the only qualifying activity is completing a session. The mechanic is intentionally trigger-agnostic so future activities (habit check-ins, workouts, etc.) can extend it without redesigning the model.
2. **Daily Gift** — a manually-claimed, once-per-day coin reward. Independent of the streak; claiming the gift never affects the streak, and the streak never affects gift *eligibility* (only its reward amount, if we choose to scale it — open item, see §6).

These two systems have a **one-directional dependency**: the Daily Gift may *read* the Streak's current value to scale rewards, but the Streak has zero knowledge of the Daily Gift and is never written to by it.

---

## 2. Streak

### 2.1 Business rules

| Rule | Decision |
|---|---|
| Qualifying activity (v1) | Any session completion |
| Minimum activity per day | 1 |
| Day boundary | The **user's own timezone**, from `Preferences.timezone`; UTC only as a fallback when unset |
| Missed-day behavior | **Hard reset** — streak drops to 0 |
| Reset mechanism | **Derived on read**, no scheduled job |
| Reset schedule | N/A — see §2.4 |
| Max streak | Historical high-water mark; only increases, never reduced by a lapse |
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

### 2.3 Update logic (triggered by session completion today; other triggers later)

Called via `StreakService.recordQualifyingActivity(user)`, which resolves `today` itself through `UserClockService` — the caller never passes a date, so no caller can get the timezone wrong:

- `lastActivityDate == today` → no-op (already counted today).
- `lastActivityDate == today.minusDays(1)` → `currentStreak++`.
- Otherwise (gap, or first-ever activity) → `currentStreak = 1`.
- `maxStreak = max(maxStreak, currentStreak)` on every update.
- `lastActivityDate = today`.

The caller (task-completion service) does not need to know streak internals; the streak service does not need to know what triggered it.

### 2.4 Lapse handling — derived, not scheduled

There is no reset job. A lapsed streak is computed at read time by `Streak.effectiveStreak(today)`:

- `lastActivityDate` is `null`, or older than `today.minusDays(1)` → returns `0`.
- Otherwise → returns the stored `currentStreak`.
- `maxStreak` is never affected; the stored `currentStreak` is left untouched and self-heals on the next recorded activity, which restarts the count at 1.

An earlier draft specified a nightly bulk `UPDATE` at 00:05 UTC. That was dropped because it is incompatible with per-user day boundaries (§2.1): at 00:05 UTC it is still the previous evening in the Americas, so the job would zero streaks that are still alive. Deriving on read also needs no distributed lock when the app scales horizontally, and cannot leave stale rows behind if a run fails.

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
4. Credit `Wallet.points` by the segment's coin value.
5. Return `{ segmentId, coinsAwarded, newBalance }` to the client — this is what drives the spin animation. The client animates a spin that visually ends on the segment it was told, it does not decide the outcome itself.

### 3.5 Why outcome selection must be server-side

If the client selected the winning segment (or was trusted to report it), the mechanic would be trivially exploitable — since real currency is credited as a result, the server must be the sole source of truth for what was won, not just a passive recipient of a claimed prize.

---

## 4. Wallet

One row per user. Credited today by session completion (the completed session's task carries an `estimatedPoints` award), and by the Daily Gift claim flow once that ships.

| Field | Type | Notes |
|---|---|---|
| `userId` | `UUID` | PK, shared with `User.id` |
| `user` | `User` | `@OneToOne` + `@MapsId` |
| `version` | `Long` | Optimistic locking |
| `points` | `long` | Current balance |

---

## 5. Service boundaries

```
SessionService ──▶ GamificationService.onSessionCompleted(user, session)
                            │
                            ├──▶ WalletService.credit(user, task.estimatedPoints)  ──▶ Wallet (entity)
                            │
                            └──▶ StreakService.recordQualifyingActivity(user)      ──▶ Streak (entity)

DailyGiftService ──spins wheel (server-side)──▶ WheelSpinService ──▶ credits Wallet
                  ──writes──▶ DailyGiftClaim
```

`GamificationService` is the single façade: `SessionService` calls it once and stays ignorant of both mechanics. It also serves the combined read (`getProgress` → `UserProgressResponse`), which is the one place points and streak appear together.

Key invariant: **`Streak` and the Daily Wheel are fully independent.** `Streak` is never aware of `DailyGiftClaim` or `Wallet`, and the wheel does not read from `Streak`. This keeps both mechanics reusable/replaceable without cross-impact. Note that `GamificationService` writing both on session completion does not breach this — the two writes are unaware of each other.

---

## 6. Open questions

- ~~Whether `Wallet` auto-creates on user registration (event listener) or lazily on first credit.~~ **Resolved:** both `Wallet` and `Streak` are created eagerly at registration by `UserProvisioningService`, the single point every login path goes through. Lazy creation raced two concurrent callers into inserting the same `user_id` PK, and forced null-handling into every read.
- Whether additional qualifying activities will be added to the streak, and if/when a multi-streak-type model becomes necessary (not needed for v1).
- Whether the wheel prize table migrates to a DB-backed model later (needed only if runtime tuning / promotions become a requirement).

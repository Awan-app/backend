# Gamification — Frontend Guide

This is the mobile developer's reference for the gamification features: **points**, **streak**,
**store**, and **inventory**. It explains the user-facing behavior, the endpoints to call, and the
exact JSON each returns.

All endpoints are under `/api/v1` and need `Authorization: Bearer <accessToken>`.

---

## 1. The big picture

| Feature | What it is | Where the user sees it |
|---|---|---|
| **Points** | Currency earned by finishing sessions, spent in the store. | Profile / wallet, store checkout |
| **Streak** | Consecutive days with at least one completed session. | Profile / home banner |
| **Store** | Catalog of cosmetic items, each with a price in points. | Store screen |
| **Inventory** | Items the user has bought. | Profile / inventory screen |
| **Equipped items** | The cosmetics currently shown on the user's profile / avatar — one per type. | Profile / avatar |

Two important rules:

1. **A session gives rewards only the first time it's completed.** Completing it again later gives
   nothing — you'll still get the response, just with `awarded: false`.
2. **Points only ever change through session completion and store purchases.** There is no "add
   points" endpoint.
3. **Session status only changes through dedicated endpoints** (`complete` / `uncomplete` /
   `cancel`). The old generic status-update endpoint is deprecated — see Section 3.
4. **Equipping or unequipping never changes points.** It only picks which owned item of each type
   is shown on the profile.

---

## 2. Earning points & streak (completing a session)

### Endpoint

```
POST /api/v1/sessions/{sessionId}/complete
```

No request body.

### Behavior

- Marks the session as completed.
- On the session's **first** completion, the user earns:
  - **Points** = the task's `estimatedPoints` (if the task has no points, none are earned).
  - **Streak** — this counts as an "activity day" (see Section 3).
- Every completion returns the same shape; the `reward` tells you whether anything was actually
  awarded.

### Response — 200 OK

```json
{
  "session": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "start": "2026-07-22T09:00:00",
    "end": "2026-07-22T10:00:00",
    "status": "COMPLETED",
    "locked": false,
    "firstCompletedAt": "2026-07-22T09:00:00Z",
    "zoneId": "550e8400-e29b-41d4-a716-446655440002",
    "taskId": "550e8400-e29b-41d4-a716-446655440099"
  },
  "reward": {
    "points": { "awarded": true, "amount": 25, "oldValue": 150, "newValue": 175 },
    "streak": { "updated": true, "oldValue": 5, "newValue": 6, "maxStreakBroken": true, "maxStreakOld": 6, "maxStreakNew": 7 }
  }
}
```

### The `reward` object — how to animate/celebrate

| Field | Meaning |
|---|---|
| `reward.points.awarded` | `true` = points were actually added. **Show the "+25 points" toast/confetti only when this is true.** |
| `reward.points.amount` | Points added (`0` when `awarded` is false). |
| `reward.points.oldValue` / `newValue` | Balance before / after. |
| `reward.streak.updated` | `true` = the visible streak changed. |
| `reward.streak.newValue` | New streak number to display. |
| `reward.streak.maxStreakBroken` | `true` = new personal best. **Good moment for a special celebration.** |
| `reward.streak.maxStreakNew` | New best streak. |

**Frontend tip:** always read `awarded`/`updated` — don't assume completing a session always gives
points. On re-completion you'll get `awarded: false`, `updated: false`, and the values will be
unchanged.

### Errors

| HTTP | Code | When |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in / expired token. |
| 404 | `SESSION_NOT_FOUND` | Session doesn't exist or isn't the user's. |
| 400 | `INVALID_OPERATION` | Session can't be completed from its current state (e.g. already cancelled). |

---

## 3. Session status — what changed

Sessions now change status only through **dedicated endpoints**. There is no generic "set the
status" endpoint anymore, and creating or editing a session never sets its status.

### How a session's status changes

| Action | Endpoint | Result |
|---|---|---|
| Mark as done (earns points/streak) | `POST /api/v1/sessions/{sessionId}/complete` | `COMPLETED` |
| Undo a completion (no points are taken back) | `POST /api/v1/sessions/{sessionId}/uncomplete` | `SCHEDULED` |
| Call it off | `POST /api/v1/sessions/{sessionId}/cancel` | `CANCELLED` |

### What is deprecated / ignored

- **`PATCH /api/v1/sessions/{sessionId}/status`** is deprecated. If you still call it, it just
  forwards to the matching endpoint above (`COMPLETED` → complete, `SCHEDULED` → uncomplete,
  `CANCELLED` → cancel). **Migrate to the dedicated endpoints** — the old one will be removed.
- **`status` in the create/edit request bodies is ignored.** Sessions are always created
  `SCHEDULED`, and editing a session only changes its times — never its status. Remove `status`
  from your payloads.

### Rules to know

- A completed session can only go back to `SCHEDULED` (`uncomplete`). It can't be cancelled.
- A cancelled session stays cancelled — it can't be reopened or completed.
- `uncomplete` and `cancel` return a plain `SessionResponse` (no `reward`). Rewards only come from
  `complete`.

### Errors

| HTTP | Code | When |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in / expired token. |
| 404 | `SESSION_NOT_FOUND` | Session doesn't exist or isn't the user's. |
| 400 | `INVALID_OPERATION` | Illegal transition (e.g. completing a cancelled session). |

---

## 4. Streak

The streak counts **consecutive days with at least one completed session**. Completing several
sessions on the same day still counts as one day.

### Rules the user will feel

- Complete something today → streak grows by 1 (if you were active yesterday too).
- Complete nothing for a full day → streak resets to 0.
- **No manual reset anywhere** — it only resets by missing a day.

### `GET /api/v1/gamification/progress`

The one call for the profile / streak banner.

**Response — 200 OK:**

```json
{
  "points": 175,
  "streak": 6,
  "maxStreak": 7
}
```

| Field | Meaning |
|---|---|
| `points` | Total points available to spend. |
| `streak` | Current streak (`0` if the user missed a day). |
| `maxStreak` | Best streak ever. |

### `GET /api/v1/gamification/activity-dates?startDate=...&endDate=...`

Used to render a "streak calendar" (which days were active). Dates are `YYYY-MM-DD`.

**Query parameters:**

| Param | Required | Example |
|---|---|---|
| `startDate` | yes | `2026-07-01` |
| `endDate` | yes | `2026-07-31` |

**Response — 200 OK:**

```json
["2026-07-01", "2026-07-02", "2026-07-03"]
```

Empty array (`[]`) means no activity in that range.

---

## 5. Store

### `GET /api/v1/store/items?type=FRAME`

Lists everything available to buy. `type` is optional (`FRAME`, `SKIN`, `THEME`, `ICON`); omit it
to get the whole catalog.

**Response — 200 OK:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Gold Frame",
    "description": "A shiny gold frame for your profile.",
    "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
    "info": null,
    "price": 100,
    "version": "1.0",
    "type": "FRAME"
  }
]
```

| Field | Meaning |
|---|---|
| `id` | Use this to buy the item. |
| `name` / `description` | Display text. |
| `image` | Image URL (may be `null`). |
| `price` | Cost in points. |
| `type` | Category: `FRAME`, `SKIN`, `THEME`, `ICON`. |

**Tip:** compare `price` against the `points` from `/progress` to enable/disable the Buy button.

---

## 6. Inventory

### `GET /api/v1/store/inventory`

Everything the user already owns.

**Response — 200 OK:**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440011",
    "item": {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Gold Frame",
      "description": "A shiny gold frame for your profile.",
      "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
      "info": null,
      "price": 100,
      "version": "1.0",
      "type": "FRAME"
    },
    "boughtAt": "2026-07-22T10:00:00Z"
  }
]
```

### `POST /api/v1/store/items/{itemId}/buy`

Buys an item. No request body. Deducts the price from points and adds the item to inventory.

**Response — 200 OK:**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440011",
  "item": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Gold Frame",
    "description": "A shiny gold frame for your profile.",
    "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
    "info": null,
    "price": 100,
    "version": "1.0",
    "type": "FRAME"
  },
  "boughtAt": "2026-07-22T10:00:00Z"
}
```

### Buy errors to handle in the UI

| HTTP | Code | What to tell the user |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in. |
| 404 | `ITEM_NOT_FOUND` | Item doesn't exist (shouldn't happen from the catalog). |
| 409 | `ITEM_ALREADY_OWNED` | "You already own this item" — disable Buy for owned items (compare with inventory). |
| 400 | `INSUFFICIENT_POINTS` | "Not enough points." The response `info` includes `currentPoints` and `requestedPoints`. |

---

## 7. Equipping items

You can pick which owned cosmetic is shown for each type (`FRAME`, `SKIN`, `THEME`, `ICON`). There
is **one slot per type** — so at most 4 equipped items at once. Equipping never costs points; it
only changes what's displayed.

### `GET /api/v1/store/equipped`

Lists everything currently equipped — the source of truth for rendering the profile/avatar.

**Response — 200 OK:**

```json
[
  {
    "type": "FRAME",
    "item": {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Gold Frame",
      "description": "A shiny gold frame for your profile.",
      "image": "https://res.cloudinary.com/demo/image/upload/v1/ezdo/store/gold_frame.png",
      "info": null,
      "price": 100,
      "version": "1.0",
      "type": "FRAME"
    },
    "equippedAt": "2026-07-22T12:00:00Z"
  }
]
```

| Field | Meaning |
|---|---|
| `type` | Which slot this fills: `FRAME`, `SKIN`, `THEME`, `ICON`. |
| `item` | The equipped item (same shape as inventory items). |
| `equippedAt` | When it was equipped. |

Empty array (`[]`) means nothing is equipped yet.

### `POST /api/v1/store/items/{itemId}/equip`

Equips an owned item. No request body.

- The item must be in the user's **inventory** — otherwise `ITEM_NOT_OWNED` (409).
- One slot per type: equipping an item whose type is already occupied **silently replaces** the old
  one. No error, the old item just goes back to being "owned but not equipped".
- Re-equipping the same item is a **no-op** — safe to call again, returns the same slot.

**Response — 200 OK:** the `EquippedItemResponse` above for that slot.

### `DELETE /api/v1/store/equipped/{type}`

Clears the slot for a type. Path param is the `ItemType` (`FRAME`, `SKIN`, `THEME`, `ICON`).

**Response — 204:** No content.

Idempotent — clearing an empty slot still returns `204`, so the client doesn't need to check first.

### Equip/unequip errors to handle in the UI

| HTTP | Code | What to tell the user |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Not logged in. |
| 404 | `ITEM_NOT_FOUND` | Item doesn't exist (shouldn't happen from the catalog). |
| 404 | `USER_NOT_FOUND` | User not found. |
| 409 | `ITEM_NOT_OWNED` | "You don't own this item" — only enable equip for items in the inventory. |
| 400 | `TYPE_MISMATCH` | Invalid `type` value in the delete path (not one of the four). |

### Where to get the equipped state

`GET /api/v1/users/me` returns an `equippedItems` array with the same `EquippedItemResponse`
shape. It's convenient for pre-filling the profile/avatar and for the "which item of each type is
active" indicator — no separate call needed. `/store/equipped` and the profile always agree.

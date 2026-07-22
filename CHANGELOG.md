# Changelog

## [1.0.0] - 2026-07-22

### Added

- **Sessions by date endpoint** — `GET /api/v1/sessions/date/{date}` returns sessions for a specific date, sorted by start time ascending.
- **Sessions by date range endpoint** — `GET /api/v1/sessions/range?startDate=...&endDate=...` returns sessions grouped by date in a map, sorted within each group. Validates that `endDate` is not before `startDate`.
- **Zone overlap validation** — Creating or updating zones now checks for time range overlaps within the same template or override. Returns `409 CONFLICT` with `ZONE_OVERLAP` error code on conflict.
- **`taskId` field to `SessionResponse`** — All session responses now include the associated task ID.
- **Tasks by date endpoint** — `GET /api/v1/tasks/date/{date}` returns tasks that have sessions on that date, each with its sessions for the day.
- **Tasks by date range endpoint** — `GET /api/v1/tasks/range?startDate=...&endDate=...` returns tasks grouped by date, each with its sessions for that day. Validates that `endDate` is not before `startDate`.

### Fixed

- **Zones by date sorting** — `GET /api/v1/zones/date/{date}` now returns zones sorted by `startTime` ascending.

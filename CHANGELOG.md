# Changelog

## [1.0.0] - 2026-07-22

### Added

- **Sessions by date endpoint** — `GET /api/v1/sessions/date/{date}` returns sessions for a specific date, sorted by start time ascending.
- **Sessions by date range endpoint** — `GET /api/v1/sessions/range?startDate=...&endDate=...` returns sessions grouped by date in a map, sorted within each group. Validates that `endDate` is not before `startDate`.
- **Zone overlap validation** — Creating or updating zones now checks for time range overlaps within the same template or override. Returns `409 CONFLICT` with `ZONE_OVERLAP` error code on conflict.
- **`taskId` field to `SessionResponse`** — All session responses now include the associated task ID.

### Fixed

- **Zones by date sorting** — `GET /api/v1/zones/date/{date}` now returns zones sorted by `startTime` ascending.

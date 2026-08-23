# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Branch: `feat/v1-watchlist`
- Frozen v1 spec: `docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md`
- Plan: `docs/superpowers/plans/2026-08-22-fund-helper-v1.md` (10 tasks)
- Tasks 1–4 done: Compose/Hilt/Room Gradle skeleton, `ExplanationMapper`, TEFAS JSON fixtures, `OkHttpTefasClient` behind `TefasClient`. `applicationId` `com.burha.fundhelper`. Unit tests for mapper + JSON fixtures pass. Nothing installed on the A23.

## Next

Task 5: Room follows and snapshots. Then repository, three screens, debug APK on the A23.

## Blockers

- None for Task 5. Expected later: TEFAS may challenge a mobile user-agent (Akamai). Swap `TefasClient` only.

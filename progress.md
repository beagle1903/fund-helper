# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Tasks 1–4 are on `main` ([PR #1](https://github.com/beagle1903/fund-helper/pull/1)).
- Task 5 (this PR): Room `follows` + `snapshots`, `SnapshotMapper`, Hilt `AppDatabase`, in-memory DAO fakes. Unfollow deletes the follow row only. Compile of debug + unit-test Kotlin succeeds. Nothing installed on the A23.

## Next

Task 6: `FundRepository` (TDD). Then three screens, debug APK on the A23.

## Blockers

- None for Task 6. Expected later: TEFAS may challenge a mobile user-agent (Akamai). Swap `TefasClient` only.
- Wrap OkHttp `IOException` (offline/DNS/timeout) as `TefasFetchException` so search/refresh keep cache + snackbar.

# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Tasks 1–5 are on `main` (merged PRs).
- Task 6 done: `FundRepository` with follow/unfollow/search/refresh, tests pass. OkHttp `IOException` wrapped as `TefasFetchException`. Ready on branch `feat/v1-repository`.

## Next

Task 7: Watchlist screen. Then two more screens, debug APK on the A23.

## Blockers

- None for Task 7. Expected later: TEFAS may challenge a mobile user-agent (Akamai). Swap `TefasClient` only.

# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Tasks 1–9 are on `main` (PRs #1–#6). v1 UI is complete (watchlist, search, detail).
- Task 10: unit tests passed; debug APK assembled; **sideloaded on Samsung A23** (`SM-A235F`, serial `R68TB02DR2V`). Package `com.burha.fundhelper` is installed (`pm path` returned a `package:` path).
- Manual smoke still for you on device: empty watchlist copy, search submit, follow, kill/reopen (follows remain), airplane mode snackbar + list still there, Detail disclaimer **Yatırım tavsiyesi değildir.**

## Next

Play Store (out of the v1 plan). If TEFAS challenges the phone (Akamai), swap `TefasClient` only.

## Blockers

- None for sideload. Expected later: TEFAS may challenge a mobile user-agent.

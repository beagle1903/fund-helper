# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- `main` is at [PR #14](https://github.com/beagle1903/fund-helper/pull/14) (bulk follow + RESET).
- Branch `feat/refresh-inflight-guard`: `refreshFollowed` mutex and result caching so overlapping watchlist + detail auto-refreshes share one TEFAS round trip (for both success and failure). Fixed code review feedback: failed refreshes are now cached and shared to prevent retry storms during TEFAS outages. Sideloaded on the A23 with `adb install -r` (`com.burha.fundhelper`). Do **not** uninstall.

## Next

- On the phone: open the app, tap a followed fund while home is still refreshing. Confirm one TEFAS burst, not two. Pull-to-refresh still works.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

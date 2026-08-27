# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- `main` is at [PR #14](https://github.com/beagle1903/fund-helper/pull/14) (bulk follow + RESET).
- Branch `feat/refresh-inflight-guard`: `refreshFollowed` uses a mutex so concurrent watchlist + detail auto-refreshes hit TEFAS once. `FundRepositoryTest` passed including `concurrent_unforced_refreshes_share_one_tefas_round_trip`.
- A23 last verified on the [PR #12](https://github.com/beagle1903/fund-helper/pull/12) APK. Do **not** uninstall.

## Next

- Plug in the A23 and `adb install -r` (bulk follow / RESET still not on the phone, then this guard). Confirm opening a followed fund during home refresh does not fire two TEFAS bursts.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

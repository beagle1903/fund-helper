# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- `main` still has [PR #12](https://github.com/beagle1903/fund-helper/pull/12) (Turkish search fold). A23 last verified on that APK. Do **not** uninstall.
- Branch `feat/watchlist-return-sort`: watchlist sorts by headline return (worst first, best last); empty **Fon ara** and search **Ara** are full-width. `testDebugUnitTest` and `assembleDebug` succeeded. USB had no device, so this APK is not on the phone yet.

## Next

- Plug in the A23 and `adb install -r` this branch. Confirm a mixed-return watchlist (negative above positive; `—` at the bottom) and the full-width empty/search buttons.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

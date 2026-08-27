# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Amendment on `feat/bulk-follow-reset` (not merged): Search **Ara** classifies exact `RESET` (clear Room follows + empty Downloads backup; snapshots stay), comma-separated exact catalog codes (`followAll` append, one backup write; keep existing snapshot price/priceDate when the catalog row has none), or today's text search. Reset and bulk follow pop to the watchlist. `RESET,` is not a wipe. Watchlist sort unchanged. `testDebugUnitTest` BUILD SUCCESSFUL.
- `main` is at [PR #13](https://github.com/beagle1903/fund-helper/pull/13) (watchlist sort by headline return; full-width empty/search buttons).
- A23 last verified on the [PR #12](https://github.com/beagle1903/fund-helper/pull/12) APK. The sort APK may still not be on the phone. No USB device this session. Sideload with `adb install -r` only. Do **not** uninstall.

## Next

- Plug in the A23 and `adb install -r` this branch. Confirm: `AAK, AAL` lands on the watchlist with both followed and does **not** blank existing prices; unknown codes skipped; `RESET` empties the list and Downloads JSON (one file, empty codes); a single code still shows search results; `RESET, AAK` does not wipe.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

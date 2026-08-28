# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Watchlist Pay/Kişi + fetched-at moved to full-width `below` slot on `FundRowCard`; price stays in `supporting`. Search cards unchanged (no `below`). Living docs updated.
- `:app:testDebugUnitTest` PASS (BUILD SUCCESSFUL, no live TEFAS). A23 sideload skipped (no device on `adb devices`).

## Next

- Sideload when A23 is connected (`.\gradlew.bat :app:installDebug`). Manual: followed card shows `Pay … · Kişi …` on one line under price/return; fetched-at below that; search card unchanged.
- If pay/kişi counts show `—` on the phone, confirm TEFAS JSON keys (`tedPaySayisi` / `kisiSayisi`) against a live payload.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

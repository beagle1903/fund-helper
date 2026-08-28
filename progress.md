# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Pay/kişi day-over-day % on watchlist + totals on detail are in the tree; on-screen disclaimer gone. Living docs updated (ADR 007). Do **not** uninstall (`com.burha.fundhelper`).
- `:app:testDebugUnitTest` PASS (no live TEFAS). A23 sideload skipped this session (no device on `adb devices`).

## Next

- Sideload when A23 is connected (`.\gradlew.bat :app:installDebug`). Manual: followed card both %; detail totals + %; no disclaimer; search unchanged; airplane keeps list.
- If pay/kişi counts show `—` on the phone, confirm TEFAS JSON keys (`tedPaySayisi` / `kisiSayisi`) against a live payload.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

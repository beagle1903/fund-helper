# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- UI polish is on `feat/ui-polish` (teal Material 3, fund cards, signed return colors). Spec is Approved.
- Unit tests: 29/29 PASS (`FundRepositoryTest`, `FollowBackupCodecTest`, `ReturnSignTest` included). `assembleDebug` succeeded (`app/build/outputs/apk/debug/app-debug.apk`).
- A23 was not on `adb devices` this session. Sideload skipped. Do **not** uninstall.

## Next

- When the A23 is `device`: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Manual: empty watchlist, a followed card, search card, detail disclaimer visible, light and dark if the phone can switch.
- Re-follow funds once so `Download/com.burha.fundhelper-follows.json` exists.

## Blockers

- USB empty this session; polish APK is not on the A23 yet.
- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

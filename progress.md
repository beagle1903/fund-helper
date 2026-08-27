# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Turkish search fold is on `feat/turkish-search-fold`
- Unit tests 37/37 PASS and `assembleDebug` succeeded this session (APK at `app/build/outputs/apk/debug/app-debug.apk`)
- A23 sideload skipped this session (no USB) — do **not** uninstall

## Next

- Sideload with `adb install -r` when the phone is `device`; manual search `yatirim` / `degisken` / `AAK`; re-follow so Downloads backup exists

## Blockers

- USB empty; fold APK (and polish) not verified on the A23 this session
- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered; re-follow so `Download/com.burha.fundhelper-follows.json` exists
- Expected later: TEFAS may challenge a mobile user-agent

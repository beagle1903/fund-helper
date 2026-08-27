# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- v1 (Tasks 1–10), colorful launcher (PR #8), and follow durability (PR #9) are on `main` (`777e7c2`). Only `main` remains locally and on origin.
- Follows still live in Room. Codes are also mirrored to `Download/com.burha.fundhelper-follows.json` and restored when Room is empty. Settings uninstall can keep app data (`hasFragileUserData`).
- Durability APK is **not** on the A23 yet (USB was empty this session).

## Next

- Sideload with `adb install -r` (do **not** uninstall). Re-follow funds once so the Downloads file exists.
- UI polish (default Material 3 still looks dull).

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

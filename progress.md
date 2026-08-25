# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- v1 (Tasks 1–10) is on `main`. Watchlist / search / detail work; debug APK was sideloaded on the A23 (`SM-A235F`).
- Colorful adaptive launcher (teal + gold/coral/mint bars) is on `feat/colorful-launcher-icon`. `assembleDebug` succeeded; A23 was **not** on USB for this install.

## Next

- Sideload the icon APK when the A23 is plugged in (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
- After that PR merges: UI polish (default Material 3 still looks dull).

## Blockers

- USB data cable needed for the icon sideload. Expected later: TEFAS may challenge a mobile user-agent.

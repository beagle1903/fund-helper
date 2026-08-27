# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- v1 (Tasks 1-10) is on `main`. Watchlist / search / detail work; debug APK was sideloaded on the A23 (`SM-A235F`).
- Colorful adaptive launcher is on `main` (PR #8). Uninstall-then-reinstall to refresh the icon **wiped follows** (Room lives in `/data/data/com.burha.fundhelper/...`; uninstall deletes that. `adb install -r` does not).
- Follow durability is on `feat/follow-durability`: Room still holds the live list; followed codes are mirrored to `Download/com.burha.fundhelper-follows.json` and restored when Room is empty; `hasFragileUserData` for Settings uninstall.

## Next

- Sideload the durability APK with `adb install -r` (do not uninstall). Re-follow funds once; later icon updates must use `-r`.
- UI polish (default Material 3 still looks dull).

## Blockers

- The list lost in the icon-refresh uninstall cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

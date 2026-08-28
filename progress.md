# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- `main` is at [PR #16](https://github.com/beagle1903/fund-helper/pull/16). Pull requests and pushes to `main` run `:app:testDebugUnitTest` on GitHub Actions (no live TEFAS, no emulator). The merge check was green.
- Sideloaded on the A23 (`com.burha.fundhelper`). Do **not** uninstall.

## Next

- On the phone: open the app, tap a followed fund while home is still refreshing. Confirm one TEFAS burst, not two. Pull-to-refresh still works.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

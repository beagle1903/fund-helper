# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- `main` is at [PR #15](https://github.com/beagle1903/fund-helper/pull/15) (refresh mutex + shared failed-refresh cache). Sideloaded on the A23 (`com.burha.fundhelper`). Do **not** uninstall.
- Branch `feat/ci-unit-tests`: GitHub Actions runs `:app:testDebugUnitTest` on pull requests and on pushes to `main`. No live TEFAS. No emulator.

## Next

- Confirm the CI check appears on the PR.
- On the phone: open the app, tap a followed fund while home is still refreshing. Confirm one TEFAS burst, not two.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

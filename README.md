# fund-helper

Local follow-only watchlist of Turkish TEFAS yatırım fonları. Search by code or name, follow or unfollow, and see latest price, returns, type, risk, and fees. Fund detail explains official TEFAS fields in short Turkish.

It is not a chat consultant, broker, or investment adviser. **Yatırım tavsiyesi değildir.**

## Status

Personal debug APK on a Samsung Galaxy A23 first. Play Store is later. Follows and snapshots stay on the phone (Room). No accounts or backend in v1.

## Stack

Kotlin, Jetpack Compose, Hilt, Room. UI talks only to `FundRepository`, which uses Room and a swappable `TefasClient` (`tefas.gov.tr` JSON).

## Build

Needs JDK 17+ and the Android SDK (`compileSdk` / `targetSdk` 36, `minSdk` 26). From the repo root:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Package: `com.burha.fundhelper`

## Sideload (A23)

See [`.cursor/skills/sideload-a23/SKILL.md`](.cursor/skills/sideload-a23/SKILL.md): USB debugging, then `.\gradlew.bat :app:installDebug` (replaces the debug install, keeps Room). Do not uninstall.

## CI

Pull requests and pushes to `main` run `:app:testDebugUnitTest` on GitHub Actions. No live TEFAS. No emulator.

## Docs

| File | Role |
|------|------|
| [`docs/context.md`](docs/context.md) | Product one-pager |
| [`docs/architecture.md`](docs/architecture.md) | Technical map |
| [`docs/decisions.md`](docs/decisions.md) | Architecture decisions |
| [`docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md`](docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md) | Frozen v1 design |
| [`docs/superpowers/plans/2026-08-22-fund-helper-v1.md`](docs/superpowers/plans/2026-08-22-fund-helper-v1.md) | Implementation plan |
| [`progress.md`](progress.md) | Short session handoff |

## License / advice

Informational only. Not investment advice. **Yatırım tavsiyesi değildir.**

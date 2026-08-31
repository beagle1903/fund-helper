# fund-helper

Local follow-only watchlist of Turkish TEFAS yatırım fonları. Search by code or name, follow or unfollow, and see latest price, returns, type, risk, and fees. Fund detail explains official TEFAS fields in short Turkish.

It is not a chat consultant, broker, or investment adviser. **Informational only — not investment advice.**

## Status

Personal debug APK on a Samsung Galaxy A23 first. Play Store is later. Follows and snapshots stay on the phone (Room). No accounts or backend in v1.

## Wiki pages

- [Product](Product) — what v1 does and does not do
- [Architecture](Architecture) — stack, boundaries, data flow

## Screenshots

Wireframes of the v1 screens (sample TEFAS-style data, not live prices).

| Takip listesi | Boş liste |
| --- | --- |
| <img src="https://raw.githubusercontent.com/beagle1903/fund-helper/main/docs/screenshots/watchlist.png" alt="Takip listesi" width="240"> | <img src="https://raw.githubusercontent.com/beagle1903/fund-helper/main/docs/screenshots/watchlist-empty.png" alt="Boş takip listesi" width="240"> |

| Fon ara | Fon detayı |
| --- | --- |
| <img src="https://raw.githubusercontent.com/beagle1903/fund-helper/main/docs/screenshots/search.png" alt="Fon ara" width="240"> | <img src="https://raw.githubusercontent.com/beagle1903/fund-helper/main/docs/screenshots/detail.png" alt="Fon detayı" width="240"> |

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

USB debugging, then `.\gradlew.bat :app:installDebug` (replaces the debug install, keeps Room). **Do not uninstall.**

## CI

Pull requests and pushes to `main` run `:app:testDebugUnitTest` on GitHub Actions. No live TEFAS. No emulator.

## Docs in repo

| File | Role |
| --- | --- |
| [docs/context.md](https://github.com/beagle1903/fund-helper/blob/main/docs/context.md) | Product one-pager |
| [docs/architecture.md](https://github.com/beagle1903/fund-helper/blob/main/docs/architecture.md) | Technical map |
| [docs/decisions.md](https://github.com/beagle1903/fund-helper/blob/main/docs/decisions.md) | Architecture decisions |
| [Frozen v1 design](https://github.com/beagle1903/fund-helper/blob/main/docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md) | Frozen v1 spec |
| [Implementation plan](https://github.com/beagle1903/fund-helper/blob/main/docs/superpowers/plans/2026-08-22-fund-helper-v1.md) | v1 plan |
| [progress.md](https://github.com/beagle1903/fund-helper/blob/main/progress.md) | Short session handoff |

---

*Canonical version: [README.md](https://github.com/beagle1903/fund-helper/blob/main/README.md)*

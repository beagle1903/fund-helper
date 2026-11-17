# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- Günlük on `main`, sideloaded on the A23 (`:app:installDebug`, `com.burha.fundhelper`). Do **not** uninstall.
- PR #60 (open): AGP 9.5.1 / Gradle 9.6.0 + instrumented tests. `:app:connectedInstrumentedAndroidTest` passes 2/2 on the `Medium_Phone_API_37.0` AVD (SDK `D:nd-sdk`); tests install only `com.burha.fundhelper.test`. A23 smoke check on the AGP 9 build is still pending.
- Issue #26 QA passed on device: empty state (**Henüz olay yok.**); airplane refresh → **HATA** with network detail; snackbar + **Yeniden dene** unchanged; **Temizle**; process kill clears the session log.

## Next

- Contingent #27 only if TEFAS/Akamai blocks the phone.
- Play listing (#28) and store/SPK copy (#29) are `postponed` (no Play Console fee for now). #30 stays contingent later.

## Blockers

- Icon-refresh uninstall wiped the old follow list; that copy cannot be recovered.
- Expected later: TEFAS may challenge a mobile user-agent.

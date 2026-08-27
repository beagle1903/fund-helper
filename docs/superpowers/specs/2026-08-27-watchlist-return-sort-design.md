# Watchlist return sort + empty/search tap polish — Design Spec

**Date:** 2026-08-27
**Status:** Approved for implementation. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** The home list is `ORDER BY code`, so AFA sits above YAS for no product reason. Empty watchlist and the search **Ara** button are also small to scan/tap on the A23.

## Goal

Show followed funds in **past-month performance order**: largest loss at the top, largest gain at the bottom. Keep opening detail for extra periods. Make the empty watchlist and search submit easier to use. No new screens, no charts, no advice.

## Non-goals

- A sort menu (Kod / Ad / Getiri). Order is always this one.
- A second return period on the card.
- Persisting a sort preference (DataStore / SharedPreferences).
- Changing Room schema, TEFAS, follow-backup, or `FundRepository.observeWatchlist()` order.
- Charts, ranking language, “iyi/kötü” copy, Play listing.

## Approaches considered

1. **Overflow menu with Kod / Ad / Getiri.** Flexible; extra chrome the user did not ask for. Rejected.
2. **Always sort by the headline return already on the card, ascending.** Matches “highest negative first, highest positive last.” **Chosen.**
3. **Sort only by TEFAS `1M`, ignore the card fallback.** The number on the card would then disagree with list order when `1M` is missing.

## Sort

Use the same **headline** value the card already shows (`ReturnKeys.headline`: `1M` first, then the existing fallback chain). Do not add extra periods to the row.

Order:

1. Numeric headline return **ascending** (most negative first, most positive last).
2. Missing `%` **last** (still shows `—`).
3. Equal returns: **code** ascending so the list is stable.

Apply in a pure domain helper from `WatchlistViewModel` after `observeWatchlist()`. Room can keep `ORDER BY code`. Repository tests that assert code order stay valid.

No ranking labels. Sign-colored `%` stays as today.

## Polish (C)

- **Empty watchlist:** center the existing title, add one supporting line, full-width **Fon ara**.
- **Search:** full-width **Ara** under the query field (A23 tap target). Query still submits on IME Search as well.

Sideload with `adb install -r` only.

## Testing

No live `tefas.gov.tr`. Unit-test the sorter: negative before zero before positive, missing last, code tiebreak, empty list. Do not regress `FundRepository` follow/search tests. Theme/layout has no required unit tests.

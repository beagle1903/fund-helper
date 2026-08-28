# Watchlist Pay/Kişi full-width line — Design Spec

**Date:** 2026-08-28
**Status:** Approved for implementation. Does not rewrite the frozen 2026-08-22 v1 spec or the 2026-08-28 pay-investor spec.
**Problem:** Watchlist cards put `Pay {signed%} · Kişi {signed%}` in the left column next to headline return and the star. That column is about half the card, so the line wraps after `Kişi` and the second `%` sits on its own row with empty space to the right.

## Goal

Show the existing Pay/Kişi string on **one full-width line** under the price / return row so both percentages stay together. Search cards, detail, data, and copy stay the same.

## Non-goals

- Two dedicated lines for Pay and Kişi (rejected this pass).
- Color, sort, or new fields for pay/kişi.
- Search cards gaining a pay/kişi line.
- Changing `FundRepository`, TEFAS, Room, or follow-backup.
- Rewriting frozen specs. Living `docs/architecture.md` may note the full-width placement during implementation.

## Approaches considered

1. **Full-width single line under the top row.** Uses the empty space; typically one line on the A23. **Chosen.**
2. **Two dedicated lines** (`Pay …` then `Kişi …`), also full width. More scan-friendly, taller cards. Not this pass.
3. **Give the left column more width** (drop trailing `weight`). Still shares the row with return + star; large `%` can wrap mid-metric. Rejected.

## Layout

`FundRowCard` keeps the top row: **code**, **name**, and (on watchlist) **price** on the left; **trailing** (headline return + star) on the right.

Add an optional **full-width slot under that row**. Watchlist uses it for:

1. `Pay {signed%} · Kişi {signed%}` — same `R.string.watchlist_pay_kisi`, `bodySmall`, `onSurfaceVariant`. Missing `%` → `—`. Do not color these values.
2. Fetched-at, unchanged style, under that line.

Search does not pass the slot. Do not ellipsis or force `maxLines = 1` on Pay/Kişi; if a pathological value still wraps, it wraps on the full card width, not beside the star.

Sideload with `:app:installDebug` / `adb install -r` only. Do not uninstall.

## Testing

- No live TEFAS. No required unit tests for this layout. Do not regress follow / search / pay-investor tests.
- Manual on A23: followed card shows Pay and Kişi on one line under price/return; fetched-at below that; search card unchanged.

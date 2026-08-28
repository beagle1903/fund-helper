# Pay adedi + yatırımcı change, drop disclaimer — Design Spec

**Date:** 2026-08-28
**Status:** Approved for implementation. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** Watchlist cards show price and headline return only. Day-to-day change in shares outstanding and investor count is already in the price payload and unused. The on-screen disclaimer is no longer wanted.

## Goal

Show **pay adedi** and **yatırımcı sayısı** day-over-day signed % on watchlist cards, and the latest totals plus those % on fund detail. Derive both from the existing 7-day `fonGnlBlgSiraliGetir` window. Remove **Yatırım tavsiyesi değildir.** from the app and from living product rules. No new screens, no extra TEFAS call, no advice copy.

## Non-goals

- Search cards (catalog has no pay/kişi fields).
- Fon büyüklüğü / AUM / işlem hacmi.
- Green/red color on these % (returns stay sign-colored).
- Sorting the watchlist by pay or kişi.
- Charts, holdings, extra HTTP, or a new endpoint.
- Rewriting the frozen 2026-08-22 spec file.
- Play listing / store disclaimer (later).

## Approaches considered

1. **Parse latest + previous priced day from the existing price window.** Persist four counts on the snapshot. **Chosen.**
2. **Diff against the last Room snapshot.** “Recent” becomes last-open, not last TEFAS day. Rejected.
3. **Per-fund extra HTTP.** Breaks the refresh budget. Rejected.

## Data

Wire names live only in `TefasJsonMapper` (2026 `fonGnlBlgSiraliGetir` `resultList` rows):

| JSON | Meaning |
|------|---------|
| `tedPaySayisi` | Tedavüldeki pay adedi |
| `kisiSayisi` | Yatırımcı / kişi sayısı |

If a live payload uses a synonym, change only the mapper. Do not add a third TEFAS URL. Do not call `dagilimSiraliGetirT` or `fonFiyatBilgiGetir`.

`parseLatestPrices` still skips rows with missing/`≤ 0` `fiyat`. Per code:

1. **Latest** = max `tarih` among remaining rows (same as today: price + `priceDate`).
2. **Previous** = next-latest priced day in that window, if any.

Read `tedPaySayisi` and `kisiSayisi` from those two rows. A missing or unparseable field is null, not a fetch failure.

`FundSnapshot` stores four nullable numbers: `payCount`, `prevPayCount`, `investorCount`, `prevInvestorCount`. Do not persist computed %.

Pure `percentChange(current, previous)`:

- null if either side is null
- null if `previous == 0`
- else `(current − previous) / previous * 100`

Current `0` is a valid total. Zero change is `0` (show `0%`, no `+`).

### Room

`snapshots` version **2**. Additive `ALTER TABLE` nullable columns for the four counts. `exportSchema` stays false. Do **not** use destructive migration (that would wipe follows). Existing rows: all four null until the next successful followed refresh. `FollowBackup` unchanged (codes only).

### Merge (`FundRepository`)

UI still talks only to `FundRepository`. Watchlist sort is still headline return.

- **Refresh succeeds and a price row exists for the code:** that row is source of truth for `price`, `priceDate`, and all four count fields, including nulls. Do not keep stale counts next to a new price date.
- **Refresh succeeds but that code has no price row:** keep previous snapshot price and counts (same pattern as today’s price fallback).
- **Refresh throws:** Room unchanged, including counts. Follows never wiped because TEFAS failed.
- **Search catalog upsert:** merge into an existing snapshot. Catalog may update name / type / risk / returns; it must not clear `price`, `priceDate`, or the four counts.
- **`followAll`:** same preserve-from-previous-snapshot rule for price and counts.

`observeWatchlist()` exposes `payChangePct` and `investorChangePct` via `percentChange`. Detail reads the four counts from the snapshot and uses the same helper.

## UI

### Watchlist card

Keep code, name, price, fetched-at, sign-colored headline return, star. Between price and fetched-at, one `onSurfaceVariant` line, both labels always:

`Pay {signed%} · Kişi {signed%}`

Missing % → `—`. Positive non-zero: prefix `+`. Zero → `0%` (no `+`). Format the number with existing `formatNumber` (Turkish decimal). Do **not** use `ReturnPercentText` for these values.

### Search card

Unchanged. No pay/kişi line.

### Detail

After the returns block, two labeled rows (outside the price/returns card):

| Label | Value |
|-------|--------|
| Pay adedi | Whole-number count (tr-TR grouping, no 4-decimal price format) + same signed % |
| Yatırımcı sayısı | Same pattern |

Missing total → `TEFAS kaydında bu bilgi yok.` (existing missing-field string). If a total exists but % does not, show the total and `—` for the %.

No AUM. Follow/unfollow button stays. Sideload with `adb install -r` only.

## Disclaimer

Remove **Yatırım tavsiyesi değildir.** from `DetailScreen` and `R.string.disclaimer`. `ExplanationMapper` still must not emit that sentence or buy/sell language.

Update living docs in the same implementation: `docs/context.md`, `docs/architecture.md`, `docs/decisions.md` (amend ADR 005; add ADR 007: on-screen disclaimer removed), `README.md`, `progress.md`. Do not edit `docs/superpowers/specs/2026-08-22-fund-helper-v1-design.md`.

The app remains informational and must not recommend buy/sell/hold. Store-facing disclaimer is a later Play task, not this pass.

## Testing

No live `tefas.gov.tr`.

- Mapper: two priced days → four counts; one priced day → current counts, null previous; `fiyat ≤ 0` skipped; unknown JSON keys ignored.
- `percentChange`: normal ratio; previous `0` / null → null; `0` current with nonzero previous is a number.
- Repository: price row overwrites counts; failed refresh keeps Room; search merge does not wipe price/pay/kişi.
- Room v2 migration keeps follow rows.
- Do not regress follow / search / sort tests.
- `R.string.disclaimer` is deleted; detail does not show that sentence.

Manual on A23: followed card shows both %; detail shows totals + %; no disclaimer; search card unchanged; airplane mode keeps the list.

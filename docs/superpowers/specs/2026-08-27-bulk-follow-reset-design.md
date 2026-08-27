# Bulk follow + RESET — Design Spec

**Date:** 2026-08-27
**Status:** Approved for implementation. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** Following is one star tap per fund. Pasting a list of codes into search should append them. A typed `RESET` should empty the list, including the Downloads backup.

## Goal

Reuse the search box and **Ara** (and IME Search). After trim:

1. Exact `RESET` (any case, no comma) clears all follows and the Downloads backup, then pops to the watchlist.
2. A comma in the query means bulk follow: split codes, append those that exist in the YAT catalog, skip the rest silently, then pop to the watchlist.
3. No comma → today’s search (results on screen, star to follow, stay on search).

## Non-goals

- A second paste field, overflow “Listeyi temizle”, or confirm dialogs.
- Replacing the list from a comma paste (`RESET` inside a list is not a wipe).
- Name matching or prefix matching in bulk follow (codes only, exact).
- Snackbars for skipped codes or catalog failure on bulk follow.
- Changing watchlist sort, Room schema, TEFAS endpoints, or single-fund follow/unfollow.
- Charts, advice, Play listing.

## Approaches considered

1. **Magic search box.** **Ara** classifies `RESET` / comma list / text search. **Chosen.**
2. **Search stays search; RESET on the watchlist.** Safer wipe; extra chrome; not typed into search. Rejected.
3. **Preview then “Tümünü takip et”.** Safer; two taps. Rejected.

## Classification (`parseSearchCommand`)

Pure domain helper. Input is the raw query; trim first.

| Trimmed query | Result |
|---------------|--------|
| empty | text search (existing empty-query hint, stay on search) |
| `RESET` / `reset` / mixed case, **no comma** | `Reset` |
| contains `,` | `BulkFollow(codes)` |
| anything else | `TextSearch` (current `FundRepository.search`) |

Comma split: trim each token, drop empties, drop a token that equals `RESET` (any case), keep unique codes (first wins). Codes are matched later against the catalog, not here.

`RESET,` and `RESET, AAK` are bulk follow, not reset. Only the whole trimmed query equal to `RESET` wipes.

## Bulk follow

- Tokens are **fund codes**, not names. Match catalog `code` **exactly**, case-insensitive. Use the catalog’s canonical code when inserting.
- Follow each resolved code (already-followed stays followed). Upsert those snapshots so watchlist cards are not blank. One Downloads backup write.
- Unknown codes, duplicates, and `RESET` tokens: skip, no message.
- Catalog/TEFAS failure: resolve zero codes, add nothing, do not wipe, still pop to the watchlist. No search error snackbar.
- Watchlist order unchanged (headline return, worst first).

## RESET

- No TEFAS call.
- Delete every Room follow, then write an **empty** Downloads backup (one write). Snapshots may remain; they are cache.
- No confirm. Pop to the empty watchlist.
- `restoreFollowsIfNeeded` stays as-is (empty Room + empty backup → no restore). If the empty backup write fails, Room stays empty this session; a later launch may restore from a stale file (existing backup-miss behavior). Do not re-insert follows when the write fails.

## UI

No new screen. `SearchViewModel.submit` branches on `parseSearchCommand`. After `Reset` or `BulkFollow`, pop back to the watchlist. **Ara** label unchanged. Sideload with `adb install -r` only.

## Testing

No live `tefas.gov.tr`.

- Parser: `RESET` / `reset`; comma split; `RESET` in a list dropped; `RESET,` is not a wipe; no comma stays text search.
- Repository: `followAll` appends and writes backup once; unknown codes skipped; `clearFollows` empties Room and the backup file.
- Submit: comma or `RESET` pops to the watchlist; a single code still shows results.

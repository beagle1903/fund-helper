# Turkish search fold — Design Spec

**Date:** 2026-08-27
**Status:** Approved. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** Search lowercases with `tr-TR`, so `I`→`ı` and `İ`→`i` stay different letters. Typing `yatirim` on a typical keyboard misses a fund named `Yatırım`.

## Goal

Search by code or name treats Turkish letters as equivalent to their ASCII pair **in both directions**. Official TEFAS spelling on screen does not change. No new screens, no charts, no advice.

## Non-goals

- Changing watchlist, detail, Room snapshots, or TEFAS payloads.
- Folding any letters beyond the map below (including treating `â`/`î`/`û` as extra Turkish letters).
- Fuzzy / typo search, word-order search, or search-as-you-type.
- Locale display changes.

## Approaches considered

1. **Keep `lowercase(tr)` only.** Correct for Turkish casing; fails for `i`/`ı` search. Rejected.
2. **Fold the i-family only.** Fixes the reported miss; `ş`/`ğ`/`ü`/`ö`/`ç` still fail the same way.
3. **Fold both query and catalog text with one map.** Bidirectional. **Chosen.**

## Matching (unchanged except fold)

Trim the query. Empty / whitespace → no catalog call, existing empty-query copy. Else a fund matches if folded **code starts with** the folded query **or** folded **name contains** the folded query. YAT-only. Submit on Ara / IME, not per keystroke.

Apply the same fold to the query, the fund code, and the fund name. That is the vice versa rule: `i` finds `ı` and `ı` finds `i`; `s` finds `ş` and `ş` finds `s`.

## Fold map

After mapping, lowercase with `Locale.ROOT` (not `tr-TR`) so Turkish `I`/`İ` cannot split again.

| Input | Folded to |
|---|---|
| `i` `ı` `I` `İ` | `i` |
| `ş` `Ş` | `s` |
| `ğ` `Ğ` | `g` |
| `ü` `Ü` | `u` |
| `ö` `Ö` | `o` |
| `ç` `Ç` | `c` |

Circumflex marks `â` `î` `û` (and capitals) are **not** Turkish letters. They are pronunciation marks. Strip them to the base vowel (`a` `i` `u`) so they cannot block a match. Do not advertise them as part of the letter map.

Examples: `yatirim` matches `Yatırım`; `Yatırım` as query matches a name that used ASCII `i`; `degisken` matches `Değişken`; `AAK` still matches code `AAK`. A folded query must not match an unrelated fund.

## Architecture

- Pure `foldForSearch` in `domain/` (no Compose, Room, or HTTP).
- `FundRepository.matchesQuery` folds query, code, and name, then existing `startsWith` / `contains`.
- UI still talks only to `FundRepository`. Cards keep official names.
- Network / snackbar / follow behavior unchanged.

## Testing

No live `tefas.gov.tr`. Unit-test the folder and add `FundRepository.search` cases for the examples above plus empty query and an unrelated non-match. Sideload with `adb install -r` only.

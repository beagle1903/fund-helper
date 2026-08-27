# UI polish — Design Spec

**Date:** 2026-08-27
**Status:** Approved. Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** Watchlist, search, and detail work, but they use default Material 3 purple and unstyled stacked `Text`. The app feels dull next to the teal launcher icon.

## Goal

Make the three existing screens easier to scan and visually tied to the launcher (teal `#038984`) without adding features, charts, advice, or new navigation. First user is still the A23 sideload.

## Non-goals

- Charts, sparklines, or new screens
- Buy/sell language, ranking, or “iyi/kötü” copy on returns
- English as the default UI
- Dynamic Color / wallpaper theming (would fight the icon on the A23)
- Changing `FundRepository`, TEFAS, Room, or follow-backup behavior
- Play Store listing work

## Approaches considered

1. **Theme tokens only.** Swap `FundHelperTheme` to teal; leave stacked `Text`. Fast; still reads as a prototype.
2. **Theme + Material list/detail structure.** Teal `ColorScheme`, cards / list items, typography roles, signed return color. Same three screens. **Chosen.**
3. **Custom visual language.** Gold / coral / mint chips and non-Material layouts. Rejected as too loud for this pass.

Color strength: **balanced** (not quiet-white-only, not icon-rainbow). Surfaces stay mostly white / dark gray; teal is primary (top bar, buttons, followed star, focus). Return `%` uses green/red as the sign of the number only.

## Design

- Light and dark follow the system. No in-app theme toggle in this pass.
- `FundHelperTheme` uses explicit light/dark schemes seeded from launcher teal `#038984`, not `lightColorScheme()` / `darkColorScheme()` defaults.
- Watchlist and search rows become `Card`s: fund **code** as title, **name** as supporting text, price and headline return trailing. Missing price/return still show `—`.
- Headline return on the watchlist stays the existing period the ViewModel already picks; do not add extra periods to the row.
- Return values that are numbers: positive → green, negative → red, missing → on-surface variant. No extra words around the sign.
- Detail: header (code, name, price, price date, fetched-at), then a returns block, then labeled type / risk / fees, then the existing explanation mapper text, then **Yatırım tavsiyesi değildir.** as visible body text (not overflow-only). Follow/unfollow remains a filled button.
- Shared small composables are allowed (`ReturnPercentText`, a fund row card) so the three screens stay consistent. They live under `ui/` and still take data from ViewModels only.
- Empty, error, snackbar, follow, and disclaimer copy stay the existing Turkish strings. Add short labels only for detail rows that are currently unlabeled (type, risk, fees) so the new hierarchy is readable.
- Sideload with `adb install -r` only. Do not uninstall to preview the theme.

## Testing

- No live TEFAS in unit tests.
- Theme/layout has no required unit tests. Do not regress follow/search/detail ViewModel tests.
- Manual on A23: empty watchlist, a followed row (positive and negative return if available), search card, detail disclaimer still on screen in light and dark.

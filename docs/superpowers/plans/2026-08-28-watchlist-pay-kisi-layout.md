# Watchlist Pay/Kişi full-width line Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Put the existing watchlist `Pay … · Kişi …` string on a full-width row under price/return so it no longer wraps after `Kişi`.

**Architecture:** Add an optional `below` slot to `FundRowCard` under the code/name/price + trailing row. Watchlist moves Pay/Kişi and fetched-at into that slot; price stays in `supporting`. Search does not pass `below`. No repository, Room, or TEFAS changes.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 `Card` / `Column` / `Row` (`Modifier.weight` only inside the top `Row`). Fetch Compose layout APIs via Context7 (`/websites/developer_android_develop_ui_compose`) while implementing. Sideload with `:app:installDebug` only.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-28-watchlist-pay-kisi-layout-design.md`. Frozen 2026-08-22 and 2026-08-28 pay-investor spec files are not edited.
- `applicationId` / namespace: `com.burha.fundhelper`. Default UI language: Turkish.
- UI → ViewModels → `FundRepository` only. Do not change Room, TEFAS, or follow-backup.
- Same string `R.string.watchlist_pay_kisi`. No color on pay/kişi %. No two-line Pay/Kişi layout.
- Do not pass `below` from search. Do not set `maxLines` or ellipsis on the Pay/Kişi `Text`.
- No live `tefas.gov.tr` in unit tests. Do not uninstall the A23 app (`.\gradlew.bat :app:installDebug` / `adb install -r`).
- No required new unit tests for this layout. Do not regress existing `:app:testDebugUnitTest`.

## File structure

| Path | Responsibility |
|------|----------------|
| Modify `app/src/main/java/com/burha/fundhelper/ui/FundRowCard.kt` | Optional full-width `below` under the top row |
| Modify `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` | Price in `supporting`; Pay/Kişi + fetched-at in `below` |
| Modify `docs/architecture.md` | Full-width placement note |
| Modify `progress.md` | A23 handoff after sideload |

Do not change `SearchScreen.kt`. Do not edit frozen specs under `docs/superpowers/specs/` except as already landed for this assignment.

---

### Task 1: Full-width Pay/Kişi slot

**Files:**
- Modify: `app/src/main/java/com/burha/fundhelper/ui/FundRowCard.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` (`WatchlistRowItem`)
- Modify: `docs/architecture.md` (Screens bullet)
- Modify: `progress.md` (after sideload)

**Interfaces:**
- Consumes: existing `FundRowCard(code, name, onClick, modifier, supporting, trailing)` and watchlist `R.string.watchlist_pay_kisi` / `formatSignedPercent` / `formatFetchedAt`
- Produces: `FundRowCard(..., supporting: (@Composable () -> Unit)? = null, below: (@Composable () -> Unit)? = null, trailing: @Composable () -> Unit)`

- [ ] **Step 1: Confirm Compose slot layout**

Query Context7 `/websites/developer_android_develop_ui_compose` for `Column` wrapping a `Row` with `Modifier.weight`, then content after the `Row`. Keep `weight` only on children of that `Row`.

- [ ] **Step 2: Add `below` to `FundRowCard`**

Replace `FundRowCard` with this (imports already include `Column`, `Row`, `fillMaxWidth`, `padding`):

```kotlin
@Composable
fun FundRowCard(
    code: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(code, style = MaterialTheme.typography.titleMedium)
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    supporting?.invoke()
                }
                Box(Modifier.weight(1f, fill = false)) {
                    trailing()
                }
            }
            below?.invoke()
        }
    }
}
```

Search still compiles: it does not pass `below`, default `null`.

- [ ] **Step 3: Move Pay/Kişi and fetched-at into `below`**

In `WatchlistRowItem`, keep price in `supporting`. Put the Pay/Kişi line and fetched-at in `below`. Do **not** add `maxLines` or `overflow` on the Pay/Kişi `Text`.

```kotlin
    FundRowCard(
        code = row.code,
        name = row.name ?: dash,
        onClick = { onOpen(row.code) },
        supporting = {
            Text(
                row.price?.let(::formatNumber) ?: dash,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        below = {
            Text(
                stringResource(
                    R.string.watchlist_pay_kisi,
                    formatSignedPercent(row.payChangePct, dash),
                    formatSignedPercent(row.investorChangePct, dash),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            row.fetchedAt?.let {
                Text(
                    formatFetchedAt(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ReturnPercentText(
                    value = row.headlineReturn,
                    leading = period,
                )
                IconButton(onClick = onUnfollow) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(R.string.unfollow),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    )
```

- [ ] **Step 4: Run unit tests (regression)**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests PASS. No live TEFAS.

- [ ] **Step 5: Update living docs**

In `docs/architecture.md`, in the **Screens** bullet, replace the sentence that currently ends with "line (day-over-day % only)." so it reads:

Watchlist cards show an uncolored `Pay … · Kişi …` line on a full-width row under price/return (day-over-day % only).

Do not rewrite the rest of that bullet. Do not edit frozen specs.

- [ ] **Step 6: Sideload on the A23**

Follow `.cursor/skills/sideload-a23/SKILL.md`. Do not uninstall.

```powershell
adb devices
.\gradlew.bat :app:installDebug
adb shell pm path com.burha.fundhelper
```

Expected: A23 is `device`; install succeeds; `package:` path prints. Manual: followed card shows `Pay … · Kişi …` on one line under price/return; fetched-at under that; search card unchanged.

If no device, skip install, write that in `progress.md`, and continue with Step 7. Do not uninstall to retry.

- [ ] **Step 7: Prune `progress.md`**

Replace Current status / Next with a short handoff: layout is in the tree; whether `:app:installDebug` ran; A23 check (one-line Pay/Kişi, search unchanged). Keep blockers. This is not a diary.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/burha/fundhelper/ui/FundRowCard.kt app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt docs/architecture.md progress.md
git commit -m "fix: put watchlist Pay/Kişi on a full-width row"
```

- [ ] **Step 9: Open the implementation pull request**

ADR 008: commit and open a PR at the end of the implementation phase. Do not leave this phase only in the working tree.

```powershell
git push -u origin HEAD
gh pr create --title "fix: put watchlist Pay/Kişi on a full-width row" --body "$(cat <<'EOF'
## Summary
- Watchlist cards put Pay/Kişi on a full-width row under price/return.
- Search cards unchanged.

## Test plan
- [ ] ``.\gradlew.bat :app:testDebugUnitTest``
- [ ] Sideload with ``.\gradlew.bat :app:installDebug`` (do not uninstall) when the A23 is connected
- [ ] Followed card: Pay and Kişi on one line under price/return; fetched-at below that
- [ ] Search card unchanged
EOF
)"
```

On Windows PowerShell, use a here-string for `--body` if `cat <<'EOF'` is unavailable. Return the PR URL.

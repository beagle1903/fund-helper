# Agent Instructions

You are an AI coding agent assigned to build and maintain `fund-helper`, a local Android watchlist for Turkish TEFAS yatırım fonları.

Follow this progressive disclosure map for context:
1. Product one-pager: read `docs/context.md`.
2. Technical map: read `docs/architecture.md`.
3. Past decisions: read `docs/decisions.md`.
4. Current state: read `progress.md`.
5. Current assignment: read the latest files under `docs/superpowers/specs/` and `docs/superpowers/plans/` if they exist.

Session rules:
- Do not write implementation details in this file. Keep it as a short map/router.
- When finishing a session, update `progress.md` with a pruned handoff (what works on the A23, blockers). Do not let it become a diary.
- When writing Compose, Hilt, Room, OkHttp/Ktor, or Play Gradle DSL, fetch current docs via Context7. Do not guess APIs from training data.
- UI talks only to `FundRepository`. If TEFAS is unreachable, swap `TefasClient` only.
- Never give buy/sell advice. Turkish explanations map official fields only. Include “yatırım tavsiyesi değildir”.

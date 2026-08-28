# Agent Instructions

You are an AI coding agent assigned to build and maintain `fund-helper`, a local Android watchlist for Turkish TEFAS yatırım fonları.

Follow this progressive disclosure map for context:
1. Product one-pager: read `docs/context.md`.
2. Technical map: read `docs/architecture.md`.
3. Past decisions: read `docs/decisions.md`.
4. Current state: read `progress.md`.
5. Current assignment: read the latest files under `docs/superpowers/specs/` and `docs/superpowers/plans/` if they exist.

Durable facts (also in `docs/context.md`):
1. v1 is a follow-only TEFAS watchlist, not a chat consultant.
2. No buy/sell advice; Turkish explanations map official fields only.
3. Local-only; no backend or accounts in v1.
4. Sideload APK on Samsung A23 first; Play Store is later.
5. UI talks only to `FundRepository`; swap `TefasClient` if the phone cannot reach TEFAS.

Session rules:
- Do not write implementation details in this file. Keep it as a short map/router.
- When finishing a session, update `progress.md` with a pruned handoff (what works on the A23, blockers). Do not let it become a diary.
- When writing Compose, Hilt, Room, OkHttp/Ktor, or Play Gradle DSL, fetch current docs via Context7. Do not guess APIs from training data.
- Prefer subagents for execution (subagent-driven development). Do not default to inline.
- At the end of each phase — spec, plan, or implementation — create a pull request. Do not leave that phase only in the working tree. See ADR 008.

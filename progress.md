# Progress Notes

*Session-to-session handoff. Prune hard; this is not a diary.*

## Current status

- 2026-08-22: memory hierarchy is in the repo. No Android/Gradle code yet.
  - Router: `AGENTS.md`
  - Living docs: `docs/context.md`, `docs/architecture.md`, `docs/decisions.md`
  - Rules: `.cursor/rules/kotlin-compose.mdc`, `.cursor/rules/tefas-and-copy.mdc`
  - Indexing: `.cursorignore` (plus Android `.gitignore`)
  - Sideload skill: `.cursor/skills/sideload-a23/SKILL.md`
  - Spec and bite-sized impl plan are **not written yet** (`docs/superpowers/` is empty until those todos run).
- A23: nothing installed. TEFAS-from-phone / Akamai is untested.
- Git: workspace was not a repository when the harness was added; commit only if asked.
- Cursor Memories: native Memories UI/tool is not available in this Cursor version. The five backup facts are already in `AGENTS.md` / `docs/context.md` / `docs/architecture.md`. Do not add them as user-global rules.

## Next

1. Write the frozen v1 spec under `docs/superpowers/specs/`.
2. Write the bite-sized TDD plan under `docs/superpowers/plans/`.
3. Then: Gradle skeleton → tested domain/TEFAS/repository → three screens → debug APK for the A23.

## Blockers

- None for harness. Expected later: TEFAS may challenge a mobile user-agent.

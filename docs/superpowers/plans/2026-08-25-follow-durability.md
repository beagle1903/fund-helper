# Follow durability Implementation Plan

> **For agentic workers:** Implement inline in this session. TDD for codec and repository backup/restore. Do not hit live TEFAS.

**Goal:** Mirror followed fund codes to a Downloads file and restore them through `FundRepository` when Room is empty, plus `hasFragileUserData` for Settings uninstall.

**Architecture:** Room stays source of truth. `FollowBackupCodec` is a pure mapper. `FollowBackup` is injected into `FundRepository`. Production implementation uses MediaStore Downloads. UI still talks only to `FundRepository`.

**Tech Stack:** Kotlin, Room, Hilt, MediaStore (API 29+), kotlinx.serialization JSON, JUnit 4 + coroutines-test.

## Global Constraints

- `applicationId` / namespace: `com.burha.fundhelper`. No new dangerous permissions.
- UI → ViewModels → `FundRepository` only. No live `tefas.gov.tr` in unit tests.
- Restore only when Room follow set is empty. Backup failures must not wipe Room.
- No new screens. No commit until the task is complete.

## File structure

| Path | Responsibility |
|------|----------------|
| `app/src/main/java/com/burha/fundhelper/data/FollowBackupCodec.kt` | Encode/decode follow-code JSON |
| `app/src/main/java/com/burha/fundhelper/data/FollowBackup.kt` | `writeCodes` / `readCodes` |
| `app/src/main/java/com/burha/fundhelper/data/MediaStoreFollowBackup.kt` | Downloads file via MediaStore |
| `app/src/test/java/com/burha/fundhelper/data/FollowBackupCodecTest.kt` | Codec unit tests |
| `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowBackup.kt` | In-memory backup |
| Modify `FundRepository.kt`, `FundRepositoryTest.kt`, `AppModule.kt`, `WatchlistViewModel.kt`, `AndroidManifest.xml` |
| Living docs: `docs/architecture.md`, `docs/decisions.md`, `progress.md` |

### Task 1: Codec

- [ ] Failing `FollowBackupCodecTest`
- [ ] Implement `FollowBackupCodec`
- [ ] Tests pass

### Task 2: Repository backup/restore

- [ ] Failing `FundRepository` tests with `FakeFollowBackup`
- [ ] Wire `FollowBackup` into `FundRepository`
- [ ] Tests pass

### Task 3: Device wiring

- [ ] `MediaStoreFollowBackup` + Hilt + `hasFragileUserData` + watchlist restore-on-start
- [ ] Full unit tests + docs

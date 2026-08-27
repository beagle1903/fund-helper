# Follow durability — Design Spec

**Date:** 2026-08-25
**Status:** Approved for implementation (sideload v1 amendment). Does not rewrite the frozen 2026-08-22 v1 spec.
**Problem:** Followed funds disappeared after an uninstall/reinstall used to refresh the launcher icon.

## Facts (already true)

- Room follows live in the app private sandbox (`/data/data/com.burha.fundhelper/...`).
- Uninstall and clear-data delete that sandbox. Process death and `adb install -r` do not.
- The icon refresh used uninstall first, then `adb install -r`. The reinstall flag does not wipe data, but the uninstall already had. The list cannot be recovered from Room after that.
- `allowBackup` is already `true`. Auto Backup may restore later if a Google/Samsung backup actually ran; it is not a file on the phone and did not save this incident.

## Goal

Followed **fund codes** should survive on this phone across APK updates and, as far as v1 allows without a server, across uninstall/reinstall. Snapshots can be refetched from TEFAS. No accounts, no advice features, no new screens.

## Non-goals

- Recovering the list that was already wiped (no copy existed outside the sandbox).
- Cloud sync, login, or a backend.
- Import/export UI, SAF folder picker, or all-files access.
- Extra dangerous permissions (frozen v1 is `INTERNET` only). MediaStore insert for an app-owned Downloads file does not add one.
- Making `adb uninstall` guaranteed (the system is designed to wipe; we only keep a best-effort on-disk copy).

## Approaches considered

1. **Document `adb install -r` only.** Correct for APK updates; does not restore after uninstall. Insufficient alone.
2. **Auto Backup only.** Already enabled. Off-device, delayed, easy to miss on a sideload phone.
3. **`android:hasFragileUserData`.** Settings uninstall can offer to keep `/data/data`. `adb uninstall` ignores it.
4. **Mirror codes to shared storage; restore via `FundRepository`.** On-machine. Write needs no extra permission on API 29+. After reinstall, a new install may not own the leftover file on API 33+, so restore is best-effort.

**Choice:** (4) + (3) + keep (2). Smallest mix that is local, on-machine, and v1-shaped. Prefer `adb install -r` for icon updates.

## Design

- Room remains the live follow set. UI still talks only to `FundRepository`.
- `FollowBackupCodec` encodes/decodes `{"version":1,"codes":["AAK",...]}` (pure; unit-tested). Blank/duplicate codes dropped; stable sort. Bad JSON → empty list, never crash.
- `FollowBackup` writes/reads that payload. Production: MediaStore Downloads, display name `com.burha.fundhelper-follows.json`. Tests: in-memory fake. Failures are swallowed; Room is not rolled back.
- `FundRepository.follow` / `unfollow` persist Room then mirror codes. `restoreFollowsIfNeeded()` copies backup codes into Room **only when Room is empty** (do not re-follow funds the user unfollowed). Watchlist init and `refreshFollowed` call restore first.
- Manifest: `android:hasFragileUserData="true"`. Keep `allowBackup="true"`.
- No new Compose chrome. No live TEFAS in unit tests.

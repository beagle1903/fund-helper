---
name: sideload-a23
description: Assemble the debug APK and sideload it onto the Samsung Galaxy A23 over USB. Use when assembling, installing, putting the app on the phone, running adb install, or verifying the fund-helper package on device.
---

# Sideload fund-helper onto the A23

No helper scripts in v1. Run these from the repo root in PowerShell.

## 1. Build

```powershell
.\gradlew.bat assembleDebug
```

APK path:

`app/build/outputs/apk/debug/app-debug.apk`

## 2. USB debugging on the A23

1. Settings → About phone → tap **Build number** seven times.
2. Settings → Developer options → enable **USB debugging**.
3. Plug in USB. On the phone, allow this computer if prompted.

```powershell
adb devices
```

Expect the A23 listed as `device`, not `unauthorized`.

## 3. Install (replace existing debug build)

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 4. Confirm package

Intended `applicationId` is `com.burha.fundhelper` (see `docs/architecture.md`). After install:

```powershell
adb shell pm path com.burha.fundhelper
```

A `package:` path means the build is on the phone. If Gradle used a different `applicationId`, read it from `app/build.gradle.kts` and query that instead.

## Notes

- Debug only for v1. Do not `bundleRelease` or upload to Play from this skill.
- If `adb` is missing, use Android SDK `platform-tools` and ensure it is on `PATH`.

---
name: sideload-a23
description: Assemble the debug APK and sideload it onto the Samsung Galaxy A23 over USB. Use when assembling, installing, sideloading, targeting the A23, putting the app on the phone, running adb install, installDebug, or verifying the fund-helper package on device.
---

# Sideload fund-helper onto the A23

No helper scripts in v1. One Gradle task builds and replaces the debug install. Do **not** uninstall (that wipes Room).

## 1. USB debugging on the A23

1. Settings → About phone → tap **Build number** seven times.
2. Settings → Developer options → enable **USB debugging**.
3. Plug in USB. On the phone, allow this computer if prompted.

```powershell
adb devices
```

If `adb` is not on `PATH`, use `C:\Users\burha\AppData\Local\Android\Sdk\platform-tools\adb.exe`.

Expect the A23 listed as `device`, not `unauthorized`. If none, stop. If more than one device, set `ANDROID_SERIAL` to the A23.

## 2. Build and install

From the repo root:

```powershell
.\gradlew.bat :app:installDebug
```

`:app:installDebug` is the Android Gradle Plugin debug-variant task. It builds the same debug APK as `assembleDebug`, then installs with the SDK `adb` from `sdk.dir` in `local.properties` (same as `adb install -r`: replace, no uninstall). Room and the follow list stay.

It does not launch the app. It fails if no phone is connected.

## 3. Confirm package

Intended `applicationId` is `com.burha.fundhelper` (see `docs/architecture.md`). After install:

```powershell
adb shell pm path com.burha.fundhelper
```

A `package:` path means the build is on the phone. If Gradle used a different `applicationId`, read it from `app/build.gradle.kts` and query that instead.

## Notes

- Debug only for v1. Do not `bundleRelease` or upload to Play from this skill.
- APK path if you only need the file: `app/build/outputs/apk/debug/app-debug.apk`

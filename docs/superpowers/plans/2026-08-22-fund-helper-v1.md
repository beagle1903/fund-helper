# fund-helper v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a follow-only Kotlin Compose watchlist of TEFAS YAT funds (`com.burha.fundhelper`) that searches, follows, caches snapshots in Room, and sideloads as a debug APK onto a Samsung Galaxy A23.

**Architecture:** One Gradle application module. Compose screens and ViewModels talk only to `FundRepository`. The repository owns Room follows/snapshots, an in-memory YAT catalog, a 5-minute refresh timestamp, and `ExplanationMapper`. All HTTP lives in `OkHttpTefasClient` against `https://www.tefas.gov.tr/api/funds/...` JSON (never `BindHistory*`). If TEFAS blocks the phone, swap that client only.

**Tech Stack:** Kotlin 2.2, Jetpack Compose (BOM), Hilt, Room, OkHttp, kotlinx.serialization, JUnit 4 + coroutines-test. `minSdk 26`, `compileSdk`/`targetSdk` 36.

## Global Constraints

- `applicationId` / namespace: `com.burha.fundhelper`. Launcher label: `fund-helper`. `versionCode` 1, `versionName` `0.1.0`.
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`. Debug minify/shrink off. Permission: `INTERNET` only.
- Default UI locale is Turkish. All user-visible strings live in `app/src/main/res/values/strings.xml`. Do not ship English UI as the default.
- No buy/sell advice. `ExplanationMapper` maps official fields only. Detail always shows **Yatırım tavsiyesi değildir.** as visible body text. Inventing missing numbers or types is forbidden.
- Local-only: no login, accounts, backend, WorkManager, charts, holdings, LLM, Play listing, `bundleRelease`, or HTML scraping.
- HTTP: OkHttp only inside `TefasClient` / `OkHttpTefasClient` (and their tests). Desktop Chrome `User-Agent`. JSON under `https://www.tefas.gov.tr/api/funds/...`. Never `BindHistory*` or `fundturkey.com.tr/api/DB/BindHistory*`.
- Tests must not hit live `tefas.gov.tr`. Inject `FakeTefasClient`. Required coverage: mapper, DTO parsing fixtures, repository. Compose UI tests are out of v1.
- Refresh **followed funds only**. Search is on demand. Failure keeps follows and last snapshots. Snackbar + retry; never wipe the follow list.
- Fetch current Compose / Hilt / Room / OkHttp / Gradle DSL via Context7 while implementing. Do not guess APIs from training data.
- Sideload with `.cursor/skills/sideload-a23/SKILL.md`. Do not commit from the planning session that wrote this file; executing agents commit after each task below.
- Isolated worktree (if used at execution time) must come from `superpowers:using-git-worktrees`.

---

## File structure

Create these files. One Gradle `:app` module. No extra modules.

| Path | Responsibility |
|------|----------------|
| `settings.gradle.kts` | Root name `fund-helper`, include `:app`, Google/Maven repos |
| `build.gradle.kts` | Plugin aliases `apply false` |
| `gradle.properties` | AndroidX, JVM args |
| `gradle/libs.versions.toml` | AGP, Kotlin, Compose BOM, Hilt, Room, OkHttp, KSP |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.13 |
| `app/build.gradle.kts` | Application module, Compose, Hilt, Room, OkHttp, unit tests |
| `app/proguard-rules.pro` | Empty debug-safe rules file |
| `app/src/main/AndroidManifest.xml` | `INTERNET`, `@HiltAndroidApp` application, launcher activity |
| `app/src/main/res/values/strings.xml` | All Turkish UI copy |
| `app/src/main/res/values/themes.xml` | DayNight empty parent for the activity |
| `app/src/main/java/com/burha/fundhelper/FundHelperApp.kt` | `@HiltAndroidApp` |
| `app/src/main/java/com/burha/fundhelper/MainActivity.kt` | Single `ComponentActivity`, NavHost |
| `app/src/main/java/com/burha/fundhelper/di/AppModule.kt` | OkHttp, Room, DAO, `Clock` providers |
| `app/src/main/java/com/burha/fundhelper/di/TefasModule.kt` | `@Binds TefasClient` → `OkHttpTefasClient` |
| `app/src/main/java/com/burha/fundhelper/domain/FundSnapshot.kt` | Domain snapshot, `FeeLine`, `FundKind`, return-key helpers |
| `app/src/main/java/com/burha/fundhelper/domain/ExplanationMapper.kt` | Pure Turkish type/risk/fees sentences |
| `app/src/main/java/com/burha/fundhelper/domain/Clock.kt` | `nowMillis()` for `fetchedAt` and 5-minute throttle |
| `app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt` | Interface + URL constants + `TefasFetchException` |
| `app/src/main/java/com/burha/fundhelper/data/tefas/TefasJsonMapper.kt` | Fixture-testable JSON → domain (no OkHttp) |
| `app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt` | POST `/api/funds/...`, desktop Chrome UA |
| `app/src/main/java/com/burha/fundhelper/data/local/FollowEntity.kt` | Followed fund codes |
| `app/src/main/java/com/burha/fundhelper/data/local/SnapshotEntity.kt` | Last-known snapshot row |
| `app/src/main/java/com/burha/fundhelper/data/local/FollowedFund.kt` | Follow + optional snapshot `@Relation` |
| `app/src/main/java/com/burha/fundhelper/data/local/FollowDao.kt` | Follow insert/delete/observe |
| `app/src/main/java/com/burha/fundhelper/data/local/SnapshotDao.kt` | Snapshot upsert/observe |
| `app/src/main/java/com/burha/fundhelper/data/local/Converters.kt` | returns/fees JSON for Room |
| `app/src/main/java/com/burha/fundhelper/data/local/AppDatabase.kt` | Room database v1 |
| `app/src/main/java/com/burha/fundhelper/data/local/SnapshotMapper.kt` | Entity ↔ `FundSnapshot` |
| `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt` | Only UI-facing data API |
| `app/src/main/java/com/burha/fundhelper/ui/theme/Theme.kt` | Material 3 light/dark |
| `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt` | Turkish price/return/time formatting for Watchlist and Detail |
| `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt` | Routes `watchlist`, `search`, `detail/{fundCode}` |
| `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistViewModel.kt` | Watchlist `StateFlow` |
| `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt` | Home list, empty CTA, pull-to-refresh |
| `app/src/main/java/com/burha/fundhelper/ui/search/SearchViewModel.kt` | Submit-only search |
| `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt` | Query + results + follow |
| `app/src/main/java/com/burha/fundhelper/ui/detail/DetailViewModel.kt` | One fund + disclaimer data |
| `app/src/main/java/com/burha/fundhelper/ui/detail/DetailScreen.kt` | Type/risk/fees + disclaimer |
| `app/src/test/java/com/burha/fundhelper/domain/ExplanationMapperTest.kt` | Mapper unit tests |
| `app/src/test/java/com/burha/fundhelper/data/tefas/TefasJsonMapperTest.kt` | Fixture parse + HTML reject + no BindHistory |
| `app/src/test/java/com/burha/fundhelper/data/tefas/TefasEndpointsTest.kt` | Allowed URL constants |
| `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt` | Follow/search/refresh/failure |
| `app/src/test/java/com/burha/fundhelper/fakes/FakeTefasClient.kt` | In-memory client |
| `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowDao.kt` | In-memory follows |
| `app/src/test/java/com/burha/fundhelper/fakes/FakeSnapshotDao.kt` | In-memory snapshots |
| `app/src/test/java/com/burha/fundhelper/fakes/FakeClock.kt` | Controllable clock |
| `app/src/test/resources/fixtures/yat-catalog.json` | Representative Getiri JSON |
| `app/src/test/resources/fixtures/yat-prices.json` | Representative GnlBlg JSON |
| `app/src/test/resources/fixtures/challenge.html` | Akamai-like HTML body |

Do not create `app/src/androidTest` in v1.

---

### Task 1: Gradle Compose skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew.bat` (via wrapper bootstrap)
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/com/burha/fundhelper/FundHelperApp.kt`
- Create: `app/src/main/java/com/burha/fundhelper/MainActivity.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: nothing (empty repo besides docs)
- Produces: compiling `:app` with `applicationId` `com.burha.fundhelper`, Hilt application class, Compose `MainActivity`, Turkish `app_name`, `INTERNET`, SDK 26/36/36

- [ ] **Step 1: Write version catalog and Gradle scripts**

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.13.0"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"
composeBom = "2026.06.00"
hilt = "2.57.2"
hiltNavigationCompose = "1.3.0"
room = "2.7.2"
okhttp = "4.12.0"
coroutines = "1.10.2"
lifecycle = "2.9.4"
activityCompose = "1.11.0"
navigationCompose = "2.9.5"
coreKtx = "1.17.0"
serialization = "1.8.1"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "fund-helper"
include(":app")
```

Root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

`gradle.properties`:

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`gradle/wrapper/gradle-wrapper.properties`:

```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.burha.fundhelper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.burha.fundhelper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

`app/proguard-rules.pro` (comment only):

```
# Debug minify is off. Release minify stays off in v1.
```

- [ ] **Step 2: Write the Android entry files**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".FundHelperApp"
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.FundHelper">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FundHelper">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml` (include every v1 string now so later tasks do not add English):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">fund-helper</string>
    <string name="watchlist_title">Takip listesi</string>
    <string name="watchlist_empty_title">Takip ettiğiniz fon yok.</string>
    <string name="watchlist_empty_cta">Fon ara</string>
    <string name="search_title">Fon ara</string>
    <string name="search_hint">Kod veya fon adı</string>
    <string name="search_empty_query">Kod veya fon adı yazın.</string>
    <string name="search_no_results">Sonuç bulunamadı.</string>
    <string name="search_action">Ara</string>
    <string name="fetch_error">TEFAS verisi alınamadı.</string>
    <string name="retry">Yeniden dene</string>
    <string name="disclaimer">Yatırım tavsiyesi değildir.</string>
    <string name="missing_field">TEFAS kaydında bu bilgi yok.</string>
    <string name="detail_title">Fon detayı</string>
    <string name="detail_missing">Bu fon bulunamadı.</string>
    <string name="follow">Takip et</string>
    <string name="unfollow">Takipten çıkar</string>
    <string name="back">Geri</string>
    <string name="price_missing">—</string>
    <string name="return_1d">1 gün</string>
    <string name="return_1w">1 hafta</string>
    <string name="return_1m">1 ay</string>
    <string name="return_3m">3 ay</string>
    <string name="return_6m">6 ay</string>
    <string name="return_12m">12 ay</string>
    <string name="return_36m">36 ay</string>
    <string name="return_60m">60 ay</string>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.FundHelper" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/java/com/burha/fundhelper/FundHelperApp.kt`:

```kotlin
package com.burha.fundhelper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FundHelperApp : Application()
```

`app/src/main/java/com/burha/fundhelper/ui/theme/Theme.kt`:

```kotlin
package com.burha.fundhelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun FundHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}
```

`app/src/main/java/com/burha/fundhelper/MainActivity.kt`:

```kotlin
package com.burha.fundhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.burha.fundhelper.ui.theme.FundHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FundHelperTheme {
                SkeletonLabel()
            }
        }
    }
}

@Composable
private fun SkeletonLabel() {
    Text(text = "fund-helper")
}
```

`activity-compose` 1.11.0 provides `enableEdgeToEdge()`. Do not add a second activity.

- [ ] **Step 3: Bootstrap the Gradle wrapper**

From the repo root in PowerShell (requires a JDK 17+). This downloads Gradle 8.13 once, then generates `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar`:

```powershell
$ver = "8.13.0"
$zip = Join-Path $env:TEMP "gradle-$ver-bin.zip"
$dest = Join-Path $env:TEMP "gradle-$ver-bin"
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$ver-bin.zip" -OutFile $zip
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
Expand-Archive $zip $dest
$gradleBat = Get-ChildItem $dest -Recurse -Filter gradle.bat | Select-Object -First 1
& $gradleBat.FullName wrapper --gradle-version $ver --distribution-type bin
```

If `local.properties` is missing, create it (do not commit) with the machine SDK:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
if (-not (Test-Path $sdk)) { throw "Android SDK not found at $sdk" }
Set-Content -Path .\local.properties -Value "sdk.dir=$($sdk.Replace('\','\\'))"
```

- [ ] **Step 4: Assemble debug and confirm the package id**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK at `app/build/outputs/apk/debug/app-debug.apk`.

Confirm application id without installing:

```powershell
.\gradlew.bat :app:printApplicationId
```

If that task does not exist, read `applicationId` from `app/build.gradle.kts` (must be `com.burha.fundhelper`) and confirm the APK exists.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper app gradlew gradlew.bat
git commit -m "feat: add Compose Hilt Room app skeleton"
```

Do not add `local.properties` or `*.apk`.

---

### Task 2: Domain snapshot and ExplanationMapper

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/domain/FundSnapshot.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/Clock.kt`
- Create: `app/src/main/java/com/burha/fundhelper/domain/ExplanationMapper.kt`
- Test: `app/src/test/java/com/burha/fundhelper/domain/ExplanationMapperTest.kt`

**Interfaces:**
- Consumes: compiling `:app` from Task 1
- Produces: `FundKind`, `FeeLine`, `FundSnapshot`, `ReturnKeys`, `Clock`, `ExplanationMapper.explain(snapshot: FundSnapshot): String`

- [ ] **Step 1: Write the failing mapper tests**

`app/src/test/java/com/burha/fundhelper/domain/ExplanationMapperTest.kt`:

```kotlin
package com.burha.fundhelper.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplanationMapperTest {

    private val present = FundSnapshot(
        code = "AAK",
        name = "ÖRNEK FON",
        kind = FundKind.YAT,
        price = 12.34,
        priceDate = "2026-08-21",
        returns = mapOf(ReturnKeys.M1 to 1.2),
        fundType = "Hisse Senedi Fonu",
        risk = "5",
        fees = listOf(FeeLine(label = "Yönetim ücreti", value = "%2,00")),
        fetchedAt = 1L,
    )

    @Test
    fun maps_present_type_risk_and_fees() {
        val text = ExplanationMapper.explain(present)
        assertTrue(text.contains("Hisse Senedi Fonu"))
        assertTrue(text.contains("5"))
        assertTrue(text.contains("Yönetim ücreti"))
        assertTrue(text.contains("%2,00"))
        assertFalse(text.contains("Yatırım tavsiyesi değildir."))
    }

    @Test
    fun missing_fields_use_absence_sentence() {
        val text = ExplanationMapper.explain(
            present.copy(fundType = null, risk = null, fees = emptyList()),
        )
        assertTrue(text.contains("TEFAS kaydında bu bilgi yok."))
        assertFalse(text.contains("Hisse Senedi Fonu"))
    }

    @Test
    fun output_has_no_buy_sell_language() {
        val text = ExplanationMapper.explain(present)
        val banned = listOf("satın", "satmayın", "hedef fiyat", "size uygun", "Bu fonu alın")
        banned.forEach { token ->
            assertFalse("banned token: $token", text.contains(token, ignoreCase = true))
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.domain.ExplanationMapperTest
```

Expected: FAIL with `Unresolved reference: ExplanationMapper` (and `FundSnapshot`).

- [ ] **Step 3: Write minimal domain + mapper**

`app/src/main/java/com/burha/fundhelper/domain/Clock.kt`:

```kotlin
package com.burha.fundhelper.domain

fun interface Clock {
    fun nowMillis(): Long
}

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
```

`app/src/main/java/com/burha/fundhelper/domain/FundSnapshot.kt`:

```kotlin
package com.burha.fundhelper.domain

enum class FundKind { YAT }

data class FeeLine(
    val label: String,
    val value: String,
)

data class FundSnapshot(
    val code: String,
    val name: String,
    val kind: FundKind,
    val price: Double?,
    val priceDate: String?,
    val returns: Map<String, Double>,
    val fundType: String?,
    val risk: String?,
    val fees: List<FeeLine>,
    val fetchedAt: Long,
)

object ReturnKeys {
    const val D1 = "1D"
    const val W1 = "1W"
    const val M1 = "1M"
    const val M3 = "3M"
    const val M6 = "6M"
    const val M12 = "12M"
    const val M36 = "36M"
    const val M60 = "60M"

    val DISPLAY_ORDER = listOf(M1, D1, W1, M3, M6, M12, M36, M60)

    fun headline(returns: Map<String, Double>): Pair<String, Double>? {
        for (key in DISPLAY_ORDER) {
            val value = returns[key]
            if (value != null) return key to value
        }
        return null
    }
}
```

`app/src/main/java/com/burha/fundhelper/domain/ExplanationMapper.kt`:

```kotlin
package com.burha.fundhelper.domain

object ExplanationMapper {
    const val MISSING = "TEFAS kaydında bu bilgi yok."

    fun explain(snapshot: FundSnapshot): String {
        val typeSentence = if (snapshot.fundType.isNullOrBlank()) {
            "Fonun resmi türü $MISSING"
        } else {
            "Fonun resmi türü ${snapshot.fundType}."
        }
        val riskSentence = if (snapshot.risk.isNullOrBlank()) {
            "Risk değeri $MISSING"
        } else {
            "Risk değeri TEFAS kaydındaki skorudur: ${snapshot.risk}."
        }
        val feesSentence = if (snapshot.fees.isEmpty()) {
            "Ücret bilgisi $MISSING"
        } else {
            snapshot.fees.joinToString(" ") { line ->
                "${line.label} resmi kayıtta ${line.value}."
            }
        }
        return "$typeSentence $riskSentence $feesSentence"
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.domain.ExplanationMapperTest
```

Expected: `BUILD SUCCESSFUL`, 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/domain app/src/test/java/com/burha/fundhelper/domain
git commit -m "feat: map official fund fields to Turkish copy"
```

---

### Task 3: TEFAS JSON mapper (fixtures, no network)

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/tefas/TefasJsonMapper.kt`
- Create: `app/src/test/resources/fixtures/yat-catalog.json`
- Create: `app/src/test/resources/fixtures/yat-prices.json`
- Create: `app/src/test/resources/fixtures/challenge.html`
- Test: `app/src/test/java/com/burha/fundhelper/data/tefas/TefasJsonMapperTest.kt`
- Test: `app/src/test/java/com/burha/fundhelper/data/tefas/TefasEndpointsTest.kt`

**Interfaces:**
- Consumes: `FundSnapshot`, `FundKind`, `ReturnKeys`, `FeeLine`
- Produces: `TefasEndpoints` URL constants; `TefasFetchException`; `TefasClient` (`suspend fun fetchYatCatalog(): List<FundSnapshot>`, `suspend fun fetchLatestYatPrices(): List<FundSnapshot>`); `TefasJsonMapper.parseCatalog(body: String): List<FundSnapshot>`; `TefasJsonMapper.parseLatestPrices(body: String): List<FundSnapshot>`

Wire field names (2026 `resultList` rows; stay inside this mapper):

| JSON | Domain |
|------|--------|
| `fonKodu` | `code` |
| `fonUnvan` | `name` |
| `fonTurAciklama` | `fundType` |
| `riskDegeri` | `risk` (stringified) |
| `getiri1a` / `getiri3a` / `getiri6a` / `getiri1y` / `getiri3y` / `getiri5y` | returns `1M` / `3M` / `6M` / `12M` / `36M` / `60M` |
| `fiyat` | `price` (skip ≤ 0) |
| `tarih` | `priceDate` (first 10 chars) |

`fetchedAt` is `0` in parsed snapshots; `FundRepository` stamps the device clock on upsert. Fees are empty unless a fee pair is actually in the JSON (v1 Getiri payload has none — do not invent yönetim ücreti).

- [ ] **Step 1: Write fixtures and failing tests**

`app/src/test/resources/fixtures/yat-catalog.json`:

```json
{
  "errorCode": null,
  "resultList": [
    {
      "fonKodu": "AAK",
      "fonUnvan": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "fonTurAciklama": "Değişken Şemsiye Fonu",
      "riskDegeri": 4,
      "getiri1a": 1.25,
      "getiri3a": 3.5,
      "getiri6a": 6.0,
      "getiri1y": 12.0,
      "getiri3y": 40.0,
      "getiri5y": 80.0,
      "getiriyb": 8.0
    },
    {
      "fonKodu": "AAL",
      "fonUnvan": "ATA PORTFÖY PARA PİYASASI (TL) FONU",
      "fonTurAciklama": null,
      "riskDegeri": null,
      "getiri1a": 2.0
    }
  ]
}
```

`app/src/test/resources/fixtures/yat-prices.json`:

```json
{
  "errorCode": null,
  "resultList": [
    {
      "fonKodu": "AAK",
      "fonUnvan": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "tarih": "2026-08-20",
      "fiyat": 34.1
    },
    {
      "fonKodu": "AAK",
      "fonUnvan": "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
      "tarih": "2026-08-21",
      "fiyat": 35.46418
    },
    {
      "fonKodu": "AAL",
      "fonUnvan": "ATA PORTFÖY PARA PİYASASI (TL) FONU",
      "tarih": "2026-08-21",
      "fiyat": 0
    }
  ]
}
```

`app/src/test/resources/fixtures/challenge.html`:

```html
<html><body>Access Denied</body></html>
```

`app/src/test/java/com/burha/fundhelper/data/tefas/TefasEndpointsTest.kt`:

```kotlin
package com.burha.fundhelper.data.tefas

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TefasEndpointsTest {
    @Test
    fun uses_2026_funds_json_not_bind_history() {
        val urls = listOf(TefasEndpoints.CATALOG, TefasEndpoints.PRICES)
        urls.forEach { url ->
            assertTrue(url.startsWith("https://www.tefas.gov.tr/api/funds/"))
            assertFalse(url.contains("BindHistory"))
            assertFalse(url.contains("fundturkey.com.tr"))
        }
    }
}
```

`app/src/test/java/com/burha/fundhelper/data/tefas/TefasJsonMapperTest.kt`:

```kotlin
package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.ReturnKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TefasJsonMapperTest {

    private fun fixture(name: String): String {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name"))
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun parses_catalog_returns_type_and_risk() {
        val funds = TefasJsonMapper.parseCatalog(fixture("yat-catalog.json"))
        assertEquals(2, funds.size)
        val aak = funds.first { it.code == "AAK" }
        assertEquals("ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON", aak.name)
        assertEquals("Değişken Şemsiye Fonu", aak.fundType)
        assertEquals("4", aak.risk)
        assertEquals(1.25, aak.returns[ReturnKeys.M1])
        assertEquals(3.5, aak.returns[ReturnKeys.M3])
        assertEquals(12.0, aak.returns[ReturnKeys.M12])
        assertEquals(40.0, aak.returns[ReturnKeys.M36])
        assertEquals(80.0, aak.returns[ReturnKeys.M60])
        assertTrue(aak.fees.isEmpty())
        assertNull(aak.price)
    }

    @Test
    fun parses_latest_nonzero_price_per_code() {
        val funds = TefasJsonMapper.parseLatestPrices(fixture("yat-prices.json"))
        val aak = funds.single { it.code == "AAK" }
        assertEquals(35.46418, aak.price)
        assertEquals("2026-08-21", aak.priceDate)
        assertTrue(funds.none { it.code == "AAL" })
    }

    @Test(expected = TefasFetchException::class)
    fun rejects_html_challenge_as_failure() {
        TefasJsonMapper.parseCatalog(fixture("challenge.html"))
    }

    @Test(expected = TefasFetchException::class)
    fun rejects_error_code_payload() {
        TefasJsonMapper.parseCatalog("""{"errorCode":"ERR-224","resultList":[]}""")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest --tests com.burha.fundhelper.data.tefas.TefasEndpointsTest
```

Expected: FAIL with `Unresolved reference: TefasJsonMapper` / `TefasEndpoints`.

- [ ] **Step 3: Write mapper, endpoints, and client interface**

`app/src/main/java/com/burha/fundhelper/data/tefas/TefasClient.kt`:

```kotlin
package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundSnapshot

object TefasEndpoints {
    const val CATALOG = "https://www.tefas.gov.tr/api/funds/fonGetiriBazliBilgiGetir"
    const val PRICES = "https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir"
    const val ORIGIN = "https://www.tefas.gov.tr"
    const val REFERER = "https://www.tefas.gov.tr/tr/fon-detayli-analiz"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
}

class TefasFetchException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface TefasClient {
    suspend fun fetchYatCatalog(): List<FundSnapshot>
    suspend fun fetchLatestYatPrices(): List<FundSnapshot>
}
```

`app/src/main/java/com/burha/fundhelper/data/tefas/TefasJsonMapper.kt`:

```kotlin
package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object TefasJsonMapper {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseCatalog(body: String): List<FundSnapshot> {
        val rows = resultList(body)
        return rows.mapNotNull { row ->
            val code = row.string("fonKodu")?.trim().orEmpty()
            if (code.isEmpty()) return@mapNotNull null
            val returns = buildMap {
                putIfPresent(ReturnKeys.M1, row.double("getiri1a"))
                putIfPresent(ReturnKeys.M3, row.double("getiri3a"))
                putIfPresent(ReturnKeys.M6, row.double("getiri6a"))
                putIfPresent(ReturnKeys.M12, row.double("getiri1y"))
                putIfPresent(ReturnKeys.M36, row.double("getiri3y"))
                putIfPresent(ReturnKeys.M60, row.double("getiri5y"))
            }
            FundSnapshot(
                code = code,
                name = row.string("fonUnvan").orEmpty(),
                kind = FundKind.YAT,
                price = null,
                priceDate = null,
                returns = returns,
                fundType = row.string("fonTurAciklama"),
                risk = row.intOrString("riskDegeri"),
                fees = emptyList(),
                fetchedAt = 0L,
            )
        }
    }

    fun parseLatestPrices(body: String): List<FundSnapshot> {
        val best = linkedMapOf<String, FundSnapshot>()
        for (row in resultList(body)) {
            val code = row.string("fonKodu")?.trim().orEmpty()
            val price = row.double("fiyat") ?: continue
            if (code.isEmpty() || price <= 0.0) continue
            val date = row.string("tarih")?.take(10)
            val current = best[code]
            if (current == null || (date ?: "") >= (current.priceDate ?: "")) {
                best[code] = FundSnapshot(
                    code = code,
                    name = row.string("fonUnvan").orEmpty(),
                    kind = FundKind.YAT,
                    price = price,
                    priceDate = date,
                    returns = emptyMap(),
                    fundType = null,
                    risk = null,
                    fees = emptyList(),
                    fetchedAt = 0L,
                )
            }
        }
        return best.values.toList()
    }

    private fun resultList(body: String): List<JsonObject> {
        val trimmed = body.trim()
        if (!trimmed.startsWith("{")) {
            throw TefasFetchException("TEFAS returned a non-JSON body")
        }
        val root = try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (e: Exception) {
            throw TefasFetchException("TEFAS returned malformed JSON", e)
        }
        val error = root["errorCode"]
        if (error != null && error !is JsonNull && error.jsonPrimitive.contentOrNull.isNullOrBlank().not()) {
            throw TefasFetchException("TEFAS errorCode=${error.jsonPrimitive.content}")
        }
        val list = root["resultList"] as? JsonArray ?: throw TefasFetchException("TEFAS JSON missing resultList")
        return list.map { it.jsonObject }
    }

    private fun MutableMap<String, Double>.putIfPresent(key: String, value: Double?) {
        if (value != null) put(key, value)
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.asPrimitive()?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.double(key: String): Double? =
        this[key]?.asPrimitive()?.doubleOrNull

    private fun JsonObject.intOrString(key: String): String? {
        val primitive = this[key]?.asPrimitive() ?: return null
        return primitive.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive
}
```

Use `kotlinx.serialization.json.contentOrNull` and `doubleOrNull` on `JsonPrimitive`. Missing keys become `null`; do not invent numbers.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest --tests com.burha.fundhelper.data.tefas.TefasEndpointsTest
```

Expected: PASS (catalog, prices, HTML, errorCode, BindHistory URL assertions).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/tefas app/src/test/java/com/burha/fundhelper/data/tefas app/src/test/resources/fixtures
git commit -m "feat: parse 2026 TEFAS funds JSON fixtures"
```

---

### Task 4: OkHttp TefasClient

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt`
- Create: `app/src/main/java/com/burha/fundhelper/di/TefasModule.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/di/AppModule.kt` (create in this task if missing)
- Test: `app/src/test/java/com/burha/fundhelper/fakes/FakeTefasClient.kt`

**Interfaces:**
- Consumes: `TefasClient`, `TefasEndpoints`, `TefasJsonMapper`, `TefasFetchException`, `Clock`
- Produces: `OkHttpTefasClient(http: OkHttpClient, clock: Clock = unused for HTTP)` implementing `TefasClient`; `FakeTefasClient` for later repository tests; Hilt `@Binds`

Refresh uses the **bulk fallback**: `fonGetiriBazliBilgiGetir` is the YAT catalog (search + type/risk/returns). `fonGnlBlgSiraliGetir` is one bulk price call (the endpoint returns the market list for a date window, not a cheap per-code payload). Do not call `dagilimSiraliGetirT` (holdings). Do not call `fonFiyatBilgiGetir` (chart history). Sequential per-code fan-out is forbidden because it would re-download the bulk list once per follow.

- [ ] **Step 1: Write FakeTefasClient (test double used from Task 6)**

`app/src/test/java/com/burha/fundhelper/fakes/FakeTefasClient.kt`:

```kotlin
package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.data.tefas.TefasFetchException
import com.burha.fundhelper.domain.FundSnapshot

class FakeTefasClient(
    var catalog: List<FundSnapshot> = emptyList(),
    var prices: List<FundSnapshot> = emptyList(),
    var failCatalog: Boolean = false,
    var failPrices: Boolean = false,
) : TefasClient {
    var catalogCalls: Int = 0
    var priceCalls: Int = 0

    override suspend fun fetchYatCatalog(): List<FundSnapshot> {
        catalogCalls += 1
        if (failCatalog) throw TefasFetchException("catalog failed")
        return catalog
    }

    override suspend fun fetchLatestYatPrices(): List<FundSnapshot> {
        priceCalls += 1
        if (failPrices) throw TefasFetchException("prices failed")
        return prices
    }
}
```

- [ ] **Step 2: Implement OkHttpTefasClient**

`app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt`:

```kotlin
package com.burha.fundhelper.data.tefas

import com.burha.fundhelper.domain.FundSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpTefasClient @Inject constructor(
    private val http: OkHttpClient,
) : TefasClient {

    override suspend fun fetchYatCatalog(): List<FundSnapshot> {
        val body = """
            {"dil":"TR","fonTipi":"YAT","kurucuKodu":null,"sfonTurKod":null,"fonTurAciklama":null,
             "islem":1,"fonTurKod":null,"fonGrubu":null,"donemGetiri1a":"1","donemGetiri3a":"1",
             "donemGetiri6a":"1","donemGetiri1y":"1","donemGetiriyb":"1","donemGetiri3y":"1",
             "donemGetiri5y":"1","basTarih":null,"bitTarih":null,"calismaTipi":2,"getiriOrani":"1"}
        """.trimIndent()
        return TefasJsonMapper.parseCatalog(post(TefasEndpoints.CATALOG, body))
    }

    override suspend fun fetchLatestYatPrices(): List<FundSnapshot> {
        val end = LocalDate.now()
        val start = end.minusDays(7)
        val fmt = DateTimeFormatter.BASIC_ISO_DATE
        val body = """
            {"fonTipi":"YAT","fonKodu":null,"aramaMetni":null,"fonTurKod":null,"fonGrubu":null,
             "sfonTurKod":null,"fonTurAciklama":null,"kurucuKod":null,
             "basTarih":"${start.format(fmt)}","bitTarih":"${end.format(fmt)}",
             "basSira":1,"bitSira":100000,"dil":"TR","sFonTurKod":"","fonKod":"","fonGrup":"","fonUnvanTip":""}
        """.trimIndent()
        return TefasJsonMapper.parseLatestPrices(post(TefasEndpoints.PRICES, body))
    }

    private suspend fun post(url: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", TefasEndpoints.USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Origin", TefasEndpoints.ORIGIN)
            .header("Referer", TefasEndpoints.REFERER)
            .post(jsonBody.toRequestBody(JSON))
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw TefasFetchException("HTTP ${response.code}")
            }
            body
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
```

`app/src/main/java/com/burha/fundhelper/di/AppModule.kt`:

```kotlin
package com.burha.fundhelper.di

import com.burha.fundhelper.data.tefas.OkHttpTefasClient
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpTefasClient.defaultClient()

    @Provides
    @Singleton
    fun clock(): Clock = SystemClock()
}
```

`app/src/main/java/com/burha/fundhelper/di/TefasModule.kt`:

```kotlin
package com.burha.fundhelper.di

import com.burha.fundhelper.data.tefas.OkHttpTefasClient
import com.burha.fundhelper.data.tefas.TefasClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TefasModule {
    @Binds
    @Singleton
    abstract fun tefasClient(impl: OkHttpTefasClient): TefasClient
}
```

Only these two files plus `OkHttpTefasClient` may import `okhttp3`.

- [ ] **Step 3: Compile (no live TEFAS call)**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest
```

Expected: SUCCESS. Do **not** invoke `OkHttpTefasClient.fetchYatCatalog()` from a unit test.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/tefas/OkHttpTefasClient.kt app/src/main/java/com/burha/fundhelper/di app/src/test/java/com/burha/fundhelper/fakes/FakeTefasClient.kt
git commit -m "feat: isolate OkHttp TEFAS client behind TefasClient"
```

---

### Task 5: Room follows and snapshots

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/data/local/FollowEntity.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/SnapshotEntity.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/FollowedFund.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/FollowDao.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/SnapshotDao.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/Converters.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/burha/fundhelper/data/local/SnapshotMapper.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/di/AppModule.kt`
- Test: `app/src/test/java/com/burha/fundhelper/fakes/FakeFollowDao.kt`
- Test: `app/src/test/java/com/burha/fundhelper/fakes/FakeSnapshotDao.kt`

**Interfaces:**
- Consumes: `FundSnapshot`, `FeeLine`, `FundKind`
- Produces: `FollowDao.insert/delete/getCodes/observeFollowed`; `SnapshotDao.upsert/upsertAll/observe/get`; `SnapshotMapper.toDomain` / `toEntity`; Hilt `AppDatabase`

Unfollow deletes the follow row only. Snapshots stay.

- [ ] **Step 1: Write Room types and fakes**

`FollowEntity.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val code: String,
)
```

`SnapshotEntity.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val code: String,
    val name: String,
    val kind: String,
    val price: Double?,
    val priceDate: String?,
    val returnsJson: String,
    val fundType: String?,
    val risk: String?,
    val feesJson: String,
    val fetchedAt: Long,
)
```

`FollowedFund.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class FollowedFund(
    @Embedded val follow: FollowEntity,
    @Relation(parentColumn = "code", entityColumn = "code")
    val snapshot: SnapshotEntity?,
)
```

`FollowDao.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE code = :code")
    suspend fun delete(code: String)

    @Query("SELECT code FROM follows ORDER BY code")
    suspend fun getCodes(): List<String>

    @Transaction
    @Query("SELECT * FROM follows ORDER BY code")
    fun observeFollowed(): Flow<List<FollowedFund>>
}
```

`SnapshotDao.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SnapshotEntity>)

    @Query("SELECT * FROM snapshots WHERE code = :code")
    fun observe(code: String): Flow<SnapshotEntity?>

    @Query("SELECT * FROM snapshots WHERE code = :code")
    suspend fun get(code: String): SnapshotEntity?
}
```

`Converters.kt`:

```kotlin
package com.burha.fundhelper.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SnapshotJson {
    val json = Json { ignoreUnknownKeys = true }

    fun returnsToJson(returns: Map<String, Double>): String = json.encodeToString(returns)

    fun returnsFromJson(raw: String): Map<String, Double> =
        if (raw.isBlank()) emptyMap() else json.decodeFromString(raw)

    fun feesToJson(fees: List<Pair<String, String>>): String = json.encodeToString(fees)

    fun feesFromJson(raw: String): List<Pair<String, String>> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}
```

`SnapshotMapper.kt`:

```kotlin
package com.burha.fundhelper.data.local

import com.burha.fundhelper.domain.FeeLine
import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot

object SnapshotMapper {
    fun toEntity(snapshot: FundSnapshot): SnapshotEntity = SnapshotEntity(
        code = snapshot.code,
        name = snapshot.name,
        kind = snapshot.kind.name,
        price = snapshot.price,
        priceDate = snapshot.priceDate,
        returnsJson = SnapshotJson.returnsToJson(snapshot.returns),
        fundType = snapshot.fundType,
        risk = snapshot.risk,
        feesJson = SnapshotJson.feesToJson(snapshot.fees.map { it.label to it.value }),
        fetchedAt = snapshot.fetchedAt,
    )

    fun toDomain(entity: SnapshotEntity): FundSnapshot = FundSnapshot(
        code = entity.code,
        name = entity.name,
        kind = FundKind.valueOf(entity.kind),
        price = entity.price,
        priceDate = entity.priceDate,
        returns = SnapshotJson.returnsFromJson(entity.returnsJson),
        fundType = entity.fundType,
        risk = entity.risk,
        fees = SnapshotJson.feesFromJson(entity.feesJson).map { FeeLine(it.first, it.second) },
        fetchedAt = entity.fetchedAt,
    )
}
```

`AppDatabase.kt`:

```kotlin
package com.burha.fundhelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FollowEntity::class, SnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followDao(): FollowDao
    abstract fun snapshotDao(): SnapshotDao
}
```

Append to `AppModule.kt`:

```kotlin
import android.content.Context
import androidx.room.Room
import com.burha.fundhelper.data.local.AppDatabase
import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.SnapshotDao
import dagger.hilt.android.qualifiers.ApplicationContext

@Provides
@Singleton
fun database(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "fund-helper.db").build()

@Provides
fun followDao(db: AppDatabase): FollowDao = db.followDao()

@Provides
fun snapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()
```

Keep the existing `okHttpClient` and `clock` providers in the same object.

`FakeFollowDao.kt`:

```kotlin
package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.FollowEntity
import com.burha.fundhelper.data.local.FollowedFund
import com.burha.fundhelper.data.local.SnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FakeFollowDao(
    private val snapshots: FakeSnapshotDao,
) : FollowDao {
    private val codes = MutableStateFlow<List<String>>(emptyList())

    override suspend fun insert(follow: FollowEntity) {
        codes.value = (codes.value + follow.code).distinct().sorted()
    }

    override suspend fun delete(code: String) {
        codes.value = codes.value.filterNot { it == code }
    }

    override suspend fun getCodes(): List<String> = codes.value

    override fun observeFollowed(): Flow<List<FollowedFund>> =
        combine(codes, snapshots.observeAll()) { followCodes, snapMap ->
            followCodes.map { code ->
                FollowedFund(
                    follow = FollowEntity(code),
                    snapshot = snapMap[code],
                )
            }
        }
}
```

`FakeSnapshotDao.kt`:

```kotlin
package com.burha.fundhelper.fakes

import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.local.SnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSnapshotDao : SnapshotDao {
    private val rows = MutableStateFlow<Map<String, SnapshotEntity>>(emptyMap())

    fun observeAll(): Flow<Map<String, SnapshotEntity>> = rows

    override suspend fun upsert(entity: SnapshotEntity) {
        rows.value = rows.value + (entity.code to entity)
    }

    override suspend fun upsertAll(entities: List<SnapshotEntity>) {
        rows.value = rows.value + entities.associateBy { it.code }
    }

    override fun observe(code: String): Flow<SnapshotEntity?> =
        rows.map { it[code] }

    override suspend fun get(code: String): SnapshotEntity? = rows.value[code]
}
```

`FakeClock.kt`:

```kotlin
package com.burha.fundhelper.fakes

import com.burha.fundhelper.domain.Clock

class FakeClock(var now: Long = 1_000_000L) : Clock {
    override fun nowMillis(): Long = now
}
```

- [ ] **Step 2: Compile Room + fakes**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
```

Expected: SUCCESS. KSP generates `AppDatabase_Impl`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/local app/src/main/java/com/burha/fundhelper/di/AppModule.kt app/src/test/java/com/burha/fundhelper/fakes
git commit -m "feat: persist follows and fund snapshots in Room"
```

---

### Task 6: FundRepository (TDD)

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`
- Test: `app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`

**Interfaces:**
- Consumes: `FollowDao`, `SnapshotDao`, `TefasClient`, `Clock`, `ExplanationMapper`, `SnapshotMapper`, `ReturnKeys`, `TefasFetchException`
- Produces:

```kotlin
data class WatchlistRow(
    val code: String,
    val name: String?,
    val price: Double?,
    val headlinePeriod: String?,
    val headlineReturn: Double?,
    val fetchedAt: Long?,
)

data class FundDetail(
    val snapshot: FundSnapshot,
    val explanation: String,
    val isFollowed: Boolean,
)

sealed class SearchOutcome {
    data object EmptyQuery : SearchOutcome()
    data class Success(val matches: List<FundSnapshot>) : SearchOutcome()
    data class Failure(val message: String) : SearchOutcome()
}

class FundRepository @Inject constructor(
    private val followDao: FollowDao,
    private val snapshotDao: SnapshotDao,
    private val tefas: TefasClient,
    private val clock: Clock,
) {
    fun observeWatchlist(): Flow<List<WatchlistRow>>
    fun observeFund(code: String): Flow<FundDetail?>
    suspend fun follow(code: String)
    suspend fun unfollow(code: String)
    suspend fun search(query: String, refetchCatalog: Boolean = false): SearchOutcome
    suspend fun refreshFollowed(force: Boolean): Result<Unit>
}
```

Rules the tests lock:

1. Follow writes Room only; unfollow deletes follow, not snapshot.
2. Watchlist is the follow set (code at minimum).
3. `refreshFollowed` upserts **followed codes only** even if the client returns extra funds.
4. Client failure leaves follows and previous snapshots.
5. Search does not require a follow; empty query does not call the client; first success caches the catalog; retry refetches.
6. Search upserts Room snapshots for **matches only**.
7. `force = false` skips network if a refresh **succeeded** in this process within 5 minutes; empty follows skip network; `force = true` always tries (still no-op if no follows).
8. `observeFund` runs `ExplanationMapper` in the repository, not in the UI.

- [ ] **Step 1: Write the failing repository tests**

`app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt`:

```kotlin
package com.burha.fundhelper.data

import com.burha.fundhelper.domain.FundKind
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.fakes.FakeClock
import com.burha.fundhelper.fakes.FakeFollowDao
import com.burha.fundhelper.fakes.FakeSnapshotDao
import com.burha.fundhelper.fakes.FakeTefasClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FundRepositoryTest {

    private val aak = FundSnapshot(
        code = "AAK",
        name = "ATA PORTFÖY ÇOKLU VARLIK DEĞİŞKEN FON",
        kind = FundKind.YAT,
        price = null,
        priceDate = null,
        returns = mapOf(ReturnKeys.M1 to 1.25),
        fundType = "Değişken Şemsiye Fonu",
        risk = "4",
        fees = emptyList(),
        fetchedAt = 0L,
    )
    private val aal = aak.copy(code = "AAL", name = "ATA PORTFÖY PARA PİYASASI (TL) FONU")
    private val aakPriced = aak.copy(price = 35.46, priceDate = "2026-08-21")

    private fun repo(
        tefas: FakeTefasClient = FakeTefasClient(catalog = listOf(aak, aal), prices = listOf(aakPriced)),
        snapshots: FakeSnapshotDao = FakeSnapshotDao(),
        clock: FakeClock = FakeClock(),
    ): Triple<FundRepository, FakeTefasClient, FakeSnapshotDao> {
        val follows = FakeFollowDao(snapshots)
        val repository = FundRepository(follows, snapshots, tefas, clock)
        return Triple(repository, tefas, snapshots)
    }

    @Test
    fun follow_and_unfollow_persist_watchlist_codes() = runTest {
        val (repository, _, _) = repo()
        repository.follow("AAK")
        assertEquals(listOf("AAK"), repository.observeWatchlist().first().map { it.code })
        repository.unfollow("AAK")
        assertTrue(repository.observeWatchlist().first().isEmpty())
    }

    @Test
    fun unfollow_keeps_snapshot() = runTest {
        val (repository, _, snapshots) = repo()
        repository.search("AAK")
        repository.follow("AAK")
        repository.unfollow("AAK")
        assertNotNull(snapshots.get("AAK"))
    }

    @Test
    fun refresh_updates_only_followed_codes() = runTest {
        val (repository, tefas, snapshots) = repo()
        repository.follow("AAK")
        val result = repository.refreshFollowed(force = true)
        assertTrue(result.isSuccess)
        assertEquals(1, tefas.catalogCalls)
        assertEquals(1, tefas.priceCalls)
        assertNotNull(snapshots.get("AAK"))
        assertNull(snapshots.get("AAL"))
        val row = repository.observeWatchlist().first().single()
        assertEquals(35.46, row.price)
        assertEquals(ReturnKeys.M1, row.headlinePeriod)
        assertEquals(1.25, row.headlineReturn)
    }

    @Test
    fun refresh_failure_keeps_follows_and_snapshots() = runTest {
        val tefas = FakeTefasClient(catalog = listOf(aak), prices = listOf(aakPriced))
        val (repository, _, _) = repo(tefas)
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        tefas.failCatalog = true
        val failed = repository.refreshFollowed(force = true)
        assertTrue(failed.isFailure)
        val rows = repository.observeWatchlist().first()
        assertEquals(listOf("AAK"), rows.map { it.code })
        assertEquals(35.46, rows.single().price)
    }

    @Test
    fun search_does_not_require_follow_and_upserts_matches_only() = runTest {
        val (repository, tefas, snapshots) = repo()
        val outcome = repository.search("ata")
        assertTrue(outcome is SearchOutcome.Success)
        assertEquals(2, (outcome as SearchOutcome.Success).matches.size)
        assertEquals(1, tefas.catalogCalls)
        assertNotNull(snapshots.get("AAK"))
        assertNotNull(snapshots.get("AAL"))
        assertTrue(repository.observeWatchlist().first().isEmpty())
        repository.search("AAK")
        assertEquals(1, tefas.catalogCalls)
    }

    @Test
    fun empty_query_does_not_hit_client() = runTest {
        val (repository, tefas, _) = repo()
        val outcome = repository.search("  ")
        assertEquals(SearchOutcome.EmptyQuery, outcome)
        assertEquals(0, tefas.catalogCalls)
    }

    @Test
    fun search_retry_refetches_catalog() = runTest {
        val (repository, tefas, _) = repo()
        repository.search("AAK")
        repository.search("AAK", refetchCatalog = true)
        assertEquals(2, tefas.catalogCalls)
    }

    @Test
    fun auto_refresh_skips_within_five_minutes() = runTest {
        val clock = FakeClock(now = 0L)
        val (repository, tefas, _) = repo(clock = clock)
        repository.follow("AAK")
        repository.refreshFollowed(force = true)
        clock.now = 4 * 60 * 1000L
        repository.refreshFollowed(force = false)
        assertEquals(1, tefas.catalogCalls)
        clock.now = 5 * 60 * 1000L
        repository.refreshFollowed(force = false)
        assertEquals(2, tefas.catalogCalls)
    }

    @Test
    fun observe_fund_includes_mapper_paragraph() = runTest {
        val (repository, _, _) = repo()
        repository.search("AAK")
        repository.follow("AAK")
        val detail = repository.observeFund("AAK").first()
        assertNotNull(detail)
        assertTrue(detail!!.explanation.contains("Değişken Şemsiye Fonu"))
        assertTrue(detail.isFollowed)
        assertTrue(!detail.explanation.contains("Yatırım tavsiyesi değildir."))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest
```

Expected: FAIL with `Unresolved reference: FundRepository`.

- [ ] **Step 3: Write FundRepository**

`app/src/main/java/com/burha/fundhelper/data/FundRepository.kt`:

```kotlin
package com.burha.fundhelper.data

import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.FollowEntity
import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.local.SnapshotMapper
import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.data.tefas.TefasFetchException
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.ExplanationMapper
import com.burha.fundhelper.domain.FundSnapshot
import com.burha.fundhelper.domain.ReturnKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class WatchlistRow(
    val code: String,
    val name: String?,
    val price: Double?,
    val headlinePeriod: String?,
    val headlineReturn: Double?,
    val fetchedAt: Long?,
)

data class FundDetail(
    val snapshot: FundSnapshot,
    val explanation: String,
    val isFollowed: Boolean,
)

sealed class SearchOutcome {
    data object EmptyQuery : SearchOutcome()
    data class Success(val matches: List<FundSnapshot>) : SearchOutcome()
    data class Failure(val message: String) : SearchOutcome()
}

@Singleton
class FundRepository @Inject constructor(
    private val followDao: FollowDao,
    private val snapshotDao: SnapshotDao,
    private val tefas: TefasClient,
    private val clock: Clock,
) {
    private val tr = Locale("tr", "TR")
    private var catalogMemory: List<FundSnapshot>? = null
    @Volatile private var lastRefreshSuccessAt: Long = 0L

    fun observeWatchlist(): Flow<List<WatchlistRow>> = followDao.observeFollowed().map { rows ->
        rows.map { followed ->
            val snapshot = followed.snapshot?.let(SnapshotMapper::toDomain)
            val headline = snapshot?.returns?.let(ReturnKeys::headline)
            WatchlistRow(
                code = followed.follow.code,
                name = snapshot?.name,
                price = snapshot?.price,
                headlinePeriod = headline?.first,
                headlineReturn = headline?.second,
                fetchedAt = snapshot?.fetchedAt,
            )
        }
    }

    fun observeFund(code: String): Flow<FundDetail?> = combine(
        snapshotDao.observe(code),
        followDao.observeFollowed(),
    ) { entity, followed ->
        val snapshot = entity?.let(SnapshotMapper::toDomain) ?: return@combine null
        FundDetail(
            snapshot = snapshot,
            explanation = ExplanationMapper.explain(snapshot),
            isFollowed = followed.any { it.follow.code == code },
        )
    }

    suspend fun follow(code: String) {
        followDao.insert(FollowEntity(code))
    }

    suspend fun unfollow(code: String) {
        followDao.delete(code)
    }

    suspend fun search(query: String, refetchCatalog: Boolean = false): SearchOutcome {
        val needle = query.trim()
        if (needle.isEmpty()) return SearchOutcome.EmptyQuery
        return try {
            val catalog = loadCatalog(refetchCatalog)
            val matches = catalog.filter { fund -> matchesQuery(fund, needle) }
            val now = clock.nowMillis()
            snapshotDao.upsertAll(matches.map { SnapshotMapper.toEntity(it.copy(fetchedAt = now)) })
            SearchOutcome.Success(matches.map { it.copy(fetchedAt = now) })
        } catch (e: TefasFetchException) {
            SearchOutcome.Failure(e.message ?: "TEFAS")
        }
    }

    suspend fun refreshFollowed(force: Boolean): Result<Unit> {
        val codes = followDao.getCodes()
        if (codes.isEmpty()) return Result.success(Unit)
        val now = clock.nowMillis()
        if (!force && lastRefreshSuccessAt != 0L && now - lastRefreshSuccessAt < FIVE_MINUTES_MS) {
            return Result.success(Unit)
        }
        return try {
            val catalog = tefas.fetchYatCatalog().associateBy { it.code }
            catalogMemory = catalog.values.toList()
            val prices = tefas.fetchLatestYatPrices().associateBy { it.code }
            val merged = codes.mapNotNull { code ->
                val listing = catalog[code]
                val priceRow = prices[code]
                val previous = snapshotDao.get(code)?.let(SnapshotMapper::toDomain)
                val base = listing ?: previous ?: priceRow ?: return@mapNotNull null
                base.copy(
                    price = priceRow?.price ?: base.price,
                    priceDate = priceRow?.priceDate ?: base.priceDate,
                    name = listing?.name?.takeIf { it.isNotBlank() } ?: base.name,
                    fundType = listing?.fundType ?: base.fundType,
                    risk = listing?.risk ?: base.risk,
                    returns = listing?.returns?.takeIf { it.isNotEmpty() } ?: base.returns,
                    fetchedAt = now,
                )
            }
            snapshotDao.upsertAll(merged.map(SnapshotMapper::toEntity))
            lastRefreshSuccessAt = now
            Result.success(Unit)
        } catch (e: TefasFetchException) {
            Result.failure(e)
        }
    }

    private suspend fun loadCatalog(refetch: Boolean): List<FundSnapshot> {
        val cached = catalogMemory
        if (!refetch && cached != null) return cached
        val fresh = tefas.fetchYatCatalog()
        catalogMemory = fresh
        return fresh
    }

    private fun matchesQuery(fund: FundSnapshot, raw: String): Boolean {
        val q = raw.lowercase(tr)
        val code = fund.code.lowercase(tr)
        val name = fund.name.lowercase(tr)
        return code.startsWith(q) || name.contains(q)
    }

    private companion object {
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }
}
```

`observeFund` combine needs follow codes. Using `observeFollowed()` is enough. If the user is not followed and a snapshot exists (search upsert), `isFollowed` is false.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest --tests com.burha.fundhelper.domain.ExplanationMapperTest --tests com.burha.fundhelper.data.tefas.TefasJsonMapperTest
```

Expected: all PASS. No live HTTP.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/data/FundRepository.kt app/src/test/java/com/burha/fundhelper/data/FundRepositoryTest.kt
git commit -m "feat: add FundRepository follow search and refresh"
```

---

### Task 7: Watchlist screen

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/ui/UiFormat.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistViewModel.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/watchlist/WatchlistScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/MainActivity.kt`

**Interfaces:**
- Consumes: `FundRepository.observeWatchlist()`, `refreshFollowed(force)`, `unfollow(code)` — never `TefasClient` or Room
- Produces: route `watchlist` as start destination; `WatchlistUiState`; navigation callbacks `onSearch`, `onOpen(code)`

- [ ] **Step 1: ViewModel + screen + nav host**

`UiFormat.kt`:

```kotlin
package com.burha.fundhelper.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tr = Locale("tr", "TR")

fun periodLabel(period: String?): String = when (period) {
    "1D" -> "1 gün"
    "1W" -> "1 hafta"
    "1M" -> "1 ay"
    "3M" -> "3 ay"
    "6M" -> "6 ay"
    "12M" -> "12 ay"
    "36M" -> "36 ay"
    "60M" -> "60 ay"
    else -> period.orEmpty()
}

fun formatNumber(value: Double): String =
    String.format(tr, "%.4f", value).trimEnd('0').trimEnd(',')

fun formatFetchedAt(millis: Long): String {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", tr)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)
}
```

`WatchlistViewModel.kt`:

```kotlin
package com.burha.fundhelper.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.WatchlistRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val rows: List<WatchlistRow> = emptyList(),
    val isRefreshing: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val funds: FundRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WatchlistUiState())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeWatchlist().collect { rows ->
                _state.update { it.copy(rows = rows) }
            }
        }
    }

    fun refresh(force: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, showError = false) }
            val result = funds.refreshFollowed(force)
            _state.update {
                it.copy(isRefreshing = false, showError = result.isFailure)
            }
        }
    }

    fun unfollow(code: String) {
        viewModelScope.launch { funds.unfollow(code) }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}
```

`WatchlistScreen.kt`:

```kotlin
package com.burha.fundhelper.ui.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.data.WatchlistRow
import com.burha.fundhelper.ui.formatFetchedAt
import com.burha.fundhelper.ui.formatNumber
import com.burha.fundhelper.ui.periodLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onSearch: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)

    LaunchedEffect(Unit) {
        viewModel.refresh(force = false)
    }
    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(
                message = errorText,
                actionLabel = retryText,
            )
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh(force = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watchlist_title)) },
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.rows.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.watchlist_empty_title))
                    Button(onClick = onSearch, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.watchlist_empty_cta))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.code }) { row ->
                        WatchlistRowItem(
                            row = row,
                            onOpen = onOpen,
                            onUnfollow = { viewModel.unfollow(row.code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistRowItem(
    row: WatchlistRow,
    onOpen: (String) -> Unit,
    onUnfollow: () -> Unit,
) {
    val dash = stringResource(R.string.price_missing)
    val headline = row.headlineReturn?.let { value ->
        val period = periodLabel(row.headlinePeriod)
        "$period ${formatNumber(value)}%"
    } ?: dash
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(row.code) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.code)
            Text(row.name ?: dash)
            Text("${row.price?.let(::formatNumber) ?: dash} · $headline")
            row.fetchedAt?.let { Text(formatFetchedAt(it)) }
        }
        IconButton(onClick = onUnfollow) {
            Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.unfollow))
        }
    }
}
```

`FundHelperNav.kt`:

```kotlin
package com.burha.fundhelper.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burha.fundhelper.ui.watchlist.WatchlistScreen
import com.burha.fundhelper.ui.watchlist.WatchlistViewModel

object Routes {
    const val WATCHLIST = "watchlist"
    const val SEARCH = "search"
    const val DETAIL = "detail/{fundCode}"
    fun detail(code: String) = "detail/$code"
}

@Composable
fun FundHelperNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.WATCHLIST) {
        composable(Routes.WATCHLIST) {
            val vm: WatchlistViewModel = hiltViewModel()
            WatchlistScreen(
                viewModel = vm,
                onSearch = { navController.navigate(Routes.SEARCH) },
                onOpen = { code -> navController.navigate(Routes.detail(code)) },
            )
        }
        composable(Routes.SEARCH) {
            androidx.compose.material3.Text("")
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("fundCode") { type = NavType.StringType }),
        ) {
            androidx.compose.material3.Text("")
        }
    }
}
```

Replace `MainActivity.setContent` body with:

```kotlin
FundHelperTheme {
    FundHelperNav()
}
```

and add `import com.burha.fundhelper.ui.FundHelperNav`. Delete `SkeletonLabel`.

- [ ] **Step 2: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest
```

Expected: SUCCESS. `hiltViewModel` import is `androidx.hilt.navigation.compose.hiltViewModel`. `PullToRefreshBox` is `androidx.compose.material3.pulltorefresh.PullToRefreshBox`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/ui app/src/main/java/com/burha/fundhelper/MainActivity.kt
git commit -m "feat: add watchlist screen with refresh and unfollow"
```

---

### Task 8: Search screen

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/ui/search/SearchViewModel.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt`

**Interfaces:**
- Consumes: `FundRepository.search(query, refetchCatalog)`, `follow`, `unfollow`, `observeWatchlist` (to know which result rows are followed)
- Produces: Search submit on IME Search or on-screen **Ara**; empty query copy `Kod veya fon adı yazın.`; no-results `Sonuç bulunamadı.`; error snackbar `TEFAS verisi alınamadı.` + `Yeniden dene`

- [ ] **Step 1: ViewModel and screen**

`SearchViewModel.kt`:

```kotlin
package com.burha.fundhelper.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundRepository
import com.burha.fundhelper.data.SearchOutcome
import com.burha.fundhelper.domain.FundSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val matches: List<FundSnapshot> = emptyList(),
    val followedCodes: Set<String> = emptySet(),
    val emptyQueryHint: Boolean = true,
    val noResults: Boolean = false,
    val isSearching: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val funds: FundRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeWatchlist().collect { rows ->
                _state.update { it.copy(followedCodes = rows.map { row -> row.code }.toSet()) }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun submit(refetchCatalog: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, showError = false, noResults = false) }
            when (val outcome = funds.search(_state.value.query, refetchCatalog)) {
                SearchOutcome.EmptyQuery -> _state.update {
                    it.copy(
                        isSearching = false,
                        emptyQueryHint = true,
                        matches = emptyList(),
                        noResults = false,
                    )
                }
                is SearchOutcome.Success -> _state.update {
                    it.copy(
                        isSearching = false,
                        emptyQueryHint = false,
                        matches = outcome.matches,
                        noResults = outcome.matches.isEmpty(),
                    )
                }
                is SearchOutcome.Failure -> _state.update {
                    it.copy(isSearching = false, showError = true)
                }
            }
        }
    }

    fun toggleFollow(code: String, followed: Boolean) {
        viewModelScope.launch {
            if (followed) funds.unfollow(code) else funds.follow(code)
        }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}
```

`SearchScreen.kt`:

```kotlin
package com.burha.fundhelper.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.domain.FundSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)

    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(errorText, retryText)
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.submit(refetchCatalog = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
            )
            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.search_action))
            }
            when {
                state.emptyQueryHint -> Text(
                    stringResource(R.string.search_empty_query),
                    modifier = Modifier.padding(top = 16.dp),
                )
                state.noResults -> Text(
                    stringResource(R.string.search_no_results),
                    modifier = Modifier.padding(top = 16.dp),
                )
                else -> LazyColumn(Modifier.padding(top = 16.dp)) {
                    items(state.matches, key = { it.code }) { fund ->
                        val followed = fund.code in state.followedCodes
                        SearchRow(
                            fund = fund,
                            followed = followed,
                            onOpen = onOpen,
                            onToggle = { viewModel.toggleFollow(fund.code, followed) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    fund: FundSnapshot,
    followed: Boolean,
    onOpen: (String) -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(fund.code) }
            .padding(vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(fund.code)
            Text(fund.name)
        }
        IconButton(onClick = onToggle) {
            if (followed) {
                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.unfollow))
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.follow))
            }
        }
    }
}
```

In `FundHelperNav.kt` replace the Search destination:

```kotlin
composable(Routes.SEARCH) {
    val vm: SearchViewModel = hiltViewModel()
    SearchScreen(
        viewModel = vm,
        onBack = { navController.popBackStack() },
        onOpen = { code -> navController.navigate(Routes.detail(code)) },
    )
}
```

Add imports for `SearchScreen` and `SearchViewModel`.

- [ ] **Step 2: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.burha.fundhelper.data.FundRepositoryTest
```

Expected: SUCCESS. Search must not run on each keystroke — only `submit`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/ui/search app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt
git commit -m "feat: add YAT fund search by code or name"
```

---

### Task 9: Detail screen

**Files:**
- Create: `app/src/main/java/com/burha/fundhelper/ui/detail/DetailViewModel.kt`
- Create: `app/src/main/java/com/burha/fundhelper/ui/detail/DetailScreen.kt`
- Modify: `app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt`

**Interfaces:**
- Consumes: `FundRepository.observeFund(code)`, `follow`, `unfollow`, `refreshFollowed(force)` only when `isFollowed`
- Produces: Detail UI with code, name, price, priceDate, fetchedAt, every present return in `ReturnKeys.DISPLAY_ORDER`, type, risk, fees, `explanation` from repository, and **Yatırım tavsiyesi değildir.** as its own `Text`. ViewModel does not import `ExplanationMapper`.

- [ ] **Step 1: ViewModel and screen**

`DetailViewModel.kt`:

```kotlin
package com.burha.fundhelper.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burha.fundhelper.data.FundDetail
import com.burha.fundhelper.data.FundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val fundCode: String,
    val detail: FundDetail? = null,
    val loaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val showError: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val funds: FundRepository,
) : ViewModel() {
    private val fundCode: String = checkNotNull(savedStateHandle["fundCode"])
    private val _state = MutableStateFlow(DetailUiState(fundCode = fundCode))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            funds.observeFund(fundCode).collect { detail ->
                _state.update { it.copy(detail = detail, loaded = true) }
            }
        }
    }

    fun refresh(force: Boolean) {
        val followed = _state.value.detail?.isFollowed == true
        if (!followed) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, showError = false) }
            val result = funds.refreshFollowed(force)
            _state.update { it.copy(isRefreshing = false, showError = result.isFailure) }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val followed = _state.value.detail?.isFollowed == true
            if (followed) funds.unfollow(fundCode) else funds.follow(fundCode)
        }
    }

    fun consumeError() {
        _state.update { it.copy(showError = false) }
    }
}
```

`DetailScreen.kt`:

```kotlin
package com.burha.fundhelper.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burha.fundhelper.R
import com.burha.fundhelper.domain.ReturnKeys
import com.burha.fundhelper.ui.formatFetchedAt
import com.burha.fundhelper.ui.formatNumber
import com.burha.fundhelper.ui.periodLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = stringResource(R.string.fetch_error)
    val retryText = stringResource(R.string.retry)
    val dash = stringResource(R.string.price_missing)

    LaunchedEffect(state.detail?.isFollowed) {
        if (state.detail?.isFollowed == true) {
            viewModel.refresh(force = false)
        }
    }
    LaunchedEffect(state.showError) {
        if (state.showError) {
            val result = snackbarHostState.showSnackbar(errorText, retryText)
            viewModel.consumeError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh(force = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(force = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val detail = state.detail
            if (state.loaded && detail == null) {
                Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.detail_missing))
                    Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.back))
                    }
                }
            } else if (detail != null) {
                val snapshot = detail.snapshot
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Text(snapshot.code)
                    Text(snapshot.name)
                    Text(snapshot.price?.let { formatNumber(it) } ?: dash)
                    snapshot.priceDate?.let { Text(it) }
                    Text(formatFetchedAt(snapshot.fetchedAt))
                    ReturnKeys.DISPLAY_ORDER.forEach { key ->
                        val value = snapshot.returns[key] ?: return@forEach
                        Text("${periodLabel(key)}: ${formatNumber(value)}%")
                    }
                    Text(snapshot.fundType ?: stringResource(R.string.missing_field))
                    Text(snapshot.risk ?: stringResource(R.string.missing_field))
                    if (snapshot.fees.isEmpty()) {
                        Text(stringResource(R.string.missing_field))
                    } else {
                        snapshot.fees.forEach { Text("${it.label}: ${it.value}") }
                    }
                    Text(detail.explanation, modifier = Modifier.padding(top = 16.dp))
                    Text(stringResource(R.string.disclaimer), modifier = Modifier.padding(top = 16.dp))
                    Button(
                        onClick = viewModel::toggleFollow,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(
                            if (detail.isFollowed) stringResource(R.string.unfollow)
                            else stringResource(R.string.follow),
                        )
                    }
                }
            }
        }
    }
}
```

Disclaimer `Text` is a separate composable call from `detail.explanation`. Do not concatenate them.

Replace the Detail destination in `FundHelperNav.kt`:

```kotlin
composable(
    route = Routes.DETAIL,
    arguments = listOf(navArgument("fundCode") { type = NavType.StringType }),
) {
    val vm: DetailViewModel = hiltViewModel()
    DetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
```

- [ ] **Step 2: Compile and run unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: all unit tests PASS (mapper, JSON fixtures, endpoints, repository).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burha/fundhelper/ui/detail app/src/main/java/com/burha/fundhelper/ui/FundHelperNav.kt
git commit -m "feat: add fund detail with official-field explanation"
```

---

### Task 10: Debug APK on the Samsung A23

**Files:**
- Modify: `progress.md` (pruned handoff only; no diary)

**Interfaces:**
- Consumes: Tasks 1–9 compiling app; `.cursor/skills/sideload-a23/SKILL.md`
- Produces: installed package `com.burha.fundhelper` on the A23; `progress.md` records what works and any TEFAS/Akamai blocker

- [ ] **Step 1: Run the full unit-test suite again**

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 0 failed tests.

- [ ] **Step 2: Assemble debug**

```powershell
.\gradlew.bat assembleDebug
```

Expected: `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Sideload and confirm the package**

Follow `.cursor/skills/sideload-a23/SKILL.md` exactly:

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm path com.burha.fundhelper
```

Expected: A23 listed as `device`; `pm path` prints a `package:` path. Manual smoke on device: empty watchlist copy, search submit, follow, kill process, reopen (follows remain), turn airplane mode on and confirm snackbar + list still there, Detail disclaimer visible. If TEFAS returns a challenge, that is an expected client-isolation risk — keep UI, note it in `progress.md`, do not add a backend.

- [ ] **Step 4: Prune `progress.md`**

Replace the file with a short handoff: APK installed or not, unit tests passing, A23 smoke result, TEFAS blocker if any. Next milestone is Play Store (out of this plan).

- [ ] **Step 5: Commit**

```bash
git add progress.md
git commit -m "docs: record A23 sideload status for v1"
```

Do not `bundleRelease` or upload to Play.

---

## Self-review

**Spec coverage**

| Spec section | Task |
|--------------|------|
| Gradle skeleton, Hilt, Room deps, Turkish, `com.burha.fundhelper`, SDK 26/36, INTERNET | 1 |
| Domain snapshot + ExplanationMapper tests | 2 |
| 2026 `/api/funds/` JSON, not BindHistory, fixture mapping, HTML failure | 3–4 |
| Room follows + snapshots; unfollow keeps snapshot | 5 |
| Repository follow/search/refresh-followed-only/failure keeps data | 6 |
| Watchlist empty/error/offline, pull-to-refresh, 5-minute throttle | 6–7 |
| Search YAT, submit-only, matching rules, snackbar | 8 |
| Detail type/risk/fees, mapper paragraph, disclaimer, no refresh if unfollowed | 9 |
| `assembleDebug` + A23 `adb install` | 10 |
| No Play, charts, holdings, LLM, backend, WorkManager | omitted on purpose |

**Type consistency:** `TefasClient.fetchYatCatalog` / `fetchLatestYatPrices` → `FundSnapshot`; repository `SearchOutcome` / `WatchlistRow` / `FundDetail`; ViewModels depend only on `FundRepository`.

**Placeholders:** none remaining. Compose BOM is pinned to `2026.06.00`.

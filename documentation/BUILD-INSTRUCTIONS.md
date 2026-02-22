# Build Instructions

## TradeSketch Estimator
Updated: February 21, 2026

---

## 1) Prerequisites

- JDK 17+
- Android SDK Platform 35 + build tools
- Android Studio (recent stable)
- ADB (for optional device testing)

Project baseline:

- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 26`

---

## 2) Script-First Workflow (Windows)

From repo root:

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

What this covers:

- Environment checks
- Keystore generation/signing config setup
- Unit tests + lint + release AAB build

---

## 3) Direct Gradle Commands

Debug build:

```powershell
./gradlew.bat assembleDebug
```

Automated validation:

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
```

Release bundle:

```powershell
./gradlew.bat :app:bundleRelease
```

Expected release output:

- `app/build/outputs/bundle/release/app-release.aab`

---

## 4) Signing Configuration

Release signing values are read from environment variables or `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Important:

- `scripts/03-build-release.ps1` now fails if signing keys are missing/invalid.
- The release script does not auto-generate fallback signing keys for Play builds.

---

## 5) Release Build Behavior

In `app/build.gradle.kts`, release shrinking is currently disabled:

- `isMinifyEnabled = false`
- `isShrinkResources = false`

This is allowed for Play submission, but update docs/scripts together if enabling shrink in future.

---

## 6) Optional Device Install (Debug APK)

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tradesketch.estimator/.MainActivity
```

---

## 7) Troubleshooting

### Signing failure

- Re-run `./scripts/02-generate-keystore.ps1`
- Validate keys in `local.properties`
- Ensure `KEYSTORE_FILE` path and alias/password values are correct

### Gradle dependency issues

```powershell
./gradlew.bat --refresh-dependencies
```

### ADB shows no devices

```powershell
adb kill-server
adb start-server
adb devices -l
```

---

## 8) Keep in Sync

When SDK/signing/release behavior changes, update these in the same PR:

- `documentation/BUILD-INSTRUCTIONS.md`
- `documentation/COMPLIANCE-CHECKLIST.md`
- `documentation/SUBMISSION-GUIDE.md`
- `PLAY-STORE-LAUNCH-GUIDE.md`

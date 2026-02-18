# Build Instructions

## TradeSketch Estimator
Updated: February 11, 2026

This document reflects the current build setup in this repository.

---

## 1) Prerequisites

- JDK 17+
- Android SDK with Platform 35 installed
- Android Studio (recent stable)
- ADB available for device install/testing

Project baseline:

- `compileSdk = 35`
- `targetSdk = 35`
- `minSdk = 26`

---

## 2) Recommended Script-First Workflow (Windows)

From repo root:

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

What these scripts cover:

- Environment checks
- Keystore generation/signing variable setup
- Unit tests + lint + release AAB build

---

## 3) Direct Gradle Commands

Debug build:

```powershell
./gradlew.bat assembleDebug
```

Unit tests:

```powershell
./gradlew.bat :app:testDebugUnitTest
```

Lint:

```powershell
./gradlew.bat :app:lint
```

Release bundle:

```powershell
./gradlew.bat :app:bundleRelease
```

Desktop build (separate pipeline):

```powershell
./gradlew.bat :desktop:build
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

If these are missing, release build cannot produce a signed AAB.

---

## 5) Install Debug Build to Device

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tradesketch.estimator/.MainActivity
```

---

## 6) Current Release Build Behavior

In `app/build.gradle.kts`, release minification/shrinking is currently disabled:

- `isMinifyEnabled = false`
- `isShrinkResources = false`

This is acceptable for Play submission, but if you enable shrinking later, update this file and release docs/scripts together.

---

## 7) Troubleshooting

### ADB shows no device
- Reconnect USB
- Ensure USB debugging is enabled
- Accept RSA prompt on phone
- Run `adb kill-server && adb start-server`

### Build fails due to signing
- Re-run `./scripts/02-generate-keystore.ps1`
- Verify signing env vars

### Gradle dependency issues
- `./gradlew.bat --refresh-dependencies`
- Verify internet access and SDK setup

---

## 8) Keep In Sync

Whenever SDK levels, signing flow, or build type flags change, update:

- `documentation/BUILD-INSTRUCTIONS.md`
- `documentation/COMPLIANCE-CHECKLIST.md`
- `documentation/SUBMISSION-GUIDE.md`
- Relevant scripts under `scripts/`

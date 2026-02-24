# TradeSketch Estimator

Offline, blueprint-first material takeoffs for skilled trades.

## Current Release Snapshot (Audited: February 21, 2026)

- Package: `com.tradesketch.estimator`
- Version: `1.0.3` (`versionCode = 5`)
- SDK levels: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Output artifact: `app/build/outputs/bundle/release/app-release.aab`

## Product Flow (Current UI)

1. Welcome screen (`WelcomeScreenPro`)
2. Project setup (name + primary trade)
3. Workspace with rail tabs:
   - Blueprint
   - Materials
   - Quantities
   - Export
   - Settings/About

Key capabilities:

- Blueprint drafting with wall, door, window, and stair placement
- Multi-floor planning with Ground, upper floors, and basement levels
- Trade-specific takeoffs: drywall, concrete, gravel/mulch, paint
- Quantity and pricing parameter controls
- Export/share as PDF, CSV, JSON, or text intent
- Local project persistence with DataStore

## Privacy and Policy Posture

- No `INTERNET` permission in manifest
- No analytics or crash-reporting SDKs
- No account/login requirement
- `android:allowBackup="false"` to keep project data local-only by default

## Build and Validation

Windows (PowerShell):

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
```

Scripted release path:

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

## Signing Configuration

Release signing values are resolved from environment variables first, then `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

`./scripts/03-build-release.ps1` now fails fast if signing values are missing or invalid. It does not auto-generate fallback keys for Play builds.

## Play-Store Docs

- Launch runbook: `PLAY-STORE-LAUNCH-GUIDE.md`
- Submission guide: `documentation/SUBMISSION-GUIDE.md`
- Compliance checklist: `documentation/COMPLIANCE-CHECKLIST.md`
- Testing checklist: `documentation/TESTING-NOTES.md`
- Build details: `documentation/BUILD-INSTRUCTIONS.md`
- Audit report: `documentation/ANDROID-AUDIT-2026-02-21.md`

## Disclaimer

TradeSketch provides estimate-only material quantities. Always verify site conditions, local code requirements, and supplier pricing before purchase or build execution.

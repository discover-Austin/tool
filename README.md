# TradeSketch Estimator

Offline-first estimator and blueprint maker for drywall, concrete, paint, and gravel/mulch workflows, including dedicated `Measured Arc` and `Sketch Curve` blueprint tools.

## Current Release Snapshot

- Package: `com.tradesketch.estimator`
- Version: `1.0.22` (`versionCode = 24`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Release checklist: `documentation/COMPLIANCE-CHECKLIST.md`

## Product Flow

1. Welcome + project ritual
2. Workspace tabs:
   - Blueprint
   - Materials
   - Export
   - Settings
3. Export/share:
   - Estimate PDF
   - Blueprint PNG/PDF
   - CSV and JSON

## Build Commands (Windows PowerShell)

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
```

Release script:

```powershell
./scripts/03-build-release.ps1
```

## Privacy Posture

- No `INTERNET` permission in manifest
- No analytics/crash-reporting SDKs
- Offline-first local storage model

## Release Docs

- Launch guide: `PLAY-STORE-LAUNCH-GUIDE.md`
- Submission guide: `documentation/SUBMISSION-GUIDE.md`
- Compliance checklist: `documentation/COMPLIANCE-CHECKLIST.md`
- Build instructions: `documentation/BUILD-INSTRUCTIONS.md`
- Historical audit: `documentation/PLAY_READY_AUDIT_2026-03-24.md`



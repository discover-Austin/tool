# TradeSketch Estimator - Play Store Launch Guide

Last updated: March 9, 2026

Use this on release day with `RELEASE_STATUS.md` as source of truth.

## 1) Verify Build Identity

- Package: `com.tradesketch.estimator`
- Version: `1.0.8` (`versionCode = 10`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`

## 2) Run Release Gates

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
```

Or scripted:

```powershell
./scripts/03-build-release.ps1
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`

## 3) Signing Requirements

Required values via env vars or `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## 4) Device Smoke Checklist

- [ ] Welcome -> project ritual -> workspace path works on first run.
- [ ] Blueprint editing, room detection, undo/redo, and floor switching are stable.
- [ ] Materials tab recalculates correctly for each supported trade.
- [ ] Export tab can share/save estimate PDF, blueprint PNG/PDF, CSV, and JSON.
- [ ] Settings changes persist after app restart.
- [ ] Core flows still work offline (airplane mode).

## 5) Play Console Submission

1. Open Play Console app `com.tradesketch.estimator`.
2. Upload `app-release.aab` in Production track.
3. Confirm parsed version is `versionCode 10` / `versionName 1.0.8`.
4. Upload listing assets from `store-assets/`.
5. Paste release notes from `store-assets/listing/whats-new.txt`.
6. Verify screenshots are freshly captured from the current UI build.
7. Verify Data Safety and Privacy Policy URL before rollout.

## 6) Keep In Sync

If build/signing/version behavior changes, update these files in the same PR:

- `RELEASE_STATUS.md`
- `documentation/SUBMISSION-GUIDE.md`
- `documentation/COMPLIANCE-CHECKLIST.md`
- `documentation/BUILD-INSTRUCTIONS.md`



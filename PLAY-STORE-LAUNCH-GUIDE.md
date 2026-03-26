# TradeSketch Estimator - Play Store Launch Guide

Last updated: March 25, 2026

Use this on release day with `RELEASE_STATUS.md` as source of truth.

## 1) Verify Build Identity

- Package: `com.tradesketch.estimator`
- Version: `1.0.19` (`versionCode = 21`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`

## 2) Run Release Gates

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
./gradlew.bat :app:assembleSideload
```

Or scripted:

```powershell
./scripts/03-build-release.ps1
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/sideload/app-sideload.apk`
- Record the fresh bundle timestamp in `RELEASE_STATUS.md` after this build completes.
- Record the fresh SHA-256 in `RELEASE_STATUS.md` after this build completes.

## 2b) Freeze The Release Snapshot

- Use the newest signed artifacts only. Do not mix screenshots or video from older builds.
- Upload bundle: `app/build/outputs/bundle/release/app-release.aab`
- Capture package: `com.tradesketch.estimator.local`
- Capture activity: `com.tradesketch.estimator.local/com.tradesketch.estimator.MainActivity`
- Capture source: signed sideload build from the exact same commit/version as the uploaded bundle
- Replace all 8 screenshots and the 30-second showcase video together whenever the version changes.

## 3) Signing Requirements

Required values via env vars or `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## 4) Device Smoke Checklist

- [ ] Welcome -> project ritual -> workspace path works on first run.
- [ ] Blueprint editing, measured arc/sketch curve tools, room detection, undo/redo, and floor switching are stable.
- [ ] Materials tab recalculates correctly for each supported trade.
- [ ] Export tab can share/save estimate PDF, blueprint PNG/PDF, CSV, and JSON.
- [ ] Settings changes persist after app restart.
- [ ] Core flows still work offline (airplane mode).

## 5) Play Console Submission

1. Open Play Console and prepare the production app `com.tradesketch.estimator`.
2. Upload `app-release.aab` in Production track.
3. Confirm parsed version is `versionCode 21` / `versionName 1.0.19`.
4. Upload listing assets from `store-assets/`.
5. Upload all 8 fresh phone screenshots from the newest signed sideload build only.
6. Add the preview video by pasting the YouTube URL for the newest 30-second narrated showcase.
7. Paste release notes from `store-assets/listing/whats-new.txt`.
8. Verify Data Safety and Privacy Policy URL before rollout.

## 6) Keep In Sync

If build/signing/version behavior changes, update these files in the same PR:

- `RELEASE_STATUS.md`
- `documentation/SUBMISSION-GUIDE.md`
- `documentation/COMPLIANCE-CHECKLIST.md`
- `documentation/BUILD-INSTRUCTIONS.md`



# TradeSketch Estimator - Play Store Launch Guide (Windows)

Last updated: February 21, 2026

This guide reflects the current app architecture and release pipeline in this repo.

## 0) Verified Baseline (From Current Audit)

- Package: `com.tradesketch.estimator`
- Version: `1.0.3` (`versionCode = 5`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Automated checks passed:
  - `:app:testDebugUnitTest`
  - `:core:test`
  - `:app:lint`
  - `:app:lintRelease`
  - `:app:bundleRelease`
- AAB output: `app/build/outputs/bundle/release/app-release.aab`

## 1) Prerequisites

- JDK 17+
- Android SDK platform 35 + build tools
- PowerShell
- Google Play Console developer account

Check locally:

```powershell
./scripts/01-check-prerequisites.ps1
```

## 2) Signing Setup (Required)

If you do not already have a production keystore:

```powershell
./scripts/02-generate-keystore.ps1
```

Release signing values must exist via env vars or `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

`./scripts/03-build-release.ps1` is intentionally strict for Play readiness and will fail if signing values are missing/invalid.

## 3) Build a Play-Ready Signed Bundle

```powershell
./scripts/03-build-release.ps1
```

Manual equivalent:

```powershell
./gradlew.bat :app:testDebugUnitTest :core:test :app:lint :app:lintRelease :app:bundleRelease
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`

Optional verification:

```powershell
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

## 4) Smoke Test Checklist (Current UI)

Run this on the exact commit used for release:

- [ ] App opens to Welcome on first run.
- [ ] Project Ritual step 1 accepts a project name.
- [ ] Project Ritual step 2 selects a primary trade and continues.
- [ ] Workspace opens and rail tabs navigate: Blueprint, Materials, Quantities, Export, Settings/About.
- [ ] Blueprint: add/edit walls and openings without crashes.
- [ ] Materials/Quantities: switching estimate type updates results.
- [ ] Export: share report and save CSV/PDF/JSON via SAF.
- [ ] Settings: adjust values and verify they persist after app restart.
- [ ] App remains functional in airplane mode.

## 5) Store Assets and Content

Listing files:

- `store-assets/listing/title.txt`
- `store-assets/listing/short-description.txt`
- `store-assets/listing/full-description.txt`
- `store-assets/listing/whats-new.txt`
- `store-assets/listing/category.txt`

Legal files:

- `store-assets/legal/privacy-policy.html`
- `store-assets/legal/open-source-licenses.txt`
- `store-assets/listing/data-safety-answers.txt`
- `store-assets/listing/content-rating-answers.txt`
- Hosted privacy URL: `store-assets/PRIVACY_POLICY_URL.txt`

Graphics:

- Icon: `store-assets/graphics/ic_launcher_512.png`
- Feature graphic: `store-assets/graphics/feature_graphic_1024x500.png`
- Screenshots: `store-assets/screenshots/*.png`

## 6) Play Console Submission Steps

1. Create/select app in Play Console (`com.tradesketch.estimator`).
2. Complete store listing fields with files from `store-assets/listing/`.
3. Upload icon, feature graphic, and screenshots.
4. Complete Data safety using `store-assets/listing/data-safety-answers.txt`.
5. Complete Content rating using `store-assets/listing/content-rating-answers.txt`.
6. Set pricing/distribution.
7. Go to Release > Production > Create new release.
8. Upload `app-release.aab`.
9. Confirm Play parses `versionCode = 5` / `versionName = 1.0.3` for this build.
10. Paste release notes from `store-assets/listing/whats-new.txt`.
11. Review and start rollout.

## 7) Post-Upload Gate

Before pressing rollout:

- [ ] Privacy policy URL is public and reachable.
- [ ] Data safety answers match app behavior (offline, no analytics SDKs, no network permission).
- [ ] Screenshots represent the current blueprint-first UI.
- [ ] No lint/test/build failures on release commit.

## 8) Updating for Next Release

1. Bump version:

```powershell
./scripts/07-bump-version.ps1
```

2. Update `store-assets/listing/whats-new.txt`.
3. Rebuild signed AAB with `./scripts/03-build-release.ps1`.
4. Upload new AAB in Play Console production track.

## 9) Troubleshooting

### Signing failure

- Re-run `./scripts/02-generate-keystore.ps1`.
- Confirm key values in `local.properties` (or env vars).
- Confirm keystore path exists and alias/password are correct.

### Build/toolchain failure

- Verify JDK 17 and SDK 35 are installed.
- Run `./gradlew.bat --refresh-dependencies`.
- Re-run prerequisite check script.

### ADB/device issues

```powershell
adb kill-server
adb start-server
adb devices -l
```

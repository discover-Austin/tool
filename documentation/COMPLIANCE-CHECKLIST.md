# Play Store Compliance Checklist

## TradeSketch Estimator (Audit Date: February 12, 2026)

This checklist is the source of truth for Play submission readiness in this repo.

---

## 1) Platform + Policy Basics

- [x] `applicationId` set: `com.tradesketch.estimator`
- [x] `minSdk = 26`
- [x] `targetSdk = 35` (Android 15)
- [x] No dangerous runtime permissions declared in `AndroidManifest.xml`
- [x] App is offline-first and does not require account sign-in
- [x] Ads declaration should be **No**

---

## 2) Privacy + Legal

- [x] Privacy policy file exists: `store-assets/legal/privacy-policy.html`
- [x] Public privacy URL documented: `store-assets/PRIVACY_POLICY_URL.txt`
- [x] OSS attribution file exists: `store-assets/legal/open-source-licenses.txt`
- [x] Data safety answers prepared: `store-assets/listing/data-safety-answers.txt`
- [x] Content rating answers prepared: `store-assets/listing/content-rating-answers.txt`
- [ ] In-app privacy policy viewer/link implemented (recommended, not hard blocker for Play listing)

---

## 3) Store Listing Content

- [x] Title prepared: `store-assets/listing/title.txt`
- [x] Short description prepared: `store-assets/listing/short-description.txt`
- [x] Full description prepared: `store-assets/listing/full-description.txt`
- [x] What's New prepared: `store-assets/listing/whats-new.txt`
- [x] Category prepared: `store-assets/listing/category.txt`

---

## 4) Visual Assets

- [x] 6 phone screenshots present:
  - `store-assets/screenshots/01_projects.png`
  - `store-assets/screenshots/02_spaces.png`
  - `store-assets/screenshots/03_editor.png`
  - `store-assets/screenshots/04_drywall.png`
  - `store-assets/screenshots/05_concrete.png`
  - `store-assets/screenshots/06_export.png`
- [x] 512x512 Play icon present at `store-assets/graphics/ic_launcher_512.png`
- [x] 1024x500 feature graphic present at `store-assets/graphics/feature_graphic_1024x500.png`

---

## 5) Build + Release Artifacts

- [x] Prereq script available: `scripts/01-check-prerequisites.ps1`
- [x] Keystore setup script available: `scripts/02-generate-keystore.ps1`
- [x] Release build script available: `scripts/03-build-release.ps1`
- [x] Signed release AAB generated:
  - Expected output: `app/build/outputs/bundle/release/app-release.aab`
  - Last verified build: February 11, 2026 11:12 PM (local build timestamp)
- [ ] Release AAB upload-tested in Play Console

---

## 6) Functional Readiness Before Submission

- [x] App launches clean on device
- [x] Project workspace + 3D builder operational
- [x] Quick Room wizard operational
- [x] Trade-separated takeoff workflows operational
- [x] Export screen operational
- [ ] Final manual smoke pass completed on release build (`bundleRelease` output)

---

## Submission Blockers (Must Be Done)

1. Upload-test the latest signed `app-release.aab` in Play Console.
2. Run final manual smoke pass on the exact release build.

## Notes

- `targetSdk` in this project is **35**, not 34.
- Release shrinking/obfuscation behavior is controlled in `app/build.gradle.kts` and currently uses non-minified release settings.
- Keep this checklist updated whenever build config, permissions, or listing assets change.

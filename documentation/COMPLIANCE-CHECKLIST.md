# Play Store Compliance Checklist

## TradeSketch Estimator (Audit Date: February 21, 2026)

This file is the release-readiness checklist for current code in this repo.

---

## 1) Platform and Policy Basics

- [x] `applicationId`: `com.tradesketch.estimator`
- [x] `minSdk = 26`
- [x] `targetSdk = 35`
- [x] `compileSdk = 35`
- [x] No dangerous runtime permissions declared
- [x] No `INTERNET` permission declared
- [x] Offline-first usage model (no account required)

---

## 2) Privacy and Data Handling

- [x] Privacy policy file present: `store-assets/legal/privacy-policy.html`
- [x] Hosted privacy URL recorded: `store-assets/PRIVACY_POLICY_URL.txt`
- [x] `android:allowBackup` disabled in manifest
- [x] No analytics/crash SDK dependencies in `app/build.gradle.kts`
- [x] Data safety answer file prepared: `store-assets/listing/data-safety-answers.txt`
- [x] Content rating answer file prepared: `store-assets/listing/content-rating-answers.txt`

---

## 3) Store Listing Assets

- [x] Title: `store-assets/listing/title.txt`
- [x] Short description: `store-assets/listing/short-description.txt`
- [x] Full description: `store-assets/listing/full-description.txt`
- [x] What's new: `store-assets/listing/whats-new.txt`
- [x] Category: `store-assets/listing/category.txt`
- [x] App icon: `store-assets/graphics/ic_launcher_512.png`
- [x] Feature graphic: `store-assets/graphics/feature_graphic_1024x500.png`
- [x] Screenshot set present: `store-assets/screenshots/*.png`

---

## 4) Build and Signing

- [x] Keystore workflow script available: `scripts/02-generate-keystore.ps1`
- [x] Release script available: `scripts/03-build-release.ps1`
- [x] Release script fails fast when signing config is missing/invalid
- [x] Current signed AAB generated: `app/build/outputs/bundle/release/app-release.aab`
- [x] Current version in build config: `versionCode = 5`, `versionName = "1.0.3"`

---

## 5) Validation Status (Current Audit)

- [x] `:app:testDebugUnitTest` passed
- [x] `:core:test` passed
- [x] `:app:lint` passed
- [x] `:app:lintRelease` passed
- [x] `:app:bundleRelease` passed

---

## 6) Manual Submission Gates (Do Before Rollout)

- [ ] Final smoke run on the same commit as uploaded AAB
- [ ] Verify Play Console parsed version matches expected release
- [ ] Confirm screenshots and listing text match current UI/behavior
- [ ] Confirm production privacy URL resolves publicly
- [ ] Upload-tested in Play Console production track

---

## 7) Source of Truth

For exact audited build outputs and fingerprints, see:

- `documentation/ANDROID-AUDIT-2026-02-21.md`

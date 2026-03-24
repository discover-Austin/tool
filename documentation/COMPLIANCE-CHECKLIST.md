# Play Store Compliance Checklist

Updated: March 23, 2026

## Platform

- [x] `applicationId = com.tradesketch.estimator`
- [x] `minSdk = 26`
- [x] `targetSdk = 35`
- [x] `compileSdk = 35`
- [x] Current release version is `versionCode = 20`, `versionName = "1.0.18"`

## Privacy / Data Safety

- [x] No `INTERNET` permission in manifest
- [x] No analytics/crash SDK dependency configured
- [x] Offline-first local data model
- [x] Privacy policy file exists: `store-assets/legal/privacy-policy.html`
- [x] Hosted privacy URL source tracked: `store-assets/PRIVACY_POLICY_URL.txt`
- [x] Data safety answers file exists: `store-assets/listing/data-safety-answers.txt`

## Store Listing Assets

- [x] Listing text files present in `store-assets/listing/`
- [x] Feature graphic and icon present in `store-assets/graphics/`
- [x] Screenshot set present in `store-assets/screenshots/`

## Build / Quality Gates

- [x] `:app:testDebugUnitTest`
- [x] `:core:test`
- [x] `:app:lint`
- [x] `:app:lintRelease`
- [ ] `:app:bundleRelease` on the current worktree

## Manual Pre-Rollout Checks

- [ ] Validate smoke flow on the exact upload commit
- [ ] Confirm Play Console parses `versionCode 20` and `versionName 1.0.18`
- [ ] Confirm screenshots/listing text match current UI
- [x] Confirm privacy policy URL is publicly reachable



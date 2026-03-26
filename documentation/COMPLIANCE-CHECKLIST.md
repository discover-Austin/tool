# Play Store Compliance Checklist

Updated: March 25, 2026

## Platform

- [x] `applicationId = com.tradesketch.estimator`
- [x] `minSdk = 26`
- [x] `targetSdk = 35`
- [x] `compileSdk = 35`
- [x] Current release version is `versionCode = 21`, `versionName = "1.0.19"`

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
- [x] All 8 screenshots refreshed from the newest signed sideload build only
- [x] 30-second narrated showcase refreshed from the newest signed sideload build only
- [x] Showcase promo is frozen to the latest verified screenshot set for deterministic review consistency

## Build / Quality Gates

- [x] `:app:testDebugUnitTest`
- [x] `:core:test`
- [x] `:app:lint`
- [x] `:app:lintRelease`
- [x] `:app:bundleRelease` on the current worktree
- [x] `:app:assembleSideload` on the current worktree

## Manual Pre-Rollout Checks

- [ ] Validate smoke flow on the exact upload commit
- [ ] Confirm Play Console parses `versionCode 21` and `versionName 1.0.19`
- [x] Confirm screenshots/video/listing text all match the newest UI build
- [x] Confirm privacy policy URL is publicly reachable



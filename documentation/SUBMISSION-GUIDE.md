# Google Play Submission Guide

Updated: April 13, 2026

Use together with `documentation/COMPLIANCE-CHECKLIST.md` and `README.md`.

## Build Gate (Must Pass)

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
./gradlew.bat :app:assembleSideload
```

Expected output:

- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/sideload/app-sideload.apk`
- Record the bundle timestamp after `:app:bundleRelease` completes on the release commit.
- Record the bundle SHA-256 after `:app:bundleRelease` completes on the release commit.
- Refresh all 8 screenshots and the 30-second showcase from the newest signed sideload build on the same commit.

Expected metadata:

- Package: `com.tradesketch.estimator`
- Version: `1.0.22` (`versionCode = 24`)

## Signing Gate

Configure one of:

- Environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Or `local.properties` entries with same keys

## Store Asset Checklist

- Listing text: `store-assets/listing/*.txt`
- Legal: `store-assets/legal/privacy-policy.html`, `store-assets/legal/open-source-licenses.txt`
- Policy responses: `store-assets/listing/data-safety-answers.txt`, `store-assets/listing/content-rating-answers.txt`
- Graphics/screenshots: `store-assets/graphics/*`, `store-assets/screenshots/*`
- Showcase video: `media/play_store_showcase/tradesketch_showcase_play_store_30s.mp4`

## Submission Steps

1. Upload `app-release.aab` to Production.
2. Confirm parsed version is `24 / 1.0.22`.
3. Apply release notes from `store-assets/listing/whats-new.txt`.
4. Upload the newest screenshot set and the newest preview video that were captured from `com.tradesketch.estimator.local`.
5. Complete listing, privacy, data safety, and content rating sections.
6. Run final rollout checklist and start rollout.




# Google Play Submission Guide

Updated: March 23, 2026

Use together with `RELEASE_STATUS.md`.

## Build Gate (Must Pass)

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
```

Expected output:

- `app/build/outputs/bundle/release/app-release.aab`
- Record the bundle timestamp in `RELEASE_STATUS.md` after `:app:bundleRelease` completes on the release commit.
- Record the bundle SHA-256 in `RELEASE_STATUS.md` after `:app:bundleRelease` completes on the release commit.

Expected metadata:

- Package: `com.tradesketch.estimator`
- Version: `1.0.18` (`versionCode = 20`)

## Signing Gate

Configure one of:

- Environment variables: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Or `local.properties` entries with same keys

## Store Asset Checklist

- Listing text: `store-assets/listing/*.txt`
- Legal: `store-assets/legal/privacy-policy.html`, `store-assets/legal/open-source-licenses.txt`
- Policy responses: `store-assets/listing/data-safety-answers.txt`, `store-assets/listing/content-rating-answers.txt`
- Graphics/screenshots: `store-assets/graphics/*`, `store-assets/screenshots/*`

## Submission Steps

1. Upload `app-release.aab` to Production.
2. Confirm parsed version is `20 / 1.0.18`.
3. Apply release notes from `store-assets/listing/whats-new.txt`.
4. Complete listing, privacy, data safety, and content rating sections.
5. Run final rollout checklist and start rollout.




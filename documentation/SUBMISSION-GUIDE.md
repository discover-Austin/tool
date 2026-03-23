# Google Play Submission Guide

Updated: March 22, 2026

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
- Latest locally verified build: `2026-03-22 02:10:04 -04:00`
- Latest locally verified SHA-256: `C7E97313D3DBD3728F46A5FB376B5562CC1DDCAA24625858EFF78487A87CA637`

Expected metadata:

- Package: `com.tradesketch.estimator`
- Version: `1.0.16` (`versionCode = 18`)

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
2. Confirm parsed version is `18 / 1.0.16`.
3. Apply release notes from `store-assets/listing/whats-new.txt`.
4. Complete listing, privacy, data safety, and content rating sections.
5. Run final rollout checklist and start rollout.




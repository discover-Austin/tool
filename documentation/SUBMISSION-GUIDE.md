# Google Play Submission Guide

## TradeSketch Estimator
Updated: February 21, 2026

Use this as the concise checklist for submission day.

---

## 1) Build Gate (Must Pass)

From repo root:

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
./gradlew.bat :app:bundleRelease
```

Expected artifact:

- `app/build/outputs/bundle/release/app-release.aab`

Expected metadata for current release:

- Package: `com.tradesketch.estimator`
- Version: `1.0.3` (`versionCode = 5`)

---

## 2) Signing Gate

Release signing values must be configured in env vars or `local.properties`:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Recommended helper scripts:

```powershell
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

---

## 3) Required Store Files

### Listing Text

- `store-assets/listing/title.txt`
- `store-assets/listing/short-description.txt`
- `store-assets/listing/full-description.txt`
- `store-assets/listing/whats-new.txt`
- `store-assets/listing/category.txt`

### Legal / Policy

- `store-assets/legal/privacy-policy.html`
- `store-assets/legal/open-source-licenses.txt`
- `store-assets/listing/data-safety-answers.txt`
- `store-assets/listing/content-rating-answers.txt`
- Hosted URL: `store-assets/PRIVACY_POLICY_URL.txt`

### Graphics

- Icon: `store-assets/graphics/ic_launcher_512.png`
- Feature graphic: `store-assets/graphics/feature_graphic_1024x500.png`
- Screenshots: `store-assets/screenshots/*.png`

---

## 4) Play Console Steps

1. Open Play Console and select/create app `com.tradesketch.estimator`.
2. Fill Store listing from `store-assets/listing/` files.
3. Upload icon/feature graphic/screenshots.
4. Complete Data safety and Content rating forms from prepared answer files.
5. Open Release > Production > Create new release.
6. Upload `app-release.aab`.
7. Confirm parsed version matches current build (`5` / `1.0.3`).
8. Paste release notes from `store-assets/listing/whats-new.txt`.
9. Review and roll out.

---

## 5) Final Submit Checklist

- [ ] AAB exists and is signed.
- [ ] Tests/lint/build all passed on release commit.
- [ ] Privacy policy URL is publicly accessible.
- [ ] Data safety declarations match current code behavior.
- [ ] Screenshots reflect current app UI (welcome + blueprint-first workspace).
- [ ] No placeholder or stale version text in listing fields.

---

## 6) Notes

- App is offline-first and has no `INTERNET` permission.
- `android:allowBackup` is disabled for local-only data posture.
- If release flow changes, update this file and `documentation/COMPLIANCE-CHECKLIST.md` in the same change.

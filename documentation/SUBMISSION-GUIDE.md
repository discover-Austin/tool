# Google Play Submission Guide

## TradeSketch Estimator
Updated: February 12, 2026

This guide is aligned with the current repo structure and scripts.

---

## 1) Release Prep (Local)

Run from repo root on Windows PowerShell:

```powershell
./scripts/01-check-prerequisites.ps1
./scripts/02-generate-keystore.ps1
./scripts/03-build-release.ps1
```

Expected release artifact:

- `app/build/outputs/bundle/release/app-release.aab`

Recommended quick validation after build:

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat lint
```

---

## 2) Required Store Files (Repo Paths)

### Store Listing Text
- `store-assets/listing/title.txt`
- `store-assets/listing/short-description.txt`
- `store-assets/listing/full-description.txt`
- `store-assets/listing/whats-new.txt`
- `store-assets/listing/category.txt`

### Legal / Policy
- `store-assets/legal/privacy-policy.html`
- `store-assets/PRIVACY_POLICY_URL.txt`
- `store-assets/legal/open-source-licenses.txt`
- `store-assets/listing/data-safety-answers.txt`
- `store-assets/listing/content-rating-answers.txt`

### Graphics / Screenshots
- Screenshots (already present): `store-assets/screenshots/*.png`
- Required Play icon: `store-assets/graphics/ic_launcher_512.png`
- Required feature graphic: `store-assets/graphics/feature_graphic_1024x500.png`

---

## 3) Screenshot Refresh Workflow

Use the guided capture script:

```powershell
./scripts/04-capture-screenshots.ps1
```

Current expected screenshot file set:

1. `01_projects.png`
2. `02_spaces.png`
3. `03_editor.png`
4. `04_drywall.png`
5. `05_concrete.png`
6. `06_export.png`

---

## 4) Privacy Policy Hosting Workflow

Deploy/update hosted privacy page:

```powershell
./scripts/05-deploy-privacy-policy.ps1
```

This updates:

- `store-assets/PRIVACY_POLICY_URL.txt`

Use that URL in Play Console for both:
- Store listing privacy policy URL
- Data Safety privacy policy URL

---

## 5) Play Console Setup Checklist

### App Setup
- App name: from `store-assets/listing/title.txt`
- Category: Productivity
- App type: App
- Free/Paid: choose business model

### Policy Declarations
- Ads: No
- App access restrictions: No (no login required)
- Data safety: use `store-assets/listing/data-safety-answers.txt`
- Content rating: use `store-assets/listing/content-rating-answers.txt`
- Government app: No
- News app: No
- COVID tracing app: No

### Store Listing
- Paste title/short/full description from `store-assets/listing/`
- Upload icon + feature graphic from `store-assets/graphics/`
- Upload screenshots from `store-assets/screenshots/`
- Set contact email: `built.to.cell@gmail.com`

---

## 6) Production Release Steps

1. Open **Release > Production** in Play Console.
2. Create new release.
3. Upload `app-release.aab`.
4. Paste release notes from `store-assets/listing/whats-new.txt`.
5. Review generated device support.
6. Submit for review.

---

## 7) Must-Pass Final Gate

Before pressing submit:

- [ ] `app-release.aab` exists and is signed
- [ ] Privacy policy URL is live and publicly accessible
- [ ] Play icon uploaded
- [ ] Feature graphic uploaded
- [ ] All 6 screenshots uploaded
- [ ] Data safety form submitted
- [ ] Content rating form submitted
- [ ] Store listing has no placeholders
- [ ] Launch smoke test passed on current build

---

## 8) Project Technical Baseline (Current)

- Package: `com.tradesketch.estimator`
- `minSdk = 26`
- `targetSdk = 35`
- Offline-first architecture
- No dangerous permissions in manifest

If any of these change, update this document and `documentation/COMPLIANCE-CHECKLIST.md` in the same PR.

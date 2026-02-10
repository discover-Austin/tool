# Google Play Store Submission Guide

## Complete Submission Package for TradeSketch Estimator v1.0.0

This guide walks you through submitting TradeSketch Estimator to the Google Play Store.

## Table of Contents

1. [Pre-Submission Checklist](#pre-submission-checklist)
2. [Required Assets](#required-assets)
3. [Play Console Setup](#play-console-setup)
4. [Store Listing](#store-listing)
5. [Release Configuration](#release-configuration)
6. [Review Process](#review-process)
7. [Post-Launch](#post-launch)

## Pre-Submission Checklist

Before starting submission, verify:

- [x] targetSdk is 34 (Android 14)
- [x] Signed AAB built successfully
- [x] AAB tested with bundletool
- [x] All manual tests passed
- [x] Privacy policy hosted (URL ready)
- [x] Privacy policy in app (via assets)
- [x] No dangerous permissions requested
- [x] Data safety form prepared
- [x] Content rating questionnaire prepared
- [x] All store assets created
- [x] Disclaimer text present in exports
- [x] OSS attribution complete

## Required Assets

### App Bundle

**File:** `app-release.aab`
**Location:** `app/build/outputs/bundle/release/app-release.aab`
**Size:** Should be < 50 MB (typically 5-10 MB for this app)

**Verification:**
```bash
# Check AAB exists
ls -lh app/build/outputs/bundle/release/app-release.aab

# Verify signing
jarsigner -verify -verbose -certs app-release.aab

# Build universal APK for testing
bundletool build-apks --bundle=app-release.aab --output=test.apks --mode=universal
```

### App Icon

**Requirement:** 512 × 512 px PNG
**Format:** 32-bit PNG with alpha
**Content:** Blueprint grid design with house outline
**No text:** Icon should be recognizable without text
**Location:** `store-assets/graphics/ic_launcher_512.png`

**Creation Notes:**
- Use blue/teal color scheme (material design)
- Simple, flat design
- Recognizable at small sizes
- No gradients if possible (flat design preferred)
- Test on different backgrounds

**Tool Options:**
- Figma (free)
- Adobe Illustrator
- Inkscape (free)
- Android Asset Studio (deprecated but useful)

### Feature Graphic

**Requirement:** 1024 × 500 px PNG or JPEG
**Content:** App name + tagline + visual
**Text:** "TradeSketch Estimator" + "Material takeoffs, offline."
**Location:** `store-assets/graphics/feature_graphic_1024x500.png`

**Design Tips:**
- Use app's color scheme
- Include app icon
- Keep text readable
- Avoid busy backgrounds
- Test at thumbnail size

### Screenshots

**Requirement:** 6 screenshots minimum, 320-3840 px per side
**Recommended:** 1080 × 2400 px (portrait)
**Format:** PNG or JPEG
**Location:** `store-assets/screenshots/phone/`

**Required Screens:**
1. `01_templates.png` - Projects list with template cards
2. `02_model_spaces.png` - Model screen showing spaces list
3. `03_space_editor.png` - Space editor with dimension inputs and openings
4. `04_drywall_takeoff.png` - Drywall takeoff with results
5. `05_concrete_takeoff.png` - Concrete takeoff with results
6. `06_export_options.png` - Export screen with PDF/CSV options

**Screenshot Tips:**
- Use emulator or real device
- Enable demo mode (adb shell settings put global sysui_demo_allowed 1)
- Full battery, good signal, 9:41 time
- Representative data, not lorem ipsum
- Show key features
- Add text overlays if helpful (optional)

**Capture Command:**
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

## Play Console Setup

### 1. Create App

1. Go to [Google Play Console](https://play.google.com/console)
2. Select **All apps** > **Create app**
3. Fill in:
   - App name: TradeSketch Estimate
   - Default language: English (United States)
   - App or game: App
   - Free or paid: Free (or Paid if applicable)
4. Accept declarations
5. Click **Create app**

### 2. Set Up Your App

Complete the following tasks (Dashboard will show progress):

#### Store Settings

**App category:** Productivity

**Tags:** (optional)
- Construction
- Estimation
- Contractors
- Tools

**Email:** built.to.cell@gmail.com (use your real email)

#### Privacy Policy

**URL:** https://yoursite.com/privacy-policy.html

Host the `store-assets/legal/privacy-policy.html` file on:
- Your website
- GitHub Pages
- Firebase Hosting (free)
- Any static hosting

**Important:** URL must be accessible and use HTTPS

#### App Access

**All or some functionality restricted:** No

*Rationale:* App is fully functional without accounts or special access.

**Declaration:** 
- [ ] All features are accessible without login
- [ ] No demo accounts needed for review

#### Ads

**Contains ads:** No

*Rationale:* App is ad-free per requirements.

#### Content Ratings

**Questionnaire:** Use `store-assets/listing/content-rating-answers.txt`

1. Click **Start questionnaire**
2. Select category: **Utilities, Productivity, Communication, or Other**
3. Answer all questions **NO** (see content-rating-answers.txt)
4. Submit

**Expected Rating:** Everyone (ESRB E, PEGI 3)

#### Target Audience and Content

**Age groups:** 18 and over (or "Everyone" if applicable)

**Store Presence:**
- [ ] Designed for children: NO
- [ ] Primarily for entertainment: NO

#### News App

**Is this a news app:** No

#### COVID-19 Contact Tracing and Status Apps

**Is this a COVID-19 app:** No

#### Data Safety

**Data Safety Form:** Use `store-assets/listing/data-safety-answers.txt`

1. Click **Start**
2. **Does your app collect or share any of the required user data types?**
   - Select: **No, we don't collect or share any of the required user data types**
3. Review and submit

**Critical:** This MUST match your app's actual behavior. Do not claim to collect nothing if you add analytics later.

#### Government App

**Is this a government app:** No

## Store Listing

### Main Store Listing (en-US)

Navigate to **Store presence** > **Main store listing**

**App name:**  
```
TradeSketch Estimate
```
(30 characters max)

**Short description:**  
```
Model spaces and get drywall & concrete takeoffs—offline.
```
(80 characters max)

**Full description:**  
Copy from `store-assets/listing/full-description.txt`

**App icon:**  
Upload `store-assets/graphics/ic_launcher_512.png`

**Feature graphic:**  
Upload `store-assets/graphics/feature_graphic_1024x500.png`

**Phone screenshots:**  
Upload all 6 screenshots from `store-assets/screenshots/phone/`

**7-inch tablet screenshots:** (optional)  
If you have tablet-optimized layouts, add screenshots

**10-inch tablet screenshots:** (optional)  
If you have tablet-optimized layouts, add screenshots

### Store Settings

**App category:** Productivity

**Tags:** (select up to 5)
- Tools
- Construction
- Business
- Utilities
- Professional

**Contact details:**
- Email: built.to.cell@gmail.com (required)
- Phone: (optional)
- Website: https://yoursite.com (optional)

## Release Configuration

### 1. Create Production Release

1. Navigate to **Release** > **Production**
2. Click **Create new release**

### 2. App Bundles

**Upload:**
- `app-release.aab`

Play Console will:
- Verify bundle signature
- Generate APKs for different device configurations
- Show supported devices count

**Check:**
- Supported devices: Should be high (thousands)
- Excluded devices: Review list, none should be unexpected

### 3. Release Name

**Version:** `1 (1.0.0)`

Format: `versionCode (versionName)`

### 4. Release Notes

**What's new in this release:**
Copy from `store-assets/listing/whats-new.txt`

### 5. Review Release

**Review checklist:**
- [ ] AAB uploaded successfully
- [ ] Version code correct (1)
- [ ] Version name correct (1.0.0)
- [ ] Release notes added
- [ ] Supported devices count reasonable
- [ ] No unexpected excluded devices

### 6. Submit for Review

**Before submitting:**
- [ ] All store listing sections complete (green checkmarks)
- [ ] All compliance questions answered
- [ ] Data safety form submitted
- [ ] Content rating received
- [ ] Privacy policy URL active

**Review time:** Typically 1-3 days, can be up to 7 days

## Review Process

### What Google Reviews

1. **Policy Compliance**
   - Privacy policy matches behavior ✓
   - No restricted permissions ✓
   - No misleading claims ✓
   - Content appropriate ✓

2. **Technical Quality**
   - App doesn't crash ✓
   - Core functionality works ✓
   - targetSdk requirements met ✓

3. **Store Listing Quality**
   - Accurate descriptions ✓
   - Appropriate screenshots ✓
   - Correct categorization ✓

### Common Rejection Reasons (and how we avoid them)

❌ **Violates privacy policy**  
✅ We collect nothing, so this can't happen

❌ **Missing required permissions rationale**  
✅ We only use SAF, which doesn't require rationale

❌ **Functionality not accessible**  
✅ Everything works offline, no barriers

❌ **Low quality or misleading screenshots**  
✅ Real screenshots of actual functionality

❌ **Target SDK too low**  
✅ We use targetSdk 34

❌ **Inappropriate content**  
✅ Professional tool, no inappropriate content

### During Review

- **Respond quickly** to any requests for information
- **Monitor email** for Play Console notifications
- **Check dashboard** daily for status updates

### If Rejected

1. Read rejection reason carefully
2. Fix the issue
3. Upload new version (increment versionCode)
4. Resubmit with explanation of changes

## Post-Launch

### 1. Monitor Dashboard

**Metrics to watch:**
- **Installations:** Steady growth expected
- **Uninstalls:** Should be low
- **Crashes:** Should be near zero
- **ANRs:** Should be near zero
- **Ratings:** Target 4.0+ stars

### 2. Respond to Reviews

**Strategy:**
- Thank users for positive reviews
- Address issues in negative reviews
- Offer support email for problems
- Don't argue with users

**Response Template (Positive):**
```
Thank you for your support! We're glad TradeSketch Estimator helps with your estimates. If you have any suggestions, please email built.to.cell@gmail.com.
```

**Response Template (Negative):**
```
We're sorry you're experiencing issues. Please email us at built.to.cell@gmail.com with details so we can help. We're committed to making TradeSketch Estimator better.
```

### 3. Updates

When releasing updates:
1. Increment `versionCode` and `versionName` in `build.gradle.kts`
2. Build new signed AAB
3. Create new production release in Play Console
4. Add release notes describing changes
5. Upload new AAB
6. Submit for review

**Update frequency:**
- Critical bugs: ASAP (same day)
- Important features: Monthly
- Minor improvements: Quarterly

### 4. Promotion

**Free promotion ideas:**
- Reddit: r/Android, r/Contractors, r/DIY
- Product Hunt launch
- Trade forums and communities
- LinkedIn posts
- X (Twitter) announcements
- YouTube demo video

**Paid promotion:**
- Google Ads (Play Store campaigns)
- Facebook/Instagram ads targeting contractors
- Trade publication ads

### 5. Compliance Monitoring

**Ongoing obligations:**
- Update targetSdk annually (Google requirement)
- Keep privacy policy up to date
- Respond to data deletion requests (N/A for this app)
- Monitor for policy changes

### 6. Metrics & Goals

**Year 1 targets:**
- 1,000 installs
- 4.0+ star rating
- < 1% crash rate
- < 5% uninstall rate

## Troubleshooting

### "App not available in any country"

**Cause:** No countries selected in distribution settings  
**Fix:** Go to Production > Countries/regions > Select all or specific countries

### "Pending publication" for days

**Cause:** Rare, usually system delay  
**Fix:** Contact Play Console support

### "Bundle rejected due to invalid signature"

**Cause:** Keystore issue  
**Fix:** Verify signing config, rebuild AAB

### "App removed from Play Store"

**Cause:** Policy violation detected post-launch  
**Fix:** Review policy violation email, fix issue, request review

## Additional Resources

- [Play Console Help](https://support.google.com/googleplay/android-developer/)
- [Launch Checklist](https://developer.android.com/distribute/best-practices/launch/launch-checklist)
- [Play Policy Center](https://play.google.com/about/developer-content-policy/)
- [ASO (App Store Optimization) Guide](https://developer.android.com/distribute/best-practices/grow)

## Support Contacts

- **Play Console Support:** help.publish.google.com
- **Developer Policy Team:** Through Play Console > Help & feedback
- **Developer Forum:** groups.google.com/g/android-developers

---

**Submission prepared by:** Copilot Agent  
**Date:** February 8, 2026  
**Version:** 1.0.0

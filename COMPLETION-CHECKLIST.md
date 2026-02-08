# TradeSketch Estimator - Completion Checklist

Use this checklist to track progress toward Play Store submission.

## Phase 1: Local Build & Verification

- [ ] Clone repository to local machine with network access
- [ ] Run `./gradlew clean`
- [ ] Run `./gradlew test` - all tests should pass (6/6)
- [ ] Run `./gradlew assembleDebug`
- [ ] Install debug APK on device/emulator
- [ ] Verify app launches without crash
- [ ] Verify templates create projects
- [ ] Verify basic navigation works

**Success:** Debug APK installs and basic functionality works

---

## Phase 2: Complete UI Implementation

### Space Editor Screen
- [ ] Create Space Editor composable
- [ ] Add dimension input fields (length, width, height)
- [ ] Add opening inputs (door/window dimensions, count)
- [ ] Display live area/volume preview
- [ ] Add save/cancel buttons
- [ ] Handle input validation (positive numbers only)
- [ ] Show friendly error messages for invalid input

### Takeoff Display Screen
- [ ] Create Takeoff composable
- [ ] Add preset selector dropdown (Drywall, Concrete, Gravel, Paint)
- [ ] Display parameter cards (waste %, sheet size, etc.)
- [ ] Add editable parameter fields
- [ ] Create results card component (pinned at top)
- [ ] Display item list with quantities and units
- [ ] Add disclaimer text: "Estimate only—verify onsite"
- [ ] Hook up to TakeoffCalculator
- [ ] Verify calculations update in real-time

### Export Screen
- [ ] Create Export composable
- [ ] Add "Copy Summary" button with clipboard integration
- [ ] Add "Share" button with Android share sheet
- [ ] Implement CSV export with SAF (ACTION_CREATE_DOCUMENT)
- [ ] Implement PDF export with Android PdfDocument
- [ ] Format PDF: header, date, items table, totals, disclaimer footer
- [ ] Show success toasts after exports
- [ ] Handle permission denials gracefully

### Settings/About Screen
- [ ] Create Settings composable
- [ ] Add default waste percentage preference
- [ ] Add units preference (Imperial/Metric) - optional
- [ ] Add "Privacy Policy" button → WebView with privacy-policy.html
- [ ] Add "Open Source Licenses" button → Text view
- [ ] Display app version
- [ ] Add support email link (opens email app)

### ViewModels
- [ ] Create ProjectsViewModel (manage project list)
- [ ] Create ProjectDetailViewModel (manage current project)
- [ ] Create TakeoffViewModel (manage calculation state)
- [ ] Create ExportViewModel (manage export operations)
- [ ] Use StateFlow for UI state
- [ ] Handle loading, success, error states

**Success:** Complete user flow works: Create project → Add space → Calculate takeoff → Export PDF

---

## Phase 3: Data Persistence

- [ ] Verify DataStore dependency added (already in build.gradle)
- [ ] Complete ProjectDataStore implementation
- [ ] Create SettingsDataStore for preferences
- [ ] Test save/load projects
- [ ] Test app close → reopen (data persists)
- [ ] Test project updates save correctly
- [ ] Test project deletion works

**Success:** Projects survive app restart

---

## Phase 4: Create Graphics Assets

### App Icon (512×512)
- [ ] Open design tool (Figma, Illustrator, Inkscape, etc.)
- [ ] Create 512×512 canvas
- [ ] Design icon: blueprint theme + house outline
- [ ] Use color scheme: Blue (#1976D2), Cyan (#00BCD4)
- [ ] Export as PNG-32 (with alpha)
- [ ] Save to `store-assets/graphics/ic_launcher_512.png`
- [ ] Test visibility at 48×48 (shrink to check)
- [ ] Generate Android launcher icons in Android Studio

### Feature Graphic (1024×500)
- [ ] Create 1024×500 canvas
- [ ] Add app icon (left side, ~200×200)
- [ ] Add app name: "TradeSketch Estimator" (large, bold)
- [ ] Add tagline: "Material takeoffs, offline."
- [ ] Add blueprint/construction themed background
- [ ] Export as PNG or JPEG
- [ ] Save to `store-assets/graphics/feature_graphic_1024x500.png`
- [ ] Verify text readable at thumbnail size

### Screenshots (6 required)
- [ ] Set up device in demo mode (9:41 time, full battery, 4 bars)
- [ ] Capture "Projects List" screen → `01_templates.png`
- [ ] Capture "Model Spaces" screen → `02_model_spaces.png`
- [ ] Capture "Space Editor" screen → `03_space_editor.png`
- [ ] Capture "Drywall Takeoff" screen → `04_drywall_takeoff.png`
- [ ] Capture "Concrete Takeoff" screen → `05_concrete_takeoff.png`
- [ ] Capture "Export Options" screen → `06_export_options.png`
- [ ] Verify all are 1080×2400 or similar 9:19.5 ratio
- [ ] Save to `store-assets/screenshots/phone/`
- [ ] Optional: Add text overlays highlighting features

**Success:** All assets created and saved in correct directories

---

## Phase 5: Build Release AAB

### Generate Keystore (first time only)
- [ ] Run keytool command (see BUILD-INSTRUCTIONS.md)
- [ ] Save keystore file securely (backup!)
- [ ] Record keystore password
- [ ] Record key alias and password
- [ ] Add `*.keystore` to .gitignore (verify not committed)

### Configure Signing
- [ ] Edit `app/build.gradle.kts`
- [ ] Add signingConfigs block for release
- [ ] Set environment variables OR create local.properties
- [ ] Test signing config: `./gradlew tasks --all | grep sign`

### Build AAB
- [ ] Run `./gradlew clean`
- [ ] Run `./gradlew bundleRelease`
- [ ] Verify AAB exists: `app/build/outputs/bundle/release/app-release.aab`
- [ ] Check AAB size (should be 5-15 MB)
- [ ] Run jarsigner to verify signing
- [ ] Test AAB with bundletool:
  - [ ] Build APKs: `bundletool build-apks ...`
  - [ ] Install: `bundletool install-apks ...`
  - [ ] Verify app works from AAB-generated APK

**Success:** Signed app-release.aab created and tested

---

## Phase 6: Manual Testing

Use `documentation/TESTING-NOTES.md` for full checklist. Key tests:

### Functionality
- [ ] Create project from each template (4 templates)
- [ ] Add custom space to project
- [ ] Edit space dimensions
- [ ] Calculate drywall takeoff - verify results
- [ ] Calculate concrete takeoff - verify results
- [ ] Calculate gravel takeoff - verify results
- [ ] Calculate paint takeoff - verify results
- [ ] Export to CSV - verify file created and correct
- [ ] Export to PDF - verify file created and formatted
- [ ] Share estimate - verify text shared correctly
- [ ] Copy summary - verify text copied to clipboard

### Error Handling
- [ ] Enter negative dimensions → should prevent or show error
- [ ] Enter zero dimensions → should handle gracefully
- [ ] Enter text in number field → should prevent or show error
- [ ] Try to export empty project → should show appropriate message

### Offline
- [ ] Enable airplane mode
- [ ] Create project
- [ ] Calculate takeoff
- [ ] Export PDF
- [ ] Verify no error messages about network
- [ ] Disable airplane mode

### Accessibility
- [ ] Enable TalkBack
- [ ] Navigate through app with TalkBack
- [ ] Verify all buttons have descriptions
- [ ] Verify focus order is logical
- [ ] Disable TalkBack

### Configuration Changes
- [ ] Rotate device on each screen
- [ ] Verify state persists
- [ ] Verify no crashes

### Performance
- [ ] Note app startup time (should be < 2 seconds)
- [ ] Note calculation time (should be < 200ms)
- [ ] Check for UI jank during scrolling
- [ ] Monitor memory usage (should stay < 100MB)

**Success:** All critical tests pass, no crashes, performance acceptable

---

## Phase 7: Host Privacy Policy

- [ ] Choose hosting option (GitHub Pages, Firebase, your domain)
- [ ] Upload `store-assets/legal/privacy-policy.html`
- [ ] Verify URL is HTTPS
- [ ] Verify privacy policy loads correctly in browser
- [ ] Record final URL: ________________________
- [ ] Update README.md with privacy policy URL
- [ ] Update SUBMISSION-GUIDE.md with privacy policy URL

**Success:** Privacy policy accessible at HTTPS URL

---

## Phase 8: Pre-Submission Review

### Compliance Checklist
- [ ] Review `documentation/COMPLIANCE-CHECKLIST.md`
- [ ] Verify all critical requirements met
- [ ] Address any pending items
- [ ] Update status from ⚠️ to ✅ where applicable

### Excellence Scorecard
- [ ] Review `documentation/EXCELLENCE-SCORECARD.md`
- [ ] Verify score is ≥ 90/100 (or close)
- [ ] Note any remaining improvements
- [ ] Accept current score if ≥ 88 in all categories

### Final Checks
- [ ] Run `./gradlew lint` - aim for 0 errors
- [ ] Run all unit tests - 100% pass rate
- [ ] Review all store listing text for typos
- [ ] Verify all assets present and correct dimensions
- [ ] Verify app version in build.gradle matches listing
- [ ] Confirm signing keystore backed up safely

**Success:** All checklists reviewed, no critical issues

---

## Phase 9: Play Store Submission

Follow `documentation/SUBMISSION-GUIDE.md` step-by-step:

### Create App
- [ ] Log in to Google Play Console
- [ ] Click "Create app"
- [ ] Enter app name, language, type
- [ ] Accept declarations
- [ ] App created successfully

### Complete Store Settings
- [ ] Set app category: Productivity
- [ ] Enter email address
- [ ] Enter privacy policy URL
- [ ] Declare: No restricted features
- [ ] Declare: No ads

### Data Safety Form
- [ ] Start data safety form
- [ ] Select: "No data collected or shared"
- [ ] Review and submit

### Content Rating
- [ ] Start questionnaire
- [ ] Select category: Productivity/Tools
- [ ] Answer all questions "NO" (see content-rating-answers.txt)
- [ ] Submit and receive rating (should be Everyone/E)

### Store Listing
- [ ] Upload app icon (512×512)
- [ ] Upload feature graphic (1024×500)
- [ ] Upload 6 phone screenshots
- [ ] Enter short description (80 chars)
- [ ] Enter full description (~2100 chars)
- [ ] Save listing

### Create Release
- [ ] Navigate to Production track
- [ ] Click "Create new release"
- [ ] Upload app-release.aab
- [ ] Enter release name: 1 (1.0.0)
- [ ] Enter release notes (what's new)
- [ ] Review supported devices (should be thousands)
- [ ] Save release

### Final Review & Submit
- [ ] Review all sections (all should have green checkmarks)
- [ ] Click "Review release"
- [ ] Verify everything correct
- [ ] Click "Start rollout to Production"
- [ ] Confirm submission

**Success:** App submitted for review

---

## Phase 10: Review & Launch

### During Review (1-7 days)
- [ ] Monitor email for Play Console notifications
- [ ] Check Play Console dashboard daily
- [ ] Respond promptly to any reviewer questions
- [ ] If rejected, read reason carefully and fix

### After Approval
- [ ] Celebrate! 🎉
- [ ] Share app link with friends/colleagues
- [ ] Post to relevant communities (Reddit, forums)
- [ ] Monitor crash reports in Play Console
- [ ] Respond to user reviews
- [ ] Track metrics (installs, ratings, retention)

### First Week Monitoring
- [ ] Check for crashes daily
- [ ] Read all user reviews
- [ ] Respond to negative reviews with support
- [ ] Fix critical bugs immediately
- [ ] Note feature requests for future versions

**Success:** App live on Play Store with positive reviews

---

## Long-Term Maintenance

### Monthly
- [ ] Check Play Console for crashes/ANRs
- [ ] Read and respond to reviews
- [ ] Monitor rating (target 4.0+ stars)
- [ ] Plan feature updates based on feedback

### Quarterly
- [ ] Release minor update with improvements
- [ ] Update dependencies to latest stable versions
- [ ] Review and update documentation
- [ ] Refresh screenshots if UI changed

### Annually
- [ ] Update targetSdk to latest (Google requirement)
- [ ] Major feature update or refactor
- [ ] Review and update privacy policy if needed
- [ ] Conduct security audit

---

## Troubleshooting Guide

### Build Fails
- See `documentation/BUILD-INSTRUCTIONS.md` troubleshooting section
- Check ANDROID_HOME environment variable
- Verify network access to Maven repositories
- Try `./gradlew clean --refresh-dependencies`

### Tests Fail
- Read error message carefully
- Check if test expectations changed
- Run single test to isolate: `./gradlew test --tests ClassName.testName`
- Review test code vs implementation

### App Crashes
- Check logcat: `adb logcat | grep -i exception`
- Identify crash location from stack trace
- Add null checks or try-catch
- Test fix thoroughly

### Play Console Issues
- Read Google's error messages carefully
- Check SUBMISSION-GUIDE.md for common issues
- Contact Play Console support if stuck
- Join developer forums for advice

---

## Resources

- **Documentation:** See `documentation/` directory (7 guides)
- **Store Assets:** See `store-assets/ASSET-CREATION-NOTES.md`
- **Build Issues:** See `documentation/BUILD-INSTRUCTIONS.md`
- **Testing:** See `documentation/TESTING-NOTES.md`
- **Submission:** See `documentation/SUBMISSION-GUIDE.md`
- **Status:** See `PROJECT-STATUS.md`

---

## Completion Tracking

**Overall Progress:** ___ / 100 tasks completed

**Estimated Hours Remaining:** ________

**Target Submission Date:** ________

**Notes:**
_____________________________________________
_____________________________________________
_____________________________________________

---

**Last Updated:** February 8, 2026  
**Prepared By:** Copilot Coding Agent

**Good luck with your submission! 🚀**

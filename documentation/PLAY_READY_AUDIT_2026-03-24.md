# TradeSketch Estimator - Play Ready Audit (Historical March 24 Baseline)

Last updated: March 24, 2026

This audit captures the concrete completion gaps I could verify from the current codebase, release pipeline, and live-device smoke pass.

Use `README.md` and `documentation/COMPLIANCE-CHECKLIST.md` for the current release snapshot. This March 24 audit is retained as historical context and is not the current build-of-record.

## What I Used

- Local repo audit of release docs, Gradle config, manifest, and store-asset scripts
- Unit test verification with `:app:testDebugUnitTest` and `:core:test`
- Live-device walkthrough on Android hardware via ADB using the production package `com.tradesketch.estimator` (`1.0.18`)
- Google Play listing requirement check against current official help pages

## User-Facing App Status

- First-run flow works: welcome screen, project ritual, project creation, workspace entry
- Main navigation works: Open, Blueprint, Materials, Export, Settings
- Materials, Export, Settings, and Saved Projects surfaces all rendered on-device
- No blocking crash or dead-end was found during the smoke journey

## Missing Parts Found And Closed

- Release-signing safety in Gradle was incomplete for direct `release` and `sideload` task execution
  - Fixed by failing fast when signing values or the keystore file are missing
- Release script relied on ambient Android/Gradle home paths
  - Fixed by pinning `ANDROID_USER_HOME` and `GRADLE_USER_HOME` into local project folders
- Keystore ignore rules allowed an accidental tracked release keystore pattern
  - Fixed by removing the allow rule and explicitly ignoring common release keystore filenames
- Store screenshot workflow only documented and prompted for 6 screenshots
  - Fixed to support an 8-shot Play listing set
- Screenshot and video scripts defaulted to the production package name
  - Fixed to target the production package after confirming the `.local` install on-device was the older side build
- Store asset notes implied direct Play upload of MP4/WebM promo video
  - Fixed to document the actual preview-video flow: upload to YouTube, then use the Play Console URL
- Showcase copy used the old "TradeSketch Pro" name
  - Fixed to match the shipped product name: `TradeSketch Estimator`

## Remaining Potential Gaps To Watch

- The smoke pass did not exhaustively validate every export/share target on physical storage providers
- The current walkthrough confirmed navigation and rendering, but not every advanced blueprint-editing edge case
- The Play screenshot directory now has an 8-shot set, but only `07` and `08` were freshly recaptured during this pass; refresh `01` through `06` too if you want a fully same-day store set
- A final pre-submission pass should still confirm the exact YouTube video URL and final screenshot ordering in Play Console

## Release Recommendation

The app appears launch-ready from the surfaces tested in this pass. The remaining risk is mostly submission polish and wider regression coverage, not an obvious missing core feature.

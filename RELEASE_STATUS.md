# TradeSketch Estimator - Release Status (Authoritative)

Last updated: March 9, 2026

This file is the single source of truth for current release readiness in this repository.

## Build Identity

- Package: `com.tradesketch.estimator`
- Version: `1.0.8` (`versionCode = 10`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Release shrinking: enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)

## Truly Complete

- Offline-first project workflow with local persistence (`DataStore` + file-backed project storage).
- Blueprint editing with walls/openings/room detection, floor tagging, snap controls, and undo/redo.
- Trade takeoffs for drywall, concrete, gravel/mulch, and paint.
- Costing with material/labor/markup/tax rollups.
- Export outputs for text, CSV, JSON, estimate PDF, blueprint PNG, and blueprint PDF.
- Play-store asset/documentation folders (`store-assets/`, `documentation/`) present.

## Newly Added In This Hardening Pass

- Truth-sync updates to release docs and metadata references.
- Safer blueprint editing:
  - Preserves manual/non-loop rooms during wall-loop re-detection.
  - Validates opening placement for wall fit, endpoint clearance, overlap, and wall-height bounds.
  - Clears stale redo history on non-undo-tracked edits.
  - Keeps floor selection available outside stair workflow.
- Additional blueprint consistency hardening:
  - Wall updates and wall-height changes now sanitize existing openings against current geometry.
  - Selection state now self-heals when selected items are removed by geometry edits.
- Estimator math fix:
  - Drywall ceiling takeoff now respects `room.ceiling.enabled`.
- Estimator trust updates:
  - Gravel summary now matches tagged-room takeoff behavior.
  - Takeoff warnings now explicitly call out all-floor aggregation and stair-opening handling.
- Export hardening:
  - CSV escaping for quotes/newlines.
  - Spreadsheet formula-injection protection for text fields.
  - Higher-collision-resistance export filenames.
  - Unified estimate identity/timestamp across text/CSV/JSON/PDF exports.
  - Blueprint PDF export actions now exposed in the Export tab workflow.
- Play/policy hardening:
  - Manifest app label now uses `@string/app_name`.
  - Added explicit `dataExtractionRules` + `fullBackupContent` XML to keep backup behavior explicit with `allowBackup=false`.
  - Adaptive launcher icon resources moved off obsolete `mipmap-anydpi-v26` qualifier for `minSdk 26`.
- Test additions:
  - Ceiling-enabled drywall regression coverage.
  - CSV escaping/injection regression coverage.
  - Export filename uniqueness coverage.
  - Stair-opening and gravel-tagged-room calculation regression coverage.
  - Expanded `androidTest` Compose smoke coverage for onboarding + workspace tab journey/navigation.
- Accessibility/localization prep:
  - Centralized additional high-frequency onboarding/workspace strings into `strings.xml`.

## Optional Work Remaining (Not Required For Current Launch)

- Full `WindowSizeClass`/`NavigationSuiteScaffold` migration for compact vs medium/expanded navigation patterns.
- More comprehensive keyboard/mouse shortcuts for blueprint editing on Chromebook/desktop Android.
- Additional `androidTest` journeys for deep export/share edge cases (beyond current onboarding + workspace navigation smoke).
- Additional premium report theming/branding controls (logo upload, template themes).
- Recapture Play Store screenshots from current app UI before submission.



# TradeSketch Estimator - Release Status (Authoritative)

Last updated: March 8, 2026

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
- Estimator math fix:
  - Drywall ceiling takeoff now respects `room.ceiling.enabled`.
- Export hardening:
  - CSV escaping for quotes/newlines.
  - Spreadsheet formula-injection protection for text fields.
  - Higher-collision-resistance export filenames.
- Test additions:
  - Ceiling-enabled drywall regression coverage.
  - CSV escaping/injection regression coverage.
  - Export filename uniqueness coverage.
  - Baseline `androidTest` Compose smoke coverage.

## Optional Work Remaining (Not Required For Current Launch)

- Full `WindowSizeClass`/`NavigationSuiteScaffold` migration for compact vs medium/expanded navigation patterns.
- More comprehensive keyboard/mouse shortcuts for blueprint editing on Chromebook/desktop Android.
- Expanded `androidTest` journeys for deep export/share and project-switch edge cases.
- Additional premium report theming/branding controls (logo upload, template themes).



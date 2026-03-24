# TradeSketch Estimator - Release Status (Authoritative)

Last updated: March 23, 2026

This file is the single source of truth for current release readiness in this repository.

## Build Identity

- Package: `com.tradesketch.estimator`
- Version: `1.0.18` (`versionCode = 20`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Release shrinking: enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)
- Current worktree verification:
  - `:app:testDebugUnitTest` passed on `2026-03-23`
  - `:core:test` passed on `2026-03-23`
  - `:app:lint` and `:app:lintRelease` generated reports with `0` lint errors on `2026-03-23`
  - `:app:bundleRelease` is still pending for the current worktree
- Last bundle artifact on disk: `app/build/outputs/bundle/release/app-release.aab`
- Artifact timestamp on disk: `2026-03-23 10:30:54 -04:00`
- Artifact SHA-256 on disk: `99E20C96F0B527C498BFF88D7F3084F0728A5A9E3511B384484FBAEF0C16270D`
- Note: the on-disk bundle was not revalidated after the current audit fixes.

## Truly Complete

- Offline-first project workflow with local persistence (`DataStore` + file-backed project storage).
- Blueprint editing with walls/openings/room detection, floor tagging, snap controls, and undo/redo.
- Trade takeoffs for drywall, concrete, gravel/mulch, and paint.
- Costing with material/labor/markup/tax rollups.
- Export outputs for text, CSV, JSON, estimate PDF, blueprint PNG, and blueprint PDF.
- Play-store asset/documentation folders (`store-assets/`, `documentation/`) present.

## Newly Added In This Hardening Pass

- Truth-sync updates to release docs and metadata references.
- Blueprint geometry upgrade:
  - New `Measured Arc` tool for reproducible field curves with live chord, rise, radius, sweep, and arc-length math.
  - `Sketch Curve` stays available as a separate freeform tool with distinct iconography and help/tutorial copy.
  - Curve selections preserve reusable measurements instead of forcing users to read per-segment geometry.
  - Selected measured arcs now show center/radius guide geometry on-canvas, and committed curve groups label the full curve length as a single logical measurement.
- Shared-blueprint workflow improvements:
  - Trade geometry keeps trade-specific colors when switching trades.
  - Completed concrete and gravel/mulch polygons fill visibly while drywall stays unfilled.
  - Combined exports include only trades actually present in the blueprint, avoiding zero-value noise.
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
- Blueprint controls parity:
  - Touch mode now keeps dedicated floor/grid/cursor access while matching joystick mode for center undo/redo/zoom controls.

## Optional Work Remaining (Not Required For Current Launch)

- Full `WindowSizeClass`/`NavigationSuiteScaffold` migration for compact vs medium/expanded navigation patterns.
- More comprehensive keyboard/mouse shortcuts for blueprint editing on Chromebook/desktop Android.
- Additional `androidTest` journeys for deep export/share edge cases (beyond current onboarding + workspace navigation smoke).
- Additional premium report theming/branding controls (logo upload, template themes).
- Recapture Play Store screenshots from current app UI before submission.



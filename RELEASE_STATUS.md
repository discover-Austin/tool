# TradeSketch Estimator - Release Status (Authoritative)

Last updated: March 25, 2026

This file is the single source of truth for current release readiness in this repository.

## Build Identity

- Package: `com.tradesketch.estimator`
- Version: `1.0.19` (`versionCode = 21`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Release shrinking: enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)
- Current worktree verification:
  - `:app:testDebugUnitTest` passed on `2026-03-25`
  - `:core:test` passed on `2026-03-25`
  - `:app:lint` passed on `2026-03-25`
  - `:app:lintRelease` passed on `2026-03-25`
  - `:app:bundleRelease` passed on `2026-03-25`
  - `:app:assembleSideload` passed on `2026-03-25`
  - `jarsigner -verify app-release.aab` returned `jar verified` on `2026-03-25`
- Last bundle artifact on disk: `app/build/outputs/bundle/release/app-release.aab`
- Artifact timestamp on disk: `2026-03-25 16:22:03 -04:00`
- Artifact SHA-256 on disk: `35F2F4F1D68645D8ACA1817BBE96A95ECBE614BEA9D74BE52699C8BAE16324F2`
- Last sideload artifact on disk: `app/build/outputs/apk/sideload/app-sideload.apk`
- Sideload timestamp on disk: `2026-03-25 16:34:03 -04:00`
- Sideload SHA-256 on disk: `0EC161B03CEC901AC1F769C62DF72EEB65995F4BA10592F8926137925BC44DC6`
- Capture package for screenshots/video: `com.tradesketch.estimator.local`
- Screenshot set status: refreshed on `2026-03-25`; manifest recorded in `store-assets/screenshots/LATEST_SCREENSHOT_SNAPSHOT.txt`
- Showcase video status: fresh raw capture recorded on `2026-03-25 17:41:59 -04:00`; verified screenshot-frozen narrated/subtitled render completed on `2026-03-25 18:09:10 -04:00`
- Showcase video artifact: `media/play_store_showcase/tradesketch_showcase_play_store_30s.mp4`
- Showcase video duration: `30.000000s`
- Showcase video SHA-256: `BDEB587FF1415A593E33FEBD010F23D073DC1A0225C1DC03129E37C0FB734D65`
- Note: older March 24 media artifacts are stale. An earlier March 25 raw-capture render was rejected during frame verification and superseded by the screenshot-frozen March 25 screenshot/video set.

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
  - Release tooling now fails fast on missing signing config, keeps keystore names ignored, supports an 8-shot Play listing, and renders a 30-second production showcase video.
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
- Wider multi-device smoke coverage before rollout.



# TradeSketch Estimator - Project Status

Updated: March 8, 2026

See `RELEASE_STATUS.md` for authoritative release state.

## Current Technical Baseline

- Package: `com.tradesketch.estimator`
- Version: `1.0.8` (`versionCode = 10`)
- SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 35`
- Release shrinking: enabled (`minify + resource shrink`)

## Completed Core Product Scope

- Offline-first project lifecycle and persistence
- Blueprint editing with snapping, openings, rooms, floors, and undo/redo
- Takeoff support: drywall, concrete, paint, gravel/mulch
- Costing summary: materials, labor, markup, tax, total
- Export paths: text, CSV, JSON, estimate PDF, blueprint PNG/PDF

## Release Confidence Inputs

- Unit tests in `app` and `core`
- Lint (`debug` + `release`)
- Signed AAB generation pipeline via `scripts/03-build-release.ps1`

## Remaining Manual Gates

- Final multi-device smoke run on release commit
- Play Console upload validation (version parse, listing assets, policy responses)



# TradeSketch Estimator - Launch Checklist

Updated: March 22, 2026

## Release Checklist

- [x] `:app:testDebugUnitTest` passes
- [x] `:core:test` passes
- [x] `:app:lint` passes
- [x] `:app:lintRelease` passes
- [x] `:app:bundleRelease` passes
- [x] Signed AAB exists at `app/build/outputs/bundle/release/app-release.aab`
- [ ] Play Console parses `versionCode 18` / `versionName 1.0.16`
- [x] Release notes updated in `store-assets/listing/whats-new.txt`

## Testing Checklist

- [x] Connected phone replaced with current local debug build (`adb uninstall` + fresh install)
- [x] Debug app cold-launch smoke passed on connected phone
- [x] Measured arc mode rail button accepted a real on-device tap without process loss
- [x] Sketch curve mode rail button accepted a real on-device tap without process loss
- [ ] First-run onboarding path (Welcome -> Ritual -> Workspace)
- [ ] Project creation and saved project switching
- [ ] Blueprint draw/select/move/opening placement/undo-redo
- [ ] Floor switching behavior in blueprint workspace
- [ ] Trade switching in materials screen
- [ ] Export save/share for PDF, PNG, CSV, JSON
- [ ] Settings persistence after app restart
- [ ] Basic offline smoke test (airplane mode)

## Store Listing Checklist

- [x] Listing text reviewed (`store-assets/listing/`)
  - `full-description.txt` synced on March 22, 2026 to mention measured arcs/sketch curves and blueprint PNG/PDF export
- [ ] Screenshots match current UI
  - Current repo screenshots are dated `2026-02-10`
  - Current feature graphic/icon assets are dated `2026-02-11`
  - These likely predate the March blueprint UI hardening pass and should be recaptured/reconfirmed
- [x] Privacy policy URL reachable and current
  - Re-verified `HTTP/1.1 200 OK` on March 22, 2026: `https://tradesketch-privacy.vercel.app`
- [x] Data safety answers match app behavior

## Current Hardening Audit

- [x] Curve commit fallback fixed so untouched midpoint placeholders do not override the user's actual third tap
- [x] Wall, box, and circle touch commits now fall back to the live tap when preview state is still untouched
- [x] Tutorial step transitions now reset tool/panel state deterministically
- [x] Grouped measured arcs and circles delete as a full shape instead of removing a single segment
- [x] Selected measured arcs now expose center/radius guide geometry for the live canvas
- [x] Committed curve groups now label the full curve/arc length instead of segment fragments
- [x] Focused regression coverage added for commit fallback, tutorial state, grouped deletion, and measured-arc guide metadata
- [x] Focused regression run passed on March 21, 2026: `:app:testDebugUnitTest --tests com.tradesketch.estimator.ui.blueprint.BlueprintCurveGeometryTest`
- [x] `:app:lint` passed on March 22, 2026
- [x] `:app:lintRelease` passed on March 22, 2026
- [x] Lint HTML reports were generated during the March 22 run before the later clean bundle build
- [x] Current lint state remains warning-only, not failing:
  - Debug report title: `25 warnings`
  - Release report title: `25 warnings`
- [x] Connected-device smoke covered hot launch plus both new arc-mode rail entries before the device disconnected from `adb`
- [x] Clean release bundle rebuild passed on March 22, 2026: `:app:clean :app:bundleRelease`
- [x] Fresh AAB fingerprint captured for the current hardening state:
  - Path: `app/build/outputs/bundle/release/app-release.aab`
  - Built: `2026-03-22 02:10:04 -04:00`
  - Size: `19.99 MB`
  - SHA-256: `C7E97313D3DBD3728F46A5FB376B5562CC1DDCAA24625858EFF78487A87CA637`
- [x] `jarsigner -verify -verbose -certs` succeeded on the fresh AAB
- [ ] Reconnect phone for a longer workflow smoke pass if we want more than launch/toggle coverage

## Known Limitations

- Advanced keyboard/mouse blueprint shortcuts are limited.
- Full adaptive navigation suite migration (bottom bar vs rail) is not complete.
- Instrumented UI coverage is baseline-level and should continue expanding.



# TradeSketch Estimator - Launch Checklist

Updated: March 8, 2026

## Release Checklist

- [ ] `:app:testDebugUnitTest` passes
- [ ] `:core:test` passes
- [ ] `:app:lint` passes
- [ ] `:app:lintRelease` passes
- [ ] `:app:bundleRelease` passes
- [ ] Signed AAB exists at `app/build/outputs/bundle/release/app-release.aab`
- [ ] Play Console parses `versionCode 10` / `versionName 1.0.8`
- [ ] Release notes updated in `store-assets/listing/whats-new.txt`

## Testing Checklist

- [ ] First-run onboarding path (Welcome -> Ritual -> Workspace)
- [ ] Project creation and saved project switching
- [ ] Blueprint draw/select/move/opening placement/undo-redo
- [ ] Floor switching behavior in blueprint workspace
- [ ] Trade switching in materials screen
- [ ] Export save/share for PDF, PNG, CSV, JSON
- [ ] Settings persistence after app restart
- [ ] Basic offline smoke test (airplane mode)

## Store Listing Checklist

- [ ] Listing text reviewed (`store-assets/listing/`)
- [ ] Screenshots match current UI
- [ ] Privacy policy URL reachable and current
- [ ] Data safety answers match app behavior

## Known Limitations

- Advanced keyboard/mouse blueprint shortcuts are limited.
- Full adaptive navigation suite migration (bottom bar vs rail) is not complete.
- Instrumented UI coverage is baseline-level and should continue expanding.



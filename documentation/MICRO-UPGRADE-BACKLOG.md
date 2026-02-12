# Micro Upgrade Backlog (Steady Flow)
Last updated: 2026-02-10
Purpose: Small, precise upgrades for the implementation agent. Ship 1-2 cards at a time.

## Working Rules
- Keep WIP <= 2 cards.
- Each card should be one PR.
- Do not mix unrelated cards.

## Card 01 (XS, 20-30 min): Fix Windows wrapper tracking
Why: `scripts\01-check-prerequisites.ps1` and `scripts\03-build-release.ps1` require `gradlew.bat`, but `.gitignore` currently ignores it.
Files:
- `.gitignore`
- `gradlew.bat` (repo root)
Changes:
- Remove `gradlew.bat` from `.gitignore`.
- Ensure `gradlew.bat` exists and is tracked in Git.
Done when:
- `gradlew.bat` is present in repo.
- Wrapper check in `scripts\01-check-prerequisites.ps1` passes.

## Card 02 (XS, 20-30 min): Keystore ignore hardening
Why: Root keystore exists (`tradesketch-release.keystore`) and `.gitignore` has a risky allow rule (`!release.keystore`).
Files:
- `.gitignore`
Changes:
- Remove `!release.keystore`.
- Add explicit ignore for `tradesketch-release.keystore`.
- Keep `*.keystore` ignore.
Done when:
- `git status` never shows keystore files.
- Ignore rules are explicit and safe.

## Card 03 (S, 30-45 min): Lock release build strategy (intentional minify choice)
Why: Release currently builds with `isMinifyEnabled=false` and `isShrinkResources=false`. This is documented now; decision should be explicit before production submission.
Files:
- `app/build.gradle.kts`
- `scripts\03-build-release.ps1`
- `documentation/BUILD-INSTRUCTIONS.md`
Changes:
- Choose one and make it final for launch:
  - Enable minify/shrink for release, OR
  - Keep non-minified release and document rationale.
- Keep script + docs aligned with that decision.
Done when:
- Build behavior, script messaging, and documentation match exactly.

## Card 04 (S, 30-45 min): Fail fast on missing signing config
Why: Release signing in `app/build.gradle.kts` is optional at configuration time and can fail late.
Files:
- `app/build.gradle.kts`
Changes:
- Add explicit release-signing validation with clear `GradleException` when `KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD` are missing.
Done when:
- `bundleRelease` without signing fails immediately with a clear error.

## Card 05 (S, 45-60 min): Add flow error handling in ViewModels
Why: Long-lived collectors can silently die on repository/data errors.
Files:
- `app/src/main/java/com/tradesketch/estimator/ui/viewmodel/ProjectDetailViewModel.kt`
- `app/src/main/java/com/tradesketch/estimator/ui/viewmodel/TakeoffViewModel.kt`
- `app/src/main/java/com/tradesketch/estimator/ui/viewmodel/ExportViewModel.kt`
Changes:
- Add `.catch { ... }` before `.collect { ... }` on repository flows.
- Update UI state with actionable error text.
Done when:
- Injected flow exceptions produce UI error state instead of silent stop/crash.

## Card 06 (M, 60-90 min): Prevent parse-error data wipe in ProjectDataStore
Why: `ProjectDataStore.projects` catches parse exceptions and emits `emptyList()`. `saveProject()` then uses `projects.first()` and can overwrite valid stored data.
Files:
- `app/src/main/java/com/tradesketch/estimator/data/local/ProjectDataStore.kt`
Changes:
- Stop treating parse failure as empty dataset.
- Make write paths (`saveProject`, `deleteProject`) fail safely when current JSON cannot be parsed.
- Surface explicit error (exception/result) so UI can prompt recovery.
Done when:
- Corrupt JSON does not get overwritten by empty data.

## Card 07 (M, 60-90 min): Debounce and background takeoff recomputation
Why: `TakeoffViewModel` recalculates on every parameter update and project/settings emission.
Files:
- `app/src/main/java/com/tradesketch/estimator/ui/viewmodel/TakeoffViewModel.kt`
Changes:
- Build a single derived input flow (project + settings + selected type + params).
- Apply `debounce(200)` and `distinctUntilChanged()`.
- Run calculation on `Dispatchers.Default`, publish results on main thread.
Done when:
- Fast typing no longer triggers excessive recomputation/jank.

## Card 08 (S, 45-60 min): Stop writing settings on every keystroke
Why: `SettingsScreen` calls save methods directly from each `OutlinedTextField.onValueChange`.
Files:
- `app/src/main/java/com/tradesketch/estimator/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/tradesketch/estimator/ui/viewmodel/SettingsViewModel.kt`
Changes:
- Keep editable draft strings in screen state.
- Persist on explicit save, IME done, or blur (one commit action per field).
Done when:
- DataStore writes happen on commit events, not every character typed.

## Card 09 (S, 30-45 min): Correct CSV escaping
Why: `ExportFormatter.formatAsCSV` quotes fields but does not escape embedded quotes/newlines.
Files:
- `app/src/main/java/com/tradesketch/estimator/utils/ExportFormatter.kt`
Changes:
- Add `escapeCsv(String)` helper: replace `"` with `""`, quote fields containing comma/quote/newline.
- Use it for project name, takeoff type, item names, and units.
Done when:
- Names like `Bob "A", Inc` export as valid CSV.

## Card 10 (S, 30-45 min): Fix money conversion precision in export
Why: `ExportFormatter` does `Money(value.toLong())`, which truncates and likely misinterprets dollars/cents.
Files:
- `app/src/main/java/com/tradesketch/estimator/utils/ExportFormatter.kt`
Changes:
- Define one conversion rule: if input is dollars `Double`, convert via `(value * 100).roundToLong()` before `Money(...)`.
- Apply to `unitCost`, `extendedCost`, and `totalCost` formatting.
Done when:
- `12.34` renders as `$12.34` (not `$0.12`).

## Card 11 (S, 45-60 min): Add tests for formatter and parser edge cases
Why: Current tests do not cover CSV escaping, money conversion, or dimension parser edge cases.
Files:
- `app/src/test/java/com/tradesketch/estimator/utils/ExportFormatterTest.kt` (new)
- `app/src/test/java/com/tradesketch/estimator/utils/ValidatorsTest.kt` (new)
Changes:
- Add tests for embedded quotes/commas/newlines in CSV output.
- Add tests for money conversion rounding.
- Add parser tests for valid and invalid dimension formats.
Done when:
- Edge-case tests fail before fix and pass after fix.

## Card 12 (XS, 20-30 min): Privacy posture explicit in manifest
Why: App claims privacy-first/offline behavior; manifest currently has `android:allowBackup="true"`.
Files:
- `app/src/main/AndroidManifest.xml`
Changes:
- Set `android:allowBackup="false"` (or add explicit backup rules if backups are intentionally supported).
- Optional hardening: `android:usesCleartextTraffic="false"`.
Done when:
- Manifest policy matches documented privacy posture.

## Suggested First 2-Card Cycle
1. Card 01 (wrapper tracking)
2. Card 05 (flow error handling)

## Suggested Second 2-Card Cycle
1. Card 03 (release behavior/message alignment)
2. Card 09 (CSV escaping)

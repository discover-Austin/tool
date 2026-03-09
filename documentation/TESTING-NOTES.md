# Testing Notes

## TradeSketch Estimator
Updated: March 8, 2026

This checklist targets the current blueprint-first UI.

---

## 1) Automated Checks

Run before device QA:

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :core:test
./gradlew.bat :app:lint
./gradlew.bat :app:lintRelease
```

Expected: zero failing tests and no lint task failures.

---

## 2) Device Smoke Sequence

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -n com.tradesketch.estimator/.MainActivity
```

Crash scan:

```powershell
adb logcat -d -v brief | findstr /C:"FATAL EXCEPTION" /C:"AndroidRuntime" /C:"com.tradesketch.estimator"
```

Expected: no startup/runtime fatal exceptions on normal flows.

---

## 3) Core Manual QA Checklist (Current Flow)

### First-Run and Project Setup
- [ ] Welcome screen renders and Continue works.
- [ ] Project setup step 1 validates project name entry.
- [ ] Project setup step 2 trade selection completes and opens workspace.

### Workspace Navigation
- [ ] Rail opens tabs: Blueprint, Materials, Export, Settings.
- [ ] Saved projects panel opens/closes.
- [ ] Creating a starter/new project works.
- [ ] Switching projects from Saved list updates active project.

### Blueprint
- [ ] Draw wall interactions work without crashes.
- [ ] Door/window placement on walls works.
- [ ] Undo/redo and delete actions behave correctly.
- [ ] Blueprint-derived live quantities update as geometry changes.

### Materials
- [ ] Estimate type switching (drywall/concrete/gravel/mulch/paint) works.
- [ ] Parameter changes recalculate outputs.
- [ ] Smart checks/warnings display when expected.

### Export
- [ ] Estimate summary renders for selected scope.
- [ ] Share Full Report opens Android share sheet.
- [ ] Save CSV/PDF/JSON works.
- [ ] Download/Share estimate PDF actions complete.

### Settings/About
- [ ] Core preference toggles persist (trade, units, reduced motion).
- [ ] Quantity/pricing defaults persist after restart.
- [ ] Business identity fields persist and appear in exports.
- [ ] Reset Settings restores defaults after confirmation.

---

## 4) Regression Risk Areas

Retest these whenever related code changes:

- Blueprint editing gestures and snapping behavior
- Trade scope switching and recomputation
- Export SAF write flows (CSV/PDF/JSON)
- DataStore persistence and project switching

---

## 5) Play Submission QA Gate

Before production rollout:

- [ ] Build signed AAB from release commit (`:app:bundleRelease`)
- [ ] Run final smoke test on same commit
- [ ] Verify no fatal exceptions in logcat on main flows
- [ ] Confirm listing screenshots match current UI
- [ ] Confirm privacy policy URL resolves publicly

---

## 6) Notes

- If a crash occurs, capture full stack trace and fix root cause.
- Refresh screenshots after major UI changes using `scripts/04-capture-screenshots.ps1`.

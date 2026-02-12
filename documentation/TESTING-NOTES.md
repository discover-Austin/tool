# Testing Notes

## TradeSketch Estimator
Updated: February 11, 2026

This checklist is aligned with the current UI and workflows in the app.

---

## 1) Automated Checks

Run before device QA:

```powershell
./gradlew.bat compileDebugKotlin
./gradlew.bat testDebugUnitTest
./gradlew.bat lint
```

Expected: no failing unit tests, no blocking lint errors.

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

Expected: no fatal exception for normal navigation.

---

## 3) Core Manual QA Checklist

### Projects + Navigation
- [ ] App launches to projects screen
- [ ] Template project creation works
- [ ] Project opens into workspace
- [ ] Bottom nav transitions between Model / Takeoff / Export are smooth

### Project Workspace (3D + List)
- [ ] 3D Workspace mode renders scene
- [ ] Space List mode renders correctly
- [ ] Add Space / Room button opens add flow
- [ ] Auto Arrange button works with multiple spaces
- [ ] Project snapshot metrics render (spaces/openings/gross/net/volume)

### 3D Builder
- [ ] Camera gestures work (orbit/zoom)
- [ ] View presets work (Iso / Front / Top / Side)
- [ ] Object selection works
- [ ] Transform sliders update preview
- [ ] Nudge controls (X/Z/Raise/Lower) work
- [ ] Yaw snap controls apply expected orientation
- [ ] Apply Transform persists final transform

### Quick Room Wizard
- [ ] Add Space -> Quick Room opens wizard
- [ ] Live Room Summary updates as dimensions change
- [ ] Presets apply dimensions/door/window defaults
- [ ] Optional wall editing works
- [ ] Optional custom doors/windows work
- [ ] Optional ceiling toggle works
- [ ] Continue to next room keeps wizard open and resets entry state
- [ ] Create Room adds expected spaces to project

### Space Editor
- [ ] Open existing space editor from list/3D object actions
- [ ] Geometry editing works for supported geometry types
- [ ] Opening edits save correctly
- [ ] Duplicate and delete actions work

### Takeoff
- [ ] Trade scope cards switch between drywall/concrete/gravel-paint workflows
- [ ] "Detected Scope" card displays matching project geometry context
- [ ] Parameter edits recalculate results
- [ ] Result Snapshot and detailed lines render correctly
- [ ] Job Cost Stack values render correctly when costs present

### Export
- [ ] Export screen opens without errors
- [ ] Preview content loads
- [ ] Copy/share actions function
- [ ] CSV/PDF generation path behaves as expected

---

## 4) Regression Risk Areas

Retest these whenever related code changes:

- 3D color rendering and selection highlighting
- Quick Room validation + opening distribution logic
- Takeoff type switching and live recomputation
- Space transform persistence in DataStore

---

## 5) Play Submission QA Gate

Before submitting to Play Console, ensure:

- [ ] Release AAB built and signed (`bundleRelease`)
- [ ] Final smoke test run on same commit used for release
- [ ] No startup crash in logcat
- [ ] All listing screenshots represent current UI
- [ ] Privacy policy URL resolves publicly

---

## 6) Notes

- If a crash occurs, capture full stack trace first and patch from root cause.
- Any UI flow changes should trigger screenshot refresh (`scripts/04-capture-screenshots.ps1`).

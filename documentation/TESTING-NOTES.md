# Testing Notes - TradeSketch Estimator

## Test Strategy

TradeSketch Estimator uses a combination of automated unit tests and manual testing to ensure quality and reliability.

## Automated Testing

### Unit Tests

Located in: `app/src/test/java/com/tradesketch/estimator/`

#### Domain Logic Tests (`domain/calc/TakeoffCalculatorTest.kt`)

**Test Coverage:**
- ✅ Drywall takeoff with waste percentage
- ✅ Drywall takeoff with openings (doors/windows)
- ✅ Concrete takeoff calculations
- ✅ Gravel/mulch takeoff with density
- ✅ Paint takeoff with multiple coats
- ✅ Negative input handling (all clamped to zero)

**Running Tests:**
```bash
./gradlew test
./gradlew testDebugUnitTest
```

**Expected Results:** All tests pass (0 failures)

#### Unit Conversion Tests (if added)

- Millimeters ↔ Feet/Inches
- Square feet calculations
- Cubic feet/yards calculations
- Money cents ↔ Dollars

### Test Results Location

```
app/build/reports/tests/testDebugUnitTest/index.html
```

## Manual Testing

### Test Environment

- **Devices:** Minimum 2 devices with different screen sizes
  - Phone (5"-6.5")
  - Tablet (7"+) if available
- **Android Versions:** Test on API 26, 29, 31, 34
- **Network:** Test in airplane mode to verify offline functionality
- **Accessibility:** Test with TalkBack enabled

### Pre-Release Testing Checklist

#### 1. Installation & Launch
- [ ] App installs successfully from AAB
- [ ] App launches without crash
- [ ] No permission dialogs on first launch
- [ ] App icon displays correctly
- [ ] App name displays correctly in launcher

#### 2. Projects List Screen
- [ ] Empty state displays correct message
- [ ] "Create blank project" button works
- [ ] Template cards display correctly
- [ ] Each template "Use" button creates project
- [ ] Recent projects list shows created projects
- [ ] FAB (floating action button) visible
- [ ] FAB creates new blank project

#### 3. Template Projects
Test each template:

**Bedroom Template:**
- [ ] Creates 4 walls + ceiling
- [ ] Walls have correct dimensions (12'×8', 10'×8')
- [ ] Door opening present (3'×7')
- [ ] Window openings present (4'×5', count=2)

**Garage Template:**
- [ ] Creates 20'×20' slab
- [ ] Thickness is 4" (0.33')

**Driveway Template:**
- [ ] Creates 40'×12' slab
- [ ] Thickness is 4" (0.33')

**Yard Bed Template:**
- [ ] Creates 15'×8' rectangular bed

#### 4. Project Detail Navigation
- [ ] Bottom navigation shows 3 tabs: Model, Takeoff, Export
- [ ] Tapping each tab switches content
- [ ] Selected tab highlighted
- [ ] Navigation state persists on rotation

#### 5. Model Screen
- [ ] Shows list of spaces in project
- [ ] "Add Space" button visible
- [ ] Can add new space (future implementation)
- [ ] Can edit space dimensions (future implementation)
- [ ] Can delete space (future implementation)
- [ ] Live area/volume preview updates (future implementation)

#### 6. Takeoff Screen
- [ ] Preset selector shows: Drywall, Concrete, Gravel/Mulch, Paint
- [ ] Selecting preset shows relevant parameters
- [ ] Default values are reasonable
- [ ] Can edit waste percentage
- [ ] Can edit material parameters
- [ ] Calculations update in real-time
- [ ] Results card shows totals
- [ ] Item list shows individual quantities
- [ ] Quantities are formatted correctly
- [ ] Units display correctly
- [ ] Disclaimer text visible: "Estimate only—verify onsite"

#### 7. Takeoff Calculations
Test with known values:

**Drywall:**
- 12'×8' wall = 96 sq ft
- With 10% waste, 32 sq ft sheets = 4 sheets ✓
- 32 screws/sheet × 4 = 128 screws ✓

**Concrete:**
- 20'×20'×4" slab = 400 sq ft × 0.33 ft = 132 cu ft
- 132 / 27 = 4.89 cubic yards
- With 5% waste = 5.13 cubic yards ✓

**Paint:**
- 100 sq ft wall, 350 sq ft/gal coverage, 2 coats
- 100 × 2 / 350 = 0.57 gallons ✓

#### 8. Export Screen
- [ ] "Copy summary" button works
  - [ ] Copies text to clipboard
  - [ ] Shows confirmation toast
- [ ] "Share" button works
  - [ ] Opens Android share sheet
  - [ ] Text contains project name, items, totals
- [ ] "Export CSV" button works
  - [ ] Opens SAF file picker
  - [ ] Creates .csv file
  - [ ] CSV contains correct data
  - [ ] CSV opens in spreadsheet app
- [ ] "Export PDF" button works
  - [ ] Opens SAF file picker
  - [ ] Creates .pdf file
  - [ ] PDF contains project name, date, items, totals
  - [ ] PDF contains disclaimer footer
  - [ ] PDF opens in PDF viewer

#### 9. Settings/About Screen
- [ ] Accessible from top bar menu
- [ ] Default settings section shows:
  - [ ] Unit preference (Imperial/Metric) - if implemented
  - [ ] Default waste percentage
- [ ] Privacy policy viewer works
  - [ ] Loads privacy-policy.html from assets
  - [ ] Displays correctly
  - [ ] Scrollable
- [ ] OSS licenses button works
  - [ ] Shows list of libraries
  - [ ] Shows license text
- [ ] App version displayed
- [ ] Support email link works (opens email app)

#### 10. Data Persistence
- [ ] Create a project, close app, reopen
- [ ] Project persists
- [ ] Edit project, close app, reopen
- [ ] Changes persist
- [ ] Delete project, close app, reopen
- [ ] Project deleted

#### 11. Offline Functionality
- [ ] Enable airplane mode
- [ ] All features work
- [ ] No error messages about network
- [ ] No loading spinners waiting for network

#### 12. Error Handling
- [ ] Enter invalid dimensions (negative, zero, non-numeric)
  - [ ] Input validation prevents or corrects
  - [ ] Friendly error message shown
  - [ ] No crashes
- [ ] Try to export with no spaces
  - [ ] Graceful handling
  - [ ] Shows appropriate message
- [ ] Fill out space with extreme values (999999)
  - [ ] Calculations handle correctly
  - [ ] No overflow errors

#### 13. Accessibility
- [ ] Enable TalkBack
- [ ] All interactive elements announced
- [ ] All buttons have content descriptions
- [ ] Touch targets are ≥48dp
- [ ] Focus order logical
- [ ] Text contrast sufficient (WCAG AA)
- [ ] Text size respects system font size

#### 14. UI/UX
- [ ] Material Design 3 theming consistent
- [ ] Dynamic color (Android 12+) works if enabled
- [ ] Dark mode supported
- [ ] Animations smooth
- [ ] No UI jank or lag
- [ ] Empty states helpful
- [ ] Error states clear
- [ ] Loading states appropriate

#### 15. Rotation & Configuration Changes
- [ ] Rotate device on each screen
- [ ] State persists
- [ ] No crashes
- [ ] No memory leaks

#### 16. Performance
- [ ] App startup < 2 seconds (cold start)
- [ ] Takeoff calculations < 200ms
- [ ] No frame drops during scrolling
- [ ] Memory usage reasonable (< 100MB typical)
- [ ] No ANRs (Application Not Responding)

#### 17. Permissions
- [ ] No permission dialogs on app launch
- [ ] SAF permission only when exporting
- [ ] Permission rationale shown if needed
- [ ] Can deny export permission - app continues working

## Edge Cases

### Dimension Edge Cases
- [ ] Very small dimensions (0.1 ft)
- [ ] Very large dimensions (1000 ft)
- [ ] Zero dimensions (should handle gracefully)
- [ ] Negative dimensions (should be prevented or clamped)

### Calculation Edge Cases
- [ ] Zero waste percentage
- [ ] 100% waste percentage
- [ ] Very high waste (500%)
- [ ] Division by zero scenarios (coverage=0)
- [ ] Overflow scenarios (gigantic projects)

### Data Edge Cases
- [ ] Empty project (no spaces)
- [ ] Project with 100+ spaces
- [ ] Very long project names
- [ ] Special characters in project names
- [ ] Emoji in project names

## Known Issues / Limitations

### v1.0.0
- None at release (target)

## Regression Testing

When making changes, re-test:
1. Affected feature
2. Related features
3. Critical path: Create template → Takeoff → Export
4. All unit tests

## Bug Reporting Template

When reporting bugs, include:
- **Device:** Model, Android version
- **Steps to reproduce:** Detailed steps
- **Expected behavior:** What should happen
- **Actual behavior:** What actually happened
- **Screenshots:** If UI-related
- **Logs:** From `adb logcat` if crash

## Test Metrics

Target metrics for release:
- **Unit test coverage:** ≥ 80% for domain layer
- **Manual test pass rate:** 100% of checklist items
- **Critical bugs:** 0
- **Major bugs:** 0
- **Minor bugs:** ≤ 3 (documented and accepted)
- **Crash rate:** < 0.1% (post-launch)

## Post-Launch Monitoring

Even without analytics, monitor:
- Play Console crash reports
- User reviews mentioning bugs
- Support emails

## Future Test Additions

- UI tests with Compose Testing
- Integration tests for DataStore
- Screenshot tests for regression
- Performance benchmarks

---

**Last Updated:** February 8, 2026

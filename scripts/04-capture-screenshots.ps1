<#
.SYNOPSIS
    TradeSketch Estimator - Screenshot Capture
    Automates capturing all 6 Play Store screenshots from a connected device.

.DESCRIPTION
    This script:
      1. Connects to your Android device/emulator via ADB
      2. Puts the status bar into clean "demo mode" (full battery, clean clock, no notifications)
      3. Walks you screen-by-screen through the app
      4. Captures each screenshot automatically when you press Enter
      5. Pulls the screenshots to store-assets\screenshots\ on your PC
      6. Exits demo mode when done

    BEFORE RUNNING: Install the app on your phone/emulator and open it at least once.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$screenshotDir = Join-Path $projectRoot "store-assets\screenshots"

# Find ADB
$adb = "adb"
if ($env:ANDROID_HOME) {
    $adbPath = "$env:ANDROID_HOME\platform-tools\adb.exe"
    if (Test-Path $adbPath) { $adb = $adbPath }
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Screenshot Capture" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# ── CHECK DEVICE ─────────────────────────────────────────────────────────────

Write-Host "[Step 1] Checking for connected device..." -ForegroundColor Yellow

try {
    $deviceList = & $adb devices 2>&1
    $connected = $deviceList | Select-String "device$"
    if (-not $connected) {
        Write-Host ""
        Write-Host "  NO DEVICE FOUND." -ForegroundColor Red
        Write-Host ""
        Write-Host "  Option A - Use a REAL PHONE:" -ForegroundColor White
        Write-Host "    1. On your phone, go to Settings > About Phone" -ForegroundColor DarkGray
        Write-Host "    2. Tap 'Build Number' 7 times to enable Developer Options" -ForegroundColor DarkGray
        Write-Host "    3. Go to Settings > Developer Options" -ForegroundColor DarkGray
        Write-Host "    4. Turn on 'USB Debugging'" -ForegroundColor DarkGray
        Write-Host "    5. Connect your phone to your PC with a USB cable" -ForegroundColor DarkGray
        Write-Host "    6. Tap 'Allow' on the USB debugging prompt on your phone" -ForegroundColor DarkGray
        Write-Host ""
        Write-Host "  Option B - Use an EMULATOR:" -ForegroundColor White
        Write-Host "    1. Open Android Studio" -ForegroundColor DarkGray
        Write-Host "    2. Click the 'Device Manager' icon (phone with Android logo)" -ForegroundColor DarkGray
        Write-Host "    3. Click 'Create Virtual Device'" -ForegroundColor DarkGray
        Write-Host "    4. Choose 'Pixel 7' > Next > Select 'API 35' image > Next > Finish" -ForegroundColor DarkGray
        Write-Host "    5. Click the Play button to start the emulator" -ForegroundColor DarkGray
        Write-Host ""
        Write-Host "  Then run this script again." -ForegroundColor Yellow
        exit 1
    }
    Write-Host "  PASS: Device connected." -ForegroundColor Green
} catch {
    Write-Host "  FAIL: ADB not found. Is Android SDK installed?" -ForegroundColor Red
    exit 1
}

# ── CREATE OUTPUT DIRECTORY ──────────────────────────────────────────────────

if (-not (Test-Path $screenshotDir)) {
    New-Item -ItemType Directory -Path $screenshotDir | Out-Null
}

# ── ENTER DEMO MODE ─────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[Step 2] Setting up clean status bar (demo mode)..." -ForegroundColor Yellow

& $adb shell settings put global sysui_demo_allowed 1
& $adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
& $adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
& $adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
& $adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
& $adb shell am broadcast -a com.android.systemui.demo -e command status -e volume hide

Write-Host "  DONE: Status bar is now clean (9:41, full battery, Wi-Fi, no notifications)." -ForegroundColor Green

# ── LAUNCH THE APP ───────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[Step 3] Launching TradeSketch Estimator..." -ForegroundColor Yellow
& $adb shell am start -n com.tradesketch.estimator/.MainActivity 2>&1 | Out-Null
Start-Sleep -Seconds 2
Write-Host "  App launched." -ForegroundColor Green

# ── CAPTURE SCREENSHOTS ──────────────────────────────────────────────────────

$screenshots = @(
    @{
        File = "01_projects.png"
        Screen = "PROJECTS LIST (home screen)"
        Instructions = @(
            "You should see the Projects list screen right now."
            "Make sure the template cards are visible (Bedroom, Garage, Driveway, Yard Bed)."
            "If the screen looks empty, that's fine - templates should still show."
        )
    },
    @{
        File = "02_spaces.png"
        Screen = "PROJECT DETAIL - SPACES LIST"
        Instructions = @(
            "Tap on a template (e.g. 'Bedroom') to create a project from it."
            "You should see the project detail screen showing the list of spaces"
            "with their dimensions (walls, ceiling, etc.)."
        )
    },
    @{
        File = "03_editor.png"
        Screen = "SPACE EDITOR"
        Instructions = @(
            "Tap on any space to open the editor."
            "You should see dimension input fields (length, width, height)."
            "The openings section (doors/windows) should be visible."
            "A live area calculation should appear."
        )
    },
    @{
        File = "04_drywall.png"
        Screen = "DRYWALL TAKEOFF RESULTS"
        Instructions = @(
            "Go back to the project detail, then tap the Takeoff tab."
            "Select the 'Drywall' preset."
            "You should see results: sheets, screws, joint compound."
            "Parameters like waste% and sheet size should be visible."
        )
    },
    @{
        File = "05_concrete.png"
        Screen = "CONCRETE TAKEOFF RESULTS"
        Instructions = @(
            "Switch the preset to 'Concrete'."
            "You should see cubic yards calculated."
            "If you used a bedroom template, go back and create a 'Garage'"
            "template project first, then view its concrete takeoff."
        )
    },
    @{
        File = "06_export.png"
        Screen = "EXPORT OPTIONS"
        Instructions = @(
            "Navigate to the Export screen."
            "You should see buttons for: Copy, Share, CSV, PDF."
            "A preview of the estimate summary text should be visible."
            "The disclaimer at the bottom should be visible."
        )
    }
)

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  SCREENSHOT CAPTURE" -ForegroundColor Cyan
Write-Host "  I'll walk you through each screen. Navigate to the right" -ForegroundColor White
Write-Host "  screen on your phone, then press ENTER to capture." -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Cyan

foreach ($i in 0..($screenshots.Count - 1)) {
    $shot = $screenshots[$i]
    $num = $i + 1

    Write-Host ""
    Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host "  Screenshot $num of 6: $($shot.Screen)" -ForegroundColor Yellow
    Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    foreach ($line in $shot.Instructions) {
        Write-Host "  >> $line" -ForegroundColor White
    }
    Write-Host ""
    Read-Host "  Press ENTER when the screen is ready"

    Write-Host "  Capturing..." -ForegroundColor DarkGray
    $devicePath = "/sdcard/tradesketch_screenshot.png"
    & $adb shell screencap -p $devicePath
    $localPath = Join-Path $screenshotDir $shot.File
    & $adb pull $devicePath $localPath 2>&1 | Out-Null
    & $adb shell rm $devicePath

    if (Test-Path $localPath) {
        $size = [math]::Round((Get-Item $localPath).Length / 1KB, 1)
        Write-Host "  SAVED: $($shot.File) (${size} KB)" -ForegroundColor Green
    } else {
        Write-Host "  WARNING: Screenshot may not have saved correctly." -ForegroundColor Red
    }
}

# ── EXIT DEMO MODE ───────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[Step 4] Exiting demo mode..." -ForegroundColor Yellow
& $adb shell am broadcast -a com.android.systemui.demo -e command exit
Write-Host "  Status bar restored to normal." -ForegroundColor Green

# ── SUMMARY ──────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  SCREENSHOTS COMPLETE!" -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
Write-Host "  Saved to: $screenshotDir" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
$captured = Get-ChildItem $screenshotDir -Filter "*.png" -ErrorAction SilentlyContinue
foreach ($f in $captured) {
    $size = [math]::Round($f.Length / 1KB, 1)
    Write-Host "    $($f.Name) ($size KB)" -ForegroundColor White
}
Write-Host "" -ForegroundColor Cyan
Write-Host "  Optional: Add text overlays using Canva or Figma." -ForegroundColor DarkGray
Write-Host "  Next step: Run .\05-deploy-privacy-policy.ps1" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

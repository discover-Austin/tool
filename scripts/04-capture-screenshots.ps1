param(
    [string]$PackageActivity = "com.tradesketch.estimator.local/com.tradesketch.estimator.MainActivity",
    [string]$ExpectedPackage = "com.tradesketch.estimator.local",
    [string]$SnapshotLabel = "play-store-v1.0.22-build-24"
)

<#
.SYNOPSIS
    TradeSketch Estimator - Screenshot Capture
    Automates capturing all 8 Play Store screenshots from a connected device.

.DESCRIPTION
    This script:
      1. Connects to your Android device/emulator via ADB
      2. Puts the status bar into clean "demo mode" (full battery, clean clock, no notifications)
      3. Walks you screen-by-screen through the app
      4. Captures each screenshot automatically when you press Enter
      5. Pulls the screenshots to store-assets\screenshots\ on your PC
      6. Exits demo mode when done

    BEFORE RUNNING: Install the newest signed sideload build on your phone/emulator and open it at least once.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator.local
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$screenshotDir = Join-Path $projectRoot "store-assets\screenshots"
$manifestPath = Join-Path $screenshotDir "LATEST_SCREENSHOT_SNAPSHOT.txt"

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
& $adb shell am start -n $PackageActivity 2>&1 | Out-Null
Start-Sleep -Seconds 2
Write-Host "  App launched." -ForegroundColor Green

$deviceModel = (& $adb shell getprop ro.product.model).Trim()
$captureStarted = Get-Date

function Assert-TradeSketchForeground {
    param(
        [string]$Reason
    )
    $windowDump = & $adb shell dumpsys window windows 2>&1
    $focusLine = $windowDump |
        Select-String -Pattern "mCurrentFocus|mFocusedApp" |
        Select-Object -First 1
    $focusText = if ($focusLine) { $focusLine.ToString() } else { "" }
    if ($focusText -notmatch [regex]::Escape($ExpectedPackage)) {
        Write-Host ""
        Write-Host "  WARNING: TradeSketch does not appear to be in the foreground." -ForegroundColor Yellow
        Write-Host "  Focus line: $focusText" -ForegroundColor DarkGray
        Write-Host "  Expected package: $ExpectedPackage" -ForegroundColor White
        Write-Host "  Reason: $Reason" -ForegroundColor White
        $continue = Read-Host "  Continue anyway? (y/N)"
        if ($continue -notin @("y", "Y")) {
            throw "Screenshot capture aborted because TradeSketch was not foreground."
        }
    }
}

# ── CAPTURE SCREENSHOTS ──────────────────────────────────────────────────────

$screenshots = @(
    @{
        File = "01_welcome.png"
        Screen = "WELCOME HOME"
        Instructions = @(
            "Launch the app to the welcome screen."
            "Keep the TradeSketch header and the first-run value proposition visible."
            "Show this as the clean starting point for new users."
        )
    },
    @{
        File = "02_saved_projects.png"
        Screen = "SAVED PROJECTS ON HOME"
        Instructions = @(
            "Stay on the welcome screen with at least one saved project visible."
            "Keep the saved-project card and tap-to-return workflow readable."
            "Show this as the quick reopen path for repeat jobs."
        )
    },
    @{
        File = "03_project_type.png"
        Screen = "PROJECT TYPE CHOICE"
        Instructions = @(
            "Open the project-type selection step."
            "Keep both blueprint and direct-entry choices visible."
            "Show the app supports both measured takeoffs and fast manual entry."
        )
    },
    @{
        File = "04_blueprint_overview.png"
        Screen = "BLUEPRINT OVERVIEW"
        Instructions = @(
            "Enter an active blueprint project."
            "Show walls, rooms, or openings with the top overlays visible."
            "Keep floor and scale controls readable."
        )
    },
    @{
        File = "05_blueprint_controls.png"
        Screen = "BLUEPRINT EDITING CONTROLS"
        Instructions = @(
            "Open one editing panel such as Doors, Windows, or Params."
            "Keep the bottom tool rail and precision controls visible."
            "Show this as an active drafting screen, not an empty canvas."
        )
    },
    @{
        File = "06_materials.png"
        Screen = "MATERIALS AND PRICING"
        Instructions = @(
            "Open the Materials tab for a real project."
            "Show linked quantities, pricing controls, and totals together."
            "Keep the screen focused on trust-building estimate detail."
        )
    },
    @{
        File = "07_export.png"
        Screen = "EXPORT OPTIONS"
        Instructions = @(
            "Navigate to the Export screen."
            "Show Share/Save actions for Estimate PDF, Blueprint PNG/PDF, CSV, and JSON."
            "Keep the preview deck and action card in the shot."
        )
    },
    @{
        File = "08_settings.png"
        Screen = "SETTINGS AND HELP"
        Instructions = @(
            "Navigate to the Settings screen."
            "Show preferences, reduced motion, and the Help & Onboarding card."
            "Keep the replay-tour or support actions visible."
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
    Write-Host "  Screenshot $num of 8: $($shot.Screen)" -ForegroundColor Yellow
    Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    foreach ($line in $shot.Instructions) {
        Write-Host "  >> $line" -ForegroundColor White
    }
    Write-Host ""
    Read-Host "  Press ENTER when the screen is ready"
    Assert-TradeSketchForeground -Reason "Capturing $($shot.File)"

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

@(
    "Snapshot Label: $SnapshotLabel"
    "Captured At: $($captureStarted.ToString("yyyy-MM-dd HH:mm:ss zzz"))"
    "Device Model: $deviceModel"
    "Expected Package: $ExpectedPackage"
    "Activity: $PackageActivity"
    "Rule: newest signed build only"
    ""
    "Files:"
) + ($captured | Sort-Object Name | ForEach-Object {
    "$($_.Name)`t$($_.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss zzz"))`t$($_.Length)"
}) | Set-Content -Path $manifestPath

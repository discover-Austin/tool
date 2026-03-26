param(
    [string]$OutputDir = "C:\Users\grand\tool\media\play_store_showcase",
    [string]$PackageActivity = "com.tradesketch.estimator.local/com.tradesketch.estimator.MainActivity",
    [string]$ExpectedPackage = "com.tradesketch.estimator.local",
    [string]$SnapshotLabel = "play-store-v1.0.19-build-21"
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & adb @Args | Out-Null
}

function Tap {
    param(
        [int]$X,
        [int]$Y,
        [int]$DelayMs = 700
    )
    Invoke-Adb shell input tap $X $Y
    Start-Sleep -Milliseconds $DelayMs
}

function KeyEvent {
    param(
        [int]$Code,
        [int]$DelayMs = 300
    )
    Invoke-Adb shell input keyevent $Code
    Start-Sleep -Milliseconds $DelayMs
}

function Input-Text {
    param(
        [string]$Text,
        [int]$DelayMs = 800
    )
    Invoke-Adb shell input text $Text
    Start-Sleep -Milliseconds $DelayMs
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$deviceFile = "/sdcard/tradesketch_showcase_raw.mp4"
$hostFile = Join-Path $OutputDir "tradesketch_showcase_raw.mp4"
$manifestFile = Join-Path $OutputDir "LATEST_SHOWCASE_SNAPSHOT.txt"
$deviceModel = (& adb shell getprop ro.product.model).Trim()
$captureStarted = Get-Date

Invoke-Adb @("shell", "rm", "-f", $deviceFile)
Invoke-Adb @("shell", "settings", "put", "system", "show_touches", "1")
Invoke-Adb @("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "clock", "-e", "hhmm", "0941")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "notifications", "-e", "visible", "false")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "status", "-e", "volume", "hide")
Invoke-Adb @("shell", "am", "start", "-n", $PackageActivity)
Start-Sleep -Seconds 2

$screenRecord = Start-Process adb `
    -ArgumentList @("shell", "screenrecord", "--bit-rate", "16000000", "--time-limit", "30", $deviceFile) `
    -PassThru `
    -WindowStyle Hidden

Start-Sleep -Seconds 2

# Establish blueprint overview.
Tap 120 1178 1300

# Shape tools: open params, select curved wall, close panel.
Tap 840 2835 1100
Tap 625 1158 900
Tap 840 2835 900

# Curved wall: tap start, end, then bend.
Tap 500 1800 700
Tap 970 1800 700
Tap 735 1380 1400

# Reopen params, switch to circle, close panel.
Tap 840 2835 1100
Tap 565 1345 900
Tap 840 2835 900

# Circle: tap center, then radius.
Tap 720 1710 700
Tap 960 1710 1400

# Materials: edit a manual quantity.
Tap 120 1338 1400
Tap 846 2404 700
KeyEvent 123 250
KeyEvent 67 250
KeyEvent 67 250
KeyEvent 67 250
Input-Text "125"
KeyEvent 66 1200

# Export: hold the final summary view.
Tap 120 1505 1600
Start-Sleep -Seconds 5

$screenRecord.WaitForExit()
Invoke-Adb @("pull", $deviceFile, $hostFile)
Invoke-Adb @("shell", "settings", "put", "system", "show_touches", "0")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit")

@(
    "Snapshot Label: $SnapshotLabel"
    "Captured At: $($captureStarted.ToString("yyyy-MM-dd HH:mm:ss zzz"))"
    "Device Model: $deviceModel"
    "Expected Package: $ExpectedPackage"
    "Activity: $PackageActivity"
    "Raw Video: $hostFile"
    "Rule: newest signed build only"
) | Set-Content -Path $manifestFile

Write-Output $hostFile

param(
    [string]$OutputDir = "",
    [string]$PackageActivity = "com.tradesketch.estimator.local/com.tradesketch.estimator.MainActivity",
    [string]$ExpectedPackage = "com.tradesketch.estimator.local",
    [string]$SnapshotLabel = "play-store-v1.0.22-build-24"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $projectRoot "media\play_store_showcase"
}

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
$relativeHostFile = [System.IO.Path]::GetRelativePath($projectRoot, $hostFile)
$deviceModel = (& adb shell getprop ro.product.model).Trim()
$captureStarted = Get-Date

Invoke-Adb @("shell", "rm", "-f", $deviceFile)
Invoke-Adb @("shell", "settings", "put", "system", "show_touches", "0")
Invoke-Adb @("shell", "settings", "put", "global", "sysui_demo_allowed", "1")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "clock", "-e", "hhmm", "0941")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "notifications", "-e", "visible", "false")
Invoke-Adb @("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "status", "-e", "volume", "hide")
Invoke-Adb @("shell", "am", "start", "-n", $PackageActivity)
Start-Sleep -Seconds 2

# Load the richer saved project before recording.
Tap 120 1085 1200
Tap 749 1092 2200
Tap 120 1250 1200

$screenRecord = Start-Process adb `
    -ArgumentList @("shell", "screenrecord", "--bit-rate", "16000000", "--time-limit", "30", $deviceFile) `
    -PassThru `
    -WindowStyle Hidden

Start-Sleep -Seconds 4

# Blueprint precision controls.
Tap 274 3005 900
Start-Sleep -Seconds 2

# Materials overview and result stack.
Tap 120 1415 1800
Start-Sleep -Seconds 2
Invoke-Adb shell input swipe 720 2350 720 1150 500
Start-Sleep -Seconds 3
Invoke-Adb shell input swipe 720 2450 720 900 500
Start-Sleep -Seconds 1

# Export preview and file actions.
Tap 120 1575 1800
Start-Sleep -Seconds 2
Start-Sleep -Seconds 1
Invoke-Adb shell input swipe 720 2550 720 850 500
Start-Sleep -Seconds 3

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
    "Raw Video: $relativeHostFile"
    "Rule: newest signed build only"
) | Set-Content -Path $manifestFile

Write-Output $hostFile

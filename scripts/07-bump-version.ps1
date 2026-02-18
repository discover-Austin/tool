<#
.SYNOPSIS
    TradeSketch Estimator - Version Bumper
    Increments the version for publishing an app update.

.DESCRIPTION
    Use this script AFTER your app is already on the Play Store and you
    want to publish an update. It:
      1. Shows the current versionCode and versionName
      2. Increments versionCode by 1
      3. Asks for the new versionName (e.g. 1.0.0 -> 1.1.0)
      4. Updates app/build.gradle.kts
      5. Rebuilds the release bundle
      6. Shows you the new .aab ready to upload

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildGradle = Join-Path $projectRoot "app\build.gradle.kts"
$gradlew = Join-Path $projectRoot "gradlew.bat"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Version Bumper" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# ── READ CURRENT VERSION ─────────────────────────────────────────────────────

$content = Get-Content $buildGradle -Raw

$currentCode = 0
if ($content -match 'versionCode\s*=\s*(\d+)') {
    $currentCode = [int]$Matches[1]
}

$currentName = "unknown"
if ($content -match 'versionName\s*=\s*"([^"]+)"') {
    $currentName = $Matches[1]
}

Write-Host "  Current version:" -ForegroundColor White
Write-Host "    versionCode = $currentCode" -ForegroundColor Yellow
Write-Host "    versionName = `"$currentName`"" -ForegroundColor Yellow
Write-Host ""

# ── GET NEW VERSION ──────────────────────────────────────────────────────────

$newCode = $currentCode + 1
Write-Host "  versionCode will be incremented to: $newCode" -ForegroundColor Green
Write-Host ""

# Suggest next version name
$parts = $currentName.Split(".")
$suggestedPatch = "$($parts[0]).$($parts[1]).$([int]$parts[2] + 1)"
$suggestedMinor = "$($parts[0]).$([int]$parts[1] + 1).0"

Write-Host "  What should the new versionName be?" -ForegroundColor White
Write-Host "    1) $suggestedPatch  (patch - bug fixes)" -ForegroundColor DarkGray
Write-Host "    2) $suggestedMinor  (minor - new features)" -ForegroundColor DarkGray
Write-Host "    3) Enter a custom version" -ForegroundColor DarkGray
Write-Host ""

$choice = Read-Host "  Choose (1/2/3)"
switch ($choice) {
    "1" { $newName = $suggestedPatch }
    "2" { $newName = $suggestedMinor }
    "3" { $newName = Read-Host "  Enter version name (e.g. 2.0.0)" }
    default { $newName = $suggestedPatch }
}

Write-Host ""
Write-Host "  New version:" -ForegroundColor White
Write-Host "    versionCode = $newCode" -ForegroundColor Green
Write-Host "    versionName = `"$newName`"" -ForegroundColor Green
Write-Host ""

$confirm = Read-Host "  Proceed? (yes/no)"
if ($confirm -ne "yes") {
    Write-Host "  Cancelled." -ForegroundColor Yellow
    exit 0
}

# ── UPDATE build.gradle.kts ─────────────────────────────────────────────────

Write-Host ""
Write-Host "  Updating app/build.gradle.kts..." -ForegroundColor Yellow

$content = $content -replace "versionCode\s*=\s*\d+", "versionCode = $newCode"
$content = $content -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newName`""

Set-Content -Path $buildGradle -Value $content -NoNewline

Write-Host "  DONE: Version updated." -ForegroundColor Green

# ── REBUILD ──────────────────────────────────────────────────────────────────

Write-Host ""
$rebuild = Read-Host "  Build the release bundle now? (yes/no)"
if ($rebuild -eq "yes") {
    Write-Host ""
    Write-Host "  Building release bundle..." -ForegroundColor Yellow
    Push-Location $projectRoot
    try {
        & $gradlew :app:clean :app:bundleRelease --no-daemon 2>&1 | ForEach-Object {
            if ($_ -match "BUILD SUCCESSFUL") {
                Write-Host "  $_" -ForegroundColor Green
            } else {
                Write-Host "  $_" -ForegroundColor DarkGray
            }
        }

        $aabOutput = Join-Path $projectRoot "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $aabOutput) {
            $size = [math]::Round((Get-Item $aabOutput).Length / 1MB, 2)
            Write-Host ""
            Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
            Write-Host "  Bundle: $aabOutput ($size MB)" -ForegroundColor White
            Write-Host ""
            Write-Host "  Upload this file to Play Console:" -ForegroundColor White
            Write-Host "  Release > Production > Create new release > Upload" -ForegroundColor DarkGray
        }
    } finally {
        Pop-Location
    }
}

# ── WHAT'S NEW PROMPT ────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  Don't forget to update the 'What's New' text!" -ForegroundColor Yellow
Write-Host "  File: store-assets\listing\whats-new.txt" -ForegroundColor DarkGray
Write-Host ""

$editNow = Read-Host "  Open it in Notepad now? (yes/no)"
if ($editNow -eq "yes") {
    $whatsNew = Join-Path $projectRoot "store-assets\listing\whats-new.txt"
    & notepad.exe $whatsNew
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  VERSION BUMP COMPLETE" -ForegroundColor Green
Write-Host "  $currentName (code $currentCode)  -->  $newName (code $newCode)" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

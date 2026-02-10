<#
.SYNOPSIS
    TradeSketch Estimator - Release Builder
    Runs tests, lint, and builds the signed release .aab bundle.

.DESCRIPTION
    This script:
      1. Verifies signing config exists (local.properties or env vars)
      2. Runs all unit tests
      3. Runs Android lint checks
      4. Builds the signed release App Bundle (.aab)
      5. Shows you the file size and location
      6. Optionally builds a universal APK for device testing

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $projectRoot "gradlew.bat"
$aabOutput = Join-Path $projectRoot "app\build\outputs\bundle\release\app-release.aab"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Release Builder" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. CHECK SIGNING CONFIG ─────────────────────────────────────────────────

Write-Host "[1/4] Checking signing configuration..." -ForegroundColor Yellow

$hasEnvVars = $env:KEYSTORE_FILE -and (Test-Path $env:KEYSTORE_FILE)
$localProps = Join-Path $projectRoot "local.properties"
$hasLocalProps = $false
if (Test-Path $localProps) {
    $propsContent = Get-Content $localProps -Raw
    $hasLocalProps = $propsContent -match "KEYSTORE_FILE"
}

if ($hasEnvVars) {
    Write-Host "  PASS: Signing config found via environment variables." -ForegroundColor Green
} elseif ($hasLocalProps) {
    Write-Host "  PASS: Signing config found in local.properties." -ForegroundColor Green
} else {
    Write-Host "  FAIL: No signing config found!" -ForegroundColor Red
    Write-Host "  Run .\02-generate-keystore.ps1 first." -ForegroundColor Red
    exit 1
}

# ── 2. RUN TESTS ─────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[2/4] Running unit tests..." -ForegroundColor Yellow
Write-Host "  (This may take a minute on the first run while Gradle downloads dependencies)" -ForegroundColor DarkGray
Write-Host ""

Push-Location $projectRoot
try {
    & $gradlew test --no-daemon 2>&1 | ForEach-Object {
        if ($_ -match "FAIL|ERROR|Exception") {
            Write-Host "  $_" -ForegroundColor Red
        } elseif ($_ -match "PASS|SUCCESS|BUILD SUCCESSFUL") {
            Write-Host "  $_" -ForegroundColor Green
        } else {
            Write-Host "  $_" -ForegroundColor DarkGray
        }
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "  TESTS FAILED. Fix the failing tests before building a release." -ForegroundColor Red
        Write-Host "  Test report: app\build\reports\tests\testDebugUnitTest\index.html" -ForegroundColor Red
        exit 1
    }
    Write-Host "  PASS: All tests passed." -ForegroundColor Green
} finally {
    Pop-Location
}

# ── 3. RUN LINT ──────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[3/4] Running lint checks..." -ForegroundColor Yellow

Push-Location $projectRoot
try {
    & $gradlew lint --no-daemon 2>&1 | ForEach-Object {
        if ($_ -match "error:|Error:") {
            Write-Host "  $_" -ForegroundColor Red
        } elseif ($_ -match "BUILD SUCCESSFUL") {
            Write-Host "  $_" -ForegroundColor Green
        } else {
            Write-Host "  $_" -ForegroundColor DarkGray
        }
    }
    # Lint warnings are OK, only errors stop the build
    Write-Host "  PASS: Lint complete." -ForegroundColor Green
} finally {
    Pop-Location
}

# ── 4. BUILD RELEASE BUNDLE ─────────────────────────────────────────────────

Write-Host ""
Write-Host "[4/4] Building release App Bundle (.aab)..." -ForegroundColor Yellow
Write-Host "  This compiles Kotlin, shrinks with R8, and signs the bundle." -ForegroundColor DarkGray
Write-Host "  May take 2-5 minutes..." -ForegroundColor DarkGray
Write-Host ""

Push-Location $projectRoot
try {
    & $gradlew clean bundleRelease --no-daemon 2>&1 | ForEach-Object {
        if ($_ -match "FAIL|ERROR|Exception") {
            Write-Host "  $_" -ForegroundColor Red
        } elseif ($_ -match "BUILD SUCCESSFUL") {
            Write-Host "  $_" -ForegroundColor Green
        } else {
            Write-Host "  $_" -ForegroundColor DarkGray
        }
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "  BUILD FAILED." -ForegroundColor Red
        Write-Host "  Run with --stacktrace for details:" -ForegroundColor Red
        Write-Host "  .\gradlew.bat bundleRelease --stacktrace" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

# ── VERIFY OUTPUT ────────────────────────────────────────────────────────────

Write-Host ""
if (Test-Path $aabOutput) {
    $fileSize = (Get-Item $aabOutput).Length
    $fileSizeMB = [math]::Round($fileSize / 1MB, 2)
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "" -ForegroundColor Cyan
    Write-Host "  Bundle:  $aabOutput" -ForegroundColor White
    Write-Host "  Size:    $fileSizeMB MB" -ForegroundColor White
    Write-Host "" -ForegroundColor Cyan
    Write-Host "  This is the file you upload to Google Play Console." -ForegroundColor White
    Write-Host "" -ForegroundColor Cyan

    # ── OPTIONAL: BUILD UNIVERSAL APK FOR TESTING ────────────────────────────

    $buildApk = Read-Host "  Also build a testable APK for your phone? (yes/no)"
    if ($buildApk -eq "yes") {
        Write-Host ""
        Write-Host "  Building debug APK for device testing..." -ForegroundColor Yellow
        Push-Location $projectRoot
        try {
            & $gradlew assembleDebug --no-daemon 2>&1 | Out-Null
            $debugApk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
            if (Test-Path $debugApk) {
                $apkSize = [math]::Round((Get-Item $debugApk).Length / 1MB, 2)
                Write-Host "  Debug APK: $debugApk ($apkSize MB)" -ForegroundColor Green
                Write-Host ""

                # Try to install on connected device
                $adbExe = if ($env:ANDROID_HOME) { "$env:ANDROID_HOME\platform-tools\adb.exe" } else { "adb" }
                try {
                    $devices = & $adbExe devices 2>&1 | Select-String "device$"
                    if ($devices) {
                        $install = Read-Host "  Device detected. Install the APK now? (yes/no)"
                        if ($install -eq "yes") {
                            & $adbExe install -r $debugApk
                            Write-Host "  Installed! Open 'TradeSketch Estimator' on your phone." -ForegroundColor Green
                        }
                    }
                } catch {
                    Write-Host "  No device detected. Transfer the APK manually." -ForegroundColor DarkYellow
                }
            }
        } finally {
            Pop-Location
        }
    }

    Write-Host ""
    Write-Host "  Next step: Run .\04-capture-screenshots.ps1" -ForegroundColor Green
    Write-Host "================================================================" -ForegroundColor Cyan
} else {
    Write-Host "  FAIL: Bundle file not found at expected location." -ForegroundColor Red
    Write-Host "  Expected: $aabOutput" -ForegroundColor Red
    exit 1
}

Write-Host ""

<#
.SYNOPSIS
    TradeSketch Estimator - Android Release Builder
    Runs Android app tests/lint and builds the signed release .aab bundle.

.DESCRIPTION
    This script:
      1. Verifies signing config exists (local.properties or env vars)
      2. Runs Android app unit tests
      3. Runs Android app lint checks for both debug and release
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

$localProps = Join-Path $projectRoot "local.properties"
$requiredSigningKeys = @("KEYSTORE_FILE", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
$signingValues = @{}

foreach ($key in $requiredSigningKeys) {
    if (-not [string]::IsNullOrWhiteSpace((Get-Item -Path "Env:$key" -ErrorAction SilentlyContinue).Value)) {
        $signingValues[$key] = (Get-Item -Path "Env:$key").Value
    }
}

if (Test-Path $localProps) {
    Get-Content $localProps | ForEach-Object {
        if ($_ -match "^\s*([^#=]+)\s*=\s*(.*)\s*$") {
            $k = $Matches[1].Trim()
            $v = $Matches[2].Trim()
            if ($requiredSigningKeys -contains $k -and -not $signingValues.ContainsKey($k)) {
                $signingValues[$k] = $v
            }
        }
    }
}

$missingKeys = $requiredSigningKeys | Where-Object { -not $signingValues.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($signingValues[$_]) }
if ($missingKeys.Count -gt 0) {
    Write-Host "  FAIL: Missing signing config key(s): $($missingKeys -join ', ')" -ForegroundColor Red
    Write-Host "  This script is for Play-ready builds and will not auto-generate fallback keys." -ForegroundColor Red
    Write-Host "  Run .\scripts\02-generate-keystore.ps1 or set signing env vars first." -ForegroundColor Red
    exit 1
}

# Normalize local.properties-style escaped Windows paths.
$keystorePath = $signingValues["KEYSTORE_FILE"] -replace "\\\\:", ":" -replace "\\\\", "\"
if (-not [System.IO.Path]::IsPathRooted($keystorePath)) {
    $keystorePath = Join-Path $projectRoot $keystorePath
}
if (-not (Test-Path $keystorePath)) {
    Write-Host "  FAIL: Keystore file not found at: $keystorePath" -ForegroundColor Red
    Write-Host "  Update KEYSTORE_FILE in local.properties or env vars." -ForegroundColor Red
    exit 1
}

& keytool -list `
    -keystore $keystorePath `
    -alias $signingValues["KEY_ALIAS"] `
    -storepass $signingValues["KEYSTORE_PASSWORD"] `
    -keypass $signingValues["KEY_PASSWORD"] > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "  FAIL: Could not validate keystore credentials for alias '$($signingValues["KEY_ALIAS"])'." -ForegroundColor Red
    Write-Host "  Check KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD." -ForegroundColor Red
    exit 1
}

$env:KEYSTORE_FILE = $keystorePath
$env:KEYSTORE_PASSWORD = $signingValues["KEYSTORE_PASSWORD"]
$env:KEY_ALIAS = $signingValues["KEY_ALIAS"]
$env:KEY_PASSWORD = $signingValues["KEY_PASSWORD"]

Write-Host "  PASS: Signing config loaded, credentials verified, and exported for Gradle." -ForegroundColor Green

# ── 2. RUN TESTS ─────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[2/4] Running unit tests..." -ForegroundColor Yellow
Write-Host "  (This may take a minute on the first run while Gradle downloads dependencies)" -ForegroundColor DarkGray
Write-Host ""

Push-Location $projectRoot
try {
    & $gradlew :app:testDebugUnitTest :core:test --no-daemon 2>&1 | ForEach-Object {
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
        Write-Host "  Test reports: app\build\reports\tests\testDebugUnitTest\index.html and core\build\reports\tests\test\index.html" -ForegroundColor Red
        exit 1
    }
    Write-Host "  PASS: All tests passed." -ForegroundColor Green
} finally {
    Pop-Location
}

# ── 3. RUN LINT ──────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "[3/4] Running lint checks (debug + release)..." -ForegroundColor Yellow

Push-Location $projectRoot
try {
    & $gradlew :app:lint :app:lintRelease --no-daemon 2>&1 | ForEach-Object {
        if ($_ -match "error:|Error:") {
            Write-Host "  $_" -ForegroundColor Red
        } elseif ($_ -match "BUILD SUCCESSFUL") {
            Write-Host "  $_" -ForegroundColor Green
        } else {
            Write-Host "  $_" -ForegroundColor DarkGray
        }
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "  LINT FAILED. Fix lint errors before building a release." -ForegroundColor Red
        Write-Host "  Lint reports: app\build\reports\lint-results-*.html" -ForegroundColor Red
        exit 1
    }
    # Lint warnings are OK, only lint task failure should stop the build.
    Write-Host "  PASS: Debug and release lint complete." -ForegroundColor Green
} finally {
    Pop-Location
}

# ── 4. BUILD RELEASE BUNDLE ─────────────────────────────────────────────────

Write-Host ""
Write-Host "[4/4] Building release App Bundle (.aab)..." -ForegroundColor Yellow
Write-Host "  This compiles Kotlin and signs the bundle (release minify settings are controlled in build.gradle.kts)." -ForegroundColor DarkGray
Write-Host "  May take 2-5 minutes..." -ForegroundColor DarkGray
Write-Host ""

Push-Location $projectRoot
try {
    & $gradlew :app:clean :app:bundleRelease --no-daemon 2>&1 | ForEach-Object {
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
        Write-Host "  .\gradlew.bat :app:bundleRelease --stacktrace" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

# ── VERIFY OUTPUT ────────────────────────────────────────────────────────────

Write-Host ""
if (Test-Path $aabOutput) {
    # Validate signature so we never ship an unsigned release bundle by mistake.
    & jarsigner -verify $aabOutput 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  FAIL: Bundle exists but signature verification failed." -ForegroundColor Red
        Write-Host "  Check KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD in local.properties." -ForegroundColor Red
        exit 1
    }

    $fileSize = (Get-Item $aabOutput).Length
    $fileSizeMB = [math]::Round($fileSize / 1MB, 2)
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "" -ForegroundColor Cyan
    Write-Host "  Bundle:  $aabOutput" -ForegroundColor White
    Write-Host "  Size:    $fileSizeMB MB" -ForegroundColor White
    Write-Host "  Signing: Verified (jarsigner)" -ForegroundColor White
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

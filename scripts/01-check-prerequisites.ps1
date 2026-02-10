<#
.SYNOPSIS
    TradeSketch Estimator - Prerequisites Check
    Verifies that everything you need is installed before you can build.

.DESCRIPTION
    This script checks your Windows machine for:
      - Java 17+
      - ANDROID_HOME environment variable
      - Android SDK Platform 35
      - Android SDK Build-Tools
      - ADB (Android Debug Bridge)
      - Git
      - Gradle wrapper in the project

    Run this FIRST before doing anything else.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Prerequisites Check" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

$allGood = $true

# ── 1. JAVA ──────────────────────────────────────────────────────────────────

Write-Host "[1/6] Checking Java..." -ForegroundColor Yellow

$javaVersion = $null
try {
    $javaOutput = & java -version 2>&1 | Out-String
    if ($javaOutput -match '"(\d+)') {
        $javaVersion = [int]$Matches[1]
    }
} catch {}

if ($javaVersion -and $javaVersion -ge 17) {
    Write-Host "  PASS: Java $javaVersion found." -ForegroundColor Green
} else {
    Write-Host "  FAIL: Java 17 or higher is required." -ForegroundColor Red
    Write-Host "        Download from: https://adoptium.net/" -ForegroundColor Red
    Write-Host "        Choose 'Temurin 17 LTS' for Windows x64." -ForegroundColor Red
    Write-Host "        IMPORTANT: Check 'Set JAVA_HOME variable' during install." -ForegroundColor Red
    $allGood = $false
}

# ── 2. JAVA_HOME ─────────────────────────────────────────────────────────────

Write-Host "[2/6] Checking JAVA_HOME..." -ForegroundColor Yellow

$javaHome = $env:JAVA_HOME
if ($javaHome -and (Test-Path "$javaHome\bin\java.exe")) {
    Write-Host "  PASS: JAVA_HOME = $javaHome" -ForegroundColor Green
} elseif ($javaHome) {
    Write-Host "  WARN: JAVA_HOME is set to '$javaHome' but java.exe not found there." -ForegroundColor DarkYellow
    Write-Host "        The Adoptium installer should have set this correctly." -ForegroundColor DarkYellow
    Write-Host "        Try restarting your terminal or PC." -ForegroundColor DarkYellow
} else {
    Write-Host "  FAIL: JAVA_HOME is not set." -ForegroundColor Red
    Write-Host "        Reinstall JDK 17 and check 'Set JAVA_HOME variable'." -ForegroundColor Red
    Write-Host "        Or set it manually:" -ForegroundColor Red
    Write-Host '        [System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17...", "User")' -ForegroundColor DarkGray
    $allGood = $false
}

# ── 3. ANDROID_HOME / ANDROID SDK ────────────────────────────────────────────

Write-Host "[3/6] Checking Android SDK..." -ForegroundColor Yellow

$androidHome = $env:ANDROID_HOME
if (-not $androidHome) {
    $androidHome = $env:ANDROID_SDK_ROOT
}
if (-not $androidHome) {
    # Try the default Android Studio install location
    $defaultPath = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $defaultPath) {
        $androidHome = $defaultPath
        Write-Host "  INFO: ANDROID_HOME not set, but SDK found at default location." -ForegroundColor DarkYellow
        Write-Host "        Setting ANDROID_HOME for this session..." -ForegroundColor DarkYellow
        $env:ANDROID_HOME = $androidHome
    }
}

if ($androidHome -and (Test-Path $androidHome)) {
    Write-Host "  PASS: Android SDK found at $androidHome" -ForegroundColor Green

    # Check for platform 35
    $platform35 = "$androidHome\platforms\android-35"
    if (Test-Path $platform35) {
        Write-Host "  PASS: Android SDK Platform 35 (Android 15) installed." -ForegroundColor Green
    } else {
        Write-Host "  FAIL: Android SDK Platform 35 NOT installed." -ForegroundColor Red
        Write-Host "        Open Android Studio > Settings > Languages & Frameworks > Android SDK" -ForegroundColor Red
        Write-Host "        > SDK Platforms tab > Check 'Android 15 (API 35)' > Click Apply." -ForegroundColor Red
        $allGood = $false
    }

    # Check for build-tools
    $buildToolsDir = "$androidHome\build-tools"
    if (Test-Path $buildToolsDir) {
        $latestBT = Get-ChildItem $buildToolsDir -Directory | Sort-Object Name -Descending | Select-Object -First 1
        if ($latestBT) {
            Write-Host "  PASS: Build-Tools found: $($latestBT.Name)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "  FAIL: Android SDK not found." -ForegroundColor Red
    Write-Host "        Install Android Studio from: https://developer.android.com/studio" -ForegroundColor Red
    Write-Host "        Then set ANDROID_HOME:" -ForegroundColor Red
    Write-Host '        [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:LOCALAPPDATA\Android\Sdk", "User")' -ForegroundColor DarkGray
    $allGood = $false
}

# ── 4. ADB ───────────────────────────────────────────────────────────────────

Write-Host "[4/6] Checking ADB..." -ForegroundColor Yellow

$adbPath = $null
if ($androidHome) {
    $adbPath = "$androidHome\platform-tools\adb.exe"
}

if ($adbPath -and (Test-Path $adbPath)) {
    $adbVer = & $adbPath version 2>&1 | Select-Object -First 1
    Write-Host "  PASS: $adbVer" -ForegroundColor Green
} else {
    try {
        $adbVer = & adb version 2>&1 | Select-Object -First 1
        Write-Host "  PASS: $adbVer" -ForegroundColor Green
    } catch {
        Write-Host "  WARN: ADB not found in PATH." -ForegroundColor DarkYellow
        Write-Host "        ADB is needed for screenshots and device testing." -ForegroundColor DarkYellow
        Write-Host "        It should be at: $androidHome\platform-tools\adb.exe" -ForegroundColor DarkYellow
        Write-Host "        Add that folder to your PATH." -ForegroundColor DarkYellow
    }
}

# ── 5. GIT ───────────────────────────────────────────────────────────────────

Write-Host "[5/6] Checking Git..." -ForegroundColor Yellow

try {
    $gitVer = & git --version 2>&1
    Write-Host "  PASS: $gitVer" -ForegroundColor Green
} catch {
    Write-Host "  FAIL: Git not found." -ForegroundColor Red
    Write-Host "        Download from: https://git-scm.com/download/win" -ForegroundColor Red
    $allGood = $false
}

# ── 6. GRADLE WRAPPER ────────────────────────────────────────────────────────

Write-Host "[6/6] Checking Gradle wrapper..." -ForegroundColor Yellow

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew = "$projectRoot\gradlew.bat"
if (Test-Path $gradlew) {
    Write-Host "  PASS: gradlew.bat found at $gradlew" -ForegroundColor Green
} else {
    Write-Host "  FAIL: gradlew.bat not found in $projectRoot." -ForegroundColor Red
    Write-Host "        Make sure you cloned the repo and are running from the scripts\ folder." -ForegroundColor Red
    $allGood = $false
}

# ── SUMMARY ──────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
if ($allGood) {
    Write-Host "  ALL CHECKS PASSED - You are ready to build!" -ForegroundColor Green
    Write-Host "  Next step: Run .\02-generate-keystore.ps1" -ForegroundColor Green
} else {
    Write-Host "  SOME CHECKS FAILED - Fix the issues above first." -ForegroundColor Red
    Write-Host "  After fixing, restart your terminal and run this script again." -ForegroundColor Red
}
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

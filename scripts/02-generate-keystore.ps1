<#
.SYNOPSIS
    TradeSketch Estimator - Keystore Generator
    Generates the release signing keystore and backs it up.

.DESCRIPTION
    This script:
      1. Asks you for a keystore password (and confirms it)
      2. Asks for your name, city, state, and country code
      3. Generates a 2048-bit RSA keystore valid for ~27 years
      4. Creates a backup copy on your Desktop
      5. Writes the signing config into local.properties (gitignored)

    You only run this ONCE, ever. Guard the keystore with your life.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystoreName = "tradesketch-release.keystore"
$keystorePath = Join-Path $projectRoot $keystoreName
$keyAlias = "tradesketch-release"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Keystore Generator" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# ── CHECK IF KEYSTORE ALREADY EXISTS ─────────────────────────────────────────

if (Test-Path $keystorePath) {
    Write-Host "  WARNING: A keystore already exists at:" -ForegroundColor Yellow
    Write-Host "  $keystorePath" -ForegroundColor Yellow
    Write-Host ""
    $overwrite = Read-Host "  Overwrite it? This is IRREVERSIBLE. (yes/no)"
    if ($overwrite -ne "yes") {
        Write-Host "  Keeping existing keystore. Exiting." -ForegroundColor Green
        exit 0
    }
    Write-Host ""
}

# ── GATHER INFORMATION ───────────────────────────────────────────────────────

Write-Host "  I need some information to create your signing key." -ForegroundColor White
Write-Host "  Everything except the password is baked into the certificate" -ForegroundColor White
Write-Host "  and does NOT appear on the Play Store." -ForegroundColor White
Write-Host ""

# Password
do {
    $secPass1 = Read-Host "  Enter a keystore password (min 6 chars)" -AsSecureString
    $pass1 = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass1))
    if ($pass1.Length -lt 6) {
        Write-Host "  Password must be at least 6 characters. Try again." -ForegroundColor Red
    }
} while ($pass1.Length -lt 6)

do {
    $secPass2 = Read-Host "  Confirm password" -AsSecureString
    $pass2 = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPass2))
    if ($pass1 -ne $pass2) {
        Write-Host "  Passwords don't match. Try again." -ForegroundColor Red
    }
} while ($pass1 -ne $pass2)

Write-Host ""
$fullName = Read-Host "  Your full name (e.g. 'Austin Smith')"
if ([string]::IsNullOrWhiteSpace($fullName)) { $fullName = "Unknown" }

$orgUnit = Read-Host "  Organizational unit (press Enter to skip)"
if ([string]::IsNullOrWhiteSpace($orgUnit)) { $orgUnit = "Development" }

$org = Read-Host "  Organization / company name (press Enter to skip)"
if ([string]::IsNullOrWhiteSpace($org)) { $org = "Independent" }

$city = Read-Host "  City (e.g. 'Austin')"
if ([string]::IsNullOrWhiteSpace($city)) { $city = "Unknown" }

$state = Read-Host "  State (e.g. 'Texas')"
if ([string]::IsNullOrWhiteSpace($state)) { $state = "Unknown" }

$country = Read-Host "  Two-letter country code (e.g. 'US')"
if ([string]::IsNullOrWhiteSpace($country)) { $country = "US" }

# ── GENERATE KEYSTORE ────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  Generating keystore..." -ForegroundColor Yellow

$dname = "CN=$fullName, OU=$orgUnit, O=$org, L=$city, ST=$state, C=$country"

$keytoolArgs = @(
    "-genkey", "-v",
    "-keystore", $keystorePath,
    "-alias", $keyAlias,
    "-keyalg", "RSA",
    "-keysize", "2048",
    "-validity", "10000",
    "-storepass", $pass1,
    "-keypass", $pass1,
    "-dname", $dname
)

& keytool @keytoolArgs

if (-not (Test-Path $keystorePath)) {
    Write-Host ""
    Write-Host "  FAILED: Keystore was not created." -ForegroundColor Red
    Write-Host "  Make sure 'keytool' is in your PATH (it comes with the JDK)." -ForegroundColor Red
    Write-Host "  Try: where keytool" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "  SUCCESS: Keystore created at:" -ForegroundColor Green
Write-Host "  $keystorePath" -ForegroundColor Green

# ── BACKUP KEYSTORE ──────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  Creating backup on your Desktop..." -ForegroundColor Yellow

$desktopBackup = Join-Path ([Environment]::GetFolderPath("Desktop")) "tradesketch-keystore-BACKUP"
if (-not (Test-Path $desktopBackup)) {
    New-Item -ItemType Directory -Path $desktopBackup | Out-Null
}
Copy-Item $keystorePath -Destination "$desktopBackup\$keystoreName" -Force

# Save a reminder file with the credentials
$reminderContent = @"
TRADESKETCH ESTIMATOR - KEYSTORE CREDENTIALS
=============================================
KEEP THIS FILE SAFE. DELETE IT AFTER SAVING TO A PASSWORD MANAGER.

Keystore file:     $keystoreName
Key alias:         $keyAlias
Keystore password: $pass1
Key password:      $pass1

Certificate DN:    $dname
Created:           $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

WARNING: If you lose the keystore file or forget the password,
you can NEVER update your app on the Play Store.
Back up to a USB drive AND a password manager.
=============================================
"@

$reminderFile = Join-Path $desktopBackup "CREDENTIALS-DELETE-AFTER-SAVING.txt"
Set-Content -Path $reminderFile -Value $reminderContent

Write-Host "  BACKUP created at: $desktopBackup" -ForegroundColor Green
Write-Host "  IMPORTANT: A credentials file was saved there too." -ForegroundColor Yellow
Write-Host "  Save the password to a password manager, then DELETE that file." -ForegroundColor Yellow

# ── WRITE local.properties ───────────────────────────────────────────────────

Write-Host ""
Write-Host "  Writing signing config to local.properties..." -ForegroundColor Yellow

$localProps = Join-Path $projectRoot "local.properties"
$signingBlock = @"

# TradeSketch release signing (auto-generated $(Get-Date -Format "yyyy-MM-dd"))
KEYSTORE_FILE=$($keystorePath -replace '\\', '\\\\')
KEYSTORE_PASSWORD=$pass1
KEY_ALIAS=$keyAlias
KEY_PASSWORD=$pass1
"@

# Append to existing local.properties or create new
if (Test-Path $localProps) {
    Add-Content -Path $localProps -Value $signingBlock
} else {
    Set-Content -Path $localProps -Value $signingBlock
}

Write-Host "  DONE: Signing config written to local.properties" -ForegroundColor Green

# ── VERIFY .gitignore ────────────────────────────────────────────────────────

$gitignore = Join-Path $projectRoot ".gitignore"
$gitignoreContent = ""
if (Test-Path $gitignore) {
    $gitignoreContent = Get-Content $gitignore -Raw
}

$additions = @()
if ($gitignoreContent -notmatch "local\.properties") { $additions += "local.properties" }
if ($gitignoreContent -notmatch "\*\.keystore") { $additions += "*.keystore" }

if ($additions.Count -gt 0) {
    Write-Host "  Adding safety entries to .gitignore..." -ForegroundColor Yellow
    Add-Content -Path $gitignore -Value ("`n# Signing (auto-added)`n" + ($additions -join "`n"))
    Write-Host "  Added: $($additions -join ', ')" -ForegroundColor Green
}

# ── SUMMARY ──────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  KEYSTORE GENERATION COMPLETE" -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
Write-Host "  Keystore:  $keystorePath" -ForegroundColor White
Write-Host "  Alias:     $keyAlias" -ForegroundColor White
Write-Host "  Backup:    $desktopBackup" -ForegroundColor White
Write-Host "  Config:    local.properties (gitignored)" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  Next step: Run .\03-build-release.ps1" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

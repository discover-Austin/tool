<#
.SYNOPSIS
    TradeSketch Estimator - Privacy Policy Deployer
    Deploys the privacy policy to Vercel for Google Play compliance.

.DESCRIPTION
    This script:
      1. Verifies privacy-policy.html exists
      2. Verifies Vercel CLI is installed and authenticated
      3. Creates (if needed) and links project "tradesketch-privacy"
      4. Deploys privacy policy as a production static site
      5. Verifies URL is publicly reachable
      6. Saves the URL to store-assets\PRIVACY_POLICY_URL.txt

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$privacyHtml = Join-Path $projectRoot "store-assets\legal\privacy-policy.html"
$urlFile = Join-Path $projectRoot "store-assets\PRIVACY_POLICY_URL.txt"
$vercelProject = "tradesketch-privacy"
$canonicalUrl = "https://$vercelProject.vercel.app"
$tempDir = Join-Path $env:TEMP ("$vercelProject-vercel-" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds())

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Privacy Policy Deployer (Vercel)" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# 1) Verify policy file
Write-Host "[1/6] Checking privacy policy file..." -ForegroundColor Yellow
if (-not (Test-Path $privacyHtml)) {
    Write-Host "  FAIL: Privacy policy not found at $privacyHtml" -ForegroundColor Red
    exit 1
}
Write-Host "  PASS: Found $privacyHtml" -ForegroundColor Green

# 2) Verify Vercel CLI
Write-Host "[2/6] Checking Vercel CLI..." -ForegroundColor Yellow
try {
    $vercelVersion = (& vercel --version 2>&1 | Select-Object -First 1)
    Write-Host "  PASS: $vercelVersion" -ForegroundColor Green
} catch {
    Write-Host "  FAIL: Vercel CLI is not installed." -ForegroundColor Red
    Write-Host "  Install with: npm i -g vercel" -ForegroundColor Red
    exit 1
}

# 3) Verify auth
Write-Host "[3/6] Checking Vercel authentication..." -ForegroundColor Yellow
try {
    $vercelUser = (& vercel whoami 2>&1 | Select-Object -Last 1).Trim()
    if ([string]::IsNullOrWhiteSpace($vercelUser)) {
        throw "Could not determine Vercel account."
    }
    Write-Host "  PASS: Logged in as $vercelUser" -ForegroundColor Green
} catch {
    Write-Host "  FAIL: Not logged in to Vercel." -ForegroundColor Red
    Write-Host "  Run: vercel login" -ForegroundColor Red
    exit 1
}

# 4) Ensure project exists
Write-Host "[4/6] Ensuring Vercel project '$vercelProject' exists..." -ForegroundColor Yellow
$projectExists = $true
try {
    & vercel project inspect $vercelProject 2>&1 | Out-Null
} catch {
    $projectExists = $false
}

if (-not $projectExists) {
    Write-Host "  Project not found. Creating..." -ForegroundColor DarkYellow
    & vercel project add $vercelProject 2>&1 | Out-Null
    Write-Host "  PASS: Project created." -ForegroundColor Green
} else {
    Write-Host "  PASS: Project already exists." -ForegroundColor Green
}

# 5) Deploy
Write-Host "[5/6] Deploying privacy policy to Vercel..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
Copy-Item $privacyHtml -Destination (Join-Path $tempDir "index.html") -Force

& vercel link --cwd $tempDir --project $vercelProject --yes 2>&1 | Out-Null
$deployOutput = (& vercel deploy --cwd $tempDir --prod --yes 2>&1)
$deployOutput | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

$deployUrl = ($deployOutput |
    Select-String -Pattern "https://[a-zA-Z0-9.-]+\.vercel\.app" -AllMatches |
    ForEach-Object { $_.Matches.Value } |
    Select-Object -First 1)

if (-not $deployUrl) {
    $deployUrl = $canonicalUrl
}

# 6) Verify + save canonical URL
Write-Host "[6/6] Verifying production URL..." -ForegroundColor Yellow
$finalUrl = $canonicalUrl
try {
    $response = Invoke-WebRequest -Uri $canonicalUrl -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -ne 200) {
        throw "Status $($response.StatusCode)"
    }
    Write-Host "  PASS: $canonicalUrl is reachable." -ForegroundColor Green
} catch {
    Write-Host "  WARN: Canonical URL not ready yet. Falling back to deployment URL." -ForegroundColor DarkYellow
    $finalUrl = $deployUrl
}

Set-Content -Path $urlFile -Value $finalUrl

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  PRIVACY POLICY DEPLOYED" -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
Write-Host "  URL: $finalUrl" -ForegroundColor White
Write-Host "  Saved to: $urlFile" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  Use this URL in Play Console for:" -ForegroundColor White
Write-Host "    - Store Listing > Privacy policy URL" -ForegroundColor DarkGray
Write-Host "    - Data Safety > Privacy policy URL" -ForegroundColor DarkGray
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

try {
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
} catch {}

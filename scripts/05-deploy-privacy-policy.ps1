<#
.SYNOPSIS
    TradeSketch Estimator - Privacy Policy Deployer
    Deploys the privacy policy to GitHub Pages so Google Play can access it.

.DESCRIPTION
    This script:
      1. Checks if the GitHub CLI (gh) is installed
      2. Creates a new public GitHub repository called 'tradesketch-privacy'
      3. Copies privacy-policy.html into it as index.html
      4. Pushes it to GitHub
      5. Enables GitHub Pages
      6. Gives you the final public URL to use in Play Console

    REQUIRES: GitHub CLI (gh) - will walk you through installing if missing.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$privacyHtml = Join-Path $projectRoot "store-assets\legal\privacy-policy.html"
$repoName = "tradesketch-privacy"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Privacy Policy Deployer" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# ── 1. CHECK PRIVACY POLICY FILE ────────────────────────────────────────────

Write-Host "[1/6] Checking privacy policy file..." -ForegroundColor Yellow
if (Test-Path $privacyHtml) {
    Write-Host "  PASS: Found $privacyHtml" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Privacy policy not found at $privacyHtml" -ForegroundColor Red
    exit 1
}

# ── 2. CHECK GITHUB CLI ─────────────────────────────────────────────────────

Write-Host "[2/6] Checking GitHub CLI..." -ForegroundColor Yellow

$ghInstalled = $false
try {
    $ghVer = & gh --version 2>&1 | Select-Object -First 1
    Write-Host "  PASS: $ghVer" -ForegroundColor Green
    $ghInstalled = $true
} catch {}

if (-not $ghInstalled) {
    Write-Host "  GitHub CLI (gh) is not installed." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  Installing via winget..." -ForegroundColor Yellow
    try {
        & winget install --id GitHub.cli -e --source winget
        Write-Host "  Installed! Restart your terminal, then run this script again." -ForegroundColor Green
        exit 0
    } catch {
        Write-Host "  winget failed. Install manually:" -ForegroundColor Red
        Write-Host "  https://cli.github.com/" -ForegroundColor Red
        Write-Host "  Download the Windows installer (.msi) and run it." -ForegroundColor Red
        exit 1
    }
}

# ── 3. CHECK GH AUTH ─────────────────────────────────────────────────────────

Write-Host "[3/6] Checking GitHub authentication..." -ForegroundColor Yellow

$authStatus = & gh auth status 2>&1 | Out-String
if ($authStatus -match "Logged in") {
    $username = ""
    if ($authStatus -match "Logged in to github.com account (\S+)") {
        $username = $Matches[1]
    } elseif ($authStatus -match "account (\S+)") {
        $username = $Matches[1]
    }
    Write-Host "  PASS: Logged in as $username" -ForegroundColor Green
} else {
    Write-Host "  Not logged in. Starting login..." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  A browser window will open. Sign in with your GitHub account." -ForegroundColor White
    Write-Host "  (Use whichever GitHub account you want to host the privacy policy on.)" -ForegroundColor DarkGray
    Write-Host ""
    & gh auth login --web --git-protocol https
    $authStatus = & gh auth status 2>&1 | Out-String
    if ($authStatus -notmatch "Logged in") {
        Write-Host "  FAIL: Login failed. Try again." -ForegroundColor Red
        exit 1
    }
    if ($authStatus -match "account (\S+)") {
        $username = $Matches[1]
    }
    Write-Host "  PASS: Logged in as $username" -ForegroundColor Green
}

# ── 4. CREATE THE REPO ───────────────────────────────────────────────────────

Write-Host "[4/6] Creating GitHub repository '$repoName'..." -ForegroundColor Yellow

# Check if repo already exists
$repoExists = $false
try {
    $repoCheck = & gh repo view "$username/$repoName" 2>&1 | Out-String
    if ($repoCheck -notmatch "Could not resolve") {
        $repoExists = $true
    }
} catch {}

# Create a temp working directory
$tempDir = Join-Path $env:TEMP "tradesketch-privacy-deploy"
if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
New-Item -ItemType Directory -Path $tempDir | Out-Null

# Copy privacy policy as index.html
Copy-Item $privacyHtml -Destination (Join-Path $tempDir "index.html")

Push-Location $tempDir
try {
    & git init 2>&1 | Out-Null
    & git add index.html
    & git commit -m "Add privacy policy for TradeSketch Estimator" 2>&1 | Out-Null
    & git branch -M main

    if ($repoExists) {
        Write-Host "  Repo already exists. Pushing update..." -ForegroundColor DarkYellow
        & git remote add origin "https://github.com/$username/$repoName.git"
        & git push -u origin main --force 2>&1
    } else {
        Write-Host "  Creating new public repository..." -ForegroundColor DarkGray
        & gh repo create $repoName --public --source=. --push 2>&1
    }

    Write-Host "  PASS: Repository created/updated." -ForegroundColor Green
} finally {
    Pop-Location
}

# ── 5. ENABLE GITHUB PAGES ──────────────────────────────────────────────────

Write-Host "[5/6] Enabling GitHub Pages..." -ForegroundColor Yellow

# Use the API to enable Pages from the main branch
try {
    & gh api "repos/$username/$repoName/pages" -X POST -f "source[branch]=main" -f "source[path]=/" 2>&1 | Out-Null
    Write-Host "  PASS: GitHub Pages enabled." -ForegroundColor Green
} catch {
    # Pages might already be enabled
    Write-Host "  INFO: Pages may already be enabled (this is OK)." -ForegroundColor DarkYellow
}

# Wait for deployment
Write-Host "  Waiting for deployment (up to 60 seconds)..." -ForegroundColor DarkGray
$pagesUrl = "https://$username.github.io/$repoName/"
$deployed = $false
for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 5
    try {
        $response = Invoke-WebRequest -Uri $pagesUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $deployed = $true
            break
        }
    } catch {}
    Write-Host "  ..." -ForegroundColor DarkGray
}

# ── 6. DONE ─────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
if ($deployed) {
    Write-Host "  PRIVACY POLICY IS LIVE!" -ForegroundColor Green
} else {
    Write-Host "  PRIVACY POLICY DEPLOYED!" -ForegroundColor Green
    Write-Host "  (It may take a few more minutes to go live)" -ForegroundColor DarkYellow
}
Write-Host "" -ForegroundColor Cyan
Write-Host "  URL: $pagesUrl" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  Use this URL when filling out the Play Console:" -ForegroundColor White
Write-Host "    - Store Listing > Privacy policy URL" -ForegroundColor DarkGray
Write-Host "    - Data Safety form > Privacy policy URL" -ForegroundColor DarkGray
Write-Host "" -ForegroundColor Cyan

# Save the URL to a file for reference
$urlFile = Join-Path $projectRoot "store-assets\PRIVACY_POLICY_URL.txt"
Set-Content -Path $urlFile -Value $pagesUrl
Write-Host "  URL saved to: $urlFile" -ForegroundColor DarkGray
Write-Host "" -ForegroundColor Cyan
Write-Host "  Next step: Create graphics (icon, feature graphic)," -ForegroundColor Green
Write-Host "  then follow the Play Console steps in the guide." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# Cleanup temp directory
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue

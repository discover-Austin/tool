<#
.SYNOPSIS
    TradeSketch Estimator - Store Listing Text Copier
    Copies all Play Store listing text to your clipboard, one field at a time.

.DESCRIPTION
    This script reads the pre-written store listing text from the store-assets
    folder and copies each field to your clipboard one at a time. You just
    paste into Play Console after each copy.

    This saves you from manually finding and copying text from files.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$projectRoot = Split-Path -Parent $PSScriptRoot
$listingDir = Join-Path $projectRoot "store-assets\listing"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Store Listing Copier" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  This script copies each store listing field to your clipboard." -ForegroundColor White
Write-Host "  Open Play Console in your browser, then follow along." -ForegroundColor White
Write-Host ""
Write-Host "  Play Console URL:" -ForegroundColor DarkGray
Write-Host "  https://play.google.com/console/" -ForegroundColor DarkGray
Write-Host "  > Your app > Grow > Store presence > Main store listing" -ForegroundColor DarkGray
Write-Host ""

# ── HELPER FUNCTION ──────────────────────────────────────────────────────────

function Copy-Field {
    param(
        [string]$Label,
        [string]$FilePath,
        [string]$Note = ""
    )

    Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host "  FIELD: $Label" -ForegroundColor Yellow

    if (-not (Test-Path $FilePath)) {
        Write-Host "  ERROR: File not found: $FilePath" -ForegroundColor Red
        return
    }

    $content = (Get-Content $FilePath -Raw).Trim()
    $charCount = $content.Length
    Write-Host "  Characters: $charCount" -ForegroundColor DarkGray

    if ($Note) {
        Write-Host "  Note: $Note" -ForegroundColor DarkGray
    }

    # Show preview (first 100 chars)
    $preview = if ($content.Length -gt 100) { $content.Substring(0, 100) + "..." } else { $content }
    Write-Host "  Preview: $preview" -ForegroundColor DarkGray
    Write-Host ""

    Read-Host "  Press ENTER to copy to clipboard"
    Set-Clipboard -Value $content
    Write-Host "  COPIED! Paste it into Play Console now (Ctrl+V)." -ForegroundColor Green
    Write-Host ""
    Read-Host "  Press ENTER when you've pasted it and are ready for the next field"
}

# ── COPY FIELDS ONE BY ONE ───────────────────────────────────────────────────

# App Name
Copy-Field -Label "APP NAME" `
    -FilePath (Join-Path $listingDir "title.txt") `
    -Note "Max 30 characters. Goes in the 'App name' field."

# Short Description
Copy-Field -Label "SHORT DESCRIPTION" `
    -FilePath (Join-Path $listingDir "short-description.txt") `
    -Note "Max 80 characters. Shown on search results."

# Full Description
Copy-Field -Label "FULL DESCRIPTION" `
    -FilePath (Join-Path $listingDir "full-description.txt") `
    -Note "Max 4000 characters. This is the main listing text."

# What's New
Copy-Field -Label "WHAT'S NEW (Release Notes)" `
    -FilePath (Join-Path $listingDir "whats-new.txt") `
    -Note "Goes in Release > Production > Create release > Release notes."

# ── CONTACT EMAIL ────────────────────────────────────────────────────────────

Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  FIELD: CONTACT EMAIL" -ForegroundColor Yellow
Write-Host "  Value: built.to.cell@gmail.com" -ForegroundColor White
Write-Host ""
Read-Host "  Press ENTER to copy to clipboard"
Set-Clipboard -Value "built.to.cell@gmail.com"
Write-Host "  COPIED! Paste into the 'Email' field under Contact Details." -ForegroundColor Green
Write-Host ""
Read-Host "  Press ENTER when done"

# ── PRIVACY POLICY URL ──────────────────────────────────────────────────────

Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor DarkGray
Write-Host "  FIELD: PRIVACY POLICY URL" -ForegroundColor Yellow

$urlFile = Join-Path $projectRoot "store-assets\PRIVACY_POLICY_URL.txt"
if (Test-Path $urlFile) {
    $privacyUrl = (Get-Content $urlFile -Raw).Trim()
    Write-Host "  URL: $privacyUrl" -ForegroundColor White
    Read-Host "  Press ENTER to copy to clipboard"
    Set-Clipboard -Value $privacyUrl
    Write-Host "  COPIED! Paste into 'Privacy policy URL' field." -ForegroundColor Green
} else {
    Write-Host "  URL file not found. Run 05-deploy-privacy-policy.ps1 first." -ForegroundColor Red
    Write-Host "  Or manually enter your privacy policy URL." -ForegroundColor Red
}

# ── SUMMARY ──────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  ALL STORE LISTING FIELDS COPIED!" -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
Write-Host "  Don't forget to also upload:" -ForegroundColor White
Write-Host "    - App icon (512x512 PNG)" -ForegroundColor White
Write-Host "    - Feature graphic (1024x500 PNG)" -ForegroundColor White
Write-Host "    - 6 phone screenshots (from store-assets\screenshots\)" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  Then proceed to:" -ForegroundColor White
Write-Host "    - Content Rating questionnaire" -ForegroundColor White
Write-Host "    - Pricing setup" -ForegroundColor White
Write-Host "    - Data Safety form" -ForegroundColor White
Write-Host "    - Upload the .aab bundle" -ForegroundColor White
Write-Host "    - Submit for review" -ForegroundColor White
Write-Host "" -ForegroundColor Cyan
Write-Host "  See PLAY-STORE-LAUNCH-GUIDE.md Phases 10-14 for details." -ForegroundColor DarkGray
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

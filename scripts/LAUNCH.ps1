<#
.SYNOPSIS
    TradeSketch Estimator - MASTER LAUNCH SCRIPT
    Your one-stop command center for getting to the Play Store.

.DESCRIPTION
    Run this script to see where you are in the process and launch
    the right step. It checks what's already been done and shows
    you what's next.

.NOTES
    Email:   built.to.cell@gmail.com
    App:     TradeSketch Estimator
    Package: com.tradesketch.estimator
#>

$projectRoot = Split-Path -Parent $PSScriptRoot

function Show-Status {
    param([string]$Label, [bool]$Done)
    if ($Done) {
        Write-Host "  [DONE]    $Label" -ForegroundColor Green
    } else {
        Write-Host "  [------]  $Label" -ForegroundColor DarkGray
    }
}

while ($true) {
    Clear-Host
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "      TRADESKETCH ESTIMATOR - PLAY STORE LAUNCH CENTER" -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host ""

    # Check status of each phase
    $hasJava = $false
    try { $jOut = & java -version 2>&1 | Out-String; if ($jOut -match '"(\d+)' -and [int]$Matches[1] -ge 17) { $hasJava = $true } } catch {}

    $hasAndroidSdk = ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) -or (Test-Path "$env:LOCALAPPDATA\Android\Sdk")
    $hasKeystore = Test-Path (Join-Path $projectRoot "tradesketch-release.keystore")
    $hasLocalProps = (Test-Path (Join-Path $projectRoot "local.properties")) -and ((Get-Content (Join-Path $projectRoot "local.properties") -Raw -ErrorAction SilentlyContinue) -match "KEYSTORE_FILE")
    $hasBundle = Test-Path (Join-Path $projectRoot "app\build\outputs\bundle\release\app-release.aab")
    $hasScreenshots = (Test-Path (Join-Path $projectRoot "store-assets\screenshots")) -and ((Get-ChildItem (Join-Path $projectRoot "store-assets\screenshots") -Filter "*.png" -ErrorAction SilentlyContinue).Count -ge 4)
    $hasPrivacyUrl = Test-Path (Join-Path $projectRoot "store-assets\PRIVACY_POLICY_URL.txt")

    Write-Host "  STATUS:" -ForegroundColor White
    Write-Host ""
    Show-Status "Prerequisites (Java 17, Android SDK)" ($hasJava -and $hasAndroidSdk)
    Show-Status "Signing keystore generated" $hasKeystore
    Show-Status "Signing config in local.properties" $hasLocalProps
    Show-Status "Release bundle built (.aab)" $hasBundle
    Show-Status "Screenshots captured (4+ images)" $hasScreenshots
    Show-Status "Privacy policy deployed" $hasPrivacyUrl

    Write-Host ""
    Write-Host "  AUTOMATION SCRIPTS:" -ForegroundColor White
    Write-Host ""
    Write-Host "  [1]  Check prerequisites        (01-check-prerequisites.ps1)" -ForegroundColor White
    Write-Host "  [2]  Generate keystore           (02-generate-keystore.ps1)" -ForegroundColor White
    Write-Host "  [3]  Build release bundle        (03-build-release.ps1)" -ForegroundColor White
    Write-Host "  [4]  Capture screenshots         (04-capture-screenshots.ps1)" -ForegroundColor White
    Write-Host "  [5]  Deploy privacy policy       (05-deploy-privacy-policy.ps1)" -ForegroundColor White
    Write-Host "  [6]  Copy store listing text      (06-copy-store-listing.ps1)" -ForegroundColor White
    Write-Host "  [7]  Bump version (for updates)  (07-bump-version.ps1)" -ForegroundColor White
    Write-Host ""
    Write-Host "  [G]  Open the full launch GUIDE in Notepad" -ForegroundColor DarkYellow
    Write-Host "  [P]  Open Play Console in browser" -ForegroundColor DarkYellow
    Write-Host "  [Q]  Quit" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
    $choice = Read-Host "  Enter your choice"

    $scriptDir = $PSScriptRoot

    switch ($choice) {
        "1" { & "$scriptDir\01-check-prerequisites.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "2" { & "$scriptDir\02-generate-keystore.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "3" { & "$scriptDir\03-build-release.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "4" { & "$scriptDir\04-capture-screenshots.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "5" { & "$scriptDir\05-deploy-privacy-policy.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "6" { & "$scriptDir\06-copy-store-listing.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "7" { & "$scriptDir\07-bump-version.ps1"; Read-Host "`n  Press ENTER to return to menu" }
        "G" { & notepad.exe (Join-Path $projectRoot "PLAY-STORE-LAUNCH-GUIDE.md") }
        "g" { & notepad.exe (Join-Path $projectRoot "PLAY-STORE-LAUNCH-GUIDE.md") }
        "P" { Start-Process "https://play.google.com/console/" }
        "p" { Start-Process "https://play.google.com/console/" }
        "Q" { Write-Host "  Good luck with the launch!"; exit 0 }
        "q" { Write-Host "  Good luck with the launch!"; exit 0 }
        default { Write-Host "  Invalid choice. Try again." -ForegroundColor Red; Start-Sleep -Seconds 1 }
    }
}

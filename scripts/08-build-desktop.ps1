<#
.SYNOPSIS
    TradeSketch Estimator - Desktop Builder
    Builds only the desktop module so desktop and Android pipelines stay independent.

.DESCRIPTION
    This script runs:
      1. :desktop:build
      2. Prints output paths for desktop artifacts
#>

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $projectRoot "gradlew.bat"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  TradeSketch Estimator - Desktop Builder" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

Push-Location $projectRoot
try {
    & $gradlew :desktop:build --no-daemon 2>&1 | ForEach-Object {
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
        Write-Host "  Desktop build failed." -ForegroundColor Red
        Write-Host "  Run with --stacktrace for details:" -ForegroundColor Red
        Write-Host "  .\gradlew.bat :desktop:build --stacktrace" -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host "  Desktop outputs are under: desktop\build\" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

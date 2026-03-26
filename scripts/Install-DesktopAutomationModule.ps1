param(
    [string]$ModuleName = "DesktopAutomation",
    [string[]]$ModuleRoots = @(
        (Join-Path ([Environment]::GetFolderPath("MyDocuments")) "PowerShell\Modules"),
        (Join-Path ([Environment]::GetFolderPath("MyDocuments")) "WindowsPowerShell\Modules")
    ),
    [switch]$SkipProfileImport
)

$ErrorActionPreference = "Stop"

$sourceRoot = Split-Path -Parent $PSCommandPath
$profilePaths = @(
    (Join-Path ([Environment]::GetFolderPath("MyDocuments")) "PowerShell\Microsoft.PowerShell_profile.ps1"),
    (Join-Path ([Environment]::GetFolderPath("MyDocuments")) "WindowsPowerShell\Microsoft.PowerShell_profile.ps1")
) | Select-Object -Unique
$profileMarker = "DesktopAutomation auto-import"
$profilePattern = '(?ms)^# DesktopAutomation auto-import\r?\nif \(-not \(Get-Module -Name DesktopAutomation\).+?\r?\n\}'
$profileSnippet = @'
# DesktopAutomation auto-import
if (-not (Get-Module -Name DesktopAutomation) -and (Get-Module -ListAvailable -Name DesktopAutomation)) {
    Import-Module DesktopAutomation -DisableNameChecking -ErrorAction SilentlyContinue
}
'@

$filesToCopy = @(
    "DesktopAutomation.Common.ps1",
    "DesktopAutomation.psm1",
    "DesktopAutomation.psd1"
)

Get-Module -Name $ModuleName -All -ErrorAction SilentlyContinue | Remove-Module -Force -ErrorAction SilentlyContinue

function Install-DesktopAutomationModuleFiles {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ModuleRoot
    )

    $targetRoot = Join-Path $ModuleRoot $ModuleName
    New-Item -ItemType Directory -Force -Path $targetRoot | Out-Null

    foreach ($fileName in $filesToCopy) {
        Copy-Item `
            -Path (Join-Path $sourceRoot $fileName) `
            -Destination (Join-Path $targetRoot $fileName) `
            -Force
    }

    return $targetRoot
}

function Update-DesktopAutomationProfile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProfilePath
    )

    $profileDir = Split-Path -Parent $ProfilePath
    New-Item -ItemType Directory -Force -Path $profileDir | Out-Null
    if (-not (Test-Path $ProfilePath)) {
        New-Item -ItemType File -Path $ProfilePath | Out-Null
    }

    $updated = $false
    $existingContent = Get-Content -Path $ProfilePath -Raw -ErrorAction SilentlyContinue
    if ($existingContent -match $profilePattern) {
        $updatedContent = [System.Text.RegularExpressions.Regex]::Replace(
            $existingContent,
            $profilePattern,
            $profileSnippet.TrimEnd()
        )
        if ($updatedContent -ne $existingContent) {
            Set-Content -Path $ProfilePath -Value $updatedContent
            $updated = $true
        }
    } elseif (-not (Select-String -Path $ProfilePath -SimpleMatch $profileMarker -Quiet)) {
        if (-not [string]::IsNullOrWhiteSpace($existingContent) -and -not $existingContent.EndsWith([Environment]::NewLine)) {
            Add-Content -Path $ProfilePath -Value ""
        }
        Add-Content -Path $ProfilePath -Value $profileSnippet
        $updated = $true
    }

    return [pscustomobject]@{
        ProfilePath = $ProfilePath
        Updated = $updated
    }
}

$installedModulePaths = @(
    $ModuleRoots |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique |
        ForEach-Object { Install-DesktopAutomationModuleFiles -ModuleRoot $_ }
)

$profileResults = @()
if (-not $SkipProfileImport) {
    $profileResults = @(
        $profilePaths |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            Select-Object -Unique |
            ForEach-Object { Update-DesktopAutomationProfile -ProfilePath $_ }
    )
}

$manifestPath = Join-Path $installedModulePaths[0] "$ModuleName.psd1"
Import-Module $manifestPath -Force -DisableNameChecking
$module = Get-Module -Name $ModuleName

[pscustomobject]@{
    ModuleName = $ModuleName
    ModulePath = $module.Path
    InstalledModulePaths = $installedModulePaths
    ProfileResults = $profileResults
    ExportedCommands = @($module.ExportedCommands.Keys | Sort-Object)
}

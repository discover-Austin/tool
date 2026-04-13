param(
    [string]$OutputDir = "",
    [string]$FreshIntroDir = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $projectRoot "store-assets\screenshots"
}
if ([string]::IsNullOrWhiteSpace($FreshIntroDir)) {
    $FreshIntroDir = Join-Path $projectRoot ".codex-screens\raw_20260412"
}

$fontBold = "C:\Windows\Fonts\segoeuib.ttf"
$fontRegular = "C:\Windows\Fonts\segoeui.ttf"
$tempDir = Join-Path $OutputDir "_compose_tmp"
$snapshotPath = Join-Path $OutputDir "LATEST_SCREENSHOT_SNAPSHOT.txt"
$relativeFreshIntroDir = [System.IO.Path]::GetRelativePath($projectRoot, $FreshIntroDir)
$relativeOutputDir = [System.IO.Path]::GetRelativePath($projectRoot, $OutputDir)

foreach ($path in @($OutputDir, $FreshIntroDir)) {
    if (-not (Test-Path $path)) {
        throw "Required directory not found: $path"
    }
}

foreach ($font in @($fontBold, $fontRegular)) {
    if (-not (Test-Path $font)) {
        throw "Required font not found: $font"
    }
}

New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

function Escape-DrawText {
    param([string]$Text)

    $escaped = $Text.Replace("\", "\\")
    $escaped = $escaped.Replace(":", "\:")
    $escaped = $escaped.Replace("'", "\'")
    $escaped = $escaped.Replace(",", "\,")
    return $escaped
}

function Escape-FilterPath {
    param([string]$Path)

    $escaped = $Path.Replace("\", "/")
    $escaped = $escaped.Replace(":", "\:")
    return $escaped
}

$shots = @(
    @{
        Output = "01_welcome.png"
        Source = Join-Path $FreshIntroDir "01_welcome.png"
        Headline = "Start estimates faster"
        Subline = "Open saved jobs or start a new one in one tap"
    },
    @{
        Output = "02_saved_projects.png"
        Source = Join-Path $FreshIntroDir "02_saved_projects.png"
        Headline = "Jump back into saved work"
        Subline = "Recent projects stay one tap away on home"
    },
    @{
        Output = "03_project_type.png"
        Source = Join-Path $FreshIntroDir "03_project_type.png"
        Headline = "Pick the workflow that fits"
        Subline = "Use blueprint takeoff or manual entry for the job"
    },
    @{
        Output = "04_blueprint_overview.png"
        Source = Join-Path $OutputDir "04_blueprint_overview.png"
        Headline = "Map the job visually"
        Subline = "See layout scale and takeoff context together"
    },
    @{
        Output = "05_blueprint_controls.png"
        Source = Join-Path $OutputDir "05_blueprint_controls.png"
        Headline = "Edit with precision"
        Subline = "Place openings and adjust the plan without losing context"
    },
    @{
        Output = "06_materials.png"
        Source = Join-Path $OutputDir "06_materials.png"
        Headline = "Keep pricing tied to takeoff"
        Subline = "Quantities costs and totals update together"
    },
    @{
        Output = "07_export.png"
        Source = Join-Path $OutputDir "07_export.png"
        Headline = "Send the estimate your way"
        Subline = "Export PDF PNG CSV or JSON in a few taps"
    },
    @{
        Output = "08_settings.png"
        Source = Join-Path $OutputDir "08_settings.png"
        Headline = "Set it up for real work"
        Subline = "Tune motion controls and onboarding help for the crew"
    }
)

$renderedAt = Get-Date

foreach ($shot in $shots) {
    if (-not (Test-Path $shot.Source)) {
        throw "Screenshot source not found: $($shot.Source)"
    }

    $outputPath = Join-Path $OutputDir $shot.Output
    $sourcePath = $shot.Source
    if ([System.IO.Path]::GetFullPath($sourcePath) -eq [System.IO.Path]::GetFullPath($outputPath)) {
        $tempSourcePath = Join-Path $tempDir ("source_" + $shot.Output)
        Copy-Item -LiteralPath $sourcePath -Destination $tempSourcePath -Force
        $sourcePath = $tempSourcePath
    }

    $brandText = Escape-DrawText "TRADESKETCH ESTIMATOR"
    $headlineText = Escape-DrawText $shot.Headline
    $sublineText = Escape-DrawText $shot.Subline
    $safeSource = $sourcePath.Replace("\", "/")
    $safeOutput = $outputPath.Replace("\", "/")
    $safeBold = Escape-FilterPath $fontBold
    $safeRegular = Escape-FilterPath $fontRegular
    $backgroundChain =
        "[1:v]drawbox=x=72:y=72:w=1296:h=320:color=0x17344D:t=fill," +
        "drawbox=x=104:y=424:w=1232:h=2632:color=0xFFFFFF:t=fill," +
        "drawtext=fontfile='$safeBold':text='$brandText':fontcolor=0xE7C56A:fontsize=34:x=96:y=104," +
        "drawtext=fontfile='$safeBold':text='$headlineText':fontcolor=white:fontsize=74:x=96:y=150," +
        "drawtext=fontfile='$safeRegular':text='$sublineText':fontcolor=0xF0F3F7:fontsize=38:x=96:y=258[bg]"

    $filter = @(
        "[0:v]scale=1200:2600:force_original_aspect_ratio=decrease[shot]"
        $backgroundChain
        "[bg][shot]overlay=x=120:y=440,format=rgb24[v]"
    ) -join ";"

    & ffmpeg -y `
        -i $safeSource `
        -f lavfi -i "color=c=#F2ECE2:s=1440x3120:d=1" `
        -filter_complex $filter `
        -map "[v]" `
        -update 1 `
        -frames:v 1 `
        $safeOutput | Out-Null

    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $outputPath)) {
        throw "ffmpeg failed while composing $($shot.Output)"
    }
}

$files = $shots | ForEach-Object {
    Get-Item (Join-Path $OutputDir $_.Output)
}

@(
    "Snapshot Label: play-store-v1.0.22-build-24-refresh-20260412"
    "Rendered At: $($renderedAt.ToString("yyyy-MM-dd HH:mm:ss zzz"))"
    "Fresh Intro Source: $relativeFreshIntroDir"
    "Workspace Source: verified existing v24 store capture set"
    "Output Directory: $relativeOutputDir"
    "Layout: padded headline band with non-overlapping descriptive text"
    ""
    "Files:"
) + ($files | ForEach-Object {
    "$($_.Name)`t$($_.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss zzz"))`t$($_.Length)"
}) | Set-Content -Path $snapshotPath

Remove-Item -Recurse -Force $tempDir

Write-Output $OutputDir

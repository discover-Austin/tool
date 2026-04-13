param(
    [string]$MediaDir = "",
    [string]$OutputFile = "tradesketch_showcase_play_store_30s.mp4",
    [ValidateSet("RawVideo", "Screenshots")]
    [string]$SourceMode = "Screenshots",
    [string]$ScreenshotDir = "",
    [double]$TotalDurationSeconds = 29.8
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($MediaDir)) {
    $MediaDir = Join-Path $projectRoot "media\play_store_showcase"
}
if ([string]::IsNullOrWhiteSpace($ScreenshotDir)) {
    $ScreenshotDir = Join-Path $projectRoot "store-assets\screenshots"
}

Set-Location $MediaDir

$outputPath = Join-Path $MediaDir $OutputFile
$durationText = $TotalDurationSeconds.ToString("0.###", [System.Globalization.CultureInfo]::InvariantCulture)
$voiceoverCandidates = @(
    (Join-Path $MediaDir "showcase_voiceover.mp3"),
    (Join-Path $MediaDir "showcase_voiceover.wav")
)
$voiceoverFile = $voiceoverCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

Remove-Item -Force $outputPath -ErrorAction SilentlyContinue

$commonArgs = @("-y")

if ($SourceMode -eq "RawVideo") {
    $commonArgs += @("-i", "tradesketch_showcase_raw.mp4")
} else {
    $screenshotFiles = @(
        "01_welcome.png",
        "02_saved_projects.png",
        "03_project_type.png",
        "04_blueprint_overview.png",
        "05_blueprint_controls.png",
        "06_materials.png",
        "07_export.png",
        "08_settings.png"
    )
    $sceneDuration = $TotalDurationSeconds / $screenshotFiles.Count
    $sceneDurationText = $sceneDuration.ToString("0.###", [System.Globalization.CultureInfo]::InvariantCulture)

    foreach ($file in $screenshotFiles) {
        $fullPath = Join-Path $ScreenshotDir $file
        if (-not (Test-Path $fullPath)) {
            throw "Missing screenshot for showcase render: $fullPath"
        }
        $commonArgs += @("-loop", "1", "-t", $sceneDurationText, "-i", $fullPath)
    }
}

if ($voiceoverFile) {
    $commonArgs += @("-i", (Split-Path $voiceoverFile -Leaf))
} else {
    $commonArgs += @("-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=48000")
}

$subtitleStyle = "FontName=Arial,FontSize=12,PrimaryColour=&H00FFFFFF,OutlineColour=&H90000000,BackColour=&H66101820,BorderStyle=3,Outline=1,Shadow=0,Alignment=2,MarginL=84,MarginR=84,MarginV=82"

if ($SourceMode -eq "RawVideo") {
    if ($voiceoverFile) {
        $audioInputIndex = 1
        $filter = "[0:v]tpad=stop_mode=clone:stop_duration=8,fps=30,split=2[base1][base2];[base1]scale=1080:1920:force_original_aspect_ratio=increase,boxblur=12:4,crop=1080:1920[bg];[base2]scale=886:1920:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,subtitles=showcase_captions.srt:force_style='$subtitleStyle'[v];[$($audioInputIndex):a]apad=pad_dur=$durationText[a]"
        $audioMap = "[a]"
    } else {
        $filter = "[0:v]tpad=stop_mode=clone:stop_duration=8,fps=30,split=2[base1][base2];[base1]scale=1080:1920:force_original_aspect_ratio=increase,boxblur=12:4,crop=1080:1920[bg];[base2]scale=886:1920:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,subtitles=showcase_captions.srt:force_style='$subtitleStyle'[v]"
        $audioMap = "1:a"
    }
} else {
    $filterParts = New-Object System.Collections.Generic.List[string]
    $sceneCount = $screenshotFiles.Count
    for ($i = 0; $i -lt $sceneCount; $i++) {
        $filterParts.Add("[$i`:v]fps=30,split=2[bgsrc$i][fgsrc$i]")
        $filterParts.Add("[bgsrc$i]scale=1080:1920:force_original_aspect_ratio=increase,boxblur=12:4,crop=1080:1920[bg$i]")
        $filterParts.Add("[fgsrc$i]scale=886:1920:force_original_aspect_ratio=decrease[fg$i]")
        $filterParts.Add("[bg$i][fg$i]overlay=(W-w)/2:(H-h)/2,setsar=1,format=yuv420p[s$i]")
    }

    $concatInputs = for ($i = 0; $i -lt $sceneCount; $i++) {
        "[s$i]"
    }
    $filterParts.Add(($concatInputs -join "") + "concat=n=$sceneCount`:v=1:a=0[showcase]")

    $filterParts.Add("[showcase]subtitles=showcase_captions.srt:force_style='$subtitleStyle'[v]")
    $audioInputIndex = $sceneCount
    if ($voiceoverFile) {
        $filterParts.Add("[$($audioInputIndex):a]apad=pad_dur=$durationText[a]")
        $audioMap = "[a]"
    } else {
        $audioMap = "$audioInputIndex:a"
    }

    $filter = [string]::Join(";", $filterParts)
}

$commonArgs += @(
    "-filter_complex", $filter,
    "-map", "[v]",
    "-map", $audioMap
)

$commonArgs += @(
    "-r", "30",
    "-c:v", "libx264",
    "-preset", "superfast",
    "-crf", "22",
    "-pix_fmt", "yuv420p",
    "-c:a", "aac",
    "-b:a", "160k",
    "-movflags", "+faststart",
    "-t", $durationText,
    $OutputFile
)

& ffmpeg @commonArgs

Write-Output $outputPath

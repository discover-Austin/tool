param(
    [string]$MediaDir = "C:\Users\grand\tool\media\play_store_showcase",
    [string]$ScriptFile = "showcase_voiceover.txt",
    [int]$Rate = 2,
    [int]$Volume = 100
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Speech

$textPath = Join-Path $MediaDir $ScriptFile
$wavPath = Join-Path $MediaDir "showcase_voiceover.wav"
$mp3Path = Join-Path $MediaDir "showcase_voiceover.mp3"
$metaPath = Join-Path $MediaDir "showcase_voiceover.meta.txt"

if (-not (Test-Path $textPath)) {
    throw "Voiceover script not found: $textPath"
}

$text = (Get-Content $textPath -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($text)) {
    throw "Voiceover script is empty: $textPath"
}

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.Rate = $Rate
$synth.Volume = $Volume
$voiceName = $synth.Voice.Name

try {
    $synth.SetOutputToWaveFile($wavPath)
    $synth.Speak($text)
} finally {
    $synth.Dispose()
}

if (Get-Command ffmpeg -ErrorAction SilentlyContinue) {
    & ffmpeg -y -i $wavPath -codec:a libmp3lame -b:a 192k $mp3Path | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "ffmpeg failed while converting voiceover WAV to MP3."
    }
}

@(
    "Generated At: $((Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"))"
    "Voice: $voiceName"
    "Rate: $Rate"
    "Volume: $Volume"
    "Script: $textPath"
    "Wav: $wavPath"
    "Mp3: $mp3Path"
) | Set-Content -Path $metaPath

Write-Output $mp3Path

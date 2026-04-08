param(
    [string]$MediaDir = "C:\Users\grand\tool\media\play_store_showcase",
    [string]$ScriptFile = "showcase_voiceover.txt",
    [string]$Voice = "en-US-JennyNeural",
    [string]$Rate = "-5%",
    [string]$Volume = "+0%"
)

$ErrorActionPreference = "Stop"

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

$engine = "edge-tts"
$voiceName = $Voice
$edgeTts = Get-Command edge-tts.exe -ErrorAction SilentlyContinue
if (-not $edgeTts) {
    $edgeTts = Get-Command edge-tts -ErrorAction SilentlyContinue
}

Remove-Item -Force $wavPath, $mp3Path -ErrorAction SilentlyContinue

if ($edgeTts) {
    & $edgeTts.Source "--voice=$Voice" "--rate=$Rate" "--volume=$Volume" "--text=$text" "--write-media=$mp3Path"
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $mp3Path)) {
        throw "edge-tts failed while generating showcase narration."
    }
    if (Get-Command ffmpeg -ErrorAction SilentlyContinue) {
        & ffmpeg -y -i $mp3Path -acodec pcm_s16le -ar 24000 $wavPath | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "ffmpeg failed while converting voiceover MP3 to WAV."
        }
    }
} else {
    Add-Type -AssemblyName System.Speech
    $engine = "System.Speech"
    $fallbackRate = 2
    $fallbackVolume = 100
    $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
    $synth.Rate = $fallbackRate
    $synth.Volume = $fallbackVolume
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
            throw "ffmpeg failed while converting fallback voiceover WAV to MP3."
        }
    }
}

@(
    "Generated At: $((Get-Date).ToString("yyyy-MM-dd HH:mm:ss zzz"))"
    "Engine: $engine"
    "Voice: $voiceName"
    "Rate: $Rate"
    "Volume: $Volume"
    "Script: $textPath"
    "Wav: $wavPath"
    "Mp3: $mp3Path"
) | Set-Content -Path $metaPath

Write-Output $mp3Path

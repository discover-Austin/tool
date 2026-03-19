param(
    [string]$MediaDir = "C:\Users\grand\tool\media\play_store_showcase"
)

$ErrorActionPreference = "Stop"

Set-Location $MediaDir

Remove-Item -Force "tradesketch_showcase_final.mp4" -ErrorAction SilentlyContinue

& ffmpeg -y `
    -i "tradesketch_showcase_raw.mp4" `
    -i "showcase_voiceover.mp3" `
    -filter_complex "[0:v]fps=30,split=2[base1][base2];[base1]scale=1080:1920:force_original_aspect_ratio=increase,boxblur=12:4,crop=1080:1920[bg];[base2]scale=886:1920:force_original_aspect_ratio=decrease[fg];[bg][fg]overlay=(W-w)/2:(H-h)/2,subtitles=showcase_captions.srt:force_style='FontName=Arial,FontSize=12,PrimaryColour=&H00FFFFFF,OutlineColour=&H90000000,BackColour=&H66101820,BorderStyle=3,Outline=1,Shadow=0,Alignment=2,MarginL=84,MarginR=84,MarginV=82'[v];[1:a]apad=pad_dur=30[a]" `
    -map "[v]" `
    -map "[a]" `
    -r 30 `
    -c:v libx264 `
    -preset superfast `
    -crf 22 `
    -pix_fmt yuv420p `
    -c:a aac `
    -b:a 160k `
    -movflags +faststart `
    -t 30.0 `
    "tradesketch_showcase_final.mp4"

Write-Output (Join-Path $MediaDir "tradesketch_showcase_final.mp4")

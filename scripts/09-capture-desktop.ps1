param(
    [string]$OutputPath = "",
    [switch]$PrimaryScreenOnly,
    [switch]$NoCursorMarker
)

<#
.SYNOPSIS
    Captures the Windows desktop to a PNG so Codex can inspect the live UI.

.DESCRIPTION
    By default this captures the full virtual desktop across all monitors and
    draws a simple marker where the current mouse cursor is located.
#>

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $projectRoot "tmp\desktop_capture.png"
}
$outputDirectory = Split-Path -Parent $OutputPath
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
}

$bounds = if ($PrimaryScreenOnly) {
    [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
} else {
    [System.Windows.Forms.SystemInformation]::VirtualScreen
}

$bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)

try {
    $graphics.CopyFromScreen(
        $bounds.Location,
        [System.Drawing.Point]::Empty,
        $bounds.Size
    )

    if (-not $NoCursorMarker) {
        $cursor = [System.Windows.Forms.Cursor]::Position
        $relativeX = $cursor.X - $bounds.Left
        $relativeY = $cursor.Y - $bounds.Top
        $outerPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(230, 227, 57, 53)), 4
        $innerPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(230, 255, 255, 255)), 2
        try {
            $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $graphics.DrawEllipse($outerPen, $relativeX - 18, $relativeY - 18, 36, 36)
            $graphics.DrawEllipse($innerPen, $relativeX - 8, $relativeY - 8, 16, 16)
            $graphics.DrawLine($outerPen, $relativeX - 26, $relativeY, $relativeX + 26, $relativeY)
            $graphics.DrawLine($outerPen, $relativeX, $relativeY - 26, $relativeX, $relativeY + 26)
        } finally {
            $outerPen.Dispose()
            $innerPen.Dispose()
        }
    }

    $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}

Write-Output $OutputPath

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("click-text", "doubleclick-text", "rightclick-text", "type-near-text")]
    [string]$Action,
    [Nullable[long]]$Handle = $null,
    [string]$Title = "",
    [string]$TitleContains = "",
    [string]$TitleRegex = "",
    [string]$ProcessName = "",
    [Nullable[int]]$ProcessId = $null,
    [int]$WindowIndex = 0,
    [string]$Text = "",
    [ValidateSet("exact", "contains", "regex", "fuzzy")]
    [string]$Match = "contains",
    [ValidateSet("any", "line", "word")]
    [string]$Kind = "line",
    [double]$MinScore = 0.55,
    [double]$MinConfidence = 0,
    [int]$MatchIndex = 0,
    [string]$Language = "eng",
    [int]$Psm = 6,
    [string]$TypeText = "",
    [int]$DelayMs = 180,
    [Nullable[int]]$X = $null,
    [Nullable[int]]$Y = $null,
    [Nullable[int]]$Width = $null,
    [Nullable[int]]$Height = $null,
    [switch]$RelativeToWindow,
    [switch]$DryRun,
    [switch]$Json
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "DesktopAutomation.Common.ps1")

if ([string]::IsNullOrWhiteSpace($Text)) {
    throw "Provide -Text to locate an OCR target."
}

$window = Resolve-DesktopWindow `
    -Handle $Handle `
    -Title $Title `
    -TitleContains $TitleContains `
    -TitleRegex $TitleRegex `
    -ProcessName $ProcessName `
    -ProcessId $ProcessId `
    -Index $WindowIndex

$projectRoot = Split-Path -Parent $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$imagePath = Join-Path $projectRoot ("tmp\desktop_text_action_{0}.png" -f $timestamp)
$artifactsBase = Join-Path ([System.IO.Path]::GetDirectoryName($imagePath)) ([System.IO.Path]::GetFileNameWithoutExtension($imagePath))

$captureBounds = $window
if ($X -ne $null -and $Y -ne $null -and $Width -ne $null -and $Height -ne $null) {
    $left = [int]$X
    $top = [int]$Y
    if ($RelativeToWindow) {
        $left += $window.Left
        $top += $window.Top
    }
    $captureBounds = ConvertTo-DesktopBoundsObject `
        -Left $left `
        -Top $top `
        -Right ($left + [int]$Width) `
        -Bottom ($top + [int]$Height)
}

Ensure-DesktopWindowForeground -Window $window | Out-Null
Capture-DesktopImage -OutputPath $imagePath -Bounds $captureBounds | Out-Null
$ocr = Invoke-DesktopTesseractOcr `
    -ImagePath $imagePath `
    -Language $Language `
    -Psm $Psm `
    -MinConfidence $MinConfidence `
    -ArtifactsBasePath $artifactsBase

$matches = Find-DesktopOcrText `
    -Entries $ocr.Entries `
    -Text $Text `
    -Match $Match `
    -Kind $Kind `
    -MinScore $MinScore `
    -MaxResults 25
$matches = @($matches)

if (@($matches).Count -eq 0) {
    throw "OCR text target '$Text' was not found."
}
if ($MatchIndex -lt 0 -or $MatchIndex -ge @($matches).Count) {
    throw "MatchIndex $MatchIndex is out of range for $(@($matches).Count) OCR match(es)."
}

$target = $matches[$MatchIndex]
$annotatedPath = "$artifactsBase.matched.png"
Save-DesktopAnnotatedImage -ImagePath $imagePath -Items @($target) -OutputPath $annotatedPath -LabelProperty "Text" | Out-Null

$resolvedPoint = [pscustomobject]@{
    X = $captureBounds.Left + $target.CenterX
    Y = $captureBounds.Top + $target.CenterY
}

if (-not $DryRun) {
    Move-DesktopCursor -X $resolvedPoint.X -Y $resolvedPoint.Y -DelayMs 60 | Out-Null
    switch ($Action) {
        "click-text" {
            Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs $DelayMs
        }

        "doubleclick-text" {
            Invoke-DesktopMouseClick -Button left -Count 2 -DelayMs $DelayMs
        }

        "rightclick-text" {
            Invoke-DesktopMouseClick -Button right -Count 1 -DelayMs $DelayMs
        }

        "type-near-text" {
            if ([string]::IsNullOrWhiteSpace($TypeText)) {
                throw "Provide -TypeText when Action is 'type-near-text'."
            }
            Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs 80
            Send-DesktopLiteralText -Text $TypeText -DelayMs $DelayMs
        }
    }
}

$result = [ordered]@{
    Window = $window
    CaptureBounds = $captureBounds
    ImagePath = $imagePath
    TsvPath = $ocr.TsvPath
    AnnotatedOutputPath = $annotatedPath
    Action = $Action
    Query = $Text
    Match = $Match
    Kind = $Kind
    MatchIndex = $MatchIndex
    DryRun = [bool]$DryRun
    ResolvedPoint = $resolvedPoint
    Target = $target
}

if ($Json) {
    $result | ConvertTo-Json -Depth 8
} else {
    $result
}

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("capture-ocr", "find-text", "exists-text")]
    [string]$Action,
    [Nullable[long]]$Handle = $null,
    [string]$Title = "",
    [string]$TitleContains = "",
    [string]$TitleRegex = "",
    [string]$ProcessName = "",
    [Nullable[int]]$ProcessId = $null,
    [int]$WindowIndex = 0,
    [Nullable[int]]$X = $null,
    [Nullable[int]]$Y = $null,
    [Nullable[int]]$Width = $null,
    [Nullable[int]]$Height = $null,
    [string]$Text = "",
    [ValidateSet("exact", "contains", "regex", "fuzzy")]
    [string]$Match = "contains",
    [ValidateSet("any", "line", "word")]
    [string]$Kind = "line",
    [double]$MinScore = 0.55,
    [double]$MinConfidence = 0,
    [int]$MaxResults = 25,
    [string]$Language = "eng",
    [int]$Psm = 6,
    [string]$OutputImagePath = "",
    [string]$OutputJsonPath = "",
    [string]$AnnotatedOutputPath = "",
    [switch]$FocusWindow,
    [switch]$MarkCursor,
    [switch]$Json
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "DesktopAutomation.Common.ps1")

function Resolve-OcrWindow {
    if (
        $Handle -ne $null -or
        -not [string]::IsNullOrWhiteSpace($Title) -or
        -not [string]::IsNullOrWhiteSpace($TitleContains) -or
        -not [string]::IsNullOrWhiteSpace($TitleRegex) -or
        -not [string]::IsNullOrWhiteSpace($ProcessName) -or
        $ProcessId -ne $null
    ) {
        return Resolve-DesktopWindow `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId `
            -Index $WindowIndex
    }

    return $null
}

function Resolve-OcrBounds {
    param(
        [pscustomobject]$Window
    )

    if ($null -ne $Window) {
        return $Window
    }

    if ($X -ne $null -and $Y -ne $null -and $Width -ne $null -and $Height -ne $null) {
        return ConvertTo-DesktopBoundsObject `
            -Left $X.Value `
            -Top $Y.Value `
            -Right ($X.Value + $Width.Value) `
            -Bottom ($Y.Value + $Height.Value)
    }

    return $null
}

function Write-OcrResult {
    param(
        [object]$Value
    )

    if ($Json) {
        $Value | ConvertTo-Json -Depth 8
    } else {
        $Value
    }
}

$window = Resolve-OcrWindow
$bounds = Resolve-OcrBounds -Window $window
$projectRoot = Split-Path -Parent $PSScriptRoot
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

if ([string]::IsNullOrWhiteSpace($OutputImagePath)) {
    $OutputImagePath = Join-Path $projectRoot ("tmp\desktop_ocr_{0}.png" -f $timestamp)
}

if ($null -ne $window -and $FocusWindow) {
    Ensure-DesktopWindowForeground -Window $window | Out-Null
    Start-Sleep -Milliseconds 120
}

Capture-DesktopImage -OutputPath $OutputImagePath -Bounds $bounds -MarkCursor:$MarkCursor | Out-Null

$artifactsBase = Join-Path ([System.IO.Path]::GetDirectoryName($OutputImagePath)) ([System.IO.Path]::GetFileNameWithoutExtension($OutputImagePath))
$ocr = Invoke-DesktopTesseractOcr `
    -ImagePath $OutputImagePath `
    -Language $Language `
    -Psm $Psm `
    -MinConfidence $MinConfidence `
    -ArtifactsBasePath $artifactsBase

if ([string]::IsNullOrWhiteSpace($OutputJsonPath)) {
    $OutputJsonPath = "$artifactsBase.json"
}

switch ($Action) {
    "capture-ocr" {
        if ([string]::IsNullOrWhiteSpace($AnnotatedOutputPath)) {
            $AnnotatedOutputPath = "$artifactsBase.annotated.png"
        }
        if (@($ocr.Lines).Count -gt 0) {
            Save-DesktopAnnotatedImage -ImagePath $OutputImagePath -Items $ocr.Lines -OutputPath $AnnotatedOutputPath -LabelProperty "Text" | Out-Null
        } else {
            $AnnotatedOutputPath = $null
        }

        $result = [ordered]@{
            Window = $window
            ImagePath = $OutputImagePath
            JsonPath = $OutputJsonPath
            TsvPath = $ocr.TsvPath
            AnnotatedOutputPath = $AnnotatedOutputPath
            LineCount = @($ocr.Lines).Count
            WordCount = @($ocr.Words).Count
            Lines = $ocr.Lines
        }
    }

    "find-text" {
        if ([string]::IsNullOrWhiteSpace($Text)) {
            throw "Provide -Text when Action is 'find-text'."
        }

        $matches = Find-DesktopOcrText `
            -Entries $ocr.Entries `
            -Text $Text `
            -Match $Match `
            -Kind $Kind `
            -MinScore $MinScore `
            -MaxResults $MaxResults
        $matches = @($matches)

        if ([string]::IsNullOrWhiteSpace($AnnotatedOutputPath)) {
            $AnnotatedOutputPath = "$artifactsBase.matches.png"
        }
        if (@($matches).Count -gt 0) {
            Save-DesktopAnnotatedImage -ImagePath $OutputImagePath -Items $matches -OutputPath $AnnotatedOutputPath -LabelProperty "Text" | Out-Null
        } else {
            $AnnotatedOutputPath = $null
        }

        $result = [ordered]@{
            Window = $window
            ImagePath = $OutputImagePath
            JsonPath = $OutputJsonPath
            TsvPath = $ocr.TsvPath
            AnnotatedOutputPath = $AnnotatedOutputPath
            Match = $Match
            Kind = $Kind
            Query = $Text
            Matches = $matches
        }
    }

    "exists-text" {
        if ([string]::IsNullOrWhiteSpace($Text)) {
            throw "Provide -Text when Action is 'exists-text'."
        }

        $matches = Find-DesktopOcrText `
            -Entries $ocr.Entries `
            -Text $Text `
            -Match $Match `
            -Kind $Kind `
            -MinScore $MinScore `
            -MaxResults 1
        $matches = @($matches)

        if (@($matches).Count -gt 0) {
            if ([string]::IsNullOrWhiteSpace($AnnotatedOutputPath)) {
                $AnnotatedOutputPath = "$artifactsBase.exists.png"
            }
            Save-DesktopAnnotatedImage -ImagePath $OutputImagePath -Items $matches -OutputPath $AnnotatedOutputPath -LabelProperty "Text" | Out-Null
        } else {
            $AnnotatedOutputPath = $null
        }

        $result = [ordered]@{
            Window = $window
            ImagePath = $OutputImagePath
            JsonPath = $OutputJsonPath
            TsvPath = $ocr.TsvPath
            AnnotatedOutputPath = $AnnotatedOutputPath
            Match = $Match
            Kind = $Kind
            Query = $Text
            Exists = @($matches).Count -gt 0
            MatchCount = @($matches).Count
            MatchEntry = if (@($matches).Count -gt 0) { $matches[0] } else { $null }
        }
    }
}

$result | ConvertTo-Json -Depth 8 | Set-Content -Path $OutputJsonPath -Encoding UTF8
Write-OcrResult -Value $result

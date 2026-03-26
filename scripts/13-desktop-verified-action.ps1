param(
    [string]$SpecPath = "",
    [string]$SpecJson = "",
    [string]$OutputDirectory = "",
    [switch]$Json
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "DesktopAutomation.Common.ps1")

function ConvertTo-PlainValue {
    param(
        [object]$Value
    )

    if ($null -eq $Value) {
        return $null
    }

    if ($Value -is [System.Collections.IDictionary]) {
        return ConvertTo-PlainHashtable -InputObject $Value
    }

    if ($Value -is [pscustomobject]) {
        return ConvertTo-PlainHashtable -InputObject $Value
    }

    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        $items = @()
        foreach ($item in $Value) {
            $items += ,(ConvertTo-PlainValue -Value $item)
        }
        return ,$items
    }

    return $Value
}

function ConvertTo-PlainHashtable {
    param(
        [object]$InputObject
    )

    if ($null -eq $InputObject) {
        return @{}
    }

    if ($InputObject -is [System.Collections.IDictionary]) {
        $result = @{}
        foreach ($key in $InputObject.Keys) {
            $result[$key] = ConvertTo-PlainValue -Value $InputObject[$key]
        }
        return $result
    }

    $result = @{}
    foreach ($property in $InputObject.PSObject.Properties) {
        $result[$property.Name] = ConvertTo-PlainValue -Value $property.Value
    }
    return $result
}

function Merge-Hashtable {
    param(
        [hashtable]$Base = @{},
        [hashtable]$Override = @{}
    )

    $merged = @{}
    foreach ($key in $Base.Keys) {
        $merged[$key] = $Base[$key]
    }

    foreach ($key in $Override.Keys) {
        if (
            $merged.ContainsKey($key) -and
            $merged[$key] -is [System.Collections.IDictionary] -and
            $Override[$key] -is [System.Collections.IDictionary]
        ) {
            $merged[$key] = Merge-Hashtable -Base $merged[$key] -Override $Override[$key]
        } else {
            $merged[$key] = $Override[$key]
        }
    }

    return $merged
}

function Get-ConfigSection {
    param(
        [hashtable]$Defaults,
        [hashtable]$Step,
        [string]$Name
    )

    $base = if ($Defaults.ContainsKey($Name) -and $Defaults[$Name] -is [System.Collections.IDictionary]) {
        $Defaults[$Name]
    } else {
        @{}
    }
    $override = if ($Step.ContainsKey($Name) -and $Step[$Name] -is [System.Collections.IDictionary]) {
        $Step[$Name]
    } else {
        @{}
    }
    return Merge-Hashtable -Base $base -Override $override
}

function Get-ConfigScalar {
    param(
        [hashtable]$Defaults,
        [hashtable]$Step,
        [string]$Name,
        [object]$Fallback = $null
    )

    if ($Step.ContainsKey($Name)) {
        return $Step[$Name]
    }
    if ($Defaults.ContainsKey($Name)) {
        return $Defaults[$Name]
    }
    return $Fallback
}

function Test-SelectorHasValue {
    param(
        [hashtable]$Selector
    )

    if ($null -eq $Selector -or $Selector.Count -eq 0) {
        return $false
    }

    foreach ($key in $Selector.Keys) {
        $value = $Selector[$key]
        if ($value -is [string]) {
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                return $true
            }
        } elseif ($null -ne $value) {
            return $true
        }
    }

    return $false
}

function Unwrap-SingleItemArray {
    param(
        [object]$Value
    )

    $current = $Value
    while ($current -is [System.Array] -and $current.Count -eq 1) {
        $current = $current[0]
    }
    return $current
}

function New-WindowSelectorParams {
    param(
        [hashtable]$Selector
    )

    $params = @{}
    if ($Selector.ContainsKey("handle") -and $null -ne $Selector["handle"]) { $params.Handle = [long]$Selector["handle"] }
    if ($Selector.ContainsKey("title")) { $params.Title = [string]$Selector["title"] }
    if ($Selector.ContainsKey("titleContains")) { $params.TitleContains = [string]$Selector["titleContains"] }
    if ($Selector.ContainsKey("titleRegex")) { $params.TitleRegex = [string]$Selector["titleRegex"] }
    if ($Selector.ContainsKey("processName")) { $params.ProcessName = [string]$Selector["processName"] }
    if ($Selector.ContainsKey("processId") -and $null -ne $Selector["processId"]) { $params.ProcessId = [int]$Selector["processId"] }
    if ($Selector.ContainsKey("index") -and $null -ne $Selector["index"]) { $params.Index = [int]$Selector["index"] }
    return $params
}

function Resolve-VerifiedWindow {
    param(
        [hashtable]$Selector,
        [switch]$AllowEmpty
    )

    if (-not (Test-SelectorHasValue -Selector $Selector)) {
        if ($AllowEmpty) {
            return $null
        }
        throw "This step requires a window selector, but none was provided."
    }

    $params = New-WindowSelectorParams -Selector $Selector
    if (-not $params.ContainsKey("Index")) {
        $params.Index = 0
    }
    return Resolve-DesktopWindow @params
}

function New-ElementSelectorParams {
    param(
        [hashtable]$Selector
    )

    $params = @{}
    if ($Selector.ContainsKey("name")) { $params.Name = [string]$Selector["name"] }
    if ($Selector.ContainsKey("nameContains")) { $params.NameContains = [string]$Selector["nameContains"] }
    if ($Selector.ContainsKey("nameRegex")) { $params.NameRegex = [string]$Selector["nameRegex"] }
    if ($Selector.ContainsKey("automationId")) { $params.AutomationId = [string]$Selector["automationId"] }
    if ($Selector.ContainsKey("className")) { $params.ClassName = [string]$Selector["className"] }
    if ($Selector.ContainsKey("controlType")) { $params.ControlType = [string]$Selector["controlType"] }
    if ($Selector.ContainsKey("index") -and $null -ne $Selector["index"]) { $params.Index = [int]$Selector["index"] }
    if ($Selector.ContainsKey("includeOffscreen") -and $null -ne $Selector["includeOffscreen"]) { $params.IncludeOffscreen = [bool]$Selector["includeOffscreen"] }
    if ($Selector.ContainsKey("maxResults") -and $null -ne $Selector["maxResults"]) { $params.MaxResults = [int]$Selector["maxResults"] }
    return $params
}

function Resolve-VerifiedElement {
    param(
        [pscustomobject]$Window,
        [hashtable]$Selector
    )

    if ($null -eq $Window) {
        throw "Element selectors require a resolved window context."
    }
    if (-not (Test-SelectorHasValue -Selector $Selector)) {
        throw "This step requires an element selector, but none was provided."
    }

    $params = New-ElementSelectorParams -Selector $Selector
    if (-not $params.ContainsKey("Index")) {
        $params.Index = 0
    }
    return Get-DesktopUiElement -Window $Window @params
}

function Resolve-VerifiedCaptureBounds {
    param(
        [string]$CaptureScope,
        [pscustomobject]$Window
    )

    if ($CaptureScope -eq "window" -and $null -ne $Window) {
        return $Window
    }

    $virtual = [System.Windows.Forms.SystemInformation]::VirtualScreen
    return ConvertTo-DesktopBoundsObject -Left $virtual.Left -Top $virtual.Top -Right $virtual.Right -Bottom $virtual.Bottom
}

function Test-HybridTargetSelector {
    param(
        [hashtable]$Selector
    )

    if ($null -eq $Selector) {
        return $false
    }

    return $Selector.ContainsKey("uia") -or $Selector.ContainsKey("ocr") -or $Selector.ContainsKey("fallback")
}

function ConvertTo-OcrActionTarget {
    param(
        [pscustomobject]$Match,
        [pscustomobject]$CaptureBounds
    )

    [pscustomobject]@{
        Mode = "OCR"
        Text = $Match.Text
        Confidence = $Match.Confidence
        Score = $Match.Score
        Left = $CaptureBounds.Left + $Match.Left
        Top = $CaptureBounds.Top + $Match.Top
        Right = $CaptureBounds.Left + $Match.Right
        Bottom = $CaptureBounds.Top + $Match.Bottom
        Width = $Match.Width
        Height = $Match.Height
        CenterX = $CaptureBounds.Left + $Match.CenterX
        CenterY = $CaptureBounds.Top + $Match.CenterY
    }
}

function Resolve-FallbackTarget {
    param(
        [hashtable]$Selector,
        [pscustomobject]$Window,
        [pscustomobject]$CaptureBounds
    )

    $anchor = if ($null -ne $Window) { $Window } else { $CaptureBounds }
    if ($null -eq $anchor) {
        throw "Fallback targeting requires a resolved window or capture bounds."
    }

    if ($Selector.ContainsKey("relativeX") -and $Selector.ContainsKey("relativeY")) {
        $x = [int][Math]::Round($anchor.Left + ($anchor.Width * [double]$Selector["relativeX"]))
        $y = [int][Math]::Round($anchor.Top + ($anchor.Height * [double]$Selector["relativeY"]))
        return [pscustomobject]@{
            Mode = "Fallback"
            Left = $x
            Top = $y
            Right = $x
            Bottom = $y
            Width = 0
            Height = 0
            CenterX = $x
            CenterY = $y
        }
    }

    if ($Selector.ContainsKey("x") -and $Selector.ContainsKey("y")) {
        $x = [int]$Selector["x"]
        $y = [int]$Selector["y"]
        if ($Selector.ContainsKey("relativeToWindow") -and [bool]$Selector["relativeToWindow"]) {
            $x += $anchor.Left
            $y += $anchor.Top
        }
        return [pscustomobject]@{
            Mode = "Fallback"
            Left = $x
            Top = $y
            Right = $x
            Bottom = $y
            Width = 0
            Height = 0
            CenterX = $x
            CenterY = $y
        }
    }

    throw "Fallback selectors require relativeX/relativeY or x/y."
}

function New-TargetResolutionAttempt {
    param(
        [string]$Mode,
        [hashtable]$Selector,
        [bool]$Succeeded,
        [int]$MatchCount = 0,
        [object]$Details = $null,
        [string]$FailureCode = "",
        [string]$Error = ""
    )

    $attempt = [ordered]@{
        Mode = $Mode
        Selector = $Selector
        Succeeded = $Succeeded
        MatchCount = $MatchCount
        Details = $Details
    }

    if (-not [string]::IsNullOrWhiteSpace($FailureCode)) {
        $attempt["FailureCode"] = $FailureCode
    }
    if (-not [string]::IsNullOrWhiteSpace($Error)) {
        $attempt["Error"] = $Error
    }

    return [pscustomobject]$attempt
}

function Resolve-StepTarget {
    param(
        [hashtable]$TargetSelector,
        [pscustomobject]$Window,
        [string]$CaptureImagePath,
        [pscustomobject]$CaptureBounds
    )

    if (-not (Test-SelectorHasValue -Selector $TargetSelector) -and -not (Test-HybridTargetSelector -Selector $TargetSelector)) {
        return [pscustomobject]@{
            Succeeded = $true
            Mode = $null
            MatchCount = 0
            Selector = $null
            Target = $null
            Details = $null
            Attempts = @()
            FailureCode = $null
            Error = $null
        }
    }

    $resolutionAttempts = New-Object "System.Collections.Generic.List[object]"

    if (-not (Test-HybridTargetSelector -Selector $TargetSelector)) {
        try {
            $target = Resolve-VerifiedElement -Window $Window -Selector $TargetSelector
            $details = ConvertTo-DesktopUiSummary -InputObject $target
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "UIA" -Selector $TargetSelector -Succeeded $true -MatchCount 1 -Details $details)) | Out-Null
            return [pscustomobject]@{
                Succeeded = $true
                Mode = "UIA"
                MatchCount = 1
                Selector = $TargetSelector
                Target = $target
                Details = $details
                Attempts = $resolutionAttempts.ToArray()
                FailureCode = $null
                Error = $null
            }
        } catch {
            $message = $_.Exception.Message
            $failureCode = Resolve-AutomationFailureCode -Message $message -Default "UIAElementNotFound"
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "UIA" -Selector $TargetSelector -Succeeded $false -MatchCount 0 -Details $null -FailureCode $failureCode -Error $message)) | Out-Null
            return [pscustomobject]@{
                Succeeded = $false
                Mode = $null
                MatchCount = 0
                Selector = $TargetSelector
                Target = $null
                Details = $null
                Attempts = $resolutionAttempts.ToArray()
                FailureCode = $failureCode
                Error = $message
            }
        }
    }

    $lastError = $null
    $lastFailureCode = $null

    if ($TargetSelector.ContainsKey("uia")) {
        $uiaSelector = ConvertTo-PlainHashtable -InputObject $TargetSelector["uia"]
        if (Test-SelectorHasValue -Selector $uiaSelector) {
            try {
                $target = Resolve-VerifiedElement -Window $Window -Selector $uiaSelector
                $details = ConvertTo-DesktopUiSummary -InputObject $target
                $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "UIA" -Selector $uiaSelector -Succeeded $true -MatchCount 1 -Details $details)) | Out-Null
                return [pscustomobject]@{
                    Succeeded = $true
                    Mode = "UIA"
                    MatchCount = 1
                    Selector = $uiaSelector
                    Target = $target
                    Details = $details
                    Attempts = $resolutionAttempts.ToArray()
                    FailureCode = $null
                    Error = $null
                }
            } catch {
                $lastError = $_.Exception.Message
                $lastFailureCode = Resolve-AutomationFailureCode -Message $lastError -Default "UIAElementNotFound"
                $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "UIA" -Selector $uiaSelector -Succeeded $false -MatchCount 0 -Details $null -FailureCode $lastFailureCode -Error $lastError)) | Out-Null
            }
        }
    }

    if ($TargetSelector.ContainsKey("ocr")) {
        $ocrSelector = ConvertTo-PlainHashtable -InputObject $TargetSelector["ocr"]
        if (-not $ocrSelector.ContainsKey("text")) {
            throw "OCR target selectors require a text value."
        }
        if ([string]::IsNullOrWhiteSpace($CaptureImagePath)) {
            throw "OCR target selectors require a pre-action capture image."
        }

        $language = if ($ocrSelector.ContainsKey("language")) { [string]$ocrSelector["language"] } else { "eng" }
        $psm = if ($ocrSelector.ContainsKey("psm") -and $null -ne $ocrSelector["psm"]) { [int]$ocrSelector["psm"] } else { 6 }
        $minConfidence = if ($ocrSelector.ContainsKey("minConfidence") -and $null -ne $ocrSelector["minConfidence"]) { [double]$ocrSelector["minConfidence"] } else { 0 }
        $matchMode = if ($ocrSelector.ContainsKey("match")) { [string]$ocrSelector["match"] } else { "contains" }
        $kind = if ($ocrSelector.ContainsKey("kind")) { [string]$ocrSelector["kind"] } else { "line" }
        $minScore = if ($ocrSelector.ContainsKey("minScore") -and $null -ne $ocrSelector["minScore"]) { [double]$ocrSelector["minScore"] } else { 0.55 }
        $matchIndex = if ($ocrSelector.ContainsKey("index") -and $null -ne $ocrSelector["index"]) { [int]$ocrSelector["index"] } else { 0 }

        try {
            $ocrResult = Invoke-DesktopTesseractOcr -ImagePath $CaptureImagePath -Language $language -Psm $psm -MinConfidence $minConfidence
            $matches = @(Find-DesktopOcrText -Entries $ocrResult.Entries -Text ([string]$ocrSelector["text"]) -Match $matchMode -Kind $kind -MinScore $minScore -MaxResults 25)
            if ($matchIndex -lt 0 -or $matchIndex -ge @($matches).Count) {
                throw "OCR text target '$($ocrSelector["text"])' was not found."
            }

            $resolvedMatch = @($matches)[$matchIndex]
            $target = ConvertTo-OcrActionTarget -Match $resolvedMatch -CaptureBounds $CaptureBounds
            $details = [ordered]@{
                Match = $resolvedMatch
                Confidence = $resolvedMatch.Confidence
                Score = $resolvedMatch.Score
            }
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "OCR" -Selector $ocrSelector -Succeeded $true -MatchCount @($matches).Count -Details $details)) | Out-Null
            return [pscustomobject]@{
                Succeeded = $true
                Mode = "OCR"
                MatchCount = @($matches).Count
                Selector = $ocrSelector
                Target = $target
                Details = $details
                Attempts = $resolutionAttempts.ToArray()
                FailureCode = $null
                Error = $null
            }
        } catch {
            $lastError = $_.Exception.Message
            $lastFailureCode = Resolve-AutomationFailureCode -Message $lastError -Default "OCRTextNotFound"
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "OCR" -Selector $ocrSelector -Succeeded $false -MatchCount 0 -Details $null -FailureCode $lastFailureCode -Error $lastError)) | Out-Null
        }
    }

    if ($TargetSelector.ContainsKey("fallback")) {
        $fallbackSelector = ConvertTo-PlainHashtable -InputObject $TargetSelector["fallback"]
        try {
            $target = Resolve-FallbackTarget -Selector $fallbackSelector -Window $Window -CaptureBounds $CaptureBounds
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "Fallback" -Selector $fallbackSelector -Succeeded $true -MatchCount 1 -Details $target)) | Out-Null
            return [pscustomobject]@{
                Succeeded = $true
                Mode = "Fallback"
                MatchCount = 1
                Selector = $fallbackSelector
                Target = $target
                Details = $target
                Attempts = $resolutionAttempts.ToArray()
                FailureCode = $null
                Error = $null
            }
        } catch {
            $lastError = $_.Exception.Message
            $lastFailureCode = Resolve-AutomationFailureCode -Message $lastError -Default "TargetNotResolved"
            $resolutionAttempts.Add((New-TargetResolutionAttempt -Mode "Fallback" -Selector $fallbackSelector -Succeeded $false -MatchCount 0 -Details $null -FailureCode $lastFailureCode -Error $lastError)) | Out-Null
        }
    }

    $message = if (-not [string]::IsNullOrWhiteSpace($lastError)) { $lastError } else { "No target matched via UIA, OCR, or fallback." }
    $failureCode = if (-not [string]::IsNullOrWhiteSpace($lastFailureCode)) { $lastFailureCode } else { "TargetNotResolved" }
    return [pscustomobject]@{
        Succeeded = $false
        Mode = $null
        MatchCount = 0
        Selector = $null
        Target = $null
        Details = $null
        Attempts = $resolutionAttempts.ToArray()
        FailureCode = $failureCode
        Error = $message
    }
}

function Find-VerifiedElements {
    param(
        [pscustomobject]$Window,
        [hashtable]$Selector
    )

    if ($null -eq $Window) {
        throw "Element selectors require a resolved window context."
    }
    if (-not (Test-SelectorHasValue -Selector $Selector)) {
        return @()
    }

    $params = New-ElementSelectorParams -Selector $Selector
    if (-not $params.ContainsKey("MaxResults")) {
        $params.MaxResults = 25
    }
    return Find-DesktopUiElements -Window $Window @params
}

function Test-WindowMatchesSelector {
    param(
        [pscustomobject]$Window,
        [hashtable]$Selector
    )

    if ($null -eq $Window) {
        return $false
    }
    if (-not (Test-SelectorHasValue -Selector $Selector)) {
        return $true
    }

    if ($Selector.ContainsKey("handle") -and $Window.Handle -ne [long]$Selector["handle"]) { return $false }
    if ($Selector.ContainsKey("title") -and $Window.Title -ne [string]$Selector["title"]) { return $false }
    if ($Selector.ContainsKey("titleContains")) {
        $needle = ([string]$Selector["titleContains"]).ToLowerInvariant()
        if (-not ([string]$Window.Title).ToLowerInvariant().Contains($needle)) { return $false }
    }
    if ($Selector.ContainsKey("titleRegex") -and -not ([string]$Window.Title -match [string]$Selector["titleRegex"])) { return $false }
    if ($Selector.ContainsKey("processName")) {
        if (([string]$Window.ProcessName).ToLowerInvariant() -ne ([string]$Selector["processName"]).ToLowerInvariant()) { return $false }
    }
    if ($Selector.ContainsKey("processId") -and $Window.ProcessId -ne [int]$Selector["processId"]) { return $false }
    return $true
}

function Get-WindowContextForCheck {
    param(
        [pscustomobject]$StepWindow,
        [pscustomobject]$ForegroundWindow
    )

    if ($null -ne $StepWindow) {
        return $StepWindow
    }
    return $ForegroundWindow
}

function ConvertTo-SafeFileToken {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return "step"
    }

    $safe = ($Value -replace "[^A-Za-z0-9]+", "_").Trim("_")
    if ([string]::IsNullOrWhiteSpace($safe)) {
        return "step"
    }
    return $safe
}

function Capture-VerifiedState {
    param(
        [string]$Phase,
        [int]$StepNumber,
        [int]$AttemptNumber,
        [string]$StepName,
        [string]$CaptureScope,
        [pscustomobject]$Window,
        [bool]$MarkCursor
    )

    if ($CaptureScope -eq "none") {
        return $null
    }

    $safeStepName = ConvertTo-SafeFileToken -Value $StepName
    $path = Join-Path $script:VerifiedOutputDirectory ("{0:00}_{1}_attempt{2}_{3}.png" -f $StepNumber, $safeStepName, $AttemptNumber, $Phase)
    $bounds = if ($CaptureScope -eq "window" -and $null -ne $Window) { $Window } else { $null }
    Capture-DesktopImage -OutputPath $path -Bounds $bounds -MarkCursor:$MarkCursor | Out-Null
    return $path
}

function Resolve-AbsolutePoint {
    param(
        [hashtable]$Action,
        [pscustomobject]$Window,
        [pscustomobject]$Target
    )

    if ($Action.ContainsKey("useTargetCenter") -and [bool]$Action["useTargetCenter"]) {
        if ($null -eq $Target) {
            throw "The action requested useTargetCenter, but no target element was resolved."
        }
        return [pscustomobject]@{
            X = $Target.CenterX
            Y = $Target.CenterY
        }
    }

    if ($Action.ContainsKey("x") -and $Action.ContainsKey("y")) {
        $x = [int]$Action["x"]
        $y = [int]$Action["y"]
        if ($Action.ContainsKey("relativeToWindow") -and [bool]$Action["relativeToWindow"]) {
            if ($null -eq $Window) {
                throw "relativeToWindow requires a resolved window."
            }
            $x += $Window.Left
            $y += $Window.Top
        }
        return [pscustomobject]@{
            X = $x
            Y = $y
        }
    }

    return $null
}

function Invoke-VerifiedStepAction {
    param(
        [hashtable]$Action,
        [pscustomobject]$Window,
        [pscustomobject]$Target,
        [int]$SettleMs
    )

    $actionType = if ($Action.ContainsKey("type")) { [string]$Action["type"] } else { "" }
    if ([string]::IsNullOrWhiteSpace($actionType)) {
        throw "Each step needs an action.type value."
    }

    $targetHasUiElement = $null -ne $Target -and @($Target.PSObject.Properties.Match("Element")).Count -gt 0
    $targetPoint = if ($null -ne $Target -and @($Target.PSObject.Properties.Match("CenterX")).Count -gt 0 -and @($Target.PSObject.Properties.Match("CenterY")).Count -gt 0) {
        [pscustomobject]@{
            X = [int]$Target.CenterX
            Y = [int]$Target.CenterY
        }
    } else {
        $null
    }

    switch ($actionType) {
        "focusWindow" {
            if ($null -eq $Window) {
                throw "focusWindow requires a resolved window."
            }
            Focus-DesktopWindow -Window $Window -DelayMs $SettleMs | Out-Null
        }

        "capture" {
            Start-Sleep -Milliseconds ([Math]::Max(80, $SettleMs))
        }

        "click" {
            if ($targetHasUiElement) {
                Invoke-DesktopUiAction -ElementInfo $Target -Action click -DelayMs $SettleMs
            } elseif ($null -ne $targetPoint) {
                Move-DesktopCursor -X $targetPoint.X -Y $targetPoint.Y -DelayMs 60 | Out-Null
                $button = if ($Action.ContainsKey("button")) { [string]$Action["button"] } else { "left" }
                $count = if ($Action.ContainsKey("count") -and $null -ne $Action["count"]) { [int]$Action["count"] } else { 1 }
                Invoke-DesktopMouseClick -Button $button -Count $count -DelayMs $SettleMs
            } else {
                $point = Resolve-AbsolutePoint -Action $Action -Window $Window -Target $Target
                if ($null -eq $point) {
                    throw "click requires either a target element or explicit x/y coordinates."
                }
                Move-DesktopCursor -X $point.X -Y $point.Y -DelayMs 60 | Out-Null
                $button = if ($Action.ContainsKey("button")) { [string]$Action["button"] } else { "left" }
                $count = if ($Action.ContainsKey("count") -and $null -ne $Action["count"]) { [int]$Action["count"] } else { 1 }
                Invoke-DesktopMouseClick -Button $button -Count $count -DelayMs $SettleMs
            }
        }

        "doubleClick" {
            if ($null -ne $targetPoint) {
                Move-DesktopCursor -X $targetPoint.X -Y $targetPoint.Y -DelayMs 60 | Out-Null
            } else {
                $point = Resolve-AbsolutePoint -Action $Action -Window $Window -Target $Target
                if ($null -eq $point) {
                    throw "doubleClick requires either a target element or explicit x/y coordinates."
                }
                Move-DesktopCursor -X $point.X -Y $point.Y -DelayMs 60 | Out-Null
            }
            Invoke-DesktopMouseClick -Button left -Count 2 -DelayMs $SettleMs
        }

        "invoke" {
            if (-not $targetHasUiElement) {
                throw "invoke requires a target element selector."
            }
            Invoke-DesktopUiAction -ElementInfo $Target -Action invoke -DelayMs $SettleMs
        }

        "setValue" {
            if (-not $targetHasUiElement) {
                throw "setValue requires a target element selector."
            }
            if (-not $Action.ContainsKey("value")) {
                throw "setValue requires action.value."
            }
            Invoke-DesktopUiAction -ElementInfo $Target -Action setvalue -Value ([string]$Action["value"]) -DelayMs $SettleMs
        }

        "type" {
            if ($targetHasUiElement) {
                Invoke-DesktopUiAction -ElementInfo $Target -Action click -DelayMs 80
            } elseif ($null -ne $targetPoint) {
                Move-DesktopCursor -X $targetPoint.X -Y $targetPoint.Y -DelayMs 60 | Out-Null
                Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs 80
            }
            if (-not $Action.ContainsKey("text")) {
                throw "type requires action.text."
            }
            Send-DesktopLiteralText -Text ([string]$Action["text"]) -DelayMs $SettleMs
        }

        "keys" {
            if ($targetHasUiElement) {
                Invoke-DesktopUiAction -ElementInfo $Target -Action click -DelayMs 80
            } elseif ($null -ne $targetPoint) {
                Move-DesktopCursor -X $targetPoint.X -Y $targetPoint.Y -DelayMs 60 | Out-Null
                Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs 80
            }
            if (-not $Action.ContainsKey("keys")) {
                throw "keys requires action.keys."
            }
            Send-DesktopKeys -Keys ([string]$Action["keys"]) -DelayMs $SettleMs
        }

        "scroll" {
            if ($null -ne $targetPoint) {
                Move-DesktopCursor -X $targetPoint.X -Y $targetPoint.Y -DelayMs 60 | Out-Null
            } else {
                $point = Resolve-AbsolutePoint -Action $Action -Window $Window -Target $Target
                if ($null -ne $point) {
                    Move-DesktopCursor -X $point.X -Y $point.Y -DelayMs 60 | Out-Null
                }
            }
            $delta = if ($Action.ContainsKey("delta") -and $null -ne $Action["delta"]) { [int]$Action["delta"] } else { 120 }
            Invoke-DesktopScroll -Delta $delta -DelayMs $SettleMs
        }

        "drag" {
            foreach ($required in @("startX", "startY", "endX", "endY")) {
                if (-not $Action.ContainsKey($required)) {
                    throw "drag requires action.$required."
                }
            }

            $startX = [int]$Action["startX"]
            $startY = [int]$Action["startY"]
            $endX = [int]$Action["endX"]
            $endY = [int]$Action["endY"]
            if ($Action.ContainsKey("relativeToWindow") -and [bool]$Action["relativeToWindow"]) {
                if ($null -eq $Window) {
                    throw "drag with relativeToWindow requires a resolved window."
                }
                $startX += $Window.Left
                $startY += $Window.Top
                $endX += $Window.Left
                $endY += $Window.Top
            }

            $steps = if ($Action.ContainsKey("steps") -and $null -ne $Action["steps"]) { [int]$Action["steps"] } else { 18 }
            $stepDelayMs = if ($Action.ContainsKey("stepDelayMs") -and $null -ne $Action["stepDelayMs"]) { [int]$Action["stepDelayMs"] } else { 20 }
            Invoke-DesktopDrag -StartX $startX -StartY $startY -EndX $endX -EndY $endY -Steps $steps -StepDelayMs $stepDelayMs
            Start-Sleep -Milliseconds $SettleMs
        }

        default {
            throw "Unsupported action.type '$actionType'."
        }
    }
}

function Add-VerificationCheck {
    param(
        [System.Collections.Generic.List[object]]$Checks,
        [string]$Name,
        [bool]$Passed,
        [string]$Detail,
        [string]$Code = ""
    )

    $Checks.Add([pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Detail = $Detail
        Code = $Code
    }) | Out-Null
}

function Resolve-AutomationFailureCode {
    param(
        [string]$Message,
        [string]$Default = "AutomationError"
    )

    $text = if ($null -eq $Message) { "" } else { $Message }
    switch -Wildcard ($text) {
        "*No visible desktop window matched*" { return "WindowNotFound" }
        "*No UI element matched*" { return "UIAElementNotFound" }
        "*OCR text target*" { return "OCRTextNotFound" }
        "*No target matched via UIA, OCR, or fallback*" { return "TargetNotResolved" }
        "*could not be confirmed as foreground*" { return "ForegroundMismatch" }
        "*action.type*" { return "ActionConfigInvalid" }
        "*out of range*" { return "SelectorIndexOutOfRange" }
        default { return $Default }
    }
}

function Test-VerifiedExpectations {
    param(
        [hashtable]$Expect,
        [pscustomobject]$StepWindow,
        [pscustomobject]$ForegroundWindow,
        [string]$CaptureImagePath = ""
    )

    $checks = New-Object "System.Collections.Generic.List[object]"
    if ($null -eq $Expect -or $Expect.Count -eq 0) {
        Add-VerificationCheck -Checks $checks -Name "NoExplicitExpectations" -Passed $true -Detail "No explicit expectations were defined for this step."
        return [pscustomobject]@{
            Passed = $true
            Checks = $checks.ToArray()
            FailureCode = $null
        }
    }

    if ($Expect.ContainsKey("windowFocused") -and [bool]$Expect["windowFocused"]) {
        $passed = $null -ne $StepWindow -and $null -ne $ForegroundWindow -and $StepWindow.Handle -eq $ForegroundWindow.Handle
        $detail = if ($passed) {
            "Foreground window matches the step window."
        } else {
            "Foreground window did not match the step window."
        }
        Add-VerificationCheck -Checks $checks -Name "WindowFocused" -Passed $passed -Detail $detail -Code "FocusStolen"
    }

    if ($Expect.ContainsKey("foregroundWindow")) {
        $selector = ConvertTo-PlainHashtable -InputObject $Expect["foregroundWindow"]
        $passed = Test-WindowMatchesSelector -Window $ForegroundWindow -Selector $selector
        $detail = if ($passed) {
            "Foreground window matched the expected selector."
        } else {
            "Foreground window did not match the expected selector."
        }
        Add-VerificationCheck -Checks $checks -Name "ForegroundWindowSelector" -Passed $passed -Detail $detail -Code "ForegroundMismatch"
    }

    if ($Expect.ContainsKey("window")) {
        $selector = ConvertTo-PlainHashtable -InputObject $Expect["window"]
        try {
            Resolve-VerifiedWindow -Selector $selector | Out-Null
            Add-VerificationCheck -Checks $checks -Name "WindowExists" -Passed $true -Detail "A window matched the expected selector." -Code "WindowNotFound"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "WindowExists" -Passed $false -Detail $_.Exception.Message -Code "WindowNotFound"
        }
    }

    $windowContext = Get-WindowContextForCheck -StepWindow $StepWindow -ForegroundWindow $ForegroundWindow
    $ocrResult = $null

    function Get-ExpectationOcrResult {
        if ([string]::IsNullOrWhiteSpace($CaptureImagePath)) {
            throw "OCR verification requires a captured image path."
        }
        if ($null -eq $ocrResult) {
            $ocrResult = Invoke-DesktopTesseractOcr -ImagePath $CaptureImagePath
        }
        return $ocrResult
    }

    function Resolve-OcrExpectationConfig {
        param(
            [object]$ConfigValue
        )

        if ($ConfigValue -is [string]) {
            return @{
                text = [string]$ConfigValue
                match = "contains"
                kind = "line"
                minScore = 0.55
                minConfidence = 0
            }
        }

        $config = ConvertTo-PlainHashtable -InputObject $ConfigValue
        if (-not $config.ContainsKey("text")) {
            throw "OCR verification requires a text value."
        }
        if (-not $config.ContainsKey("match")) { $config["match"] = "contains" }
        if (-not $config.ContainsKey("kind")) { $config["kind"] = "line" }
        if (-not $config.ContainsKey("minScore")) { $config["minScore"] = 0.55 }
        if (-not $config.ContainsKey("minConfidence")) { $config["minConfidence"] = 0 }
        return $config
    }

    if ($Expect.ContainsKey("element")) {
        $selector = ConvertTo-PlainHashtable -InputObject $Expect["element"]
        try {
            $element = Resolve-VerifiedElement -Window $windowContext -Selector $selector
            Add-VerificationCheck -Checks $checks -Name "ElementExists" -Passed $true -Detail ("Matched element '{0}'." -f $element.Name) -Code "UIAElementNotFound"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "ElementExists" -Passed $false -Detail $_.Exception.Message -Code "UIAElementNotFound"
        }
    }

    if ($Expect.ContainsKey("elementAbsent")) {
        $selector = ConvertTo-PlainHashtable -InputObject $Expect["elementAbsent"]
        try {
            $elements = Find-VerifiedElements -Window $windowContext -Selector $selector
            $passed = $elements.Count -eq 0
            $detail = if ($passed) {
                "No matching element was present after the action."
            } else {
                "Found $($elements.Count) matching element(s) after the action."
            }
            Add-VerificationCheck -Checks $checks -Name "ElementAbsent" -Passed $passed -Detail $detail -Code "VerificationFailed"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "ElementAbsent" -Passed $false -Detail $_.Exception.Message -Code "VerificationFailed"
        }
    }

    if ($Expect.ContainsKey("focusedElement")) {
        $selector = ConvertTo-PlainHashtable -InputObject $Expect["focusedElement"]
        try {
            $element = Resolve-VerifiedElement -Window $windowContext -Selector $selector
            $passed = [bool]$element.HasKeyboardFocus
            $detail = if ($passed) {
                "Element '$($element.Name)' has keyboard focus."
            } else {
                "Element '$($element.Name)' did not have keyboard focus."
            }
            Add-VerificationCheck -Checks $checks -Name "FocusedElement" -Passed $passed -Detail $detail -Code "FocusStolen"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "FocusedElement" -Passed $false -Detail $_.Exception.Message -Code "FocusStolen"
        }
    }

    if ($Expect.ContainsKey("valueContains")) {
        $valueCheck = ConvertTo-PlainHashtable -InputObject $Expect["valueContains"]
        $expectedText = if ($valueCheck.ContainsKey("text")) { [string]$valueCheck["text"] } else { "" }
        $null = $valueCheck.Remove("text")
        try {
            $element = Resolve-VerifiedElement -Window $windowContext -Selector $valueCheck
            $passed = -not [string]::IsNullOrEmpty($expectedText) -and ([string]$element.Value).Contains($expectedText)
            $detail = if ($passed) {
                "Element value contained the expected text."
            } else {
                "Element value did not contain '$expectedText'."
            }
            Add-VerificationCheck -Checks $checks -Name "ElementValueContains" -Passed $passed -Detail $detail -Code "VerificationFailed"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "ElementValueContains" -Passed $false -Detail $_.Exception.Message -Code "VerificationFailed"
        }
    }

    if ($Expect.ContainsKey("textVisible")) {
        try {
            $textCheck = Resolve-OcrExpectationConfig -ConfigValue $Expect["textVisible"]
            $ocr = Get-ExpectationOcrResult
            $baseEntries = if ($null -ne $ocr -and $null -ne $ocr.Entries) {
                @($ocr.Entries)
            } else {
                @()
            }
            $entries = if ($textCheck.ContainsKey("minConfidence")) {
                @($baseEntries | Where-Object { $_.Confidence -ge [double]$textCheck["minConfidence"] })
            } else {
                $baseEntries
            }
            $matches = if (@($entries).Count -gt 0) {
                @(Find-DesktopOcrText -Entries $entries -Text ([string]$textCheck["text"]) -Match ([string]$textCheck["match"]) -Kind ([string]$textCheck["kind"]) -MinScore ([double]$textCheck["minScore"]) -MaxResults 1)
            } else {
                @()
            }
            $passed = @($matches).Count -gt 0
            $detail = if ($passed) {
                "OCR found the expected text."
            } else {
                "OCR did not find '$($textCheck["text"])'."
            }
            Add-VerificationCheck -Checks $checks -Name "TextVisible" -Passed $passed -Detail $detail -Code "OCRTextNotFound"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "TextVisible" -Passed $false -Detail $_.Exception.Message -Code "OCRTextNotFound"
        }
    }

    if ($Expect.ContainsKey("textAbsent")) {
        try {
            $textCheck = Resolve-OcrExpectationConfig -ConfigValue $Expect["textAbsent"]
            $ocr = Get-ExpectationOcrResult
            $baseEntries = if ($null -ne $ocr -and $null -ne $ocr.Entries) {
                @($ocr.Entries)
            } else {
                @()
            }
            $entries = if ($textCheck.ContainsKey("minConfidence")) {
                @($baseEntries | Where-Object { $_.Confidence -ge [double]$textCheck["minConfidence"] })
            } else {
                $baseEntries
            }
            $matches = if (@($entries).Count -gt 0) {
                @(Find-DesktopOcrText -Entries $entries -Text ([string]$textCheck["text"]) -Match ([string]$textCheck["match"]) -Kind ([string]$textCheck["kind"]) -MinScore ([double]$textCheck["minScore"]) -MaxResults 1)
            } else {
                @()
            }
            $passed = @($matches).Count -eq 0
            $detail = if ($passed) {
                "OCR did not find the forbidden text."
            } else {
                "OCR still found '$($textCheck["text"])' after the action."
            }
            Add-VerificationCheck -Checks $checks -Name "TextAbsent" -Passed $passed -Detail $detail -Code "VerificationFailed"
        } catch {
            Add-VerificationCheck -Checks $checks -Name "TextAbsent" -Passed $false -Detail $_.Exception.Message -Code "VerificationFailed"
        }
    }

    $passed = @($checks | Where-Object { -not $_.Passed }).Count -eq 0
    $firstFailed = $checks | Where-Object { -not $_.Passed } | Select-Object -First 1
    return [pscustomobject]@{
        Passed = $passed
        Checks = $checks.ToArray()
        FailureCode = if ($null -ne $firstFailed -and -not [string]::IsNullOrWhiteSpace([string]$firstFailed.Code)) { [string]$firstFailed.Code } else { $null }
    }
}

if ([string]::IsNullOrWhiteSpace($SpecPath) -and [string]::IsNullOrWhiteSpace($SpecJson)) {
    throw "Provide either -SpecPath or -SpecJson."
}

$specContent = if (-not [string]::IsNullOrWhiteSpace($SpecPath)) {
    Get-Content -Raw $SpecPath
} else {
    $SpecJson
}

$specMap = ConvertFrom-Json -InputObject $specContent -AsHashtable -Depth 20
$defaults = if ($specMap.ContainsKey("defaults") -and $specMap["defaults"] -is [System.Collections.IDictionary]) {
    $specMap["defaults"]
} else {
    @{}
}
$steps = @()
if ($specMap.ContainsKey("steps")) {
    $stepsValue = $specMap["steps"]
    if ($stepsValue -is [System.Collections.IDictionary]) {
        $steps = [object[]]@($stepsValue)
    } elseif ($stepsValue -is [System.Array]) {
        $steps = [object[]]$stepsValue
    } else {
        $steps = [object[]]@($stepsValue)
    }
}
if ($steps.Count -eq 0) {
    throw "The spec did not include any steps."
}

$script:VerifiedOutputDirectory = if (-not [string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory
} elseif ($defaults.ContainsKey("captureDirectory") -and -not [string]::IsNullOrWhiteSpace([string]$defaults["captureDirectory"])) {
    [string]$defaults["captureDirectory"]
} else {
    Join-Path (Split-Path -Parent $PSScriptRoot) ("tmp\desktop_verified_{0}" -f (Get-Date -Format "yyyyMMdd_HHmmss"))
}
New-Item -ItemType Directory -Force -Path $script:VerifiedOutputDirectory | Out-Null

$runLog = New-Object "System.Collections.ArrayList"

for ($stepIndex = 0; $stepIndex -lt $steps.Count; $stepIndex++) {
    $stepItem = Unwrap-SingleItemArray -Value $steps[$stepIndex]
    $stepMap = if ($stepItem -is [System.Collections.IDictionary]) {
        $stepItem
    } else {
        ConvertTo-PlainHashtable -InputObject $stepItem
    }
    $stepNumber = $stepIndex + 1
    $stepName = if ($stepMap.ContainsKey("name") -and -not [string]::IsNullOrWhiteSpace([string]$stepMap["name"])) {
        [string]$stepMap["name"]
    } else {
        "Step $stepNumber"
    }

    $windowSelector = Get-ConfigSection -Defaults $defaults -Step $stepMap -Name "window"
    $targetSelector = Get-ConfigSection -Defaults $defaults -Step $stepMap -Name "target"
    $actionConfig = Get-ConfigSection -Defaults $defaults -Step $stepMap -Name "action"
    $expectConfig = Get-ConfigSection -Defaults $defaults -Step $stepMap -Name "expect"
    $focusWindow = [bool](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "focusWindow" -Fallback $true)
    $focusRetries = [int](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "focusRetries" -Fallback 2)
    $focusRetryDelayMs = [int](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "focusRetryDelayMs" -Fallback 160)
    $settleMs = [int](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "settleMs" -Fallback 240)
    $retries = [int](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "retries" -Fallback 0)
    $captureScope = [string](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "captureScope" -Fallback "window")
    $markCursor = [bool](Get-ConfigScalar -Defaults $defaults -Step $stepMap -Name "markCursor" -Fallback $true)

    $stepResult = [ordered]@{
        Name = $stepName
        StepNumber = $stepNumber
        Succeeded = $false
        Attempts = @()
    }

    $lastFailure = $null
    $lastFailureCode = $null
    for ($attempt = 1; $attempt -le ($retries + 1); $attempt++) {
        $effectiveWindowSelector = $windowSelector
        $effectiveTargetSelector = $targetSelector
        if ($targetSelector.ContainsKey("window") -and $targetSelector["window"] -is [System.Collections.IDictionary]) {
            $effectiveWindowSelector = Merge-Hashtable -Base $windowSelector -Override (ConvertTo-PlainHashtable -InputObject $targetSelector["window"])
            $effectiveTargetSelector = Merge-Hashtable -Base @{} -Override $targetSelector
            $null = $effectiveTargetSelector.Remove("window")
        }

        $window = $null
        $target = $null
        $targetResolution = $null
        $preCapturePath = $null
        $postCapturePath = $null
        $foregroundBefore = $null
        $foregroundAfter = $null
        $verification = $null
        $focusLease = $null

        try {
            $window = Resolve-VerifiedWindow -Selector $effectiveWindowSelector -AllowEmpty
            if ($focusWindow -and $null -ne $window) {
                $focusLease = Ensure-DesktopWindowForeground `
                    -Window $window `
                    -RetryCount $focusRetries `
                    -DelayMs ([Math]::Max(140, $settleMs)) `
                    -RetryDelayMs $focusRetryDelayMs
                if (-not $focusLease.Success) {
                    throw "The target window could not be confirmed as foreground."
                }
            }

            $foregroundBefore = if ($null -ne $focusLease) { $focusLease.ForegroundWindow } else { Get-DesktopForegroundWindow }
            $captureBounds = Resolve-VerifiedCaptureBounds -CaptureScope $captureScope -Window $window
            $needsOcrCapture = $null -ne $effectiveTargetSelector -and $effectiveTargetSelector.ContainsKey("ocr")
            $preCaptureScope = $captureScope
            if ($preCaptureScope -eq "none" -and $needsOcrCapture) {
                $preCaptureScope = if ($null -ne $window) { "window" } else { "desktop" }
            }

            $preCapturePath = Capture-VerifiedState `
                -Phase "before" `
                -StepNumber $stepNumber `
                -AttemptNumber $attempt `
                -StepName $stepName `
                -CaptureScope $preCaptureScope `
                -Window $window `
                -MarkCursor $markCursor

            $targetResolution = Resolve-StepTarget `
                -TargetSelector $effectiveTargetSelector `
                -Window $window `
                -CaptureImagePath $preCapturePath `
                -CaptureBounds $captureBounds
            if (-not $targetResolution.Succeeded) {
                throw $targetResolution.Error
            }
            $target = $targetResolution.Target

            Invoke-VerifiedStepAction -Action $actionConfig -Window $window -Target $target -SettleMs $settleMs

            Start-Sleep -Milliseconds $settleMs
            $foregroundAfter = Get-DesktopForegroundWindow
            $verificationWindow = if ($null -ne $window) {
                Resolve-VerifiedWindow -Selector @{ handle = $window.Handle } -AllowEmpty
            } else {
                $window
            }

            $postCapturePath = Capture-VerifiedState `
                -Phase "after" `
                -StepNumber $stepNumber `
                -AttemptNumber $attempt `
                -StepName $stepName `
                -CaptureScope $captureScope `
                -Window $verificationWindow `
                -MarkCursor $markCursor

            $verification = Test-VerifiedExpectations -Expect $expectConfig -StepWindow $verificationWindow -ForegroundWindow $foregroundAfter -CaptureImagePath $postCapturePath
            $attemptResult = [ordered]@{
                Attempt = $attempt
                Succeeded = $verification.Passed
                Window = $window
                Target = ConvertTo-DesktopUiSummary -InputObject $target
                TargetResolution = $targetResolution
                FocusLease = $focusLease
                ForegroundBefore = $foregroundBefore
                ForegroundAfter = $foregroundAfter
                PreCapturePath = $preCapturePath
                PostCapturePath = $postCapturePath
                Verification = $verification
                FailureCode = $verification.FailureCode
            }
            $stepResult.Attempts += [pscustomobject]$attemptResult

            if ($verification.Passed) {
                $stepResult.Succeeded = $true
                break
            }

            $lastFailure = "Expectation check failed."
            $lastFailureCode = if (-not [string]::IsNullOrWhiteSpace([string]$verification.FailureCode)) { [string]$verification.FailureCode } else { "VerificationFailed" }
        } catch {
            $foregroundAfter = Get-DesktopForegroundWindow
            $attemptResult = [ordered]@{
                Attempt = $attempt
                Succeeded = $false
                Window = $window
                Target = ConvertTo-DesktopUiSummary -InputObject $target
                TargetResolution = $targetResolution
                FocusLease = $focusLease
                ForegroundBefore = $foregroundBefore
                ForegroundAfter = $foregroundAfter
                PreCapturePath = $preCapturePath
                PostCapturePath = $postCapturePath
                Error = $_.Exception.Message
                FailureCode = Resolve-AutomationFailureCode -Message $_.Exception.Message
            }
            $stepResult.Attempts += [pscustomobject]$attemptResult
            $lastFailure = $_.Exception.Message
            $lastFailureCode = $attemptResult.FailureCode
        }
    }

    if (-not $stepResult.Succeeded -and -not [string]::IsNullOrWhiteSpace($lastFailure)) {
        $stepResult.Error = $lastFailure
        $stepResult.FailureCode = $lastFailureCode
    }

    $runLog.Add([pscustomobject]$stepResult) | Out-Null
    if (-not $stepResult.Succeeded) {
        break
    }
}

$specPathValue = if ([string]::IsNullOrWhiteSpace($SpecPath)) { $null } else { $SpecPath }
$stepArray = $runLog.ToArray()
$runSucceeded = @($stepArray | Where-Object { -not $_.Succeeded }).Count -eq 0
$result = [ordered]@{
    Succeeded = $runSucceeded
    CaptureDirectory = $script:VerifiedOutputDirectory
    SpecPath = $specPathValue
    Steps = $stepArray
}

$logPath = Join-Path $script:VerifiedOutputDirectory "run_log.json"
$result | ConvertTo-Json -Depth 10 | Set-Content -Encoding UTF8 $logPath
$result["LogPath"] = $logPath

if ($Json) {
    $result | ConvertTo-Json -Depth 10
} else {
    $result
}

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("list", "focus", "bounds", "capture", "move")]
    [string]$Action,
    [Nullable[long]]$Handle = $null,
    [string]$Title = "",
    [string]$TitleContains = "",
    [string]$TitleRegex = "",
    [string]$ProcessName = "",
    [Nullable[int]]$ProcessId = $null,
    [int]$Index = 0,
    [string]$OutputPath = "",
    [Nullable[int]]$X = $null,
    [Nullable[int]]$Y = $null,
    [Nullable[int]]$Width = $null,
    [Nullable[int]]$Height = $null,
    [switch]$MarkCursor,
    [switch]$Json
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "DesktopAutomation.Common.ps1")

function Write-DesktopWindowResult {
    param(
        [object]$Value
    )

    if ($Json) {
        $Value | ConvertTo-Json -Depth 6
    } else {
        $Value
    }
}

switch ($Action) {
    "list" {
        $windows = Find-DesktopWindows `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId
        Write-DesktopWindowResult -Value $windows
    }

    "focus" {
        $window = Resolve-DesktopWindow `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId `
            -Index $Index
        Focus-DesktopWindow -Window $window | Out-Null
        Write-DesktopWindowResult -Value $window
    }

    "bounds" {
        $window = Resolve-DesktopWindow `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId `
            -Index $Index
        Write-DesktopWindowResult -Value $window
    }

    "capture" {
        $window = Resolve-DesktopWindow `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId `
            -Index $Index
        if ([string]::IsNullOrWhiteSpace($OutputPath)) {
            $OutputPath = Join-Path (Split-Path -Parent $PSScriptRoot) ("tmp\window_capture_{0}.png" -f $window.Handle)
        }
        Capture-DesktopImage -OutputPath $OutputPath -Bounds $window -MarkCursor:$MarkCursor | Out-Null
        Write-DesktopWindowResult -Value ([pscustomobject]@{
            OutputPath = $OutputPath
            Window = $window
        })
    }

    "move" {
        $window = Resolve-DesktopWindow `
            -Handle $Handle `
            -Title $Title `
            -TitleContains $TitleContains `
            -TitleRegex $TitleRegex `
            -ProcessName $ProcessName `
            -ProcessId $ProcessId `
            -Index $Index
        $targetX = if ($X -ne $null) { $X.Value } else { $window.Left }
        $targetY = if ($Y -ne $null) { $Y.Value } else { $window.Top }
        $targetWidth = if ($Width -ne $null) { $Width.Value } else { $window.Width }
        $targetHeight = if ($Height -ne $null) { $Height.Value } else { $window.Height }
        Move-DesktopWindowRect -Window $window -X $targetX -Y $targetY -Width $targetWidth -Height $targetHeight
        $updated = Resolve-DesktopWindow -Handle $window.Handle
        Write-DesktopWindowResult -Value $updated
    }
}

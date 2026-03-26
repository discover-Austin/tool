param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("dump", "find", "click", "invoke", "setvalue")]
    [string]$Action,
    [Nullable[long]]$Handle = $null,
    [string]$Title = "",
    [string]$TitleContains = "",
    [string]$TitleRegex = "",
    [string]$ProcessName = "",
    [Nullable[int]]$ProcessId = $null,
    [int]$WindowIndex = 0,
    [string]$Name = "",
    [string]$NameContains = "",
    [string]$NameRegex = "",
    [string]$AutomationId = "",
    [string]$ClassName = "",
    [string]$ControlType = "",
    [int]$ElementIndex = 0,
    [int]$MaxResults = 25,
    [string]$Value = "",
    [switch]$IncludeOffscreen,
    [switch]$Json
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "DesktopAutomation.Common.ps1")

function Resolve-DesktopUiWindow {
    return Resolve-DesktopWindow `
        -Handle $Handle `
        -Title $Title `
        -TitleContains $TitleContains `
        -TitleRegex $TitleRegex `
        -ProcessName $ProcessName `
        -ProcessId $ProcessId `
        -Index $WindowIndex
}

function Write-UiResult {
    param(
        [object]$Value
    )

    $summary = ConvertTo-DesktopUiSummary -InputObject $Value
    if ($Json) {
        $summary | ConvertTo-Json -Depth 7
    } else {
        $summary
    }
}

$window = Resolve-DesktopUiWindow

switch ($Action) {
    "dump" {
        $elements = Find-DesktopUiElements `
            -Window $window `
            -IncludeRoot `
            -IncludeOffscreen:$IncludeOffscreen `
            -MaxResults $MaxResults
        Write-UiResult -Value $elements
    }

    "find" {
        $elements = Find-DesktopUiElements `
            -Window $window `
            -Name $Name `
            -NameContains $NameContains `
            -NameRegex $NameRegex `
            -AutomationId $AutomationId `
            -ClassName $ClassName `
            -ControlType $ControlType `
            -IncludeOffscreen:$IncludeOffscreen `
            -MaxResults $MaxResults
        Write-UiResult -Value $elements
    }

    "click" {
        $element = Get-DesktopUiElement `
            -Window $window `
            -Name $Name `
            -NameContains $NameContains `
            -NameRegex $NameRegex `
            -AutomationId $AutomationId `
            -ClassName $ClassName `
            -ControlType $ControlType `
            -Index $ElementIndex `
            -IncludeOffscreen:$IncludeOffscreen
        Invoke-DesktopUiAction -ElementInfo $element -Action click
        Write-UiResult -Value $element
    }

    "invoke" {
        $element = Get-DesktopUiElement `
            -Window $window `
            -Name $Name `
            -NameContains $NameContains `
            -NameRegex $NameRegex `
            -AutomationId $AutomationId `
            -ClassName $ClassName `
            -ControlType $ControlType `
            -Index $ElementIndex `
            -IncludeOffscreen:$IncludeOffscreen
        Invoke-DesktopUiAction -ElementInfo $element -Action invoke
        Write-UiResult -Value $element
    }

    "setvalue" {
        if ([string]::IsNullOrWhiteSpace($Value)) {
            throw "Provide -Value when Action is 'setvalue'."
        }
        $element = Get-DesktopUiElement `
            -Window $window `
            -Name $Name `
            -NameContains $NameContains `
            -NameRegex $NameRegex `
            -AutomationId $AutomationId `
            -ClassName $ClassName `
            -ControlType $ControlType `
            -Index $ElementIndex `
            -IncludeOffscreen:$IncludeOffscreen
        Invoke-DesktopUiAction -ElementInfo $element -Action setvalue -Value $Value
        Write-UiResult -Value $element
    }
}

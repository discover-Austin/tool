Set-StrictMode -Version Latest

if (-not ("DesktopAutomationNative" -as [type])) {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes
    Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class DesktopAutomationNative {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc callback, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern int GetWindowTextLength(IntPtr hWnd);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int maxCount);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int command);

    [DllImport("user32.dll")]
    public static extern bool MoveWindow(IntPtr hWnd, int x, int y, int width, int height, bool repaint);

    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int x, int y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extraInfo);
}
'@
}

$script:DesktopAutomationConstants = @{
    MouseLeftDown = 0x0002
    MouseLeftUp = 0x0004
    MouseRightDown = 0x0008
    MouseRightUp = 0x0010
    MouseMiddleDown = 0x0020
    MouseMiddleUp = 0x0040
    MouseWheel = 0x0800
    ShowRestore = 9
}

function Get-DesktopCursorPosition {
    return [System.Windows.Forms.Cursor]::Position
}

function ConvertTo-DesktopBoundsObject {
    param(
        [int]$Left,
        [int]$Top,
        [int]$Right,
        [int]$Bottom
    )

    [pscustomobject]@{
        Left = $Left
        Top = $Top
        Right = $Right
        Bottom = $Bottom
        Width = [Math]::Max(0, $Right - $Left)
        Height = [Math]::Max(0, $Bottom - $Top)
    }
}

function Get-DesktopWindowTitle {
    param(
        [IntPtr]$Handle
    )

    $length = [DesktopAutomationNative]::GetWindowTextLength($Handle)
    if ($length -le 0) {
        return ""
    }
    $builder = New-Object System.Text.StringBuilder ($length + 1)
    [DesktopAutomationNative]::GetWindowText($Handle, $builder, $builder.Capacity) | Out-Null
    return $builder.ToString()
}

function Get-DesktopWindowBounds {
    param(
        [IntPtr]$Handle
    )

    $rect = New-Object DesktopAutomationNative+RECT
    if (-not [DesktopAutomationNative]::GetWindowRect($Handle, [ref]$rect)) {
        throw "Could not read bounds for window handle $($Handle.ToInt64())."
    }
    return ConvertTo-DesktopBoundsObject -Left $rect.Left -Top $rect.Top -Right $rect.Right -Bottom $rect.Bottom
}

function Get-DesktopWindows {
    $windows = New-Object "System.Collections.Generic.List[object]"
    $callback = [DesktopAutomationNative+EnumWindowsProc]{
        param(
            [IntPtr]$Handle,
            [IntPtr]$LParam
        )

        if (-not [DesktopAutomationNative]::IsWindowVisible($Handle)) {
            return $true
        }

        $title = Get-DesktopWindowTitle -Handle $Handle
        if ([string]::IsNullOrWhiteSpace($title)) {
            return $true
        }

        $processId = [uint32]0
        [DesktopAutomationNative]::GetWindowThreadProcessId($Handle, [ref]$processId) | Out-Null
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            return $true
        }

        $bounds = Get-DesktopWindowBounds -Handle $Handle
        if ($bounds.Width -le 0 -or $bounds.Height -le 0) {
            return $true
        }

        $windows.Add(
            [pscustomobject]@{
                Handle = $Handle.ToInt64()
                ProcessId = [int]$processId
                ProcessName = $process.ProcessName
                Title = $title
                Left = $bounds.Left
                Top = $bounds.Top
                Right = $bounds.Right
                Bottom = $bounds.Bottom
                Width = $bounds.Width
                Height = $bounds.Height
            }
        ) | Out-Null

        return $true
    }

    [DesktopAutomationNative]::EnumWindows($callback, [IntPtr]::Zero) | Out-Null
    return $windows | Sort-Object ProcessName, Title
}

function Resolve-DesktopWindow {
    param(
        [Nullable[long]]$Handle = $null,
        [string]$Title = "",
        [string]$TitleContains = "",
        [string]$TitleRegex = "",
        [string]$ProcessName = "",
        [Nullable[int]]$ProcessId = $null,
        [int]$Index = 0
    )

    $matches = Find-DesktopWindows `
        -Handle $Handle `
        -Title $Title `
        -TitleContains $TitleContains `
        -TitleRegex $TitleRegex `
        -ProcessName $ProcessName `
        -ProcessId $ProcessId

    $matches = @($matches)
    if ($matches.Count -eq 0) {
        throw "No visible desktop window matched the provided selector."
    }
    if ($Index -lt 0 -or $Index -ge $matches.Count) {
        throw "Window selector matched $($matches.Count) window(s), but Index $Index is out of range."
    }
    return $matches[$Index]
}

function Find-DesktopWindows {
    param(
        [Nullable[long]]$Handle = $null,
        [string]$Title = "",
        [string]$TitleContains = "",
        [string]$TitleRegex = "",
        [string]$ProcessName = "",
        [Nullable[int]]$ProcessId = $null
    )

    $matches = Get-DesktopWindows

    if ($Handle -ne $null) {
        $handleValue = [long]$Handle
        $matches = $matches | Where-Object { $_.Handle -eq $handleValue }
    }
    if (-not [string]::IsNullOrWhiteSpace($Title)) {
        $matches = $matches | Where-Object { $_.Title -eq $Title }
    }
    if (-not [string]::IsNullOrWhiteSpace($TitleContains)) {
        $needle = $TitleContains.ToLowerInvariant()
        $matches = $matches | Where-Object { $_.Title.ToLowerInvariant().Contains($needle) }
    }
    if (-not [string]::IsNullOrWhiteSpace($TitleRegex)) {
        $matches = $matches | Where-Object { $_.Title -match $TitleRegex }
    }
    if (-not [string]::IsNullOrWhiteSpace($ProcessName)) {
        $processNeedle = $ProcessName.ToLowerInvariant()
        $matches = $matches | Where-Object { $_.ProcessName.ToLowerInvariant() -eq $processNeedle }
    }
    if ($ProcessId -ne $null) {
        $processIdValue = [int]$ProcessId
        $matches = $matches | Where-Object { $_.ProcessId -eq $processIdValue }
    }

    return @($matches)
}

function Get-DesktopForegroundWindow {
    $handle = [DesktopAutomationNative]::GetForegroundWindow()
    if ($handle -eq [IntPtr]::Zero) {
        return $null
    }

    $match = @(Find-DesktopWindows -Handle $handle.ToInt64())
    if (@($match).Count -gt 0) {
        return @($match)[0]
    }

    $processId = [uint32]0
    [DesktopAutomationNative]::GetWindowThreadProcessId($handle, [ref]$processId) | Out-Null
    $bounds = Get-DesktopWindowBounds -Handle $handle
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue

    return [pscustomobject]@{
        Handle = $handle.ToInt64()
        ProcessId = [int]$processId
        ProcessName = if ($null -ne $process) { $process.ProcessName } else { "" }
        Title = Get-DesktopWindowTitle -Handle $handle
        Left = $bounds.Left
        Top = $bounds.Top
        Right = $bounds.Right
        Bottom = $bounds.Bottom
        Width = $bounds.Width
        Height = $bounds.Height
    }
}

function Focus-DesktopWindow {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [int]$DelayMs = 180
    )

    $handle = [IntPtr]$Window.Handle
    [DesktopAutomationNative]::ShowWindow($handle, $script:DesktopAutomationConstants.ShowRestore) | Out-Null
    [DesktopAutomationNative]::SetForegroundWindow($handle) | Out-Null
    Start-Sleep -Milliseconds $DelayMs
    return $Window
}

function Move-DesktopWindowRect {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [int]$X,
        [int]$Y,
        [int]$Width,
        [int]$Height,
        [int]$DelayMs = 180
    )

    [DesktopAutomationNative]::MoveWindow(
        [IntPtr]$Window.Handle,
        $X,
        $Y,
        $Width,
        $Height,
        $true
    ) | Out-Null
    Start-Sleep -Milliseconds $DelayMs
}

function Capture-DesktopImage {
    param(
        [string]$OutputPath,
        [pscustomobject]$Bounds = $null,
        [switch]$MarkCursor
    )

    if ([string]::IsNullOrWhiteSpace($OutputPath)) {
        throw "OutputPath is required."
    }

    $directory = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $resolvedBounds = if ($null -eq $Bounds) {
        $virtual = [System.Windows.Forms.SystemInformation]::VirtualScreen
        ConvertTo-DesktopBoundsObject -Left $virtual.Left -Top $virtual.Top -Right $virtual.Right -Bottom $virtual.Bottom
    } else {
        $Bounds
    }

    $bitmap = New-Object System.Drawing.Bitmap $resolvedBounds.Width, $resolvedBounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen(
            [System.Drawing.Point]::new($resolvedBounds.Left, $resolvedBounds.Top),
            [System.Drawing.Point]::Empty,
            [System.Drawing.Size]::new($resolvedBounds.Width, $resolvedBounds.Height)
        )

        if ($MarkCursor) {
            $cursor = Get-DesktopCursorPosition
            if (
                $cursor.X -ge $resolvedBounds.Left -and
                $cursor.X -lt $resolvedBounds.Right -and
                $cursor.Y -ge $resolvedBounds.Top -and
                $cursor.Y -lt $resolvedBounds.Bottom
            ) {
                $relativeX = $cursor.X - $resolvedBounds.Left
                $relativeY = $cursor.Y - $resolvedBounds.Top
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
        }

        $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }

    return $OutputPath
}

function Move-DesktopCursor {
    param(
        [int]$X,
        [int]$Y,
        [int]$DelayMs = 120
    )

    [DesktopAutomationNative]::SetCursorPos($X, $Y) | Out-Null
    Start-Sleep -Milliseconds $DelayMs
    return Get-DesktopCursorPosition
}

function Invoke-DesktopMouseClick {
    param(
        [ValidateSet("left", "right", "middle")]
        [string]$Button = "left",
        [int]$Count = 1,
        [int]$DelayMs = 120
    )

    $flags = switch ($Button) {
        "left" { @($script:DesktopAutomationConstants.MouseLeftDown, $script:DesktopAutomationConstants.MouseLeftUp) }
        "right" { @($script:DesktopAutomationConstants.MouseRightDown, $script:DesktopAutomationConstants.MouseRightUp) }
        "middle" { @($script:DesktopAutomationConstants.MouseMiddleDown, $script:DesktopAutomationConstants.MouseMiddleUp) }
    }

    foreach ($i in 1..$Count) {
        [DesktopAutomationNative]::mouse_event($flags[0], 0, 0, 0, [UIntPtr]::Zero)
        [DesktopAutomationNative]::mouse_event($flags[1], 0, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds ([Math]::Max(80, $DelayMs))
    }
}

function Invoke-DesktopScroll {
    param(
        [int]$Delta = 120,
        [int]$DelayMs = 120
    )

    [DesktopAutomationNative]::mouse_event(
        $script:DesktopAutomationConstants.MouseWheel,
        0,
        0,
        [uint32]$Delta,
        [UIntPtr]::Zero
    )
    Start-Sleep -Milliseconds $DelayMs
}

function Invoke-DesktopDrag {
    param(
        [int]$StartX,
        [int]$StartY,
        [int]$EndX,
        [int]$EndY,
        [int]$Steps = 18,
        [int]$StepDelayMs = 20
    )

    Move-DesktopCursor -X $StartX -Y $StartY -DelayMs 60 | Out-Null
    [DesktopAutomationNative]::mouse_event($script:DesktopAutomationConstants.MouseLeftDown, 0, 0, 0, [UIntPtr]::Zero)
    try {
        for ($step = 1; $step -le $Steps; $step++) {
            $progress = $step / $Steps
            $x = [int][Math]::Round($StartX + (($EndX - $StartX) * $progress))
            $y = [int][Math]::Round($StartY + (($EndY - $StartY) * $progress))
            [DesktopAutomationNative]::SetCursorPos($x, $y) | Out-Null
            Start-Sleep -Milliseconds $StepDelayMs
        }
    } finally {
        [DesktopAutomationNative]::mouse_event($script:DesktopAutomationConstants.MouseLeftUp, 0, 0, 0, [UIntPtr]::Zero)
    }
}

function ConvertTo-SendKeysLiteral {
    param(
        [string]$Value
    )

    $escaped = New-Object System.Text.StringBuilder
    foreach ($char in $Value.ToCharArray()) {
        switch ($char) {
            "`r" { continue }
            "`n" { [void]$escaped.Append("{ENTER}") }
            "+" { [void]$escaped.Append("{+}") }
            "^" { [void]$escaped.Append("{^}") }
            "%" { [void]$escaped.Append("{%}") }
            "~" { [void]$escaped.Append("{~}") }
            "(" { [void]$escaped.Append("{(}") }
            ")" { [void]$escaped.Append("{)}") }
            "[" { [void]$escaped.Append("{[}") }
            "]" { [void]$escaped.Append("{]}") }
            "{" { [void]$escaped.Append("{{}") }
            "}" { [void]$escaped.Append("{}}") }
            default { [void]$escaped.Append($char) }
        }
    }
    return $escaped.ToString()
}

function Send-DesktopLiteralText {
    param(
        [string]$Text,
        [int]$DelayMs = 120
    )

    [System.Windows.Forms.SendKeys]::SendWait((ConvertTo-SendKeysLiteral -Value $Text))
    Start-Sleep -Milliseconds $DelayMs
}

function Send-DesktopKeys {
    param(
        [string]$Keys,
        [int]$DelayMs = 120
    )

    [System.Windows.Forms.SendKeys]::SendWait($Keys)
    Start-Sleep -Milliseconds $DelayMs
}

function Get-DesktopUiRoot {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window
    )

    $root = [System.Windows.Automation.AutomationElement]::FromHandle([IntPtr]$Window.Handle)
    if ($null -eq $root) {
        throw "Could not create a UI Automation root for window '$($Window.Title)'."
    }
    return $root
}

function ConvertFrom-UiAutomationElement {
    param(
        [Parameter(Mandatory = $true)]
        [System.Windows.Automation.AutomationElement]$Element
    )

    try {
        $current = $Element.Current
        $rect = $current.BoundingRectangle
        $value = $null
        $pattern = $null
        if ($Element.TryGetCurrentPattern([System.Windows.Automation.ValuePattern]::Pattern, [ref]$pattern)) {
            try {
                $value = ([System.Windows.Automation.ValuePattern]$pattern).Current.Value
            } catch {
                $value = $null
            }
        }
        $left = [int][Math]::Round($rect.Left)
        $top = [int][Math]::Round($rect.Top)
        $right = [int][Math]::Round($rect.Right)
        $bottom = [int][Math]::Round($rect.Bottom)
        [pscustomobject]@{
            Element = $Element
            Name = $current.Name
            ControlType = ($current.ControlType.ProgrammaticName -replace "^ControlType\.", "")
            LocalizedControlType = $current.LocalizedControlType
            AutomationId = $current.AutomationId
            ClassName = $current.ClassName
            ProcessId = $current.ProcessId
            IsOffscreen = $current.IsOffscreen
            IsEnabled = $current.IsEnabled
            HasKeyboardFocus = $current.HasKeyboardFocus
            FrameworkId = $current.FrameworkId
            HelpText = $current.HelpText
            Value = $value
            Left = $left
            Top = $top
            Right = $right
            Bottom = $bottom
            Width = [Math]::Max(0, $right - $left)
            Height = [Math]::Max(0, $bottom - $top)
            CenterX = [int][Math]::Round(($left + $right) / 2)
            CenterY = [int][Math]::Round(($top + $bottom) / 2)
        }
    } catch {
        return $null
    }
}

function ConvertTo-DesktopUiSummary {
    param(
        [object]$InputObject
    )

    if ($null -eq $InputObject) {
        return $null
    }

    if ($InputObject -is [System.Array] -or $InputObject -is [System.Collections.IEnumerable] -and -not ($InputObject -is [string])) {
        $items = @()
        foreach ($item in $InputObject) {
            $items += ,(ConvertTo-DesktopUiSummary -InputObject $item)
        }
        return ,$items
    }

    if (@($InputObject.PSObject.Properties.Match("Element")).Count -eq 0) {
        return $InputObject
    }

    $summary = [ordered]@{}
    foreach ($property in $InputObject.PSObject.Properties) {
        if ($property.Name -ne "Element") {
            $summary[$property.Name] = $property.Value
        }
    }
    return [pscustomobject]$summary
}

function Find-DesktopUiElements {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [string]$Name = "",
        [string]$NameContains = "",
        [string]$NameRegex = "",
        [string]$AutomationId = "",
        [string]$ClassName = "",
        [string]$ControlType = "",
        [switch]$IncludeRoot,
        [switch]$IncludeOffscreen,
        [int]$MaxResults = 25
    )

    $root = Get-DesktopUiRoot -Window $Window
    $items = New-Object "System.Collections.Generic.List[object]"

    if ($IncludeRoot) {
        $rootItem = ConvertFrom-UiAutomationElement -Element $root
        if ($null -ne $rootItem) {
            $items.Add($rootItem) | Out-Null
        }
    }

    $all = $root.FindAll(
        [System.Windows.Automation.TreeScope]::Descendants,
        [System.Windows.Automation.Condition]::TrueCondition
    )
    foreach ($element in $all) {
        $item = ConvertFrom-UiAutomationElement -Element $element
        if ($null -ne $item) {
            $items.Add($item) | Out-Null
        }
    }

    $results = $items

    if (-not $IncludeOffscreen) {
        $results = $results | Where-Object { -not $_.IsOffscreen }
    }
    if (-not [string]::IsNullOrWhiteSpace($Name)) {
        $results = $results | Where-Object { $_.Name -eq $Name }
    }
    if (-not [string]::IsNullOrWhiteSpace($NameContains)) {
        $needle = $NameContains.ToLowerInvariant()
        $results = $results | Where-Object { ([string]$_.Name).ToLowerInvariant().Contains($needle) }
    }
    if (-not [string]::IsNullOrWhiteSpace($NameRegex)) {
        $results = $results | Where-Object { $_.Name -match $NameRegex }
    }
    if (-not [string]::IsNullOrWhiteSpace($AutomationId)) {
        $results = $results | Where-Object { $_.AutomationId -eq $AutomationId }
    }
    if (-not [string]::IsNullOrWhiteSpace($ClassName)) {
        $results = $results | Where-Object { $_.ClassName -eq $ClassName }
    }
    if (-not [string]::IsNullOrWhiteSpace($ControlType)) {
        $normalized = $ControlType -replace "^ControlType\.", ""
        $results = $results | Where-Object { $_.ControlType -eq $normalized }
    }

    return @($results | Select-Object -First $MaxResults)
}

function Get-DesktopUiElement {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [string]$Name = "",
        [string]$NameContains = "",
        [string]$NameRegex = "",
        [string]$AutomationId = "",
        [string]$ClassName = "",
        [string]$ControlType = "",
        [int]$Index = 0,
        [switch]$IncludeOffscreen
    )

    $matches = Find-DesktopUiElements `
        -Window $Window `
        -Name $Name `
        -NameContains $NameContains `
        -NameRegex $NameRegex `
        -AutomationId $AutomationId `
        -ClassName $ClassName `
        -ControlType $ControlType `
        -IncludeOffscreen:$IncludeOffscreen

    $matches = @($matches)
    if (@($matches).Count -eq 0) {
        throw "No UI element matched the provided selector in window '$($Window.Title)'."
    }
    if ($Index -lt 0 -or $Index -ge @($matches).Count) {
        throw "Element selector matched $(@($matches).Count) element(s), but Index $Index is out of range."
    }
    return @($matches)[$Index]
}

function Invoke-DesktopUiAction {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$ElementInfo,
        [ValidateSet("click", "invoke", "setvalue")]
        [string]$Action,
        [string]$Value = "",
        [int]$DelayMs = 180
    )

    $element = $ElementInfo.Element
    switch ($Action) {
        "click" {
            Move-DesktopCursor -X $ElementInfo.CenterX -Y $ElementInfo.CenterY -DelayMs 60 | Out-Null
            Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs $DelayMs
        }

        "invoke" {
            $invoked = $false
            $pattern = $null
            if ($element.TryGetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern, [ref]$pattern)) {
                ([System.Windows.Automation.InvokePattern]$pattern).Invoke()
                $invoked = $true
            } else {
                $pattern = $null
            }
            if (-not $invoked -and $element.TryGetCurrentPattern([System.Windows.Automation.SelectionItemPattern]::Pattern, [ref]$pattern)) {
                ([System.Windows.Automation.SelectionItemPattern]$pattern).Select()
                $invoked = $true
            } else {
                $pattern = $null
            }
            if (-not $invoked -and $element.TryGetCurrentPattern([System.Windows.Automation.ExpandCollapsePattern]::Pattern, [ref]$pattern)) {
                ([System.Windows.Automation.ExpandCollapsePattern]$pattern).Expand()
                $invoked = $true
            }

            if (-not $invoked) {
                Move-DesktopCursor -X $ElementInfo.CenterX -Y $ElementInfo.CenterY -DelayMs 60 | Out-Null
                Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs $DelayMs
            } else {
                Start-Sleep -Milliseconds $DelayMs
            }
        }

        "setvalue" {
            if ([string]::IsNullOrEmpty($Value)) {
                throw "Value is required for setvalue actions."
            }
            $pattern = $null
            if ($element.TryGetCurrentPattern([System.Windows.Automation.ValuePattern]::Pattern, [ref]$pattern)) {
                $valuePattern = [System.Windows.Automation.ValuePattern]$pattern
                if ($valuePattern.Current.IsReadOnly) {
                    throw "The target element is read-only."
                }
                $valuePattern.SetValue($Value)
                Start-Sleep -Milliseconds $DelayMs
            } else {
                Move-DesktopCursor -X $ElementInfo.CenterX -Y $ElementInfo.CenterY -DelayMs 60 | Out-Null
                Invoke-DesktopMouseClick -Button left -Count 1 -DelayMs 80
                Send-DesktopKeys -Keys "^a" -DelayMs 60
                Send-DesktopLiteralText -Text $Value -DelayMs $DelayMs
            }
        }
    }
}

function Test-DesktopWindowForeground {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [pscustomobject]$ForegroundWindow = $null
    )

    if ($null -eq $ForegroundWindow) {
        $ForegroundWindow = Get-DesktopForegroundWindow
    }

    return $null -ne $ForegroundWindow -and $ForegroundWindow.Handle -eq $Window.Handle
}

function Ensure-DesktopWindowForeground {
    param(
        [Parameter(Mandatory = $true)]
        [pscustomobject]$Window,
        [int]$RetryCount = 2,
        [int]$DelayMs = 180,
        [int]$RetryDelayMs = 160
    )

    $history = New-Object "System.Collections.Generic.List[object]"
    $foreground = $null

    for ($attempt = 0; $attempt -le $RetryCount; $attempt++) {
        Focus-DesktopWindow -Window $Window -DelayMs $DelayMs | Out-Null
        $foreground = Get-DesktopForegroundWindow
        $matched = Test-DesktopWindowForeground -Window $Window -ForegroundWindow $foreground
        $history.Add([pscustomobject]@{
            Attempt = $attempt + 1
            Matched = $matched
            ForegroundWindow = $foreground
        }) | Out-Null

        if ($matched) {
            return [pscustomobject]@{
                Success = $true
                Attempts = $attempt + 1
                ForegroundWindow = $foreground
                History = $history.ToArray()
            }
        }

        Start-Sleep -Milliseconds $RetryDelayMs
    }

    return [pscustomobject]@{
        Success = $false
        Attempts = $RetryCount + 1
        ForegroundWindow = $foreground
        History = $history.ToArray()
    }
}

function Get-DesktopTesseractPath {
    $candidates = @()

    if (-not [string]::IsNullOrWhiteSpace($env:TESSERACT_EXE)) {
        $candidates += $env:TESSERACT_EXE
    }

    $candidates += @(
        "C:\Program Files\Tesseract-OCR\tesseract.exe",
        "C:\Program Files (x86)\Tesseract-OCR\tesseract.exe"
    )

    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    $command = Get-Command tesseract -ErrorAction SilentlyContinue
    if ($null -ne $command -and -not [string]::IsNullOrWhiteSpace($command.Source)) {
        return $command.Source
    }

    throw "Tesseract OCR is not available. Set TESSERACT_EXE or install Tesseract."
}

function Get-DesktopStringDistance {
    param(
        [string]$Left,
        [string]$Right
    )

    $a = if ($null -eq $Left) { "" } else { $Left }
    $b = if ($null -eq $Right) { "" } else { $Right }
    $rows = $a.Length + 1
    $cols = $b.Length + 1
    $dist = New-Object 'int[,]' $rows, $cols

    for ($i = 0; $i -lt $rows; $i++) {
        $dist[$i, 0] = $i
    }
    for ($j = 0; $j -lt $cols; $j++) {
        $dist[0, $j] = $j
    }

    for ($i = 1; $i -lt $rows; $i++) {
        for ($j = 1; $j -lt $cols; $j++) {
            $cost = if ($a[$i - 1] -eq $b[$j - 1]) { 0 } else { 1 }
            $deletion = $dist[$i - 1, $j] + 1
            $insertion = $dist[$i, $j - 1] + 1
            $substitution = $dist[$i - 1, $j - 1] + $cost
            $dist[$i, $j] = [Math]::Min([Math]::Min($deletion, $insertion), $substitution)
        }
    }

    return $dist[$rows - 1, $cols - 1]
}

function Save-DesktopAnnotatedImage {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ImagePath,
        [Parameter(Mandatory = $true)]
        [object[]]$Items,
        [Parameter(Mandatory = $true)]
        [string]$OutputPath,
        [string]$LabelProperty = "Text",
        [int]$MaxLabels = 40
    )

    $directory = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }

    $bitmap = [System.Drawing.Bitmap]::FromFile($ImagePath)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 227, 57, 53)), 3
    $font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $backgroundBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(190, 33, 33, 33))

    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $labelCount = 0
        foreach ($item in $Items) {
            if ($null -eq $item) {
                continue
            }

            $left = [int]$item.Left
            $top = [int]$item.Top
            $width = [int]$item.Width
            $height = [int]$item.Height
            if ($width -le 0 -or $height -le 0) {
                continue
            }

            $graphics.DrawRectangle($pen, $left, $top, $width, $height)
            if ($labelCount -ge $MaxLabels) {
                continue
            }

            $label = ""
            if (@($item.PSObject.Properties.Match($LabelProperty)).Count -gt 0) {
                $label = [string]$item.$LabelProperty
            }
            if ([string]::IsNullOrWhiteSpace($label) -and @($item.PSObject.Properties.Match("Kind")).Count -gt 0) {
                $label = [string]$item.Kind
            }
            if ([string]::IsNullOrWhiteSpace($label)) {
                continue
            }

            $size = $graphics.MeasureString($label, $font)
            $labelRect = [System.Drawing.RectangleF]::new(
                [single]$left,
                [single][Math]::Max(0, $top - $size.Height - 4),
                [single]($size.Width + 8),
                [single]($size.Height + 4)
            )
            $graphics.FillRectangle($backgroundBrush, $labelRect)
            $graphics.DrawString($label, $font, $textBrush, $labelRect.Left + 4, $labelRect.Top + 2)
            $labelCount++
        }

        $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $backgroundBrush.Dispose()
        $textBrush.Dispose()
        $font.Dispose()
        $pen.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }

    return $OutputPath
}

function Invoke-DesktopTesseractOcr {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ImagePath,
        [string]$Language = "eng",
        [int]$Psm = 6,
        [double]$MinConfidence = 0,
        [string]$ArtifactsBasePath = ""
    )

    if (-not (Test-Path $ImagePath)) {
        throw "OCR image path '$ImagePath' does not exist."
    }

    $tesseractPath = Get-DesktopTesseractPath
    $tsvOutput = & $tesseractPath $ImagePath stdout --psm $Psm -l $Language tsv 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Tesseract OCR failed for '$ImagePath'."
    }

    $tsvText = ($tsvOutput | ForEach-Object { [string]$_ }) -join [Environment]::NewLine
    if ([string]::IsNullOrWhiteSpace($tsvText)) {
        throw "Tesseract OCR produced no output for '$ImagePath'."
    }

    if (-not [string]::IsNullOrWhiteSpace($ArtifactsBasePath)) {
        $tsvPath = "$ArtifactsBasePath.tsv"
        Set-Content -Path $tsvPath -Value $tsvText -Encoding UTF8
    } else {
        $tsvPath = $null
    }

    $rows = $tsvText | ConvertFrom-Csv -Delimiter "`t"
    $words = New-Object "System.Collections.Generic.List[object]"
    foreach ($row in $rows) {
        $text = ([string]$row.text).Trim()
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }

        $confidence = 0.0
        [double]::TryParse([string]$row.conf, [ref]$confidence) | Out-Null
        if ($confidence -lt $MinConfidence) {
            continue
        }

        $left = [int]$row.left
        $top = [int]$row.top
        $width = [int]$row.width
        $height = [int]$row.height
        $words.Add([pscustomobject]@{
            Kind = "Word"
            Page = [int]$row.page_num
            Block = [int]$row.block_num
            Paragraph = [int]$row.par_num
            Line = [int]$row.line_num
            Word = [int]$row.word_num
            Text = $text
            Confidence = [Math]::Round($confidence, 2)
            Left = $left
            Top = $top
            Right = $left + $width
            Bottom = $top + $height
            Width = $width
            Height = $height
            CenterX = [int][Math]::Round($left + ($width / 2.0))
            CenterY = [int][Math]::Round($top + ($height / 2.0))
        }) | Out-Null
    }

    $lines = New-Object "System.Collections.Generic.List[object]"
    $groupedWords = $words | Group-Object Page, Block, Paragraph, Line
    foreach ($group in $groupedWords) {
        $lineWords = @($group.Group | Sort-Object Word)
        if ($lineWords.Count -eq 0) {
            continue
        }

        $text = (($lineWords | ForEach-Object { $_.Text }) -join " ").Trim()
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }

        $left = ($lineWords | Measure-Object -Property Left -Minimum).Minimum
        $top = ($lineWords | Measure-Object -Property Top -Minimum).Minimum
        $right = ($lineWords | Measure-Object -Property Right -Maximum).Maximum
        $bottom = ($lineWords | Measure-Object -Property Bottom -Maximum).Maximum
        $averageConfidence = ($lineWords | Measure-Object -Property Confidence -Average).Average
        $first = $lineWords[0]

        $lines.Add([pscustomobject]@{
            Kind = "Line"
            Page = $first.Page
            Block = $first.Block
            Paragraph = $first.Paragraph
            Line = $first.Line
            Word = 0
            Text = $text
            Confidence = [Math]::Round($averageConfidence, 2)
            Left = [int]$left
            Top = [int]$top
            Right = [int]$right
            Bottom = [int]$bottom
            Width = [int]($right - $left)
            Height = [int]($bottom - $top)
            CenterX = [int][Math]::Round(($left + $right) / 2.0)
            CenterY = [int][Math]::Round(($top + $bottom) / 2.0)
            Words = $lineWords
        }) | Out-Null
    }

    $entries = @($lines.ToArray() + $words.ToArray())
    return [pscustomobject]@{
        Engine = "tesseract"
        ImagePath = $ImagePath
        Language = $Language
        Psm = $Psm
        TsvPath = $tsvPath
        Words = $words.ToArray()
        Lines = $lines.ToArray()
        Entries = $entries
    }
}

function Find-DesktopOcrText {
    param(
        [Parameter(Mandatory = $true)]
        [object[]]$Entries,
        [Parameter(Mandatory = $true)]
        [string]$Text,
        [ValidateSet("exact", "contains", "regex", "fuzzy")]
        [string]$Match = "contains",
        [ValidateSet("any", "line", "word")]
        [string]$Kind = "line",
        [double]$MinScore = 0.55,
        [int]$MaxResults = 25
    )

    $needle = $Text.Trim()
    if ([string]::IsNullOrWhiteSpace($needle)) {
        throw "Text is required for OCR text matching."
    }

    $pool = switch ($Kind) {
        "word" { $Entries | Where-Object { $_.Kind -eq "Word" } }
        "line" { $Entries | Where-Object { $_.Kind -eq "Line" } }
        default { $Entries }
    }

    $results = New-Object "System.Collections.Generic.List[object]"
    foreach ($entry in $pool) {
        $candidate = ([string]$entry.Text).Trim()
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }

        $isMatch = $false
        $score = 0.0
        switch ($Match) {
            "exact" {
                $isMatch = $candidate -eq $needle
                $score = if ($isMatch) { 1.0 } else { 0.0 }
            }

            "contains" {
                $isMatch = $candidate.ToLowerInvariant().Contains($needle.ToLowerInvariant())
                $score = if ($isMatch) {
                    [Math]::Max(0.7, [Math]::Min(1.0, $needle.Length / [double][Math]::Max($candidate.Length, 1)))
                } else {
                    0.0
                }
            }

            "regex" {
                $isMatch = $candidate -match $needle
                $score = if ($isMatch) { 0.9 } else { 0.0 }
            }

            "fuzzy" {
                $distance = Get-DesktopStringDistance -Left $candidate.ToLowerInvariant() -Right $needle.ToLowerInvariant()
                $maxLength = [Math]::Max($candidate.Length, $needle.Length)
                $score = if ($maxLength -eq 0) { 1.0 } else { 1.0 - ($distance / [double]$maxLength) }
                $isMatch = $score -ge $MinScore
            }
        }

        if (-not $isMatch) {
            continue
        }

        $results.Add([pscustomobject]@{
            Kind = $entry.Kind
            Text = $entry.Text
            Confidence = $entry.Confidence
            Score = [Math]::Round($score, 3)
            Left = $entry.Left
            Top = $entry.Top
            Right = $entry.Right
            Bottom = $entry.Bottom
            Width = $entry.Width
            Height = $entry.Height
            CenterX = $entry.CenterX
            CenterY = $entry.CenterY
            Source = $entry
        }) | Out-Null
    }

    return @(
        $results.ToArray() |
            Sort-Object @{ Expression = "Score"; Descending = $true }, @{ Expression = "Confidence"; Descending = $true }, Top, Left |
            Select-Object -First $MaxResults
    )
}

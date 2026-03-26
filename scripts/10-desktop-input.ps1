param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("position", "move", "click", "doubleclick", "rightclick", "middleclick", "scroll", "type", "keys")]
    [string]$Action,
    [int]$X,
    [int]$Y,
    [int]$Delta = 120,
    [int]$DelayMs = 120,
    [string]$Text = "",
    [string]$Keys = ""
)

<#
.SYNOPSIS
    Performs basic desktop mouse and keyboard actions on Windows.

.DESCRIPTION
    Uses native user32 calls for pointer actions and SendKeys for keyboard
    entry. Coordinates are screen coordinates in pixels.
#>

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms
Add-Type @'
using System;
using System.Runtime.InteropServices;

public static class DesktopInputNative {
    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int X, int Y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, UIntPtr dwExtraInfo);
}
'@

$MOUSEEVENTF_LEFTDOWN = 0x0002
$MOUSEEVENTF_LEFTUP = 0x0004
$MOUSEEVENTF_RIGHTDOWN = 0x0008
$MOUSEEVENTF_RIGHTUP = 0x0010
$MOUSEEVENTF_MIDDLEDOWN = 0x0020
$MOUSEEVENTF_MIDDLEUP = 0x0040
$MOUSEEVENTF_WHEEL = 0x0800

function Get-CursorPosition {
    return [System.Windows.Forms.Cursor]::Position
}

function Move-Cursor {
    param(
        [int]$TargetX,
        [int]$TargetY
    )
    [DesktopInputNative]::SetCursorPos($TargetX, $TargetY) | Out-Null
    Start-Sleep -Milliseconds $DelayMs
}

function Invoke-MouseClick {
    param(
        [uint32]$DownFlag,
        [uint32]$UpFlag,
        [int]$Count = 1
    )
    foreach ($i in 1..$Count) {
        [DesktopInputNative]::mouse_event($DownFlag, 0, 0, 0, [UIntPtr]::Zero)
        [DesktopInputNative]::mouse_event($UpFlag, 0, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds ([Math]::Max(80, $DelayMs))
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

switch ($Action) {
    "position" {
        $cursor = Get-CursorPosition
        Write-Output "x=$($cursor.X) y=$($cursor.Y)"
    }

    "move" {
        Move-Cursor -TargetX $X -TargetY $Y
        $cursor = Get-CursorPosition
        Write-Output "x=$($cursor.X) y=$($cursor.Y)"
    }

    "click" {
        Move-Cursor -TargetX $X -TargetY $Y
        Invoke-MouseClick -DownFlag $MOUSEEVENTF_LEFTDOWN -UpFlag $MOUSEEVENTF_LEFTUP
        $cursor = Get-CursorPosition
        Write-Output "clicked=left x=$($cursor.X) y=$($cursor.Y)"
    }

    "doubleclick" {
        Move-Cursor -TargetX $X -TargetY $Y
        Invoke-MouseClick -DownFlag $MOUSEEVENTF_LEFTDOWN -UpFlag $MOUSEEVENTF_LEFTUP -Count 2
        $cursor = Get-CursorPosition
        Write-Output "clicked=left-double x=$($cursor.X) y=$($cursor.Y)"
    }

    "rightclick" {
        Move-Cursor -TargetX $X -TargetY $Y
        Invoke-MouseClick -DownFlag $MOUSEEVENTF_RIGHTDOWN -UpFlag $MOUSEEVENTF_RIGHTUP
        $cursor = Get-CursorPosition
        Write-Output "clicked=right x=$($cursor.X) y=$($cursor.Y)"
    }

    "middleclick" {
        Move-Cursor -TargetX $X -TargetY $Y
        Invoke-MouseClick -DownFlag $MOUSEEVENTF_MIDDLEDOWN -UpFlag $MOUSEEVENTF_MIDDLEUP
        $cursor = Get-CursorPosition
        Write-Output "clicked=middle x=$($cursor.X) y=$($cursor.Y)"
    }

    "scroll" {
        if ($PSBoundParameters.ContainsKey("X") -and $PSBoundParameters.ContainsKey("Y")) {
            Move-Cursor -TargetX $X -TargetY $Y
        }
        [DesktopInputNative]::mouse_event($MOUSEEVENTF_WHEEL, 0, 0, [uint32]$Delta, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds $DelayMs
        $cursor = Get-CursorPosition
        Write-Output "scrolled=$Delta x=$($cursor.X) y=$($cursor.Y)"
    }

    "type" {
        if ([string]::IsNullOrEmpty($Text)) {
            throw "Provide -Text when Action is 'type'."
        }
        [System.Windows.Forms.SendKeys]::SendWait((ConvertTo-SendKeysLiteral -Value $Text))
        Start-Sleep -Milliseconds $DelayMs
        Write-Output "typed"
    }

    "keys" {
        if ([string]::IsNullOrEmpty($Keys)) {
            throw "Provide -Keys when Action is 'keys'."
        }
        [System.Windows.Forms.SendKeys]::SendWait($Keys)
        Start-Sleep -Milliseconds $DelayMs
        Write-Output "keys-sent"
    }
}

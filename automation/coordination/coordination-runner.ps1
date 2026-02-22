param(
    [int]$DurationMinutes = 185,
    [int]$CycleSeconds = 300
)

$ErrorActionPreference = "Continue"

$repoRoot = "C:\Users\grand\tool"
$coordScripts = "C:\Users\grand\.codex\skills\local\codex-agent-orchestration\scripts"
$channel = "tool-mission-chat"
$controller = "MAIN-A"
$target = "MAIN-B"
$leaseId = "LEASE-2026-02-20-B"
$taskId = "DEPLOY-COORD-LONGRUN"
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$aabPath = Join-Path $repoRoot "app\build\outputs\bundle\release\app-release.aab"
$logDir = Join-Path $repoRoot "automation\logs"
$logPath = Join-Path $logDir ("coordination-runner-" + (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss") + ".log")

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Write-Log {
    param([string]$Message)
    $line = ("[{0}] {1}" -f (Get-Date).ToUniversalTime().ToString("o"), $Message)
    $line | Tee-Object -FilePath $logPath -Append
}

function Send-ChannelStatus {
    param(
        [string]$Type,
        [string]$Body,
        [string]$Priority = "high"
    )
    $sendScript = Join-Path $coordScripts "send-message.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $sendScript `
        -Channel $channel `
        -From $controller `
        -To $target `
        -Type $Type `
        -LeaseId $leaseId `
        -TaskId $taskId `
        -Priority $Priority `
        -Body $Body | Out-Null
}

function Renew-Lease {
    $claimScript = Join-Path $coordScripts "claim-lease.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $claimScript `
        -Channel $channel `
        -Controller $controller `
        -LeaseId $leaseId `
        -TtlSeconds 1200 `
        -Force | Out-Null
}

function Get-DeviceSerial {
    $lines = @()
    try {
        $lines = (& adb devices) | Select-Object -Skip 1
    } catch {
        return $null
    }

    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($line -match "^\s*(\S+)\s+device\s*$") {
            return $matches[1]
        }
    }
    return $null
}

function Ensure-AppInstalled {
    param([string]$Serial)
    if ([string]::IsNullOrWhiteSpace($Serial)) {
        return "no-device"
    }
    $installed = $false
    try {
        $pkgOut = & adb -s $Serial shell pm list packages com.tradesketch.estimator
        if ($pkgOut -match "package:com\.tradesketch\.estimator") {
            $installed = $true
        }
    } catch {
        $installed = $false
    }

    if ($installed) {
        return "installed"
    }

    if (-not (Test-Path $apkPath)) {
        return "apk-missing"
    }

    try {
        $installOut = & adb -s $Serial install -r $apkPath 2>&1
        if ($LASTEXITCODE -eq 0 -and ($installOut -join " ") -match "Success") {
            return "installed-now"
        }
        return ("install-failed: " + (($installOut -join " ") -replace "\s+", " ").Trim())
    } catch {
        return ("install-exception: " + $_.Exception.Message)
    }
}

function Run-ProcessWithTimeout {
    param(
        [string]$FilePath,
        [string[]]$Args,
        [int]$TimeoutSeconds = 600
    )

    $tmpOut = Join-Path $env:TEMP ("coord-runner-" + [Guid]::NewGuid().ToString() + ".log")
    $proc = Start-Process -FilePath $FilePath `
        -ArgumentList $Args `
        -PassThru `
        -NoNewWindow `
        -RedirectStandardOutput $tmpOut `
        -RedirectStandardError $tmpOut

    $timedOut = $false
    try {
        Wait-Process -Id $proc.Id -Timeout $TimeoutSeconds -ErrorAction Stop
    } catch {
        $timedOut = $true
    }

    if ($timedOut) {
        try { $proc.Kill($true) } catch {}
    }
    $proc.Refresh()

    $out = @()
    if (Test-Path $tmpOut) {
        $out = Get-Content $tmpOut -ErrorAction SilentlyContinue
        Remove-Item $tmpOut -Force -ErrorAction SilentlyContinue
    }

    return [pscustomobject]@{
        exited = (-not $timedOut)
        exit_code = if (-not $timedOut) { $proc.ExitCode } else { -1 }
        output = $out
    }
}

function Run-HealthCompile {
    $result = Run-ProcessWithTimeout `
        -FilePath "cmd.exe" `
        -Args @("/c", "cd /d C:\Users\grand\tool && gradlew :app:compileDebugKotlin --no-daemon --max-workers=1") `
        -TimeoutSeconds 720
    $out = $result.output
    $ok = $result.exited -and ($result.exit_code -eq 0)
    $preview = (($out | Select-Object -Last 6) -join " | ") -replace "\s+", " "
    if (-not $result.exited) {
        $preview = "compile-timeout-after-720s"
    }
    if ([string]::IsNullOrWhiteSpace($preview)) {
        $preview = if ($ok) { "compile-ok" } else { "compile-failed-no-output" }
    }
    return [pscustomobject]@{
        ok = $ok
        summary = $preview
    }
}

Write-Log ("Runner started. duration_min={0} cycle_seconds={1}" -f $DurationMinutes, $CycleSeconds)
Send-ChannelStatus -Type "task" -Body "Long-run coordinator online for ~3 hours. Delegating MAIN-B to validation/read-only and posting periodic status." -Priority "high"

$sendTaskScript = Join-Path $coordScripts "send-task.ps1"
& pwsh -NoProfile -ExecutionPolicy Bypass -File $sendTaskScript `
    -Channel $channel `
    -From $controller `
    -To $target `
    -LeaseId $leaseId `
    -TaskId $taskId `
    -Objective "Run read-only validation + channel monitoring while MAIN-A runs 3-hour coordination loop." `
    -Ownership validation `
    -ScopeIn gradle_validation,channel_reporting `
    -ScopeOut code_edits,deployment `
    -DefinitionOfDone "Send status updates with command outcomes and blockers." `
    -Evidence "channel message ids + summaries" `
    -NextAction "post update each major milestone" `
    -Eta "3h" `
    -Priority high | Out-Null

$start = Get-Date
$end = $start.AddMinutes($DurationMinutes)
$cycle = 0

while ((Get-Date) -lt $end) {
    $cycle += 1
    Renew-Lease

    & adb start-server | Out-Null
    $serial = Get-DeviceSerial
    $deviceState = if ($serial) { "connected:$serial" } else { "disconnected" }
    $installState = Ensure-AppInstalled -Serial $serial

    $bundleState = if (Test-Path $aabPath) {
        $file = Get-Item $aabPath
        ("present:{0}bytes" -f $file.Length)
    } else {
        "missing"
    }

    $compileState = "delegated-main-b"

    $minsLeft = [int][Math]::Ceiling((New-TimeSpan -Start (Get-Date) -End $end).TotalMinutes)
    $status = ("Cycle={0}; device={1}; app={2}; aab={3}; compile={4}; mins_left={5}" -f $cycle, $deviceState, $installState, $bundleState, $compileState, $minsLeft)
    Write-Log $status
    Send-ChannelStatus -Type "status" -Body $status -Priority "high"

    Start-Sleep -Seconds $CycleSeconds
}

Send-ChannelStatus -Type "closeout" -Body "3-hour coordination loop complete. Lease maintained, delegated monitoring run, device/app checks posted each cycle." -Priority "high"
Write-Log "Runner complete."

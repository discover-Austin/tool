param(
    [string]$Channel = "tool-mission-chat",
    [string]$Controller = "MAIN-A",
    [string]$Peer = "MAIN-B",
    [string]$LeaseId = "LEASE-2026-02-20-C",
    [string]$GateId = "CONSENSUS-GATE-1",
    [int]$AgreeSlaSeconds = 35,
    [int]$EscalateSeconds = 70,
    [int]$PollSeconds = 4,
    [int]$MaxRunMinutes = 240,
    [switch]$ResetState
)

$ErrorActionPreference = "Continue"

$repoRoot = "C:\Users\grand\tool"
$coordScripts = "C:\Users\grand\.codex\skills\local\codex-agent-orchestration\scripts"
$statePath = Join-Path $repoRoot "automation\coordination\repo-audit-130-latency-guard-state.json"
$runnerStatePath = Join-Path $repoRoot "automation\coordination\repo-audit-130-state.json"
$logDir = Join-Path $repoRoot "automation\logs"
$logPath = Join-Path $logDir ("repo-audit-130-latency-guard-" + (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss") + ".log")

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date).ToUniversalTime().ToString("o"), $Message
    $line | Tee-Object -FilePath $logPath -Append
}

function New-GuardState {
    return [ordered]@{
        schema_version = 1
        runner = "repo-audit-130-latency-guard"
        channel = $Channel
        lease_id = $LeaseId
        gate_id = $GateId
        agree_sla_seconds = $AgreeSlaSeconds
        escalate_seconds = $EscalateSeconds
        poll_seconds = $PollSeconds
        started_at_utc = (Get-Date).ToUniversalTime().ToString("o")
        updated_at_utc = (Get-Date).ToUniversalTime().ToString("o")
        current_task_id = ""
        current_task_seen_utc = ""
        current_task_age_seconds = 0
        reminder_sent = $false
        reminder_message = ""
        escalation_sent = $false
        escalation_message = ""
        peer_agree_id = ""
        guard_status = "idle"
        log_path = $logPath
    }
}

function Load-GuardState {
    if (-not (Test-Path $statePath)) {
        return (New-GuardState)
    }
    try {
        $raw = Get-Content -Raw -Path $statePath
        $obj = $raw | ConvertFrom-Json -AsHashtable
        if (-not ($obj -is [System.Collections.IDictionary])) {
            return (New-GuardState)
        }
        if (-not $obj["current_task_id"]) { $obj["current_task_id"] = "" }
        if (-not $obj["current_task_seen_utc"]) { $obj["current_task_seen_utc"] = "" }
        if (-not $obj["guard_status"]) { $obj["guard_status"] = "idle" }
        return $obj
    } catch {
        return (New-GuardState)
    }
}

function Save-GuardState {
    param($State)
    $State["updated_at_utc"] = (Get-Date).ToUniversalTime().ToString("o")
    $State["log_path"] = $logPath
    $State | ConvertTo-Json -Depth 8 | Set-Content -Path $statePath
}

function Read-ChannelMessages {
    $readScript = Join-Path $coordScripts "read-messages.ps1"
    $raw = & pwsh -NoProfile -ExecutionPolicy Bypass -File $readScript `
        -Channel $Channel `
        -Participant $Controller `
        -Limit 800 `
        -IncludeSent
    try {
        $msgs = $raw | ConvertFrom-Json
        if ($null -eq $msgs) { return @() }
        if ($msgs -is [System.Array]) { return $msgs }
        return @($msgs)
    } catch {
        return @()
    }
}

function Send-Reminder {
    param([string]$TaskId, [int]$AgeSeconds)
    $sendScript = Join-Path $coordScripts "send-message.ps1"
    $body = "LATENCY-GUARD REMINDER: please send AGREE $TaskId now to avoid timeout (age=${AgeSeconds}s)."
    return (& pwsh -NoProfile -ExecutionPolicy Bypass -File $sendScript `
        -Channel $Channel `
        -From $Controller `
        -To $Peer `
        -Type status `
        -LeaseId $LeaseId `
        -TaskId $TaskId `
        -Priority critical `
        -Body $body)
}

function Send-ReissueTask {
    param([string]$TaskId)
    $taskScript = Join-Path $coordScripts "send-task.ps1"
    $objective = "Reminder reissue: send AGREE $TaskId immediately so the cycle can proceed under mutual gate."
    return (& pwsh -NoProfile -ExecutionPolicy Bypass -File $taskScript `
        -Channel $Channel `
        -From $Controller `
        -To $Peer `
        -LeaseId $LeaseId `
        -TaskId $TaskId `
        -Objective $objective `
        -Ownership validation `
        -ScopeIn "cross-check,review,verification" `
        -ScopeOut "unapproved-edits" `
        -DefinitionOfDone "Send AGREE $TaskId immediately; send VERIFY $TaskId after review." `
        -Evidence "AGREE message id and VERIFY status" `
        -NextAction "AGREE $TaskId" `
        -Eta "immediate" `
        -Priority critical)
}

function Try-GetRunnerStatus {
    if (-not (Test-Path $runnerStatePath)) { return $null }
    try {
        $raw = Get-Content -Raw -Path $runnerStatePath
        return ($raw | ConvertFrom-Json)
    } catch {
        return $null
    }
}

function Check-GateOpen {
    $checkScript = Join-Path $coordScripts "check-gate.ps1"
    $raw = & pwsh -NoProfile -ExecutionPolicy Bypass -File $checkScript `
        -Channel $Channel `
        -GateId $GateId
    try {
        $result = $raw | ConvertFrom-Json
        return [bool]$result.gate_open
    } catch {
        return $false
    }
}

function Get-LatestCycleTask {
    param($Messages)
    return ($Messages | Where-Object {
        $_.from -eq $Controller -and
        $_.type -eq "task" -and
        [string]$_.task_id -match "^AUDIT-CYCLE-\d{3}$"
    } | Sort-Object timestamp_utc | Select-Object -Last 1)
}

function Get-PeerAgree {
    param($Messages, [string]$TaskId)
    return ($Messages | Where-Object {
        $_.from -eq $Peer -and
        $_.task_id -eq $TaskId -and (
            $_.type -eq "ack" -or
            ([string]$_.body -match ("AGREE\s+" + [regex]::Escape($TaskId)))
        )
    } | Sort-Object timestamp_utc | Select-Object -Last 1)
}

$state = if ($ResetState) { New-GuardState } else { Load-GuardState }
$state["agree_sla_seconds"] = $AgreeSlaSeconds
$state["escalate_seconds"] = $EscalateSeconds
$state["poll_seconds"] = $PollSeconds
$state["channel"] = $Channel
$state["lease_id"] = $LeaseId
$state["gate_id"] = $GateId
Save-GuardState -State $state
Write-Log ("Latency guard start channel={0} sla={1}s escalate={2}s poll={3}s max_run={4}m" -f $Channel, $AgreeSlaSeconds, $EscalateSeconds, $PollSeconds, $MaxRunMinutes)

$started = Get-Date
while ($true) {
    if (((Get-Date) - $started).TotalMinutes -ge $MaxRunMinutes) {
        Write-Log "Max runtime reached. Guard exiting."
        break
    }

    $gateOpen = Check-GateOpen
    if (-not $gateOpen) {
        if ($state["guard_status"] -ne "gate_closed") {
            $state["guard_status"] = "gate_closed"
            Save-GuardState -State $state
            Write-Log ("Gate closed ({0}); suppressing reminder/reissue traffic." -f $GateId)
        }
        Start-Sleep -Seconds $PollSeconds
        continue
    }
    if ($state["guard_status"] -eq "gate_closed") {
        $state["guard_status"] = "watching"
        Save-GuardState -State $state
        Write-Log ("Gate reopened ({0}); resuming reminder/reissue checks." -f $GateId)
    }

    $messages = Read-ChannelMessages
    if ($messages.Count -eq 0) {
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    $latestTask = Get-LatestCycleTask -Messages $messages
    if (-not $latestTask) {
        Start-Sleep -Seconds $PollSeconds
        continue
    }

    $taskId = [string]$latestTask.task_id
    $taskSeenUtc = [string]$latestTask.timestamp_utc
    $taskChanged = ($state["current_task_id"] -ne $taskId)

    if ($taskChanged) {
        $state["current_task_id"] = $taskId
        $state["current_task_seen_utc"] = $taskSeenUtc
        $state["current_task_age_seconds"] = 0
        $state["reminder_sent"] = $false
        $state["reminder_message"] = ""
        $state["escalation_sent"] = $false
        $state["escalation_message"] = ""
        $state["peer_agree_id"] = ""
        $state["guard_status"] = "watching"
        Save-GuardState -State $state
        Write-Log ("Watching task={0}" -f $taskId)
    }

    $firstSeen = [datetime]::Parse($state["current_task_seen_utc"])
    $ageSeconds = [int]([datetime]::UtcNow - $firstSeen).TotalSeconds
    $state["current_task_age_seconds"] = $ageSeconds

    $agree = Get-PeerAgree -Messages $messages -TaskId $taskId
    if ($agree) {
        $agreeId = [string]$agree.id
        if ($state["peer_agree_id"] -ne $agreeId) {
            $state["peer_agree_id"] = $agreeId
            $state["guard_status"] = "agreed"
            Save-GuardState -State $state
            Write-Log ("Peer AGREE observed task={0} id={1}" -f $taskId, $agreeId)
        }
    } else {
        if (($ageSeconds -ge $AgreeSlaSeconds) -and ($state["reminder_sent"] -ne $true)) {
            $state["reminder_sent"] = $true
            $state["reminder_message"] = [string](Send-Reminder -TaskId $taskId -AgeSeconds $ageSeconds)
            $state["guard_status"] = "reminded"
            Save-GuardState -State $state
            Write-Log ("Reminder sent task={0} age={1}s" -f $taskId, $ageSeconds)
        }

        if (($ageSeconds -ge $EscalateSeconds) -and ($state["escalation_sent"] -ne $true)) {
            $state["escalation_sent"] = $true
            $state["escalation_message"] = [string](Send-ReissueTask -TaskId $taskId)
            $state["guard_status"] = "escalated"
            Save-GuardState -State $state
            Write-Log ("Task reissued task={0} age={1}s" -f $taskId, $ageSeconds)
        }
    }

    $runner = Try-GetRunnerStatus
    if ($runner -and [string]$runner.status -eq "complete" -and [int]$runner.cycle -ge [int]$runner.cycles_total) {
        Write-Log ("Runner complete at cycle={0}. Guard exiting." -f [int]$runner.cycle)
        break
    }

    Start-Sleep -Seconds $PollSeconds
}

$state["guard_status"] = "stopped"
Save-GuardState -State $state
Write-Log "Latency guard complete."

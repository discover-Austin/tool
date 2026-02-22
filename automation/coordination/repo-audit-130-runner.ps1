param(
    [int]$Cycles = 130,
    [int]$AgreementTimeoutSeconds = 90,
    [int]$CyclePauseSeconds = 5,
    [int]$StartCycle = 1,
    [string]$LeaseId = "LEASE-2026-02-20-C",
    [string]$GateId = "CONSENSUS-GATE-1",
    [string]$Peer = "MAIN-B",
    [string[]]$PeerCandidates = @(),
    [string[]]$GateParticipants = @(),
    [switch]$SkipSetGate
)

$ErrorActionPreference = "Continue"

$repoRoot = "C:\Users\grand\tool"
$coordScripts = "C:\Users\grand\.codex\skills\local\codex-agent-orchestration\scripts"
$channel = "tool-mission-chat"
$controller = "MAIN-A"
$peer = $Peer
$leaseId = $LeaseId
$gateId = $GateId
$masterTaskId = "AUDIT-130-CYCLES"
$logDir = Join-Path $repoRoot "automation\logs"
$logPath = Join-Path $logDir ("repo-audit-130-" + (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss") + ".log")
$statePath = Join-Path $repoRoot "automation\coordination\repo-audit-130-state.json"
$peerList = if ($PeerCandidates -and $PeerCandidates.Count -gt 0) { @($PeerCandidates) } else { @($peer) }

New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date).ToUniversalTime().ToString("o"), $Message
    $line | Tee-Object -FilePath $logPath -Append
}

function Write-RunnerState {
    param(
        [int]$Cycle,
        [string]$Status,
        [bool]$GateOpen,
        [string]$Detail
    )
    $state = [ordered]@{
        schema_version = 1
        runner = "repo-audit-130"
        channel = $channel
        lease_id = $leaseId
        gate_id = $gateId
        cycle = $Cycle
        cycles_total = $Cycles
        gate_open = $GateOpen
        status = $Status
        detail = $Detail
        log_path = $logPath
        updated_at_utc = (Get-Date).ToUniversalTime().ToString("o")
    }
    $state | ConvertTo-Json -Depth 8 | Set-Content -Path $statePath
}

function Send-CoordMessage {
    param(
        [string]$Type,
        [string]$TaskId,
        [string]$Body,
        [string]$Priority = "high"
    )
    $sendScript = Join-Path $coordScripts "send-message.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $sendScript `
        -Channel $channel `
        -From $controller `
        -To $peer `
        -Type $Type `
        -LeaseId $leaseId `
        -TaskId $TaskId `
        -Priority $Priority `
        -Body $Body | Out-Null
}

function Send-CoordTask {
    param(
        [string]$TaskId,
        [string]$Objective,
        [string]$BatchSummary
    )
    $taskScript = Join-Path $coordScripts "send-task.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $taskScript `
        -Channel $channel `
        -From $controller `
        -To $peer `
        -LeaseId $leaseId `
        -TaskId $TaskId `
        -Objective $Objective `
        -Ownership validation `
        -ScopeIn "cross-check,review,verification" `
        -ScopeOut "unapproved-edits" `
        -DefinitionOfDone "Send AGREE $TaskId before work; send VERIFY $TaskId after review." `
        -Evidence "status message with findings" `
        -NextAction "AGREE $TaskId" `
        -Eta "immediate" `
        -Priority high | Out-Null

    Send-CoordMessage -Type "status" -TaskId $TaskId -Priority "high" -Body ("MAIN-A AGREE {0}. Batch: {1}" -f $TaskId, $BatchSummary)
}

function Renew-Lease {
    $claimScript = Join-Path $coordScripts "claim-lease.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $claimScript `
        -Channel $channel `
        -Controller $controller `
        -LeaseId $leaseId `
        -TtlSeconds 1800 `
        -Force | Out-Null
}

function Check-GateOpen {
    $checkScript = Join-Path $coordScripts "check-gate.ps1"
    $resultRaw = & pwsh -NoProfile -ExecutionPolicy Bypass -File $checkScript `
        -Channel $channel `
        -GateId $gateId
    try {
        $result = $resultRaw | ConvertFrom-Json
        return [bool]$result.gate_open
    } catch {
        return $false
    }
}

function Read-RecentMessages {
    $readScript = Join-Path $coordScripts "read-messages.ps1"
    $raw = & pwsh -NoProfile -ExecutionPolicy Bypass -File $readScript `
        -Channel $channel `
        -Participant $controller `
        -Limit 120
    try {
        return ($raw | ConvertFrom-Json)
    } catch {
        return @()
    }
}

function Wait-ForPeerAgree {
    param(
        [string]$TaskId,
        [int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $msgs = Read-RecentMessages
        $agree = $msgs | Where-Object {
            $bodyAgrees = ([string]$_.body -match ("AGREE\s+" + [regex]::Escape($TaskId)))
            $typeIsAck = ([string]$_.type -eq "ack")
            ($_.task_id -eq $TaskId -and ($peerList -contains [string]$_.from) -and ($bodyAgrees -or $typeIsAck))
        } | Select-Object -Last 1
        if ($agree) {
            return [string]$agree.id
        }
        Start-Sleep -Seconds 4
    }
    return $null
}

function Select-Batch {
    param(
        [string[]]$Files,
        [int]$CycleIndex,
        [int]$BatchSize
    )
    if ($Files.Count -eq 0) { return @() }
    $start = (($CycleIndex - 1) * $BatchSize) % $Files.Count
    $batch = @()
    for ($i = 0; $i -lt $BatchSize; $i++) {
        $batch += $Files[($start + $i) % $Files.Count]
    }
    return $batch
}

function Normalize-FileText {
    param(
        [string]$FilePath,
        [string]$RelativePath
    )
    if (-not (Test-Path $FilePath)) { return $false }
    $original = Get-Content -Raw -Path $FilePath
    $updated = $original

    # Safe codemod: deprecated icon API replacement
    if ($RelativePath -like "*.kt") {
        $updated = $updated -replace "Icons\.Filled\.CallSplit", "Icons.AutoMirrored.Filled.CallSplit"
        $updated = $updated -replace "import androidx\.compose\.material\.icons\.filled\.CallSplit", "import androidx.compose.material.icons.automirrored.filled.CallSplit"
    }

    # Safe hygiene: trim trailing whitespace
    if ($RelativePath -match "\.(kt|kts|md|yml|yaml|xml|properties|txt)$") {
        $updated = [regex]::Replace($updated, "[ \t]+(?=\r?\n)", "")
    }

    if ($updated -ne $original) {
        Set-Content -Path $FilePath -Value $updated -NoNewline
        return $true
    }
    return $false
}

function Count-AuditMarkers {
    param([string[]]$Batch)
    if ($Batch.Count -eq 0) { return 0 }
    $quoted = $Batch | ForEach-Object { '"' + $_ + '"' }
    $cmd = "cd /d {0} && rg -n ""(TODO|FIXME|HACK|XXX)"" -- {1}" -f $repoRoot, ($quoted -join " ")
    $out = cmd /c $cmd 2>$null
    if (-not $out) { return 0 }
    return @($out).Count
}

Write-Log ("Runner start cycles={0} agreement_timeout_s={1}" -f $Cycles, $AgreementTimeoutSeconds)
Renew-Lease

$participantsForGate = if ($GateParticipants -and $GateParticipants.Count -gt 0) {
    @($GateParticipants)
} else {
    @($controller) + @($peerList)
}

if (-not $SkipSetGate) {
    $setGateScript = Join-Path $coordScripts "set-gate.ps1"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File $setGateScript `
        -Channel $channel `
        -GateId $gateId `
        -LeaseId $leaseId `
        -Participants $participantsForGate `
        -Rule "Both MAIN-A and MAIN-B must AGREE before any work." | Out-Null
} else {
    Write-Log "SkipSetGate enabled; preserving existing gate-state.json."
}

Send-CoordTask -TaskId $masterTaskId -Objective "Run 130 mutual-agreement audit cycles across the repo." -BatchSummary "entire-repo-rolling-batches"
Write-RunnerState -Cycle ([Math]::Max(0, $StartCycle - 1)) -Status "started" -GateOpen (Check-GateOpen) -Detail ("runner initialized (start_cycle={0})" -f $StartCycle)

$allFiles = @(git -C $repoRoot ls-files)
$candidateFiles = @(
    $allFiles | Where-Object {
        $_ -match "\.(kt|kts|md|yml|yaml|xml|properties|txt|json)$" -and
        $_ -notmatch "^(app/build|core/build|desktop/build|\.gradle|build)/"
    }
)

if ($candidateFiles.Count -eq 0) {
    Write-Log "No candidate files found."
    Send-CoordMessage -Type "closeout" -TaskId $masterTaskId -Priority "critical" -Body "No candidate files found for cycle audit."
    exit 1
}

$batchSize = [Math]::Max(1, [int][Math]::Ceiling($candidateFiles.Count / [double]$Cycles))
Write-Log ("Candidate files={0}; batch_size={1}" -f $candidateFiles.Count, $batchSize)

if ($StartCycle -lt 1) { $StartCycle = 1 }
if ($StartCycle -gt $Cycles) {
    Write-Log ("StartCycle {0} is greater than Cycles {1}. Nothing to do." -f $StartCycle, $Cycles)
    Send-CoordMessage -Type "closeout" -TaskId $masterTaskId -Priority "high" -Body ("No-op: start_cycle={0} exceeds cycles={1}." -f $StartCycle, $Cycles)
    Write-RunnerState -Cycle $Cycles -Status "complete" -GateOpen (Check-GateOpen) -Detail ("no-op complete (start_cycle={0})" -f $StartCycle)
    Write-Log "Runner complete."
    exit 0
}

for ($cycle = $StartCycle; $cycle -le $Cycles; $cycle++) {
    Renew-Lease
    $gateOpen = Check-GateOpen
    $holdNotified = $false
    while (-not $gateOpen) {
        $hold = ("Cycle {0}: gate closed ({1}) - waiting; no work executed." -f $cycle, $gateId)
        if (-not $holdNotified) {
            Write-Log $hold
            Send-CoordMessage -Type "status" -TaskId $masterTaskId -Priority "critical" -Body $hold
            $holdNotified = $true
        }
        Write-RunnerState -Cycle $cycle -Status "gate-closed" -GateOpen $false -Detail $hold
        Start-Sleep -Seconds $CyclePauseSeconds
        Renew-Lease
        $gateOpen = Check-GateOpen
    }
    if ($holdNotified) {
        $resume = ("Cycle {0}: gate reopened ({1}) - resuming work." -f $cycle, $gateId)
        Write-Log $resume
        Send-CoordMessage -Type "status" -TaskId $masterTaskId -Priority "high" -Body $resume
    }

    $cycleTaskId = "AUDIT-CYCLE-{0:D3}" -f $cycle
    $batch = Select-Batch -Files $candidateFiles -CycleIndex $cycle -BatchSize $batchSize
    $batchPreview = ($batch | Select-Object -First 3) -join ", "
    if ($batchPreview.Length -gt 180) { $batchPreview = $batchPreview.Substring(0, 180) + "..." }

    Send-CoordTask `
        -TaskId $cycleTaskId `
        -Objective ("Cycle {0}: mutual review required before MAIN-A audit/fix pass." -f $cycle) `
        -BatchSummary $batchPreview

    $agreeId = Wait-ForPeerAgree -TaskId $cycleTaskId -TimeoutSeconds $AgreementTimeoutSeconds
    while (-not $agreeId) {
        $msg = ("Cycle {0}: waiting (no MAIN-B AGREE within {1}s); reissuing same cycle." -f $cycle, $AgreementTimeoutSeconds)
        Write-Log $msg
        Write-RunnerState -Cycle $cycle -Status "peer-agree-wait" -GateOpen $true -Detail $msg
        Send-CoordMessage -Type "status" -TaskId $cycleTaskId -Priority "critical" -Body $msg
        Send-CoordTask `
            -TaskId $cycleTaskId `
            -Objective ("Cycle {0}: agreement required before MAIN-A audit/fix pass." -f $cycle) `
            -BatchSummary $batchPreview

        Start-Sleep -Seconds $CyclePauseSeconds
        Renew-Lease

        $gateOpen = Check-GateOpen
        if (-not $gateOpen) {
            $holdNotified = $false
            while (-not $gateOpen) {
                $hold = ("Cycle {0}: gate closed ({1}) while waiting for peer AGREE - holding." -f $cycle, $gateId)
                if (-not $holdNotified) {
                    Write-Log $hold
                    Send-CoordMessage -Type "status" -TaskId $cycleTaskId -Priority "critical" -Body $hold
                    $holdNotified = $true
                }
                Write-RunnerState -Cycle $cycle -Status "gate-closed" -GateOpen $false -Detail $hold
                Start-Sleep -Seconds $CyclePauseSeconds
                Renew-Lease
                $gateOpen = Check-GateOpen
            }
            if ($holdNotified) {
                $resume = ("Cycle {0}: gate reopened ({1}) - resuming peer agreement wait." -f $cycle, $gateId)
                Write-Log $resume
                Send-CoordMessage -Type "status" -TaskId $cycleTaskId -Priority "high" -Body $resume
            }
        }

        $agreeId = Wait-ForPeerAgree -TaskId $cycleTaskId -TimeoutSeconds $AgreementTimeoutSeconds
    }

    $changed = @()
    foreach ($rel in $batch) {
        $full = Join-Path $repoRoot $rel
        $didChange = Normalize-FileText -FilePath $full -RelativePath $rel
        if ($didChange) { $changed += $rel }
    }

    $markerCount = Count-AuditMarkers -Batch $batch
    $status = "Cycle {0}: AGREED({1}); files={2}; changed={3}; markers={4}" -f $cycle, $agreeId, $batch.Count, $changed.Count, $markerCount
    Write-Log $status
    Write-RunnerState -Cycle $cycle -Status "completed" -GateOpen $true -Detail $status

    $changedPreview = if ($changed.Count -gt 0) {
        ($changed | Select-Object -First 4) -join ", "
    } else {
        "none"
    }

    Send-CoordMessage `
        -Type "status" `
        -TaskId $cycleTaskId `
        -Priority "high" `
        -Body ($status + "; changed_files=" + $changedPreview + "; VERIFY " + $cycleTaskId)

    Start-Sleep -Seconds $CyclePauseSeconds
}

Send-CoordMessage -Type "closeout" -TaskId $masterTaskId -Priority "high" -Body ("Completed {0} cycles. Log: {1}" -f $Cycles, $logPath)
Write-RunnerState -Cycle $Cycles -Status "complete" -GateOpen (Check-GateOpen) -Detail "all cycles complete"
Write-Log "Runner complete."

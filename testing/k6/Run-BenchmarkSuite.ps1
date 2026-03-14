param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }),
    [string]$ArtifactRoot = $(Join-Path $PSScriptRoot "artifacts")
)

$ErrorActionPreference = "Stop"

$scenarios = @(
    "hot-sku-contention.js",
    "flash-sale-window.js",
    "reservation-expiry.js",
    "outbox-backlog-recovery.js",
    "reconciliation-load.js"
)

$k6 = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $k6) {
    throw "k6 is not installed or not on PATH."
}

New-Item -ItemType Directory -Path $ArtifactRoot -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$gitCommit = git -c safe.directory=$repoRoot -C $repoRoot rev-parse --short HEAD
$manifestScenarios = @()

foreach ($scenario in $scenarios) {
    $scenarioPath = Join-Path $PSScriptRoot $scenario
    $summaryPath = Join-Path $ArtifactRoot ("{0}-{1}.json" -f $timestamp, [System.IO.Path]::GetFileNameWithoutExtension($scenario))

    Write-Host ("Running {0} -> {1}" -f $scenario, $summaryPath)
    & $k6.Source run "-e" "BASE_URL=$BaseUrl" "--summary-export=$summaryPath" $scenarioPath

    $manifestScenarios += [pscustomobject]@{
        name = $scenario
        summary = $summaryPath
    }
}

$manifest = [pscustomobject]@{
    timestamp = (Get-Date).ToUniversalTime().ToString("o")
    gitCommit = $gitCommit.Trim()
    baseUrl = $BaseUrl
    scenarios = $manifestScenarios
    config = [pscustomobject]@{
        reservationTtl = $env:APP_RESERVATION_TTL
        lockWaitTimeout = $env:APP_LOCK_WAIT_TIMEOUT
        lockLeaseTimeout = $env:APP_LOCK_LEASE_TIMEOUT
        outboxBatchSize = $env:APP_OUTBOX_PUBLISH_BATCH_SIZE
        outboxRetryDelay = $env:APP_OUTBOX_RETRY_DELAY
        channelSyncBatchSize = $env:APP_CHANNEL_SYNC_BATCH_SIZE
        channelRetryDelay = $env:APP_CHANNEL_RETRY_DELAY
        reconciliationDelay = $env:APP_SCHEDULER_RECONCILIATION_DELAY
        channelSnapshotStaleness = $env:APP_ALERTS_CHANNEL_SNAPSHOT_STALENESS
    }
}

$manifestPath = Join-Path $ArtifactRoot ("{0}-manifest.json" -f $timestamp)
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $manifestPath

Write-Host ("Benchmark manifest written to {0}" -f $manifestPath)

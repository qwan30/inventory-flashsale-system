param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }),
    [string]$ArtifactRoot = $(Join-Path $PSScriptRoot "artifacts"),
    [string]$SuitePath = $(Join-Path $PSScriptRoot "suite.json"),
    [string]$SpringProfile = $(if ($env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE } else { "benchmark" }),
    [switch]$ValidateFixtures,
    [switch]$PromoteIfPassed,
    [string]$CommitSha
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "lib\Suite.ps1")
. (Join-Path $PSScriptRoot "lib\Report.ps1")
. (Join-Path $PSScriptRoot "lib\HttpChecks.ps1")

New-Item -ItemType Directory -Path $ArtifactRoot -Force | Out-Null

if ($ValidateFixtures) {
    . (Join-Path $PSScriptRoot "lib\Validate-BenchmarkFixtures.ps1")
    Test-BenchmarkFixtureTransformations -FixtureRoot (Join-Path $PSScriptRoot "fixtures") | Out-Null
    Write-Host "Fixture validation passed."
    return
}

$k6 = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $k6) {
    throw "k6 is not installed or not on PATH."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$timestampUtc = (Get-Date).ToUniversalTime().ToString("o")
$suite = Get-BenchmarkSuite -SuitePath $SuitePath
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$defaultCommit = git -c safe.directory=$repoRoot -C $repoRoot rev-parse --short HEAD
$gitCommit = if ([string]::IsNullOrWhiteSpace($CommitSha)) { $defaultCommit.Trim() } else { $CommitSha }
$artifactDir = Join-Path $ArtifactRoot $timestamp
$reportPath = Join-Path $artifactDir "report.json"
$manifestPath = Join-Path $artifactDir "manifest.json"
$resetScriptPath = Join-Path $PSScriptRoot "Reset-BenchmarkState.ps1"
$profileConfigPath = Join-Path $repoRoot ("apps\api\src\main\resources\application-{0}.yml" -f $SpringProfile)
$profileConfig = Get-FlatYamlMap -Path $profileConfigPath

New-Item -ItemType Directory -Path $artifactDir -Force | Out-Null

$scenarioResults = @()
$artifactSummaries = [ordered]@{}
$scenarioOrder = @($suite.scenarios | ForEach-Object { $_.name })

foreach ($scenario in $suite.scenarios) {
    $scenarioPath = Join-Path $PSScriptRoot $scenario.file
    $summaryPath = Join-Path $artifactDir ("{0}.summary.json" -f $scenario.name)

    if (-not (Test-Path -Path $scenarioPath)) {
        throw ("Scenario script not found: {0}" -f $scenarioPath)
    }
    if ($scenario.resetRequired) {
        if (-not (Test-Path -Path $resetScriptPath)) {
            throw ("Reset script not found: {0}" -f $resetScriptPath)
        }
        Write-Host ("Resetting state for scenario {0}" -f $scenario.name)
        & $resetScriptPath -Scenario $scenario.name
        if ($LASTEXITCODE -ne 0) {
            throw ("Reset script failed for scenario {0} with exit code {1}" -f $scenario.name, $LASTEXITCODE)
        }
    }

    $k6Args = @("run", "-e", "BASE_URL=$BaseUrl")
    foreach ($property in $scenario.env.PSObject.Properties) {
        $k6Args += "-e"
        $k6Args += ("{0}={1}" -f $property.Name, $property.Value)
    }
    $k6Args += "--summary-export=$summaryPath"
    $k6Args += $scenarioPath

    Write-Host ("Running {0} -> {1}" -f $scenario.name, $summaryPath)

    $scenarioStatus = "PASSED"
    $scenarioError = $null
    try {
        & $k6.Source @k6Args
        if ($LASTEXITCODE -ne 0) {
            $scenarioStatus = "FAILED"
        }
    } catch {
        $scenarioStatus = "FAILED"
        $scenarioError = $_.Exception.Message
    }

    $summary = $null
    $stats = $null
    if (Test-Path -Path $summaryPath) {
        $summary = Get-Content -Path $summaryPath -Raw | ConvertFrom-Json
        $stats = Get-K6ScenarioStats -Summary $summary
    } else {
        $scenarioStatus = "FAILED"
        if (-not $scenarioError) {
            $scenarioError = "k6 did not produce a summary export."
        }
    }

    $result = [ordered]@{
        name = $scenario.name
        file = $scenario.file
        status = $scenarioStatus
        exitCode = [int]$LASTEXITCODE
        summaryPath = $summaryPath
        stats = $stats
        postRunChecks = @($scenario.postRunChecks)
    }
    if ($scenarioError) {
        $result["error"] = $scenarioError
    }

    $scenarioResults += [pscustomobject]$result
    $artifactSummaries[$scenario.name] = $summaryPath
}

$businessCheckResult = Get-BusinessCheckSnapshot -BaseUrl $BaseUrl -Seed $suite.seed
$baselineComparison = Get-BaselineComparisonResult -BaselineTarget $suite.baselineTarget -ScenarioResults $scenarioResults

$suiteStatus = "PASSED"
if (($scenarioResults | Where-Object { $_.status -eq "FAILED" }).Count -gt 0 -or -not $businessCheckResult.businessChecks.passed) {
    $suiteStatus = "FAILED"
}

$report = New-BenchmarkReportObject `
    -SuiteStatus $suiteStatus `
    -ScenarioResults $scenarioResults `
    -BusinessChecks $businessCheckResult.businessChecks `
    -OpsSnapshots $businessCheckResult.opsSnapshots `
    -BaselineComparison $baselineComparison

$manifest = New-BenchmarkManifestObject `
    -TimestampUtc $timestampUtc `
    -GitCommit $gitCommit.Trim() `
    -BaseUrl $BaseUrl `
    -SpringProfile $SpringProfile `
    -ScenarioOrder $scenarioOrder `
    -Seed $suite.seed `
    -ConfigCaptureKeys @($suite.configCapture) `
    -ProfileConfig $profileConfig `
    -ArtifactPaths @{
        manifest = $manifestPath
        report = $reportPath
        summaries = $artifactSummaries
    }

$report | ConvertTo-Json -Depth 8 | Set-Content -Path $reportPath
$manifest | ConvertTo-Json -Depth 8 | Set-Content -Path $manifestPath

Write-Host ("Benchmark report written to {0}" -f $reportPath)
Write-Host ("Benchmark manifest written to {0}" -f $manifestPath)

$comparisonPath = Join-Path $artifactDir "comparison.json"
Write-BenchmarkComparison -Path $comparisonPath -BaselineComparison $baselineComparison

$summaryPath = Join-Path $artifactDir "summary.md"
Write-BenchmarkSummary `
    -Path $summaryPath `
    -TimestampUtc $timestampUtc `
    -Commit $gitCommit `
    -BaseUrl $BaseUrl `
    -SpringProfile $SpringProfile `
    -SuiteStatus $suiteStatus `
    -ScenarioResults $scenarioResults `
    -BusinessChecks $businessCheckResult.businessChecks `
    -BaselineComparison $baselineComparison `
    -PromotionRequested $PromoteIfPassed.IsPresent

if ($PromoteIfPassed -and $suiteStatus -eq "PASSED" -and $businessCheckResult.businessChecks.passed) {
    $evidenceRoot = Join-Path $PSScriptRoot "evidence"
    New-Item -ItemType Directory -Path $evidenceRoot -Force | Out-Null
    $promotedDirName = ("{0}-{1}" -f $timestamp, $gitCommit.Trim())
    $promotedDir = Join-Path $evidenceRoot $promotedDirName
    if (Test-Path -Path $promotedDir) {
        Remove-Item -Path $promotedDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $promotedDir -Force | Out-Null
    Copy-Item -Path (Join-Path $artifactDir "*") -Destination $promotedDir -Recurse -Force
    $evidenceIndexPath = Join-Path $evidenceRoot "index.json"
    $entry = [ordered]@{
        timestamp = $timestamp
        gitCommit = $gitCommit.Trim()
        evidenceDir = $promotedDirName
        artifactDir = (Resolve-Path $artifactDir).Path
        reportPath = (Resolve-Path (Join-Path $promotedDir "report.json")).Path
        manifestPath = (Resolve-Path (Join-Path $promotedDir "manifest.json")).Path
        summaryPath = (Resolve-Path (Join-Path $promotedDir "summary.md")).Path
        comparisonPath = (Resolve-Path (Join-Path $promotedDir "comparison.json")).Path
        baselineTarget = $suite.baselineTarget
        suiteStatus = $suiteStatus
        businessChecksPassed = $businessCheckResult.businessChecks.passed
        promotedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    }
    Update-EvidenceIndex -IndexPath $evidenceIndexPath -Entry $entry
    Write-Host ("Benchmark evidence promoted to {0}" -f $promotedDir)
}

if ($suiteStatus -eq "FAILED") {
    exit 1
}

function Format-MetricValue {
    param(
        [Parameter(Mandatory = $true)]
        $Value
    )

    if ($null -eq $Value) {
        return "n/a"
    }

    try {
        return "{0:N3}" -f [double]$Value
    } catch {
        return $Value.ToString()
    }
}

function Write-BenchmarkSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$TimestampUtc,
        [Parameter(Mandatory = $true)]
        [string]$Commit,
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$SpringProfile,
        [Parameter(Mandatory = $true)]
        [string]$SuiteStatus,
        [Parameter(Mandatory = $true)]
        [object[]]$ScenarioResults,
        [Parameter(Mandatory = $true)]
        [object]$BusinessChecks,
        [Parameter(Mandatory = $true)]
        [object]$BaselineComparison,
        [Parameter(Mandatory = $true)]
        [bool]$PromotionRequested
    )

    $lines = [System.Collections.ArrayList]@()
    $lines.Add("Suite Status: $SuiteStatus")
    $lines.Add("Timestamp (UTC): $TimestampUtc")
    $lines.Add("Git Commit: $Commit")
    $lines.Add("Base URL: $BaseUrl")
    $lines.Add("Spring Profile: $SpringProfile")
    $lines.Add("")
    $lines.Add("Scenarios:")

    foreach ($scenario in $ScenarioResults) {
        $avg = Format-MetricValue -Value ($scenario.stats.httpReqDurationAvg)
        $p95 = Format-MetricValue -Value ($scenario.stats.httpReqDurationP95)
        $failed = Format-MetricValue -Value ($scenario.stats.httpReqFailedRate)
        $checks = Format-MetricValue -Value ($scenario.stats.checksRate)
        $errorNote = if ($scenario.error) { " error={0}" -f $scenario.error } else { "" }
        $lines.Add(
            ("- {0}: {1} (avg={2}ms, p95={3}ms, failedRate={4}, checksRate={5}, exitCode={6}){7}" `
                -f $scenario.name, $scenario.status, $avg, $p95, $failed, $checks, $scenario.exitCode, $errorNote)
        )
    }

    $lines.Add("")
    $lines.Add(("Business checks passed: {0}" -f $BusinessChecks.passed))
    foreach ($check in $BusinessChecks.checks) {
        $detail = if ($check.detail) { " ({0})" -f $check.detail } else { "" }
        $lines.Add(("- {0}: {1}{2}" -f $check.check, $check.passed, $detail))
    }

    $lines.Add("")
    $available = if ($BaselineComparison.available) { "yes" } else { "no" }
    $lines.Add(("Baseline target available: {0}" -f $available))
    $baselineTargetLabel = if ($BaselineComparison.baselineTarget) { $BaselineComparison.baselineTarget } else { "n/a" }
    $lines.Add(("Baseline target: {0}" -f $baselineTargetLabel))
    $lines.Add(("Baseline note: {0}" -f $BaselineComparison.note))

    if ($BaselineComparison.scenarioDeltas) {
        $lines.Add("")
        $lines.Add("Baseline scenario deltas:")
        foreach ($delta in $BaselineComparison.scenarioDeltas) {
            if ($delta.available -and $delta.deltas) {
                $d1 = Format-MetricValue -Value ($delta.deltas.httpReqDurationAvg)
                $d2 = Format-MetricValue -Value ($delta.deltas.httpReqDurationP95)
                $d3 = Format-MetricValue -Value ($delta.deltas.httpReqFailedRate)
                $d4 = Format-MetricValue -Value ($delta.deltas.checksRate)
                $lines.Add(
                    ("- {0}: delta avg={1}, p95={2}, failedRate={3}, checksRate={4}" `
                        -f $delta.name, $d1, $d2, $d3, $d4)
                )
            } else {
                $note = if ($delta.note) { $delta.note } else { "no baseline data" }
                $lines.Add(("- {0}: {1}" -f $delta.name, $note))
            }
        }
    }

    $lines.Add("")
    $promotionLabel = if ($PromotionRequested) { "yes" } else { "no" }
    $lines.Add(("Promoted evidence requested: {0}" -f $promotionLabel))

    $lines | Set-Content -Path $Path -Encoding UTF8
}

function Write-BenchmarkComparison {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [object]$BaselineComparison
    )

    $BaselineComparison | ConvertTo-Json -Depth 8 | Set-Content -Path $Path -Encoding UTF8
}

function Update-EvidenceIndex {
    param(
        [Parameter(Mandatory = $true)]
        [string]$IndexPath,
        [Parameter(Mandatory = $true)]
        [object]$Entry
    )

    $index = [ordered]@{ entries = @() }
    if (Test-Path -Path $IndexPath) {
        $raw = Get-Content -Path $IndexPath -Raw
        if (-not [string]::IsNullOrWhiteSpace($raw)) {
            $parsed = $raw | ConvertFrom-Json
            if ($parsed.entries) {
                $index = $parsed
            }
        }
    }
    if (-not $index.entries) {
        $index.entries = @()
    }
    $index.entries += $Entry
    $index | ConvertTo-Json -Depth 8 | Set-Content -Path $IndexPath -Encoding UTF8
}

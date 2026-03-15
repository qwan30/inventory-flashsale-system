param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }),
    [string]$ArtifactRoot = $(Join-Path $PSScriptRoot "artifacts"),
    [string]$SuitePath = $(Join-Path $PSScriptRoot "suite.json"),
    [string]$SpringProfile = $(if ($env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE } else { "benchmark" }),
    [switch]$ValidateFixtures
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
$gitCommit = git -c safe.directory=$repoRoot -C $repoRoot rev-parse --short HEAD
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

if ($suiteStatus -eq "FAILED") {
    exit 1
}

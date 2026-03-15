. (Join-Path $PSScriptRoot "Report.ps1")

function Test-BenchmarkFixtureTransformations {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FixtureRoot
    )

    $passSummaryPath = Join-Path $FixtureRoot "pass-summary.json"
    $failSummaryPath = Join-Path $FixtureRoot "fail-summary.json"

    if (-not (Test-Path -Path $passSummaryPath) -or -not (Test-Path -Path $failSummaryPath)) {
        throw "Fixture files pass-summary.json and fail-summary.json are required."
    }

    $passSummary = Get-Content -Path $passSummaryPath -Raw | ConvertFrom-Json
    $failSummary = Get-Content -Path $failSummaryPath -Raw | ConvertFrom-Json

    $passStats = Get-K6ScenarioStats -Summary $passSummary
    $failStats = Get-K6ScenarioStats -Summary $failSummary

    $allPassedReport = New-BenchmarkReportObject `
        -SuiteStatus "PASSED" `
        -ScenarioResults @(
            [pscustomobject]@{ name = "fixture-pass"; status = "PASSED"; stats = $passStats }
        ) `
        -BusinessChecks ([pscustomobject]@{ passed = $true; checks = @() }) `
        -OpsSnapshots ([pscustomobject]@{}) `
        -BaselineComparison ([pscustomobject]@{ mode = "informational"; available = $false })

    if ($allPassedReport.suiteStatus -ne "PASSED") {
        throw "Fixture validation failed: expected PASSED report status."
    }

    $hasFailureReport = New-BenchmarkReportObject `
        -SuiteStatus "FAILED" `
        -ScenarioResults @(
            [pscustomobject]@{ name = "fixture-fail"; status = "FAILED"; stats = $failStats }
        ) `
        -BusinessChecks ([pscustomobject]@{ passed = $true; checks = @() }) `
        -OpsSnapshots ([pscustomobject]@{}) `
        -BaselineComparison ([pscustomobject]@{ mode = "informational"; available = $false })

    if ($hasFailureReport.suiteStatus -ne "FAILED") {
        throw "Fixture validation failed: expected FAILED report status."
    }

    return [ordered]@{
        passStats = $passStats
        failStats = $failStats
    }
}

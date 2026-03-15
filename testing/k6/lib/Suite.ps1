function Get-BenchmarkSuite {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SuitePath
    )

    if (-not (Test-Path -Path $SuitePath)) {
        throw ("Benchmark suite config not found: {0}" -f $SuitePath)
    }

    $suite = Get-Content -Path $SuitePath -Raw | ConvertFrom-Json

    if (-not $suite.scenarios -or $suite.scenarios.Count -eq 0) {
        throw "Benchmark suite must define at least one scenario."
    }

    foreach ($scenario in $suite.scenarios) {
        if (-not $scenario.name -or -not $scenario.file) {
            throw "Each scenario must include non-empty 'name' and 'file'."
        }
        if ($null -eq $scenario.resetRequired) {
            throw ("Scenario '{0}' must define 'resetRequired'." -f $scenario.name)
        }
    }

    return $suite
}

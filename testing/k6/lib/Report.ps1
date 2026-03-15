function Get-K6MetricValue {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Summary,
        [Parameter(Mandatory = $true)]
        [string]$MetricName,
        [Parameter(Mandatory = $true)]
        [string]$ValueName
    )

    if (-not $Summary.metrics) {
        return $null
    }
    if (-not ($Summary.metrics.PSObject.Properties.Name -contains $MetricName)) {
        return $null
    }

    $metric = $Summary.metrics.$MetricName
    $valueSource = $metric
    if ($metric.values) {
        $valueSource = $metric.values
    }
    if (-not ($valueSource.PSObject.Properties.Name -contains $ValueName)) {
        return $null
    }

    return $valueSource.$ValueName
}

function Get-K6MetricValueFirstAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Summary,
        [Parameter(Mandatory = $true)]
        [string]$MetricName,
        [Parameter(Mandatory = $true)]
        [string[]]$ValueNames
    )

    foreach ($valueName in $ValueNames) {
        $value = Get-K6MetricValue -Summary $Summary -MetricName $MetricName -ValueName $valueName
        if ($null -ne $value) {
            return $value
        }
    }

    return $null
}

function Get-K6ScenarioStats {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Summary
    )

    return [ordered]@{
        httpReqDurationAvg = Get-K6MetricValue -Summary $Summary -MetricName "http_req_duration" -ValueName "avg"
        httpReqDurationP95 = Get-K6MetricValue -Summary $Summary -MetricName "http_req_duration" -ValueName "p(95)"
        httpReqFailedRate = Get-K6MetricValueFirstAvailable -Summary $Summary -MetricName "http_req_failed" -ValueNames @("rate", "value")
        checksRate = Get-K6MetricValueFirstAvailable -Summary $Summary -MetricName "checks" -ValueNames @("rate", "value")
    }
}

function New-BenchmarkReportObject {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SuiteStatus,
        [Parameter(Mandatory = $true)]
        [object[]]$ScenarioResults,
        [Parameter(Mandatory = $true)]
        [object]$BusinessChecks,
        [Parameter(Mandatory = $true)]
        [object]$OpsSnapshots,
        [Parameter(Mandatory = $true)]
        [object]$BaselineComparison
    )

    return [ordered]@{
        suiteStatus = $SuiteStatus
        scenarioResults = $ScenarioResults
        businessChecks = $BusinessChecks
        opsSnapshots = $OpsSnapshots
        baselineComparison = $BaselineComparison
    }
}

function New-BenchmarkManifestObject {
    param(
        [Parameter(Mandatory = $true)]
        [string]$TimestampUtc,
        [Parameter(Mandatory = $true)]
        [string]$GitCommit,
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$SpringProfile,
        [Parameter(Mandatory = $true)]
        [string[]]$ScenarioOrder,
        [Parameter(Mandatory = $true)]
        [object]$Seed,
        [Parameter(Mandatory = $true)]
        [string[]]$ConfigCaptureKeys,
        [Parameter(Mandatory = $true)]
        [hashtable]$ProfileConfig,
        [Parameter(Mandatory = $true)]
        [hashtable]$ArtifactPaths
    )

    $config = [ordered]@{}
    foreach ($key in $ConfigCaptureKeys) {
        $envName = Convert-PropertyKeyToEnvName -PropertyKey $key
        $value = [Environment]::GetEnvironmentVariable($envName)
        if ([string]::IsNullOrWhiteSpace($value) -and $ProfileConfig.ContainsKey($key)) {
            $value = $ProfileConfig[$key]
        }
        $config[$key] = $value
    }

    return [ordered]@{
        timestampUtc = $TimestampUtc
        gitCommit = $GitCommit
        baseUrl = $BaseUrl
        springProfile = $SpringProfile
        scenarioOrder = $ScenarioOrder
        seed = $Seed
        config = $config
        artifacts = $ArtifactPaths
    }
}

function Convert-PropertyKeyToEnvName {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PropertyKey
    )

    return ($PropertyKey.ToUpperInvariant() -replace "[\.\-]", "_")
}

function Get-FlatYamlMap {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $result = @{}
    if (-not (Test-Path -Path $Path)) {
        return $result
    }

    $stack = New-Object System.Collections.Generic.List[string]
    foreach ($line in (Get-Content -Path $Path)) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("#")) {
            continue
        }

        if ($line -notmatch "^(\s*)([^:]+):(?:\s*(.*))?$") {
            continue
        }

        $indentLevel = [int]($matches[1].Length / 2)
        $key = $matches[2].Trim()
        $value = $matches[3]

        while ($stack.Count -gt $indentLevel) {
            $stack.RemoveAt($stack.Count - 1)
        }

        if ([string]::IsNullOrWhiteSpace($value)) {
            if ($stack.Count -eq $indentLevel) {
                $stack.Add($key)
            } elseif ($stack.Count -gt $indentLevel) {
                $stack[$indentLevel] = $key
            }
            continue
        }

        $normalizedValue = $value.Trim()
        if (($normalizedValue.StartsWith("'") -and $normalizedValue.EndsWith("'")) -or ($normalizedValue.StartsWith('"') -and $normalizedValue.EndsWith('"'))) {
            $normalizedValue = $normalizedValue.Substring(1, $normalizedValue.Length - 2)
        }

        $pathSegments = @($stack) + $key
        $result[($pathSegments -join ".")] = $normalizedValue
    }

    return $result
}

function Get-BaselineComparisonResult {
    param(
        [string]$BaselineTarget,
        [Parameter(Mandatory = $true)]
        [object[]]$ScenarioResults
    )

    $result = [ordered]@{
        mode = "informational"
        baselineTarget = $BaselineTarget
        available = $false
        note = "No baseline target configured."
        scenarioDeltas = @()
    }

    if ([string]::IsNullOrWhiteSpace($BaselineTarget)) {
        return $result
    }

    if (-not (Test-Path -Path $BaselineTarget)) {
        $result.note = "Baseline target path does not exist."
        return $result
    }

    $baselineReport = Get-Content -Path $BaselineTarget -Raw | ConvertFrom-Json
    if (-not $baselineReport.scenarioResults) {
        $result.note = "Baseline report does not include scenarioResults."
        return $result
    }

    $result.available = $true
    $result.note = "Baseline comparison is informational and does not affect suite pass/fail."

    foreach ($scenario in $ScenarioResults) {
        $baselineScenario = $baselineReport.scenarioResults | Where-Object { $_.name -eq $scenario.name } | Select-Object -First 1
        if (-not $baselineScenario) {
            $result.scenarioDeltas += [ordered]@{
                name = $scenario.name
                available = $false
                note = "Scenario missing in baseline."
            }
            continue
        }

        $result.scenarioDeltas += [ordered]@{
            name = $scenario.name
            available = $true
            deltas = [ordered]@{
                httpReqDurationAvg = (Get-Delta -Current $scenario.stats.httpReqDurationAvg -Baseline $baselineScenario.stats.httpReqDurationAvg)
                httpReqDurationP95 = (Get-Delta -Current $scenario.stats.httpReqDurationP95 -Baseline $baselineScenario.stats.httpReqDurationP95)
                httpReqFailedRate = (Get-Delta -Current $scenario.stats.httpReqFailedRate -Baseline $baselineScenario.stats.httpReqFailedRate)
                checksRate = (Get-Delta -Current $scenario.stats.checksRate -Baseline $baselineScenario.stats.checksRate)
            }
        }
    }

    return $result
}

function Get-Delta {
    param(
        $Current,
        $Baseline
    )

    if ($null -eq $Current -or $null -eq $Baseline) {
        return $null
    }

    return [double]$Current - [double]$Baseline
}

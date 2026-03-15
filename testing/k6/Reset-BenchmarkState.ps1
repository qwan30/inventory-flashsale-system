param(
    [string]$Scenario = "default",
    [string]$DbHost = $(if ($env:BENCHMARK_DB_HOST) { $env:BENCHMARK_DB_HOST } else { "localhost" }),
    [int]$DbPort = $(if ($env:BENCHMARK_DB_PORT) { [int]$env:BENCHMARK_DB_PORT } else { 3306 }),
    [string]$DbName = $(if ($env:BENCHMARK_DB_NAME) { $env:BENCHMARK_DB_NAME } else { "flashsale" }),
    [string]$DbUsername = $(if ($env:BENCHMARK_DB_USERNAME) { $env:BENCHMARK_DB_USERNAME } else { "flashsale" }),
    [string]$DbPassword = $(if ($env:BENCHMARK_DB_PASSWORD) { $env:BENCHMARK_DB_PASSWORD } else { "flashsale" }),
    [string]$ContainerName = $(if ($env:BENCHMARK_DB_CONTAINER) { $env:BENCHMARK_DB_CONTAINER } else { "flashsale-mysql" })
)

$ErrorActionPreference = "Stop"

$scriptRoot = $PSScriptRoot
$sqlRoot = Join-Path $scriptRoot "sql"
$baseScripts = @(
    (Join-Path $sqlRoot "00-reset-state.sql"),
    (Join-Path $sqlRoot "10-seed-base-state.sql")
)
$scenarioScript = Join-Path (Join-Path $sqlRoot "scenarios") ($Scenario + ".sql")

function Invoke-SqlViaMysqlCli {
    param([string]$SqlFilePath)
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysql) {
        return $false
    }

    $sql = Get-Content -Path $SqlFilePath -Raw
    $args = @(
        "--host=$DbHost",
        "--port=$DbPort",
        "--user=$DbUsername",
        "--password=$DbPassword",
        "--database=$DbName",
        "--default-character-set=utf8mb4"
    )
    $sql | & $mysql.Source @args
    if ($LASTEXITCODE -ne 0) {
        throw "mysql CLI execution failed for file: $SqlFilePath"
    }
    return $true
}

function Invoke-SqlViaDockerExec {
    param([string]$SqlFilePath)
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        throw "mysql CLI is not available and docker is not available for fallback execution."
    }

    $sql = Get-Content -Path $SqlFilePath -Raw
    $args = @(
        "exec", "-i", $ContainerName,
        "mysql",
        "--host=127.0.0.1",
        "--port=3306",
        "--user=$DbUsername",
        "--password=$DbPassword",
        "--database=$DbName",
        "--default-character-set=utf8mb4"
    )
    $sql | & $docker.Source @args
    if ($LASTEXITCODE -ne 0) {
        throw "docker mysql execution failed for file: $SqlFilePath (container: $ContainerName)"
    }
}

function Invoke-SqlFile {
    param([string]$SqlFilePath)
    if (-not (Test-Path -Path $SqlFilePath)) {
        throw "SQL file not found: $SqlFilePath"
    }

    Write-Host ("Applying SQL: {0}" -f $SqlFilePath)
    $executed = Invoke-SqlViaMysqlCli -SqlFilePath $SqlFilePath
    if (-not $executed) {
        Invoke-SqlViaDockerExec -SqlFilePath $SqlFilePath
    }
}

function Invoke-Readback {
    $query = @"
SELECT CONCAT(sku,':',available_qty,':',reserved_qty,':',sold_qty) AS inventory_state
FROM inventory_item
WHERE sku = 'SKU-DEMO-001'
ORDER BY sku;
SELECT CONCAT(id,':',status,':',DATE_FORMAT(starts_at,'%Y-%m-%d %H:%i:%s'),':',DATE_FORMAT(ends_at,'%Y-%m-%d %H:%i:%s')) AS campaign_state
FROM flash_sale_campaign
WHERE id IN ('campaign-demo-001', 'campaign-ended-001')
ORDER BY id;
"@
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysql) {
        $args = @(
            "--host=$DbHost",
            "--port=$DbPort",
            "--user=$DbUsername",
            "--password=$DbPassword",
            "--database=$DbName",
            "--batch",
            "--raw",
            "--skip-column-names"
        )
        $query | & $mysql.Source @args
        if ($LASTEXITCODE -ne 0) {
            throw "mysql CLI readback query failed."
        }
        return
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        $args = @(
            "exec", "-i", $ContainerName,
            "mysql",
            "--host=127.0.0.1",
            "--port=3306",
            "--user=$DbUsername",
            "--password=$DbPassword",
            "--database=$DbName",
            "--batch",
            "--raw",
            "--skip-column-names"
        )
        $query | & $docker.Source @args
        if ($LASTEXITCODE -ne 0) {
            throw "docker mysql readback query failed (container: $ContainerName)."
        }
        return
    }

    Write-Warning "No mysql readback available (mysql CLI and docker are both unavailable)."
}

Write-Host ("Resetting benchmark state for scenario '{0}'..." -f $Scenario)

foreach ($script in $baseScripts) {
    Invoke-SqlFile -SqlFilePath $script
}

if (Test-Path -Path $scenarioScript) {
    Invoke-SqlFile -SqlFilePath $scenarioScript
} else {
    Write-Host ("No scenario override found for '{0}', continuing with base seed only." -f $Scenario)
}

Write-Host "Benchmark state reset complete. Readback:"
Invoke-Readback

function Invoke-JsonGet {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    try {
        $response = Invoke-RestMethod -Method Get -Uri $Url -TimeoutSec 30
        return [ordered]@{
            ok = $true
            data = $response
            error = $null
        }
    } catch {
        return [ordered]@{
            ok = $false
            data = $null
            error = $_.Exception.Message
        }
    }
}

function Get-BusinessCheckSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [object]$Seed
    )

    $inventoryResult = Invoke-JsonGet -Url ("{0}/api/v1/inventory/{1}" -f $BaseUrl.TrimEnd('/'), $Seed.sku)
    $outboxResult = Invoke-JsonGet -Url ("{0}/api/v1/ops/outbox/backlog" -f $BaseUrl.TrimEnd('/'))
    $alertsResult = Invoke-JsonGet -Url ("{0}/api/v1/ops/alerts" -f $BaseUrl.TrimEnd('/'))
    $driftsResult = Invoke-JsonGet -Url ("{0}/api/v1/ops/reconciliation/drifts" -f $BaseUrl.TrimEnd('/'))

    $passed = $true
    $checks = @()

    if (-not $inventoryResult.ok) {
        $passed = $false
        $checks += [ordered]@{
            check = "inventory_endpoint_available"
            passed = $false
            detail = $inventoryResult.error
        }
    } else {
        $inventory = $inventoryResult.data
        $nonNegative = ($inventory.availableQty -ge 0) -and ($inventory.reservedQty -ge 0) -and ($inventory.soldQty -ge 0)
        $sum = [int]$inventory.availableQty + [int]$inventory.reservedQty + [int]$inventory.soldQty
        $stockConserved = $sum -eq [int]$Seed.totalStock

        if (-not $nonNegative -or -not $stockConserved) {
            $passed = $false
        }

        $checks += [ordered]@{
            check = "inventory_non_negative"
            passed = $nonNegative
            detail = ("available={0}, reserved={1}, sold={2}" -f $inventory.availableQty, $inventory.reservedQty, $inventory.soldQty)
        }
        $checks += [ordered]@{
            check = "inventory_stock_conservation"
            passed = $stockConserved
            detail = ("sum={0}, expected={1}" -f $sum, $Seed.totalStock)
        }
    }

    $opsSnapshots = [ordered]@{
        outboxBacklog = if ($outboxResult.ok) { $outboxResult.data } else { $null }
        alerts = if ($alertsResult.ok) { $alertsResult.data } else { $null }
        reconciliationDrifts = if ($driftsResult.ok) { $driftsResult.data } else { $null }
        endpointErrors = [ordered]@{
            outboxBacklog = if ($outboxResult.ok) { $null } else { $outboxResult.error }
            alerts = if ($alertsResult.ok) { $null } else { $alertsResult.error }
            reconciliationDrifts = if ($driftsResult.ok) { $null } else { $driftsResult.error }
        }
    }

    foreach ($endpointCheck in @(
        @{ name = "outbox_backlog_endpoint_available"; result = $outboxResult },
        @{ name = "alerts_endpoint_available"; result = $alertsResult },
        @{ name = "reconciliation_drifts_endpoint_available"; result = $driftsResult }
    )) {
        if (-not $endpointCheck.result.ok) {
            $passed = $false
        }
        $checks += [ordered]@{
            check = $endpointCheck.name
            passed = $endpointCheck.result.ok
            detail = if ($endpointCheck.result.ok) { "OK" } else { $endpointCheck.result.error }
        }
    }

    return [ordered]@{
        businessChecks = [ordered]@{
            passed = $passed
            checks = $checks
        }
        opsSnapshots = $opsSnapshots
    }
}

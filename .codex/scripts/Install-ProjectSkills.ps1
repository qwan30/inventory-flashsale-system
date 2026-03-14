[CmdletBinding()]
param(
    [string]$Bundle = "core",
    [string[]]$Skill,
    [string]$Destination,
    [switch]$Force,
    [switch]$List
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $root = Resolve-Path (Join-Path $PSScriptRoot "..\\..")
    return $root.Path
}

function Get-Registry {
    param(
        [string]$RepoRoot
    )

    $registryPath = Join-Path $RepoRoot ".codex\\skills\\registry.json"
    return Get-Content -Raw $registryPath | ConvertFrom-Json
}

function Get-DefaultDestination {
    if ($env:CODEX_HOME) {
        return Join-Path $env:CODEX_HOME "skills"
    }

    return Join-Path $HOME ".codex\\skills"
}

function Get-AllSkillEntries {
    param(
        [object]$Registry
    )

    $all = @()
    foreach ($source in $Registry.sources) {
        foreach ($entry in $source.skills) {
            $all += $entry
        }
    }

    return $all
}

function Get-SelectedSkills {
    param(
        [object]$Registry,
        [string[]]$RequestedSkills,
        [string]$RequestedBundle
    )

    $allEntries = Get-AllSkillEntries -Registry $Registry
    $byName = @{}
    foreach ($entry in $allEntries) {
        $byName[$entry.name] = $entry
    }

    if ($RequestedSkills -and $RequestedSkills.Count -gt 0) {
        $selected = @()
        foreach ($name in $RequestedSkills) {
            if (-not $byName.ContainsKey($name)) {
                throw "Unknown skill '$name'. Use -List to see available skills."
            }

            $selected += $byName[$name]
        }

        return $selected
    }

    $bundle = $Registry.bundles | Where-Object { $_.name -eq $RequestedBundle } | Select-Object -First 1
    if (-not $bundle) {
        throw "Unknown bundle '$RequestedBundle'. Use -List to see available bundles."
    }

    if ($bundle.name -eq "all") {
        return $allEntries
    }

    $selected = @()
    foreach ($name in $bundle.skills) {
        if (-not $byName.ContainsKey($name)) {
            throw "Bundle '$RequestedBundle' references missing skill '$name'."
        }

        $selected += $byName[$name]
    }

    return $selected
}

function Expand-RelativeEntryPlaceholders {
    param(
        [string]$SourcePath,
        [string]$TargetPath
    )

    foreach ($item in Get-ChildItem -Path $TargetPath -File) {
        if ($item.Name -eq "SKILL.md") {
            continue
        }

        $content = (Get-Content -Raw $item.FullName).Trim()
        if (-not $content.StartsWith("./") -and -not $content.StartsWith("../")) {
            continue
        }

        $resolvedSource = [System.IO.Path]::GetFullPath((Join-Path $SourcePath $content))
        if (-not (Test-Path $resolvedSource)) {
            continue
        }

        Remove-Item -Force $item.FullName
        Copy-Item -Path $resolvedSource -Destination $item.FullName -Recurse
    }
}

function Show-Registry {
    param(
        [object]$Registry
    )

    Write-Host "Bundles:"
    foreach ($bundle in $Registry.bundles) {
        $bundleSkills = if ($bundle.name -eq "all") { "all skills" } else { ($bundle.skills -join ", ") }
        Write-Host "  - $($bundle.name): $bundleSkills"
    }

    Write-Host ""
    Write-Host "Skills:"
    foreach ($source in $Registry.sources) {
        Write-Host "  [$($source.label)]"
        foreach ($entry in $source.skills) {
            Write-Host "    - $($entry.name)"
        }
    }
}

$repoRoot = Get-RepoRoot
$registry = Get-Registry -RepoRoot $repoRoot

if ($List) {
    Show-Registry -Registry $registry
    return
}

$selectedSkills = Get-SelectedSkills -Registry $registry -RequestedSkills $Skill -RequestedBundle $Bundle
$targetRoot = if ($Destination) { $Destination } else { Get-DefaultDestination }

New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null

foreach ($entry in $selectedSkills) {
    $sourcePath = Join-Path $repoRoot $entry.sourcePath
    $targetPath = Join-Path $targetRoot $entry.installName

    if (-not (Test-Path $sourcePath)) {
        throw "Missing source skill directory: $sourcePath"
    }

    if (Test-Path $targetPath) {
        if (-not $Force) {
            throw "Destination already exists: $targetPath. Re-run with -Force to overwrite."
        }

        Remove-Item -Recurse -Force $targetPath
    }

    Copy-Item -Path $sourcePath -Destination $targetPath -Recurse
    Expand-RelativeEntryPlaceholders -SourcePath $sourcePath -TargetPath $targetPath
    Write-Host "Installed $($entry.name) -> $targetPath"
}

Write-Host ""
Write-Host "Restart Codex to pick up new skills."

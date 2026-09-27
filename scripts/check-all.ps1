$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Invoke-ProjectCheck {
    param(
        [string]$Name,
        [string]$Directory,
        [scriptblock]$Command
    )

    Write-Host "==> $Name"
    Push-Location (Join-Path $root $Directory)
    try {
        & $Command
        if ($LASTEXITCODE -ne 0) {
            throw "$Name failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Invoke-ProjectCheck 'Backend formatting' 'back-end' { .\mvnw.cmd spotless:check }
Invoke-ProjectCheck 'Backend tests' 'back-end' { .\mvnw.cmd test }
Invoke-ProjectCheck 'Admin frontend' 'admin-front-end' { npm.cmd run check }
Invoke-ProjectCheck 'Official website' 'official-website' { npm.cmd run check }
Invoke-ProjectCheck 'User mini program' 'user-front-end' { npm.cmd run check }
Invoke-ProjectCheck 'Worker app' 'worker-front-end' { npm.cmd run check }

Write-Host 'All project checks passed.'

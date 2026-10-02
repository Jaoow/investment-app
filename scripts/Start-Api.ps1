$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root '.env.local'))) {
    throw 'Missing .env.local. Run .\scripts\Setup-Local.ps1 first.'
}

Push-Location $root
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "API exited with code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

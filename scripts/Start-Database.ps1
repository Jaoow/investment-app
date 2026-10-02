$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

& wsl.exe --cd $root --exec docker compose -f compose.local.yml up -d --wait postgres
if ($LASTEXITCODE -ne 0) {
    throw "Database startup failed with code $LASTEXITCODE."
}

Write-Host 'PostgreSQL is ready on localhost:5433. Keep this terminal open to keep WSL running.'
Write-Host 'Use Ctrl+C to release this terminal; use the documented Compose stop command to stop the database.'
& wsl.exe --exec sleep infinity
if ($LASTEXITCODE -ne 0) {
    throw "WSL keep-alive exited with code $LASTEXITCODE."
}

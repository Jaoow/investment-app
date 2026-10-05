$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    $nodeDirectory = Join-Path $env:ProgramFiles 'nodejs'
    if (-not (Test-Path (Join-Path $nodeDirectory 'node.exe'))) {
        throw 'Node.js is not available. Install Node.js or add it to PATH.'
    }
    $env:Path = "$nodeDirectory;$env:Path"
}

if (-not (Test-Path (Join-Path $root '.env.local'))) {
    throw 'Missing .env.local. Copy .env.example to .env.local and set API_PROXY_TARGET if needed.'
}
if (-not (Test-Path (Join-Path $root 'node_modules'))) {
    throw 'Frontend dependencies are missing. Run npm ci in the frontend repository.'
}

Push-Location $root
try {
    & npm.cmd run dev -- --host 127.0.0.1 --strictPort
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend exited with code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$apiEnv = Join-Path $root '.env.local'
$utf8 = New-Object System.Text.UTF8Encoding($false)

if (-not (Test-Path $apiEnv)) {
    $bytes = New-Object byte[] 64
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($bytes)
    } finally {
        $random.Dispose()
    }
    $template = [System.IO.File]::ReadAllText((Join-Path $root '.env.example'))
    $template = $template.Replace('GENERATE_WITH_SETUP_LOCAL', [Convert]::ToBase64String($bytes))
    [System.IO.File]::WriteAllText($apiEnv, $template, $utf8)
    Write-Host 'Created API .env.local with a random JWT key.'
} else {
    Write-Host 'Preserved existing API .env.local.'
}

if (-not (Select-String -Path $apiEnv -Pattern '^BRAPI_API_TOKEN=.+$' -Quiet)) {
    Write-Warning 'BRAPI_API_TOKEN is empty. Add your token to the root .env.local before testing market data. Authentication and portfolio CRUD can run without it.'
}

Write-Host 'Environment ready. Run .\scripts\Start-Database.ps1 in one terminal.'
Write-Host 'Wait for PostgreSQL to report ready, then run .\scripts\Start-Api.ps1 in another terminal.'
Write-Host 'Configure the separately maintained frontend to use http://localhost:8086.'

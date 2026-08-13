$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

Write-Host "[1/3] Backend: test + checkstyle"
Push-Location "$Root/backend"
mvn verify
Pop-Location

Write-Host "[2/3] Frontend: lint + typecheck + build"
Push-Location "$Root/frontend"
npm install --no-audit --no-fund
npm run check
Pop-Location

Write-Host "[3/3] Docker Compose config"
Push-Location $Root
docker compose config | Out-Null
Pop-Location

Write-Host "CineVerse V1 local verification passed."

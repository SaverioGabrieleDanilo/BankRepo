# Carica .env come variabili d'ambiente della sessione corrente e avvia l'app.
# Uso: ./run-local.ps1
# (le variabili d'ambiente PowerShell non sopravvivono alla chiusura del terminale:
# questo script va rilanciato ogni volta che apri una finestra nuova)

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error "Non trovo .env in $PSScriptRoot - copialo da .env.example e valorizzalo."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"')
        Set-Item -Path "Env:$name" -Value $value
    }
}

Write-Host "Variabili caricate da .env. Avvio l'app..." -ForegroundColor Green
& "$PSScriptRoot\mvnw.cmd" spring-boot:run

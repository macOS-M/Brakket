# Smoke test de la integración con Twitch Helix (EPIC-10 / RF-35).
#
# Verifica, ANTES de construir nada encima, que las credenciales del .env
# obtienen un App Access Token (client_credentials) y que Helix responde
# para el canal oficial de Brakket:
#   1. POST https://id.twitch.tv/oauth2/token
#   2. GET  /helix/users?login=<canal>
#   3. GET  /helix/streams?user_login=<canal>
#
# Uso:  powershell -File scripts\twitch-smoke.ps1
# Lee el .env de la raíz del repo. NUNCA imprime el secret ni el token.

$ErrorActionPreference = 'Stop'

$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: no existe $envFile (copiar de .env.example y completar)." -ForegroundColor Red
    exit 1
}

# Parseo mínimo de .env: líneas KEY=VALOR, ignora comentarios y vacías.
$vars = @{}
foreach ($linea in Get-Content $envFile) {
    if ($linea -match '^\s*#' -or $linea -notmatch '=') { continue }
    $idx = $linea.IndexOf('=')
    $vars[$linea.Substring(0, $idx).Trim()] = $linea.Substring($idx + 1).Trim()
}

$faltantes = @('TWITCH_CLIENT_ID', 'TWITCH_CLIENT_SECRET', 'TWITCH_CHANNEL') |
    Where-Object { -not $vars[$_] }
if ($faltantes) {
    Write-Host "ERROR: faltan variables en .env: $($faltantes -join ', ')" -ForegroundColor Red
    exit 1
}

$clientId = $vars['TWITCH_CLIENT_ID']
$canal = $vars['TWITCH_CHANNEL'].ToLower()
Write-Host "Canal a consultar: $canal"
Write-Host "Client ID: $($clientId.Substring(0, 4))... (resto oculto)"

# --- 1. App Access Token (secret en el body, jamás en la URL ni en la salida) ---
try {
    $token = Invoke-RestMethod -Method Post -Uri 'https://id.twitch.tv/oauth2/token' -Body @{
        client_id     = $clientId
        client_secret = $vars['TWITCH_CLIENT_SECRET']
        grant_type    = 'client_credentials'
    }
} catch {
    Write-Host "FALLO al obtener el App Access Token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host "1) Token OK (tipo: $($token.token_type), expira en $([math]::Round($token.expires_in / 86400)) días)" -ForegroundColor Green

$headers = @{
    'Client-Id'     = $clientId
    'Authorization' = "Bearer $($token.access_token)"
}

# --- 2. GET /helix/users ---
try {
    $usuarios = Invoke-RestMethod -Uri "https://api.twitch.tv/helix/users?login=$canal" -Headers $headers
} catch {
    Write-Host "FALLO en GET /helix/users: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
if (-not $usuarios.data) {
    Write-Host "2) GET /helix/users OK pero el canal '$canal' NO existe en Twitch." -ForegroundColor Yellow
    exit 1
}
$u = $usuarios.data[0]
Write-Host "2) Canal encontrado: $($u.display_name) (login: $($u.login), id: $($u.id))" -ForegroundColor Green

# --- 3. GET /helix/streams ---
try {
    $streams = Invoke-RestMethod -Uri "https://api.twitch.tv/helix/streams?user_login=$canal" -Headers $headers
} catch {
    Write-Host "FALLO en GET /helix/streams: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
if ($streams.data) {
    $s = $streams.data[0]
    Write-Host "3) EN VIVO: `"$($s.title)`" | $($s.viewer_count) espectadores | categoría: $($s.game_name) | desde: $($s.started_at)" -ForegroundColor Green
} else {
    Write-Host "3) GET /helix/streams OK: el canal está OFFLINE (respuesta valida con data vacía)." -ForegroundColor Green
}

Write-Host ''
Write-Host 'Smoke test COMPLETO: credenciales y Helix responden correctamente.' -ForegroundColor Green

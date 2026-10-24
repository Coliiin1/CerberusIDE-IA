param(
    [int]$Puerto = 8000,
    [string]$Cloudflared = $env:CLOUDFLARED
)

$ErrorActionPreference = "Stop"

$ApiDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CerberusDir = Split-Path -Parent $ApiDir
$Python = Join-Path $CerberusDir ".venv\Scripts\python.exe"
$KeyFile = Join-Path $ApiDir "api_key.txt"
$UrlFile = Join-Path $ApiDir "url_tunel.txt"

if (-not (Test-Path -LiteralPath $Python)) {
    $Python = (Get-Command python -ErrorAction SilentlyContinue).Source
    if (-not $Python) {
        throw "No se encontro Python. Crea el venv en CerberusAI/.venv o instala Python."
    }
}

if (-not $Cloudflared) {
    $comando = Get-Command cloudflared -ErrorAction SilentlyContinue
    if (-not $comando) {
        throw "cloudflared no esta instalado. Instalalo con: winget install --id Cloudflare.cloudflared -e"
    }
    $Cloudflared = $comando.Source
}

if ([string]::IsNullOrWhiteSpace($env:CERBERUSAI_API_KEY)) {
    if (Test-Path -LiteralPath $KeyFile) {
        $env:CERBERUSAI_API_KEY = (Get-Content -LiteralPath $KeyFile -Raw).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($env:CERBERUSAI_API_KEY)) {
        $bytes = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        $rng.GetBytes($bytes)
        $env:CERBERUSAI_API_KEY = -join ($bytes | ForEach-Object { $_.ToString("x2") })
        Set-Content -LiteralPath $KeyFile -Value $env:CERBERUSAI_API_KEY -Encoding ASCII -NoNewline
    }
}

$BaseUrl = "http://127.0.0.1:$Puerto"
$ApiProcess = $null
$TunnelProcess = $null
$ApiIniciada = $false
$ApiOut = Join-Path $env:TEMP "cerberusai-api-out.log"
$ApiErr = Join-Path $env:TEMP "cerberusai-api-err.log"
$TunnelOut = Join-Path $env:TEMP "cerberusai-tunel-out.log"
$TunnelErr = Join-Path $env:TEMP "cerberusai-tunel-err.log"

function Test-Api {
    try {
        $respuesta = Invoke-WebRequest -Uri "$BaseUrl/health" -UseBasicParsing -TimeoutSec 2 `
            -Headers @{ "X-API-Key" = $env:CERBERUSAI_API_KEY }
        return $respuesta.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Leer-UrlTunel {
    foreach ($archivo in @($TunnelOut, $TunnelErr)) {
        if (Test-Path -LiteralPath $archivo) {
            $texto = Get-Content -LiteralPath $archivo -Raw -ErrorAction SilentlyContinue
            if ($texto) {
                $match = [regex]::Match($texto, "https://[a-z0-9-]+\.trycloudflare\.com")
                if ($match.Success) {
                    return $match.Value
                }
            }
        }
    }
    return $null
}

try {
    if (-not (Test-Api)) {
        Remove-Item -LiteralPath $ApiOut, $ApiErr -ErrorAction SilentlyContinue
        $ApiProcess = Start-Process -FilePath $Python `
            -ArgumentList @("-m", "uvicorn", "main:app", "--host", "127.0.0.1", "--port", "$Puerto") `
            -WorkingDirectory $ApiDir -RedirectStandardOutput $ApiOut -RedirectStandardError $ApiErr `
            -WindowStyle Hidden -PassThru
        $ApiIniciada = $true

        $lista = $false
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Milliseconds 500
            if ($ApiProcess.HasExited) {
                $detalle = Get-Content -LiteralPath $ApiErr -Raw -ErrorAction SilentlyContinue
                throw "La API termino al arrancar. Detalle: $detalle"
            }
            if (Test-Api) {
                $lista = $true
                break
            }
        }
        if (-not $lista) {
            throw "La API no respondio en $BaseUrl. Revisa $ApiErr"
        }
    }

    Write-Host "API local lista en $BaseUrl"
    Remove-Item -LiteralPath $TunnelOut, $TunnelErr -ErrorAction SilentlyContinue
    $TunnelProcess = Start-Process -FilePath $Cloudflared `
        -ArgumentList @("tunnel", "--url", $BaseUrl, "--no-autoupdate") `
        -RedirectStandardOutput $TunnelOut -RedirectStandardError $TunnelErr `
        -WindowStyle Hidden -PassThru

    $TunnelUrl = $null
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 500
        if ($TunnelProcess.HasExited) {
            $detalle = Get-Content -LiteralPath $TunnelErr -Raw -ErrorAction SilentlyContinue
            throw "cloudflared termino al arrancar. Detalle: $detalle"
        }
        $TunnelUrl = Leer-UrlTunel
        if ($TunnelUrl) {
            break
        }
    }
    if (-not $TunnelUrl) {
        throw "No se pudo obtener la URL del tunel. Revisa $TunnelErr"
    }

    Set-Content -LiteralPath $UrlFile -Value $TunnelUrl -Encoding ASCII -NoNewline

    Write-Host ""
    Write-Host "CerberusAI accesible desde cualquier red:"
    Write-Host "  URL:     $TunnelUrl"
    Write-Host "  API Key: $($env:CERBERUSAI_API_KEY)"
    Write-Host ""
    Write-Host "En el equipo remoto configura:"
    Write-Host "  CERBERUSAI_API_URL=$TunnelUrl"
    Write-Host "  CERBERUSAI_API_KEY=$($env:CERBERUSAI_API_KEY)"
    Write-Host ""
    Write-Host "La clave se guardo en $KeyFile y la URL en $UrlFile."
    Write-Host "No compartas la API Key. Deja esta ventana abierta."
    Write-Host "Pulsa Ctrl+C para detener la API y el tunel."

    while ($true) {
        Start-Sleep -Seconds 2
        if ($TunnelProcess.HasExited) {
            throw "cloudflared se detuvo inesperadamente. Revisa $TunnelErr"
        }
    }
} finally {
    if ($TunnelProcess -and -not $TunnelProcess.HasExited) {
        Stop-Process -Id $TunnelProcess.Id -Force -ErrorAction SilentlyContinue
    }
    if ($ApiIniciada -and $ApiProcess -and -not $ApiProcess.HasExited) {
        Stop-Process -Id $ApiProcess.Id -Force -ErrorAction SilentlyContinue
    }
}

# CerberusAI — API (Fase 5)

API mínima (FastAPI) que expone un chat y reenvía las peticiones a **Ollama** con el modelo
especializado en Cerberus. Para los roles de código (`generar`/`corregir`) valida el resultado
con el **oráculo** del compilador (`audit.CompiladorCLI`).

La API corre en la PC local y puede publicarse por un **Cloudflare quick tunnel**, protegida con
`X-API-Key` y un límite de peticiones por IP.

## Requisitos

- El compilador compilado (`mvnw.cmd clean compile` → `target/classes`) para la validación.
- Ollama con el modelo (`ollama create cerberusai -f ../finetune/Modelfile`).
- Python con las dependencias de `requirements-api.txt`.
- `cloudflared` instalado para el acceso remoto:
  `winget install --id Cloudflare.cloudflared -e`.

## Instalación y arranque local

```powershell
cd CerberusAI\api
..\.venv\Scripts\python.exe -m pip install -r requirements-api.txt
$env:CERBERUSAI_API_KEY = "una-clave-larga-y-aleatoria"
..\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Si `CERBERUSAI_API_KEY` no está definida, la API queda abierta. Eso solo es aceptable para uso
local; el script de túnel siempre genera o reutiliza una clave.

## Acceso remoto desde cualquier red (recomendado)

```cmd
cd CerberusAI\api
tunel_rapido.cmd
```

`tunel_rapido.cmd` / `tunel_rapido.ps1`:

1. Reutiliza `CERBERUSAI_API_KEY` o genera una clave aleatoria en `api_key.txt` (gitignored).
2. Arranca la API en `127.0.0.1:8000` si todavía no está activa.
3. Lanza `cloudflared tunnel --url http://127.0.0.1:8000`.
4. Imprime la URL `https://...trycloudflare.com` y la guarda en `url_tunel.txt` (gitignored).
5. Mantiene la API y el túnel activos hasta pulsar `Ctrl+C`.

La URL cambia en cada arranque. En el equipo remoto configura:

```powershell
$env:CERBERUSAI_API_URL = "https://...trycloudflare.com"
$env:CERBERUSAI_API_KEY = "la-clave-mostrada-por-el-script"
```

El cliente Java (`cliente.ClienteCerberusAI`) lee ambas variables. No se abre ningún puerto en
el router: el túnel sale desde la PC hacia Cloudflare.

Prueba remota:

```powershell
curl.exe -H "X-API-Key: la-clave" https://...trycloudflare.com/health
```

## Endpoints

- `GET /health` → `{"status":"ok","modelo":"cerberusai","ollama":"http://localhost:11434"}`
  (requiere `X-API-Key` si la clave está configurada).
- `POST /chat` (requiere `X-API-Key` si la clave está configurada):

```json
// peticion
{ "role": "generar", "prompt": "imprime los numeros del 1 al 5 con un para" }

// respuesta
{
  "texto": "clase Programa(){ ... }",
  "es_codigo": true,
  "codigo": "clase Programa(){ ... }",
  "compila": true,
  "error_compilacion": null
}
```

`role` puede ser `generar`, `explicar` o `corregir`. `compila` solo se calcula para
`generar`/`corregir`.

## Autenticación y límite de peticiones

- Si `CERBERUSAI_API_KEY` está definida, todas las peticiones deben enviar
  `X-API-Key: <clave>`. Una clave ausente o incorrecta devuelve `401`.
- El límite por IP es de 20 peticiones cada 60 segundos por defecto. Al superarlo se devuelve
  `429` con la cabecera `Retry-After`.
- Detrás del túnel se usa `CF-Connecting-IP` (y `X-Forwarded-For` como respaldo) para identificar
  al cliente.
- `CERBERUSAI_RATE_LIMIT=0` desactiva el límite.

## Acceso desde la red local (alternativa sin túnel)

Por defecto la API escucha solo en `127.0.0.1`. Para probarla desde otra PC de la misma red,
arráncala en todas las interfaces y define la clave:

```powershell
$env:CERBERUSAI_API_KEY = "una-clave-larga-y-aleatoria"
..\.venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000
```

Abre el puerto una vez (PowerShell **como administrador**):

```powershell
New-NetFirewallRule -DisplayName "CerberusAI API 8000" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8000 -Profile Private
```

Desde la otra PC: `http://<IP-del-host>:8000/health` con `X-API-Key`. Este modo solo funciona
dentro de la misma red; para cualquier red usa el túnel.

## Configuración (variables de entorno)

| Variable | Default | Descripción |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL de Ollama. |
| `MODEL` | `cerberusai` | Modelo de Ollama. |
| `SYSTEM_PROMPT_PATH` | `../finetune/system_prompt.txt` | Prompt de sistema. |
| `CLASSPATH` | `../../target/classes` | Classpath del oráculo. |
| `JAVA` | `java` | Ejecutable de Java. |
| `COMPILER` | `audit.CompiladorCLI` | Clase del oráculo. |
| `TIMEOUT` | `120` | Timeout (s) de la llamada a Ollama. |
| `CERBERUSAI_API_KEY` | vacío | Clave exigida en `X-API-Key`; vacío = sin autenticación. |
| `CERBERUSAI_RATE_LIMIT` | `20` | Peticiones permitidas por IP y ventana; `0` desactiva. |
| `CERBERUSAI_RATE_WINDOW` | `60` | Ventana del límite, en segundos. |
| `CLOUDFLARED` | `cloudflared` del PATH | Ruta alternativa al ejecutable del túnel. |

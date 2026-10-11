# CerberusAI — API (Fase 5)

API mínima (FastAPI) que expone un chat y reenvía las peticiones a **Ollama** con el modelo
especializado en Cerberus. Para los roles de código (`generar`/`corregir`) valida el resultado
con el **oráculo** del compilador (`audit.CompiladorCLI`).

## Requisitos

- El compilador compilado (`mvnw.cmd clean compile` → `target/classes`) para la validación.
- Ollama con el modelo (`ollama create cerberusai -f ../finetune/Modelfile`).

## Instalación y arranque

```bash
cd CerberusAI/api
pip install -r requirements-api.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

## Endpoints

- `GET /health` → `{"status":"ok","modelo":"cerberusai","ollama":"http://localhost:11434"}`
- `POST /chat`:

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

## Acceso remoto (Cloudflare quick tunnel)

```bash
cloudflared tunnel --url http://localhost:8000
```

Imprime una URL `https://...trycloudflare.com` (cambia en cada arranque). El chat del IDE (Fase 6)
apuntará a `http://localhost:8000` (local) o a esa URL (remoto).

## Acceso desde la red local (pruebas)

Por defecto la API escucha solo en `127.0.0.1`. Para probarla desde otra PC de la misma red,
arráncala en todas las interfaces:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

y abre el puerto una vez (PowerShell **como administrador**):

```powershell
New-NetFirewallRule -DisplayName "CerberusAI API 8000" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8000 -Profile Private
```

Desde la otra PC: `http://<IP-del-host>:8000/health` (y apunta el chat del IDE a esa URL).
La API no tiene autenticación: úsala solo en una red de confianza.

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

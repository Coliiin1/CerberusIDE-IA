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

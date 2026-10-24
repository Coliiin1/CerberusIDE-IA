# CerberusAI — Módulo de Inteligencia Artificial

Pipeline de IA del lenguaje **Cerberus**: dataset validado contra el compilador, fine-tuning
QLoRA de un modelo local, evaluación automática, una API (FastAPI + Ollama) y un componente
de chat para el IDE.

```
[Chat del IDE (JavaFX) / cliente remoto]
        │  HTTP POST /chat
        ▼
[API FastAPI (Windows)]  ◄── quick tunnel Cloudflare (HTTPS) ── [cliente remoto]
        │  API de Ollama (localhost:11434)
        ▼
[Ollama] ── modelo `cerberusai` (Qwen2.5-Coder-7B + LoRA → GGUF Q8_0)
```

- El modelo (GPU) vive siempre en la máquina local; solo se expone la API.
- El oráculo (`audit.CompiladorCLI`) no forma parte del runtime: se usa para validar código y evaluar.
- Plan completo en [`../CERBERUSIA_IMPLEMENTATION.md`](../CERBERUSIA_IMPLEMENTATION.md); bitácora en [`../RUTA.md`](../RUTA.md).

## Estructura

| Carpeta | Contenido |
| :--- | :--- |
| `dataset/` | `cerberus_dataset_v1.jsonl` (original, con entradas inválidas) y `cerberus_dataset_v2.jsonl` (validado; 93 entradas, 45 `generation` que compilan) |
| `knowledge/` | `language_spec.md` — especificación del lenguaje reconciliada con el compilador real |
| `finetune/` | Fases 2–3: `to_chat.py`, `split.py`, `train_qlora.py` (QLoRA 4-bit), `export_gguf.py`, `Modelfile`, `system_prompt.txt` (ver su [README](finetune/README.md)) |
| `eval/` | Fase 4: `eval_harness.py` (genera vía Ollama, extrae código y valida con el oráculo) y `run_eval.py` (base vs fine-tuned); resultados en `resultados_base_vs_ft.txt` |
| `api/` | Fase 5: `main.py` (FastAPI `GET /health`, `POST /chat`), `requirements-api.txt` (ver su [README](api/README.md)) |

## Estado

| Fase | Entregable | Estado |
| :--- | :--- | :--- |
| 1. Oráculo | `audit/CompiladorCLI` + `audit/Evaluador` | HECHO |
| 2. Datos | `train.jsonl` (74) / `eval.jsonl` (19), seed 42 | HECHO |
| 3. Fine-tuning | Adapter LoRA (loss 0.404, 5 epochs) → GGUF Q8_0 → Ollama `cerberusai` | HECHO |
| 4. Evaluación | `qwen2.5-coder:7b` 44% → `cerberusai` **89%** compilación (exact-match 33%) | HECHO |
| 5. API | FastAPI verificada en vivo (`/health` y `/chat` con `compila: true`) + acceso remoto con `X-API-Key` y rate limit por IP | HECHO |
| 6. Chat IDE | Chat integrado en `ui/principal.fxml` con `ui/ChatCerberusController`; URL de la API en `Opciones → Configurar API de CerberusAI...`; `main.PreviewChat` como lanzador dev | HECHO |
| 7. Expansión | Más generación y corrección, re-entrenar, dominio estable | FUTURO |

Artefactos pesados (`lora-adapter/`, `merged-model/`, `*.gguf`, `*.log`) están en `.gitignore`;
el modelo queda registrado en Ollama como `cerberusai`.

## Comandos rápidos (Windows)

```cmd
REM Fase 2 — datos
py finetune\to_chat.py --input dataset\cerberus_dataset_v2.jsonl --output finetune\chat_all.jsonl
py finetune\split.py --input finetune\chat_all.jsonl --out-dir finetune --eval-ratio 0.2 --seed 42

REM Fase 4 — evaluar base vs fine-tuned (requiere Ollama con ambos modelos)
py eval\run_eval.py --base-model qwen2.5-coder:7b

REM Fase 5 — API local (la clave es opcional en local; obligatoria para el túnel)
cd api
..\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

```cmd
REM Exposición remota: arranca la API, genera/reutiliza api_key.txt y abre el túnel
cd CerberusAI\api
tunel_rapido.cmd
```

El script imprime la URL temporal `https://...trycloudflare.com` y la clave. El chat del IDE
apunta a `http://localhost:8000` por defecto y la URL del túnel se configura en
**Opciones → Configurar API de CerberusAI...** (se guarda en `Preferences`, sin campo visible en
el chat); `CERBERUSAI_API_URL` queda como fallback y la clave se toma de `CERBERUSAI_API_KEY`.
La API exige `X-API-Key`, limita las peticiones por IP y usa `CF-Connecting-IP` detrás del túnel.
`api_key.txt`, `url_tunel.txt` y los logs están en `.gitignore`.

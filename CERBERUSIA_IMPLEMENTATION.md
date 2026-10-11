# CERBERUSIA_IMPLEMENTATION.md — Plan de implementación de CerberusAI

## Objetivo
Implementar el módulo de IA completo (pasos 10–12 de `RUTA.md`): un modelo local
especializado en Cerberus (fine-tuning QLoRA + Ollama), una API (FastAPI) y un chat
integrado en el IDE (JavaFX), con acceso remoto vía túnel.

## Decisiones cerradas
- Modelo base: **Qwen2.5-Coder-7B-Instruct**.
- Fine-tuning: **QLoRA 4-bit**, en **WSL2** (Linux + CUDA) sobre RTX 3060 12 GB.
- Inferencia/servicio: **Ollama** (HTTP), modelo exportado a **GGUF**.
- API: **FastAPI** (Python), corre local en la máquina GPU.
- Acceso remoto: **Cloudflare quick tunnel** (URL temporal `trycloudflare.com`, sin dominio).
  El dominio/URL estable se aborda en la Fase 7.
- Chat: 3 roles (**generar / explicar / corregir**) + **pegar código al editor** desde v1.
- Dataset: `cerberus_dataset_v2.jsonl` (93 entradas) como base; ampliable en la Fase 7.

## Arquitectura (runtime)
```
[Chat IDE (JavaFX)]
      │ HTTP POST /chat
      ▼
[API FastAPI (local)] ◄── Cloudflare quick tunnel (HTTPS) ── [cliente remoto]
      │ API de Ollama (HTTP localhost:11434)
      ▼
[Ollama] ── modelo fine-tuned (Qwen2.5-Coder-7B + LoRA → GGUF)
```
- El modelo (GPU) vive siempre en la máquina local; el túnel solo expone la API.
- `CompiladorCLI` (oráculo) no forma parte del runtime: se usa en la evaluación.

## Fases y entregables

### Fase 0 — Entorno (WSL2 + Ollama)
- En WSL2: Python venv + PyTorch (CUDA) + `bitsandbytes` + PEFT + TRL.
- Instalar Ollama y descargar `qwen2.5-coder:7b-instruct`.

### Fase 1 — Oráculo del compilador (Java)
- Refactor: extraer un evaluador headless compartido (hoy la lógica está duplicada en
  `CompilerProbe.evaluar()` y `TestSuite.evaluar()`).
- `src/main/java/audit/CompiladorCLI.java`: lee archivo (o stdin), ejecuta lexer + parser,
  y emite por stdout
  `{"aceptado": bool, "error": "...", "linea": N, "columna": N, "lexema": "..."}`.
- Exit code `0` (acepta) / `1` (rechaza).
- Verificación: `mvnw.cmd compile` + `CompiladorCLI` sobre un `.txt` válido y otro inválido.

### Fase 2 — Preparación de datos (Python, `CerberusAI/finetune/`)
- `split.py`: partición train/eval **reproducible** (semilla fija + ratio 80/20).
- `to_chat.py`: convierte `.jsonl` al formato de chat de Qwen2.5 (estilo ChatML) con
  `system` = resumen de `language_spec.md` que describe los 3 roles (generar/explicar/corregir).
- Salidas: `train.jsonl`, `eval.jsonl`.

### Fase 3 — Fine-tuning QLoRA (en WSL2)
- `train_qlora.py`: `bitsandbytes` 4-bit + PEFT/LoRA + TRL `SFTTrainer` sobre
  `Qwen/Qwen2.5-Coder-7B-Instruct`.
- `export_gguf.py`: **merge_and_unload** del adapter LoRA → `convert_hf_to_gguf.py` → GGUF.
- `Modelfile` para `ollama create`, más `requirements-finetune.txt` y `README.md` con los
  comandos para la RTX 3060.

### Fase 4 — Harness de evaluación (Python, `CerberusAI/eval/`)
- `eval_harness.py`: genera código vía Ollama HTTP (`OLLAMA_BASE_URL` configurable), extrae
  el código (quita *fences* de markdown) y lo valida contra `CompiladorCLI`.
- Métricas: **tasa de compilación** (primaria), exact-match (secundaria) y desglose de errores.
- `run_eval.py`: compara modelo base vs fine-tuned.

### Fase 5 — API (FastAPI, local)
- `CerberusAI/api/main.py`: `POST /chat` con `{role: generar|explicar|corregir, prompt}` →
  llama a Ollama → devuelve `{texto}` (y marca si contiene código insertable).
- Exponer con **Cloudflare quick tunnel**:
  `cloudflared tunnel --url http://localhost:8000` (la URL cambia en cada arranque).

### Fase 6 — Chat en el IDE (JavaFX)
- Panel de chat en `ui/principal.fxml` + su controlador.
- Envía `POST /chat` a la API y muestra la respuesta.
- Botón **"Insertar en editor"** que pega el código generado en `txtCodigo`.

### Fase 7 — Expansión (futuro)
- Ampliar el dataset (más generación + ejemplos de **corrección** de código roto).
- Re-entrenar y re-evaluar.
- Migrar del quick tunnel a un túnel con **dominio estable** en Cloudflare.

## Secuencia y dependencias
`0 → 1 → 2 → 3 → 4 → 5 → 6 → 7`.
La Fase 1 (oráculo) es prerequisito de la Fase 4 (evaluación); las Fases 3 y 4 dependen del
modelo definido en las decisiones.

## Verificación
- Fase 1: `CompiladorCLI` sobre muestras válidas e inválidas.
- Fase 2: `split.py` / `to_chat.py` sobre `cerberus_dataset_v2.jsonl`.
- Fase 4: `eval_harness.py` contra Ollama (base y fine-tuned).
- Fases 5–6: `curl` a la API y prueba manual del chat en el IDE.

## Riesgos
- `bitsandbytes` requiere WSL2 (ya instalado).
- Dataset pequeño (93 entradas) → prueba de concepto; ampliar en la Fase 7.
- La métrica automática de "explicar/corregir" es limitada (el compilador no ejecuta código);
  se prioriza la tasa de compilación para las tareas de código.
- El quick tunnel no es persistente: la URL cambia en cada arranque.

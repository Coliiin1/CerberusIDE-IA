# GColabRoute.md — Entrenamiento de CerberusAI en Google Colab

## Objetivo
Re-entrenar `Qwen/Qwen2.5-Coder-7B-Instruct` con QLoRA 4-bit en Google Colab Pro
(L4/A100) usando los datasets ampliados y **balanceados**, y exportar el adapter a
GGUF/Ollama en local. El resto del pipeline (API, chat del IDE, compilador,
evaluación) no cambia.

## Decisiones
- Colab **Pro** (L4 24 GB / A100 40 GB) → `bf16` (auto-dtype).
- Merge + cuantización GGUF **en local** (reusa `export_gguf.py`, ~28 GB RAM).
- Datos de entrenamiento: **v2 + v3 + v4_balance**.
- Repo **público** → `git clone` sin token (`GITHUB_TOKEN` no necesario).
- Hiperparámetros **idénticos** a la corrida actual en la corrida 1.
- Entregable: notebook `.ipynb` + ajustes a scripts + documentación.
- Distribución del balanceo: **A (uniforme) — 125 nuevos por categoría = 500**.

## Fase A — Balanceo de datos (PRIMERO)
> **Estado: HECHO.** `py CerberusAI\dataset\generar_datasets.py --only balance` →
> `cerberus_dataset_v4_balance.jsonl` con 500 entradas (125 por categoría). Verificado:
> instrucciones únicas, sin acentos, mensajes de error tomados del oráculo.

Generar `CerberusAI/dataset/cerberus_dataset_v4_balance.jsonl` (500 entradas nuevas)
extendiendo `generar_datasets.py` con plantillas para las categorías minoritarias:

| Categoría | Actual (v2) | Nuevas | Total |
|---|---|---|---|
| explanation | 14 | 125 | 139 |
| syntax (Q&A) | 12 | 125 | 137 |
| errors_semantics | 14 | 125 | 139 |
| compiler_concepts | 8 | 125 | 133 |
| **Total nuevas** | | **500** | |

- `explanation` / `compiler_concepts`: texto (no pasan por el oráculo); calidad vía plantillas.
- `syntax` / `errors_semantics`: validar los mensajes contra `audit.CompiladorCLI`
  (que el mensaje citado coincida con el real).
- Formato JSONL: `{instruction, response, category, source}`; `source = "v4_balance_v1"`.
- Verificación: sin duplicados, sin entradas vacías, mensajes de error fieles.

## Fase B — Preparación de datos de entrenamiento
```
python finetune/to_chat.py --input dataset/cerberus_dataset_v2.jsonl \
    dataset/cerberus_dataset_v3_valido.jsonl \
    dataset/cerberus_dataset_v3_errores_semanticos.jsonl \
    dataset/cerberus_dataset_v3_errores_sintacticos.jsonl \
    dataset/cerberus_dataset_v4_balance.jsonl \
    --output finetune/chat_all.jsonl
python finetune/split.py --input finetune/chat_all.jsonl --out-dir finetune \
    --eval-ratio 0.2 --seed 42
python finetune/train_qlora.py --check
```
Esperado: ~1594 ejemplos (v2 93 + v3 1001 + v4 500) → train ~1275 / eval ~319.

## Fase C — Entrenamiento en Colab Pro
> **Estado: preparado.** Notebook `CerberusAI/colab/train_cerberus_colab.ipynb` +
> `requirements-colab.txt`; `train_qlora.py --dtype auto` y `to_chat.py` multi-input ya
> implementados. Falta ejecutarlo en Colab y descargar el adapter.

Notebook `CerberusAI/colab/train_cerberus_colab.ipynb`:
1. Verificar GPU (`nvidia-smi`, `torch.cuda.get_device_name`, `is_bf16_supported()`).
2. Montar Drive (`/content/drive/MyDrive/CerberusAI`).
3. `git clone -b 01-apiexpuesta https://github.com/Coliiin1/CerberusIDE-IA.git`.
4. `pip install -r CerberusAI/finetune/requirements-finetune.txt` (sin tocar `torch` de Colab).
5. Fase B (datos) + `--check`.
6. Entrenar (salida a Drive):
   `python finetune/train_qlora.py --output <Drive>/lora-adapter --epochs 5 --dtype auto`
   (si OOM: `--batch 1 --grad-accum 8`).
7. Zip + descarga de `lora-adapter.zip`.

## Fase D — Merge / GGUF / Ollama (local)
- Descomprimir en `CerberusAI/finetune/lora-adapter/`.
- `py CerberusAI\finetune\export_gguf.py` → `merged-model`.
- `convert_hf_to_gguf.py` → `llama-quantize Q8_0` → `ollama create cerberusai -f Modelfile`.

## Fase E — Evaluación
- `py CerberusAI\eval\run_eval.py` (base vs fine-tuned) y `eval_harness.py`.
- Comparar tasa de compilación y exact-match con `resultados_base_vs_ft.txt`.

## Cambios de código
- `train_qlora.py`: `--dtype auto|bf16|fp16` (auto = bf16 si soportado, si no fp16).
- `to_chat.py`: `--input` con `nargs="+"` (varios archivos).
- `generar_datasets.py`: nuevas plantillas para v4_balance.
- Nuevos: `CerberusAI/colab/train_cerberus_colab.ipynb`, `CerberusAI/colab/requirements-colab.txt`.

## Documentación a actualizar
- `AGENTS.md` (flujo Colab, `--dtype`, `to_chat` multi-input, `colab/`, v4_balance).
- `RUTA.md` (paso: balanceo + entrenamiento en Colab).
- `CerberusAI/finetune/README.md` (sección Colab).
- `CERBERUSIA_IMPLEMENTATION.md` (Colab como alternativa a WSL2).

## Verificación
- `train_qlora.py --check` OK; pérdida descendente; adapter con `adapter_model.safetensors`.
- Local: `merged-model`, GGUF Q8_0, `ollama list` con `cerberusai`.
- `run_eval` reporta mejora vs base.

## Riesgos / notas
- Sesión Pro limitada: checkpoints por época a Drive (`save_total_limit=2`).
- Si `pip` intenta sustituir el `torch` de Colab, instalar solo
  `transformers/trl/peft/accelerate/bitsandbytes/datasets`.
- El balanceo con plantillas puede repetir estructuras; vigilar duplicados y
  variedad de redacción.
- Las categorías minoritarias son Q&A de texto: `explanation`/`compiler_concepts`
  no son validables por el oráculo; `syntax`/`errors_semantics` sí.

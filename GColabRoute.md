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

## Fase C — Entrenamiento en Colab Pro (corrida controlada, desde cero)
> **Estado: preparado.** Notebook reescrito; falta ejecutarlo celda por celda en Colab.

Cada corrida se identifica con un `RUN_ID` y vive en `MyDrive/CerberusAI/runs/<RUN_ID>/`:
`run_info.json` (commit, GPU, versiones, hiperparámetros, conteos, sha256 de los 5 datasets y de
train/eval), copia de `train.jsonl`/`eval.jsonl`, `train.log`, y `lora-adapter/` (adapter final =
mejor `eval_loss`, checkpoints por época, `log_history.json`).

Notebook `CerberusAI/colab/train_cerberus_colab.ipynb` (cada celda verifica con `assert`):
0. Parámetros: `RUN_ID`, `RESUME`, `BRANCH = "main"`, HP de la corrida 1.
1. GPU: `nvidia-smi`; se detiene si no hay GPU o si es T4.
2. Drive: corrida nueva → la carpeta no debe existir; `RESUME` → debe existir y no estar terminada.
3. `git clone -b main` e imprime el commit.
4. `pip install -r CerberusAI/colab/requirements-colab.txt` + versiones; verifica que no cambió torch y que hay bf16.
5. Fase B (datos) + `--check`; `assert` total = 1594; escribe `run_info.json` (al reanudar exige sha256 idénticos).
6. Entrenar: `train_qlora.py --output <run>/lora-adapter --dtype auto` + HP (+ `--resume`), salida con `tee` a `train.log`.
7. Tabla train/eval loss por época + mejor checkpoint.
8. Zip (adapter final sin `checkpoint-*` + `run_info.json` + `train.log`) y descarga.

Si la sesión se cae: mismo `RUN_ID`, `RESUME = True`, ejecutar todo de nuevo en orden.

> **Corrida 1 (`v2-20261106-1`): HECHA** en A100 80 GB. Mejor checkpoint `checkpoint-640`
> (época 4, eval_loss 0.0205; la época 5 empieza a sobreajustar: 0.0207).

## Fases D + E — Merge, GGUF y evaluación en Colab (sin PC)
Notebook `CerberusAI/colab/exportar_evaluar_colab.ipynb` (A100, ≥ 40 GB RAM), mismo `RUN_ID`:
1. GPU + RAM; Drive (adapter v2; v1 opcional en `MyDrive/CerberusAI/runs/v1/lora-adapter/`).
2. Clona `main` y llama.cpp (`--depth 1`, sin compilar), instala `requirements-colab.txt` + `gguf-py`.
3. `export_gguf.py` (merge fp16 en CPU) → `convert_hf_to_gguf.py --outtype q8_0` →
   `runs/<RUN_ID>/gguf/cerberusai-q8_0.gguf` + `gguf_info.json` (sha256). v1 se exporta solo al disco de Colab.
4. Ollama en Colab: `cerberusai-v2` (+ `cerberusai-v1`) con el `Modelfile` (FROM absoluto) y `ollama pull` del base.
5. Oráculo: JDK Temurin 25 + `javac` de los paquetes puros (sin Maven) + `audit.TestSuite`.
6. `run_eval.py` base vs v2 (y v1 vs v2) sobre las 104 tareas `generation` de `eval.jsonl` (verificado
   por sha256 contra `run_info.json`) → `runs/<RUN_ID>/eval/resultados_*.txt`.

Nota: v1 pudo ver algunas de esas tareas en su entrenamiento (sesgo leve a favor de v1).

**Único paso en el PC, si v2 gana:** descargar el GGUF desde la web de Drive a `CerberusAI\finetune\`,
verificar el sha256, `ollama cp cerberusai cerberusai-v1` y `ollama create cerberusai -f Modelfile`
(API/IDE sin cambios).

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

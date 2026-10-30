# CerberusAI — Fine-tuning (Fase 3)

Guía para especializar **Qwen2.5-Coder-7B-Instruct** en el lenguaje Cerberus mediante
**QLoRA 4-bit** y servirlo con **Ollama**. Se ejecuta en **WSL2 (Linux + CUDA)** usando la
RTX 3060 12 GB; el merge a GGUF se hace en CPU (32 GB de RAM).

> Requisitos: WSL2 con CUDA, ~30 GB de disco libre, Python 3.10+ y Ollama instalado.

## 1. Datos (Fase 2)

`to_chat.py` acepta **varios** `--input` y los concatena. Para entrenar con v2 + v3 + v4
(1594 ejemplos) desde `CerberusAI/`:

```bash
python to_chat.py --input dataset/cerberus_dataset_v2.jsonl \
    dataset/cerberus_dataset_v3_valido.jsonl \
    dataset/cerberus_dataset_v3_errores_semanticos.jsonl \
    dataset/cerberus_dataset_v3_errores_sintacticos.jsonl \
    dataset/cerberus_dataset_v4_balance.jsonl \
    --output chat_all.jsonl
python split.py --input chat_all.jsonl --out-dir . --eval-ratio 0.2 --seed 42
# -> train.jsonl (1274) y eval.jsonl (320), estratificados y reproducibles (seed 42)
```

Si solo usas `--input ../dataset/cerberus_dataset_v2.jsonl`, el split da 74/19 (corrida original).

## 2. Entorno

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements-finetune.txt
```

## 3. Entrenamiento (QLoRA)

```bash
python train_qlora.py --epochs 5
# Salida: ./lora-adapter
```

Parámetros útiles: `--epochs`, `--lr`, `--batch`, `--grad-accum`, `--lora-r`, `--max-seq-length`.
`--dtype auto` (por defecto) usa `bf16` si la GPU lo soporta y `fp16` si no (p. ej. T4).
Con 74 ejemplos, 5 epochs es un buen punto de partida.

## 4. Exportar a GGUF

```bash
python export_gguf.py
# Salida: ./merged-model  + instrucciones para llama.cpp
```

Luego, siguiendo las instrucciones que imprime el script:

```bash
git clone https://github.com/ggerganov/llama.cpp
pip install -r llama.cpp/requirements.txt
cmake -B llama.cpp/build -S llama.cpp
cmake --build llama.cpp/build --config Release -j
python llama.cpp/convert_hf_to_gguf.py merged-model --outfile cerberusai-f16.gguf --outtype f16
llama.cpp/build/bin/llama-quantize cerberusai-f16.gguf cerberusai-q8_0.gguf Q8_0
```

## 5. Registrar en Ollama

```bash
ollama create cerberusai -f Modelfile
ollama run cerberusai
```

Prueba rápida:

```
Genera un programa Cerberus que imprima los numeros del 1 al 5 con un ciclo para.
```

## 6. Evaluación (Fase 4)

El harness está implementado en `CerberusAI/eval/` (`eval_harness.py` y `run_eval.py`): usa el
oráculo `audit.CompiladorCLI` para medir cuántos programas generados compilan, comparando el
modelo base con el fine-tuned.

```bash
py ../eval/run_eval.py
```

Resultado obtenido (9 tareas `generation` del split, semilla 42): base `qwen2.5-coder:7b`
**4/9 (44%)** de compilación; `cerberusai` **8/9 (89%)** y exact-match 3/9 (33%). Detalle en
`../eval/resultados_base_vs_ft.txt`.

## Entrenar en Google Colab (alternativa a WSL2)

Recomendado si no quieres ocupar la GPU local. Pensado para **Colab Pro** (L4 24 GB / A100 40 GB).

1. Abre `CerberusAI/colab/train_cerberus_colab.ipynb` en Colab.
2. Activa GPU (`Entorno de ejecución → Cambiar tipo de entorno → GPU`).
3. Ejecuta las celdas en orden: verifica GPU, monta Drive, clona el repo público,
   instala `colab/requirements-colab.txt`, construye los datos (v2+v3+v4), entrena con
   `--dtype auto` y descarga `lora-adapter.zip` desde Drive.
4. El **merge a GGUF sigue siendo local** (sección 4): descomprime el adapter en
   `CerberusAI/finetune/lora-adapter/` y ejecuta `export_gguf.py`.

Hiperparámetros: los mismos que en local (`--epochs 5`, `--batch 2`, `--grad-accum 4`, seed 42).
Si hay OOM en L4: `--batch 1 --grad-accum 8`.

## Notas

- Antes de entrenar puedes validar los datos con `python train_qlora.py --check` (no necesita GPU).
- **Troubleshooting (Python 3.14 sin `python3.14-dev`):** TRL 1.x usa un kernel Triton y gcc falla
  con `fatal error: Python.h: No such file or directory`. Workaround sin sudo:
  `apt-get download python3.14-dev libpython3.14-dev`, `dpkg -x` los `.deb` en `~/pyheaders` y
  exportar
  `CPATH=~/pyheaders/extracted/usr/include/python3.14:~/pyheaders/extracted/usr/include`
  antes de entrenar.
- Si el entrenamiento no cabe en VRAM: baja `--batch` a 1 y sube `--grad-accum` a 8.
- El merge de `export_gguf.py` se hace en CPU con el modelo en fp16: el pico de RAM ronda los
  **28 GB**, así que conviene cerrar otras aplicaciones durante ese paso.
- El `Modelfile` apunta a `cerberusai-q8_0.gguf`. Si omites la cuantización y usas
  `cerberusai-f16.gguf`, cambia el `FROM` para que coincida.
- Si el API de TRL/Transformers cambia, ajusta `train_qlora.py` / `export_gguf.py` (probado con
  `transformers>=5.19` y `trl>=1.15`: en transformers 5 ya no existe `warmup_ratio` (usar
  `warmup_steps`) y en TRL 1.x `max_seq_length` se llama `max_length`).
- El dataset de entrenamiento ya incluye v2 + v3 + v4 (1594 ejemplos: generación, corrección
  semántica/sintáctica y categorías de Q&A balanceadas); ver `../dataset/`.

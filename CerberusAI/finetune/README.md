# CerberusAI — Fine-tuning (Fase 3)

Guía para especializar **Qwen2.5-Coder-7B-Instruct** en el lenguaje Cerberus mediante
**QLoRA 4-bit** y servirlo con **Ollama**. Se ejecuta en **WSL2 (Linux + CUDA)** usando la
RTX 3060 12 GB; el merge a GGUF se hace en CPU (32 GB de RAM).

> Requisitos: WSL2 con CUDA, ~30 GB de disco libre, Python 3.10+ y Ollama instalado.

## 1. Datos (Fase 2)

Si `train.jsonl` / `eval.jsonl` no existen todavía, genéralos desde el dataset v2:

```bash
python to_chat.py --input ../dataset/cerberus_dataset_v2.jsonl --output chat_all.jsonl
python split.py --input chat_all.jsonl --out-dir . --eval-ratio 0.2 --seed 42
# -> train.jsonl (74) y eval.jsonl (19), estratificados y reproducibles (seed 42)
```

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

El harness (`CerberusAI/eval/`, pendiente) usará el oráculo `audit.CompiladorCLI` para medir
cuántos programas generados compilan, comparando el modelo base con el fine-tuned.

## Notas

- Si el entrenamiento no cabe en VRAM: baja `--batch` a 1 y sube `--grad-accum` a 8.
- Si el API de TRL/Transformers cambia, ajusta `train_qlora.py` (se usó la API reciente:
  `SFTConfig` + `processing_class`).
- El dataset es pequeño (prueba de concepto); ampliarlo en la Fase 7 (más generación y
  ejemplos de corrección).

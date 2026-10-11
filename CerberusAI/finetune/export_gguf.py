#!/usr/bin/env python3
"""
export_gguf.py -- Mergea el adapter LoRA sobre el modelo base y prepara el GGUF
para Ollama (Fase 3 de CERBERUSIA_IMPLEMENTATION.md).

El merge se hace en CPU con el modelo en fp16 (32 GB de RAM bastan para un 7B),
porque en la RTX 3060 (12 GB) no cabe el modelo completo en fp16.

Uso:
    python export_gguf.py
    python export_gguf.py --adapter lora-adapter --merged-dir merged-model

Salida: ./merged-model (pesos merged en fp16) + instrucciones para convertir a GGUF.
"""
import argparse
import os

import torch
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer


def ruta_local(base, valor):
    return valor if os.path.isabs(valor) else os.path.join(base, valor)


def main():
    aquidir = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description="Merge LoRA + preparacion GGUF.")
    ap.add_argument("--model", default="Qwen/Qwen2.5-Coder-7B-Instruct")
    ap.add_argument("--adapter", default="lora-adapter")
    ap.add_argument("--merged-dir", default="merged-model")
    args = ap.parse_args()

    adapter = ruta_local(aquidir, args.adapter)
    merged = ruta_local(aquidir, args.merged_dir)

    print("== Cargando modelo base en fp16 sobre CPU ==")
    base = AutoModelForCausalLM.from_pretrained(
        args.model, torch_dtype=torch.float16, device_map="cpu", trust_remote_code=True
    )
    tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=True)

    print("== Aplicando y mergeando el adapter LoRA ==")
    model = PeftModel.from_pretrained(base, adapter)
    model = model.merge_and_unload()

    print("== Guardando modelo merged en:", merged)
    model.save_pretrained(merged, safe_serialization=True)
    tokenizer.save_pretrained(merged)

    print("\nMerge completado. Para obtener el GGUF (requiere llama.cpp):\n")
    print("  # 1) clonar e instalar llama.cpp (una sola vez)")
    print("  git clone https://github.com/ggerganov/llama.cpp")
    print("  pip install -r llama.cpp/requirements.txt\n")
    print("  # 2) convertir a GGUF fp16")
    print(f'  python llama.cpp/convert_hf_to_gguf.py "{merged}" --outfile cerberusai-f16.gguf --outtype f16\n')
    print("  # 3) (recomendado para 12 GB) cuantizar a Q8_0")
    print("  llama.cpp/build/bin/llama-quantize cerberusai-f16.gguf cerberusai-q8_0.gguf Q8_0\n")
    print("  # 4) registrar en Ollama")
    print("  ollama create cerberusai -f Modelfile")


if __name__ == "__main__":
    main()

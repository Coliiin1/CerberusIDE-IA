#!/usr/bin/env python3
"""
to_chat.py -- Convierte un dataset .jsonl (campos instruction/response) al formato
de chat de Qwen2.5 (estilo ChatML).

Cada linea de entrada:
    {"instruction": "...", "response": "...", "category": "...", "source": "..."}

Cada linea de salida:
    {"messages": [ {"role": "system", ...}, {"role": "user", ...}, {"role": "assistant", ...} ],
     "text": "<|im_start|>system\n...<|im_end|>\n...", "category": "..."}

El campo "messages" es el recomendado para entrenar (el tokenizer de Qwen aplica su
plantilla). El campo "text" ya viene renderizado en formato ChatML por comodidad.

Uso:
    python to_chat.py --input ../dataset/cerberus_dataset_v2.jsonl --output chat_all.jsonl
"""
import argparse
import json
import os


def render_qwen(messages):
    """Renderiza la lista de mensajes con la plantilla ChatML de Qwen2.5."""
    parts = []
    for m in messages:
        parts.append("<|im_start|>" + m["role"] + "\n" + m["content"] + "<|im_end|>\n")
    return "".join(parts)


def cargar_system(path):
    with open(path, encoding="utf-8") as f:
        return f.read().strip()


def main():
    aqui = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description="Convierte un dataset .jsonl a formato de chat (Qwen2.5).")
    ap.add_argument("--input", required=True, help="dataset .jsonl de entrada (instruction/response).")
    ap.add_argument("--output", required=True, help="archivo .jsonl de salida en formato chat.")
    ap.add_argument("--system", default=os.path.join(aqui, "system_prompt.txt"),
                    help="archivo con el mensaje 'system' (por defecto system_prompt.txt).")
    args = ap.parse_args()

    system = cargar_system(args.system)

    total = 0
    with open(args.input, encoding="utf-8") as fin, open(args.output, "w", encoding="utf-8") as fout:
        for linea in fin:
            linea = linea.strip()
            if not linea:
                continue
            obj = json.loads(linea)
            messages = [
                {"role": "system", "content": system},
                {"role": "user", "content": obj["instruction"]},
                {"role": "assistant", "content": obj["response"]},
            ]
            salida = {
                "messages": messages,
                "text": render_qwen(messages),
                "category": obj.get("category", ""),
            }
            fout.write(json.dumps(salida, ensure_ascii=False) + "\n")
            total += 1

    print("to_chat: {} ejemplos escritos en {}".format(total, args.output))


if __name__ == "__main__":
    main()

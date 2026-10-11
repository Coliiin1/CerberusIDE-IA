#!/usr/bin/env python3
"""
split.py -- Particion train/eval reproducible, estratificada por categoria.

Lee un dataset .jsonl (cualquier formato que conserve el campo "category") y reparte
cada categoria entre train y eval usando una semilla fija, de modo que la particion
sea reproducible y ambas partes queden balanceadas.

Uso:
    python split.py --input chat_all.jsonl --out-dir . --eval-ratio 0.2 --seed 42
    # -> train.jsonl, eval.jsonl
"""
import argparse
import json
import os
import random
from collections import defaultdict


def main():
    ap = argparse.ArgumentParser(description="Split train/eval estratificado y reproducible.")
    ap.add_argument("--input", required=True, help="dataset .jsonl de entrada.")
    ap.add_argument("--out-dir", default=".", help="carpeta donde escribir train.jsonl/eval.jsonl.")
    ap.add_argument("--eval-ratio", type=float, default=0.2, help="fraccion para eval (por defecto 0.2).")
    ap.add_argument("--seed", type=int, default=42, help="semilla para reproducibilidad.")
    args = ap.parse_args()

    entradas = []
    with open(args.input, encoding="utf-8") as f:
        for linea in f:
            linea = linea.strip()
            if linea:
                entradas.append(json.loads(linea))

    por_categoria = defaultdict(list)
    for e in entradas:
        por_categoria[e.get("category", "")].append(e)

    rng = random.Random(args.seed)
    train, ev = [], []
    resumen = []

    for categoria in sorted(por_categoria):
        items = list(por_categoria[categoria])
        rng.shuffle(items)
        n = len(items)
        k = round(n * args.eval_ratio)
        if n >= 3 and k < 1:
            k = 1
        if n <= 1:
            k = 0
        else:
            k = min(k, n - 1)
        ev.extend(items[:k])
        train.extend(items[k:])
        resumen.append((categoria, n, len(items[k:]), k))

    rng.shuffle(train)
    rng.shuffle(ev)

    def escribir(nombre, datos):
        ruta = os.path.join(args.out_dir, nombre)
        with open(ruta, "w", encoding="utf-8") as f:
            for e in datos:
                f.write(json.dumps(e, ensure_ascii=False) + "\n")
        return ruta

    ruta_train = escribir("train.jsonl", train)
    ruta_eval = escribir("eval.jsonl", ev)

    print("split.py -- seed={} eval_ratio={}".format(args.seed, args.eval_ratio))
    print("{:<20} {:>6} {:>6} {:>6}".format("categoria", "total", "train", "eval"))
    for categoria, total, n_train, n_eval in resumen:
        print("{:<20} {:>6} {:>6} {:>6}".format(categoria or "(sin categoria)", total, n_train, n_eval))
    print("-" * 42)
    print("{:<20} {:>6} {:>6} {:>6}".format("TOTAL", len(train) + len(ev), len(train), len(ev)))
    print("Escrito: {}".format(ruta_train))
    print("Escrito: {}".format(ruta_eval))


if __name__ == "__main__":
    main()

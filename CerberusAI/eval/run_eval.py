#!/usr/bin/env python3
"""
run_eval.py -- Compara el modelo base con el fine-tuned (Fase 4 de CERBERUSIA_IMPLEMENTATION.md).

Ejecuta el harness sobre dos modelos de Ollama y muestra una tabla comparativa con la
tasa de compilacion y el exact-match de cada uno.

Uso:
    py run_eval.py
    py run_eval.py --base-model qwen2.5-coder:7b-instruct --ft-model cerberusai
"""
import argparse
import os
import sys

import eval_harness as eh


def parse_args():
    ap = argparse.ArgumentParser(description="Compara modelo base vs fine-tuned.")
    ap.add_argument("--base-model", default="qwen2.5-coder:7b-instruct")
    ap.add_argument("--ft-model", default="cerberusai")
    ap.add_argument("--input", default=os.path.join(eh.AQUI, "..", "finetune", "eval.jsonl"))
    ap.add_argument("--category", default="generation")
    ap.add_argument("--base-url", default="http://localhost:11434")
    ap.add_argument("--system", default=os.path.join(eh.AQUI, "..", "finetune", "system_prompt.txt"))
    ap.add_argument("--classpath", default=os.path.join(eh.AQUI, "..", "..", "target", "classes"))
    ap.add_argument("--compiler", default="audit.CompiladorCLI")
    ap.add_argument("--java", default="java")
    ap.add_argument("--max-tasks", type=int, default=0)
    ap.add_argument("--timeout", type=float, default=120.0)
    ap.add_argument("--temperature", type=float, default=0.0)
    return ap.parse_args()


def evaluar(model, tareas, system, args):
    print("\n########## Modelo: {} ##########".format(model))
    _, resumen = eh.evaluar_modelo(
        tareas, model, args.base_url, system, args.java, args.classpath,
        args.compiler, args.timeout, args.temperature)
    eh.imprimir_resumen(resumen)
    return resumen


def main():
    args = parse_args()

    system = ""
    if os.path.exists(args.system):
        with open(args.system, encoding="utf-8") as f:
            system = f.read().strip()

    tareas = eh.cargar_tareas(args.input, args.category)
    if args.max_tasks > 0:
        tareas = tareas[:args.max_tasks]
    if not tareas:
        print("No hay tareas (input={}, category={}).".format(args.input, args.category))
        return 1

    resumen_base = evaluar(args.base_model, tareas, system, args)
    resumen_ft = evaluar(args.ft_model, tareas, system, args)

    print("\n===== COMPARACION ({} tareas) =====".format(len(tareas)))
    print("{:<30} {:>14} {:>12}".format("Modelo", "Compilacion", "Exact-match"))
    for r in (resumen_base, resumen_ft):
        print("{:<30} {:>14} {:>12}".format(
            r["model"],
            "{}/{} ({:.0f}%)".format(r["compilados"], r["total"], 100 * r["tasa_compilacion"]),
            "{}/{} ({:.0f}%)".format(r["exactos"], r["total"], 100 * r["tasa_exact"])))

    delta = resumen_ft["tasa_compilacion"] - resumen_base["tasa_compilacion"]
    print("\nMejora en tasa de compilacion (fine-tuned vs base): {:+.0f} puntos porcentuales.".format(100 * delta))
    return 0


if __name__ == "__main__":
    sys.exit(main())

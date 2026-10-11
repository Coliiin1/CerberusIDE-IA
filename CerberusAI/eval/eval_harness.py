#!/usr/bin/env python3
"""
eval_harness.py -- Harness de evaluacion de CerberusAI (Fase 4).

Genera codigo Cerberus con un modelo servido por Ollama, lo valida con el oraculo
(audit.CompiladorCLI) y calcula metricas:
  - tasa de compilacion (primaria)
  - exact-match normalizado (secundaria)
  - desglose de errores

Solo usa la biblioteca estandar de Python (urllib, sin dependencias).

Uso:
    py eval_harness.py --model cerberusai
    py eval_harness.py --model qwen2.5-coder:7b-instruct --input ../finetune/eval.jsonl
"""
import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
from collections import Counter

AQUI = os.path.dirname(os.path.abspath(__file__))
FENCE = re.compile(r"```[a-zA-Z0-9_+-]*\s*\n(.*?)```", re.DOTALL)


def parse_args():
    ap = argparse.ArgumentParser(description="Evalua un modelo de Ollama contra el oraculo del compilador.")
    ap.add_argument("--input", default=os.path.join(AQUI, "..", "finetune", "eval.jsonl"),
                    help="dataset .jsonl (formato chat o instruction/response).")
    ap.add_argument("--category", default="generation", help="categoria a evaluar ('all' para todas).")
    ap.add_argument("--model", default="cerberusai", help="modelo de Ollama.")
    ap.add_argument("--base-url", default="http://localhost:11434", help="URL de Ollama.")
    ap.add_argument("--system", default=os.path.join(AQUI, "..", "finetune", "system_prompt.txt"))
    ap.add_argument("--classpath", default=os.path.join(AQUI, "..", "..", "target", "classes"))
    ap.add_argument("--compiler", default="audit.CompiladorCLI")
    ap.add_argument("--java", default="java")
    ap.add_argument("--max-tasks", type=int, default=0, help="limite de tareas (0 = todas).")
    ap.add_argument("--timeout", type=float, default=120.0, help="timeout por llamada a Ollama (s).")
    ap.add_argument("--temperature", type=float, default=0.0)
    ap.add_argument("--json", action="store_true", help="imprime tambien el resumen en JSON.")
    return ap.parse_args()


def cargar_tareas(path, categoria):
    tareas = []
    with open(path, encoding="utf-8") as f:
        for i, linea in enumerate(f):
            linea = linea.strip()
            if not linea:
                continue
            obj = json.loads(linea)
            if categoria != "all" and obj.get("category") != categoria:
                continue
            if "messages" in obj:
                msgs = obj["messages"]
                user = next((m.get("content", "") for m in msgs if m.get("role") == "user"), "")
                gold = next((m.get("content", "") for m in msgs if m.get("role") == "assistant"), "")
            else:
                user = obj.get("instruction", "")
                gold = obj.get("response", "")
            tareas.append({"id": i, "instruction": user, "gold": gold})
    return tareas


def llamar_ollama(base_url, model, system, user, temperature, timeout):
    url = base_url.rstrip("/") + "/api/chat"
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "stream": False,
        "options": {"temperature": temperature},
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        obj = json.loads(resp.read().decode("utf-8"))
    msg = obj.get("message") or {}
    return msg.get("content") or obj.get("response") or ""


def extraer_codigo(texto):
    bloques = FENCE.findall(texto)
    if bloques:
        return bloques[0].strip()
    return texto.strip()


def normalizar(s):
    return re.sub(r"\s+", " ", s).strip()


def compilar(java, classpath, compiler, codigo):
    with tempfile.NamedTemporaryFile("w", suffix=".cer", delete=False, encoding="utf-8") as tmp:
        tmp.write(codigo)
        ruta_tmp = tmp.name
    try:
        try:
            proc = subprocess.run(
                [java, "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8",
                 "-cp", classpath, compiler, ruta_tmp],
                capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60,
            )
        except Exception as e:
            return {"aceptado": False, "error": "oraculo: {}".format(e)}
        salida = (proc.stdout or "").strip().splitlines()
        linea_json = salida[-1] if salida else ""
        try:
            return json.loads(linea_json)
        except json.JSONDecodeError:
            detalle = (linea_json or proc.stderr or "")[:200]
            return {"aceptado": False, "error": "oraculo: salida no JSON: " + detalle}
    finally:
        try:
            os.remove(ruta_tmp)
        except OSError:
            pass


def evaluar_modelo(tareas, model, base_url, system, java, classpath, compiler,
                   timeout, temperature, verbose=True):
    resultados = []
    for t in tareas:
        try:
            crudo = llamar_ollama(base_url, model, system, t["instruction"], temperature, timeout)
        except Exception as e:
            resultados.append({"id": t["id"], "aceptado": False,
                               "error": "conexion: {}".format(e), "exact": False, "codigo": ""})
            if verbose:
                print("[ERR ] #{} conexion: {}".format(t["id"], e))
            continue

        codigo = extraer_codigo(crudo)
        veredicto = compilar(java, classpath, compiler, codigo)
        aceptado = bool(veredicto.get("aceptado"))
        exact = normalizar(codigo) == normalizar(t["gold"])
        resultados.append({
            "id": t["id"], "aceptado": aceptado, "error": veredicto.get("error"),
            "exact": exact, "codigo": codigo,
            "linea": veredicto.get("linea"), "columna": veredicto.get("columna"),
        })
        if verbose:
            marca = "OK   " if aceptado else "FAIL "
            extra = ""
            if not aceptado:
                extra = " -> " + str(veredicto.get("error", "")).split("\n")[0][:70]
            print("[{}] #{} {}{}".format(marca, t["id"], t["instruction"][:60], extra))

    total = len(resultados)
    compilados = sum(1 for r in resultados if r["aceptado"])
    exactos = sum(1 for r in resultados if r["exact"])
    errores = Counter()
    for r in resultados:
        if not r["aceptado"]:
            firma = (r["error"] or "(sin mensaje)").split("\n")[0][:70]
            errores[firma] += 1

    resumen = {
        "model": model,
        "total": total,
        "compilados": compilados,
        "tasa_compilacion": (compilados / total) if total else 0.0,
        "exactos": exactos,
        "tasa_exact": (exactos / total) if total else 0.0,
        "errores": dict(errores),
    }
    return resultados, resumen


def imprimir_resumen(resumen):
    print()
    print("== Resumen (modelo: {}) ==".format(resumen["model"]))
    print("Tareas:        {}".format(resumen["total"]))
    print("Compilacion:   {}/{} ({:.0f}%)".format(
        resumen["compilados"], resumen["total"], 100 * resumen["tasa_compilacion"]))
    print("Exact-match:   {}/{} ({:.0f}%)".format(
        resumen["exactos"], resumen["total"], 100 * resumen["tasa_exact"]))
    if resumen["errores"]:
        print("Errores mas comunes:")
        for firma, n in sorted(resumen["errores"].items(), key=lambda kv: -kv[1]):
            print("  {:>3}x  {}".format(n, firma))


def main():
    args = parse_args()
    system = ""
    if os.path.exists(args.system):
        with open(args.system, encoding="utf-8") as f:
            system = f.read().strip()
    tareas = cargar_tareas(args.input, args.category)
    if args.max_tasks > 0:
        tareas = tareas[:args.max_tasks]
    if not tareas:
        print("No hay tareas (input={}, category={}).".format(args.input, args.category))
        return 1
    print("Evaluando {} tarea(s) con el modelo '{}' en {}".format(len(tareas), args.model, args.base_url))
    _, resumen = evaluar_modelo(tareas, args.model, args.base_url, system,
                                args.java, args.classpath, args.compiler,
                                args.timeout, args.temperature)
    imprimir_resumen(resumen)
    if args.json:
        print()
        print(json.dumps(resumen, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())

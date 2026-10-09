# CERBERUSIA_IMPLEMENTATION.md — Plan de implementación de CerberusAI

## Objetivo
Implementar el módulo de IA (Paso 10 de RUTA.md): evaluación automática +
elección de modelo base local, con fine-tuning QLoRA y consumo vía Ollama.

## Decisiones cerradas
- Modelo base: Qwen2.5-Coder-7B-Instruct
- Método: fine-tuning QLoRA 4-bit (se ejecuta en la máquina GPU: RTX 3060 12 GB,
  32 GB DDR4, Ryzen 7 5700X)
- Inferencia y evaluación: Ollama (HTTP), consumido desde esta máquina

## Entregables

### 1. Oráculo del compilador (Java)
- src/main/java/audit/CompiladorCLI.java
  - main(args): lee archivo (o stdin), ejecuta lexer + parser headless
    (reutiliza la lógica de CompilerProbe.evaluar()).
  - Emite {"aceptado": bool, "error": "..."} por stdout, exit code 0/1.

### 2. Preparación de datos (Python, CerberusAI/finetune/)
- split.py: partición train/eval reproducible (45 generation como núcleo;
  resto como tarea de instrucción).
- to_chat.py: convierte .jsonl a ChatML (Qwen2.5) con system = resumen de
  language_spec.md.
- Salidas: train.jsonl, eval.jsonl.

### 3. Pipeline fine-tuning QLoRA (se corre en la máquina GPU)
- train_qlora.py: bitsandbytes 4-bit + PEFT/LoRA + trl SFTTrainer sobre
  Qwen/Qwen2.5-Coder-7B-Instruct.
- export_gguf.py / Modelfile: publicar el modelo fine-tuned en Ollama.
- requirements-finetune.txt + README.md (comandos para la RTX 3060).

### 4. Harness de evaluación (Python, CerberusAI/eval/)
- eval_harness.py: genera código vía Ollama HTTP (OLLAMA_BASE_URL configurable),
  extrae el código, lo valida contra CompiladorCLI y calcula métricas
  (tasa de compilación, exact-match, desglose de errores).
- run_eval.py: compara modelo base vs fine-tuned.

### 5. Documentación
- CerberusAI/MODEL_SELECTION.md (criterios y decisión).
- Actualizar RUTA.md (Paso 10).

## Secuencia y dependencias
1 → 2 → 3 → 4 → 5. A → (B, D); E define el modelo de C y D.

## Verificación
- mvnw.cmd compile + CompiladorCLI sobre un .txt de muestra.
- split.py / to_chat.py sobre cerberus_dataset_v2.jsonl.
- eval_harness.py contra Ollama (máquina GPU).

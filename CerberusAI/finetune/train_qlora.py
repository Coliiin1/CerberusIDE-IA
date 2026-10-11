#!/usr/bin/env python3
"""
train_qlora.py -- Fine-tuning QLoRA 4-bit de Qwen2.5-Coder-7B-Instruct sobre el
dataset de Cerberus (Fase 3 de CERBERUSIA_IMPLEMENTATION.md).

Se ejecuta DENTRO de WSL2 (Linux + CUDA). Requiere haber corrido antes la Fase 2
para tener train.jsonl / eval.jsonl en formato de chat (campo "text").

Uso:
    python train_qlora.py                 # usa los valores por defecto
    python train_qlora.py --epochs 5      # (por defecto 5)

Salida: un adapter LoRA en ./lora-adapter (ligero).
"""
import argparse
import os

import torch
from datasets import load_dataset
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig
from peft import LoraConfig
from trl import SFTTrainer, SFTConfig


def ruta_local(base, valor):
    """Resuelve rutas relativas respecto a la carpeta de este script."""
    return valor if os.path.isabs(valor) else os.path.join(base, valor)


def parse_args():
    aquidir = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser(description="Fine-tuning QLoRA de Cerberus.")
    ap.add_argument("--model", default="Qwen/Qwen2.5-Coder-7B-Instruct")
    ap.add_argument("--train", default="train.jsonl", help="dataset de entrenamiento (formato chat).")
    ap.add_argument("--eval", default="eval.jsonl", help="dataset de evaluacion (formato chat).")
    ap.add_argument("--output", default="lora-adapter", help="carpeta del adapter LoRA resultante.")
    ap.add_argument("--epochs", type=int, default=5)
    ap.add_argument("--max-seq-length", type=int, default=2048)
    ap.add_argument("--lr", type=float, default=2e-4)
    ap.add_argument("--batch", type=int, default=2, help="batch por dispositivo.")
    ap.add_argument("--grad-accum", type=int, default=4)
    ap.add_argument("--lora-r", type=int, default=16)
    ap.add_argument("--lora-alpha", type=int, default=32)
    ap.add_argument("--lora-dropout", type=float, default=0.05)
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()
    args.train = ruta_local(aquidir, args.train)
    args.eval = ruta_local(aquidir, args.eval)
    args.output = ruta_local(aquidir, args.output)
    return args


def main():
    args = parse_args()

    print("== Cargando tokenizer ==")
    tokenizer = AutoTokenizer.from_pretrained(args.model, trust_remote_code=True)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token
    tokenizer.padding_side = "right"

    print("== Cargando modelo en 4-bit (QLoRA) ==")
    bnb = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype=torch.bfloat16,
        bnb_4bit_use_double_quant=True,
    )
    model = AutoModelForCausalLM.from_pretrained(
        args.model, quantization_config=bnb, device_map="auto", trust_remote_code=True
    )
    model.config.use_cache = False

    lora = LoraConfig(
        r=args.lora_r,
        lora_alpha=args.lora_alpha,
        lora_dropout=args.lora_dropout,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                        "gate_proj", "up_proj", "down_proj"],
    )

    print("== Cargando dataset ==")
    data = load_dataset("json", data_files={"train": args.train, "eval": args.eval})

    cfg = SFTConfig(
        output_dir=args.output,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch,
        per_device_eval_batch_size=1,
        gradient_accumulation_steps=args.grad_accum,
        learning_rate=args.lr,
        lr_scheduler_type="cosine",
        warmup_ratio=0.05,
        optim="paged_adamw_8bit",
        bf16=True,
        gradient_checkpointing=True,
        logging_steps=5,
        save_strategy="epoch",
        eval_strategy="epoch",
        save_total_limit=2,
        report_to="none",
        seed=args.seed,
        dataset_text_field="text",
        max_seq_length=args.max_seq_length,
        packing=False,
    )

    trainer = SFTTrainer(
        model=model,
        args=cfg,
        train_dataset=data["train"],
        eval_dataset=data["eval"],
        peft_config=lora,
        processing_class=tokenizer,
    )

    print("== Entrenando ==")
    trainer.train()
    trainer.save_model(args.output)
    tokenizer.save_pretrained(args.output)
    print("Adapter LoRA guardado en:", args.output)
    print("Siguiente paso: python export_gguf.py")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
make_docx.py -- Regenera la memoria de CerberusAI en .docx a partir del Markdown.

La fuente unica de verdad es CERBERUSAI_MEMORIA.md; este script la convierte con
Pandoc (via pypandoc, instalado en CerberusAI/.venv con `pip install pypandoc_binary`).

Uso:
    python make_docx.py
"""
import os

import pypandoc

AQUI = os.path.dirname(os.path.abspath(__file__))
MD = os.path.join(AQUI, "CERBERUSAI_MEMORIA.md")
DOCX = os.path.join(AQUI, "CERBERUSAI_MEMORIA.docx")


def main():
    if not os.path.exists(MD):
        raise SystemExit("No existe la fuente: " + MD)
    pypandoc.convert_file(
        MD,
        "docx",
        outputfile=DOCX,
        extra_args=["--toc", "--toc-depth=2", "--number-sections"],
    )
    print("Generado:", DOCX)


if __name__ == "__main__":
    main()

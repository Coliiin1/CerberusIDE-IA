#!/usr/bin/env python3
"""
main.py -- API de CerberusAI (Fase 5 de CERBERUSIA_IMPLEMENTATION.md).

Expone un chat que reenvia la peticion a Ollama (modelo especializado en Cerberus) y,
para los roles de codigo (generar/corregir), valida el resultado con el oraculo del
compilador (audit.CompiladorCLI).

Arranque:
    pip install -r requirements-api.txt
    uvicorn main:app --host 127.0.0.1 --port 8000

Configuracion por variables de entorno:
    OLLAMA_BASE_URL      (default http://localhost:11434)
    MODEL                (default cerberusai)
    SYSTEM_PROMPT_PATH
    CLASSPATH            (default ../../target/classes)
    JAVA                 (default java)
    COMPILER             (default audit.CompiladorCLI)
    TIMEOUT              (segundos, default 120)
    CERBERUSAI_API_KEY   (si se define, exige la cabecera X-API-Key)
    CERBERUSAI_RATE_LIMIT  (peticiones por IP y ventana; 0 desactiva, default 20)
    CERBERUSAI_RATE_WINDOW (ventana en segundos, default 60)
    WHISPER_MODEL        (dictado por voz; default small)
    WHISPER_DEVICE       (cpu | cuda, default cpu)
    WHISPER_COMPUTE      (default int8)
    MAX_AUDIO_BYTES      (tamano maximo del WAV de /transcribir, default 4 MB)
"""
import hmac
import io
import json
import os
import re
import subprocess
import tempfile
import threading
import time
import wave
from collections import deque
from typing import Literal, Optional

import requests
from fastapi import Depends, FastAPI, HTTPException, Request
from pydantic import BaseModel, ConfigDict
from starlette.concurrency import run_in_threadpool

AQUI = os.path.dirname(os.path.abspath(__file__))

OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434")
MODEL = os.environ.get("MODEL", "cerberusai")
SYSTEM_PROMPT_PATH = os.environ.get(
    "SYSTEM_PROMPT_PATH", os.path.join(AQUI, "..", "finetune", "system_prompt.txt"))
CLASSPATH = os.environ.get("CLASSPATH", os.path.join(AQUI, "..", "..", "target", "classes"))
JAVA = os.environ.get("JAVA", "java")
COMPILER = os.environ.get("COMPILER", "audit.CompiladorCLI")
TIMEOUT = float(os.environ.get("TIMEOUT", "120"))

API_KEY = os.environ.get("CERBERUSAI_API_KEY", "").strip()


def _entero_env(nombre, por_defecto):
    try:
        return int(os.environ.get(nombre, por_defecto))
    except ValueError:
        return int(por_defecto)


RATE_LIMIT = _entero_env("CERBERUSAI_RATE_LIMIT", "20")
RATE_WINDOW = max(1, _entero_env("CERBERUSAI_RATE_WINDOW", "60"))
MAX_IPS_RASTREADAS = 4096

WHISPER_MODEL = os.environ.get("WHISPER_MODEL", "small")
WHISPER_DEVICE = os.environ.get("WHISPER_DEVICE", "cpu")
WHISPER_COMPUTE = os.environ.get("WHISPER_COMPUTE", "int8")
MAX_AUDIO_BYTES = _entero_env("MAX_AUDIO_BYTES", str(4 * 1024 * 1024))

_whisper = None
_lock_whisper = threading.Lock()

_intentos_por_ip = {}
_lock_intentos = threading.Lock()

ROLES = {
    "generar": "Genera el codigo Cerberus para: {prompt}",
    "explicar": "Explica el siguiente codigo o concepto de Cerberus: {prompt}",
    "corregir": "Corrige el siguiente codigo Cerberus y devuelve la version que compile: {prompt}",
}

FENCE = re.compile(r"```[a-zA-Z0-9_+-]*\s*\n(.*?)```", re.DOTALL)
CLASE = re.compile(r"\bclase\s+\w+\s*\(\)")

app = FastAPI(title="CerberusAI API", version="0.1.0")


def _ip_cliente(request):
    cf = request.headers.get("cf-connecting-ip")
    if cf:
        return cf.strip()
    reenviada = request.headers.get("x-forwarded-for")
    if reenviada:
        return reenviada.split(",")[0].strip()
    return request.client.host if request.client else "desconocido"


def _limpiar_intentos(ahora):
    if len(_intentos_por_ip) <= MAX_IPS_RASTREADAS:
        return
    for ip, marcas in list(_intentos_por_ip.items()):
        if not marcas or ahora - marcas[-1] >= RATE_WINDOW:
            del _intentos_por_ip[ip]


def _verificar_rate_limit(request):
    if RATE_LIMIT <= 0:
        return
    ip = _ip_cliente(request)
    ahora = time.monotonic()
    with _lock_intentos:
        _limpiar_intentos(ahora)
        marcas = _intentos_por_ip.setdefault(ip, deque())
        while marcas and ahora - marcas[0] >= RATE_WINDOW:
            marcas.popleft()
        if len(marcas) >= RATE_LIMIT:
            espera = max(1, int(RATE_WINDOW - (ahora - marcas[0]) + 0.999))
            raise HTTPException(
                status_code=429,
                detail="Demasiadas peticiones. Intenta de nuevo mas tarde.",
                headers={"Retry-After": str(espera)},
            )
        marcas.append(ahora)


def verificar_acceso(request: Request):
    if API_KEY:
        recibida = request.headers.get("x-api-key", "")
        if not hmac.compare_digest(recibida.encode("utf-8"), API_KEY.encode("utf-8")):
            raise HTTPException(status_code=401, detail="API Key ausente o invalida.")
    _verificar_rate_limit(request)


class ChatRequest(BaseModel):
    # Rol desconocido o campo mal escrito (p. ej. "rol") -> 422, en vez de caer en "generar".
    model_config = ConfigDict(extra="forbid")

    role: Literal["generar", "explicar", "corregir"] = "generar"
    prompt: str


class ChatResponse(BaseModel):
    texto: str
    es_codigo: bool
    codigo: Optional[str] = None
    compila: Optional[bool] = None
    error_compilacion: Optional[str] = None


def leer_system():
    try:
        with open(SYSTEM_PROMPT_PATH, encoding="utf-8") as f:
            return f.read().strip()
    except OSError:
        return "Eres CerberusAI, un asistente experto en el lenguaje Cerberus."


def extraer_codigo(texto):
    """Extrae el primer bloque de codigo (fences) o un programa completo sin fence."""
    bloques = FENCE.findall(texto)
    if bloques:
        return bloques[0].strip()
    if CLASE.search(texto):
        return texto.strip()
    return None


def validar_con_oraculo(codigo):
    """Valida el codigo con el oraculo. Devuelve (compila, error); None si no disponible."""
    try:
        with tempfile.NamedTemporaryFile("w", suffix=".cer", delete=False, encoding="utf-8") as tmp:
            tmp.write(codigo)
            ruta = tmp.name
    except OSError:
        return None, None
    try:
        proc = subprocess.run(
            [JAVA, "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8",
             "-cp", CLASSPATH, COMPILER, ruta],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60,
        )
        salida = (proc.stdout or "").strip().splitlines()
        linea = salida[-1] if salida else ""
        veredicto = json.loads(linea)
        return bool(veredicto.get("aceptado")), veredicto.get("error")
    except Exception:
        return None, None
    finally:
        try:
            os.remove(ruta)
        except OSError:
            pass


@app.get("/health", dependencies=[Depends(verificar_acceso)])
def health():
    return {"status": "ok", "modelo": MODEL, "ollama": OLLAMA_BASE_URL}


@app.post("/chat", response_model=ChatResponse, dependencies=[Depends(verificar_acceso)])
def chat(req: ChatRequest):
    plantilla = ROLES[req.role]
    user = plantilla.format(prompt=req.prompt)
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": leer_system()},
            {"role": "user", "content": user},
        ],
        "stream": False,
        "options": {"temperature": 0.2},
    }

    try:
        resp = requests.post(OLLAMA_BASE_URL.rstrip("/") + "/api/chat", json=payload, timeout=TIMEOUT)
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        raise HTTPException(status_code=502, detail="No se pudo consultar a Ollama: {}".format(e))

    texto = (data.get("message") or {}).get("content") or data.get("response") or ""
    codigo = extraer_codigo(texto)

    compila = None
    error_compilacion = None
    if codigo and req.role in ("generar", "corregir"):
        compila, error_compilacion = validar_con_oraculo(codigo)

    return ChatResponse(
        texto=texto,
        es_codigo=codigo is not None,
        codigo=codigo,
        compila=compila,
        error_compilacion=error_compilacion,
    )


def _modelo_whisper():
    # Carga perezosa: si nunca se dicta, ni se importa ni se descarga el modelo.
    global _whisper
    with _lock_whisper:
        if _whisper is None:
            from faster_whisper import WhisperModel
            _whisper = WhisperModel(WHISPER_MODEL, device=WHISPER_DEVICE, compute_type=WHISPER_COMPUTE)
        return _whisper


def _leer_wav(audio):
    # Se decodifica con `wave` (stdlib) en vez de PyAV: el IDE siempre manda PCM 16 kHz mono 16 bit
    # y asi se evita la incompatibilidad de PyAV >= 16 con faster-whisper 1.2.x.
    import numpy as np
    try:
        with wave.open(io.BytesIO(audio)) as w:
            formato = (w.getframerate(), w.getnchannels(), w.getsampwidth())
            pcm = w.readframes(w.getnframes())
    except (wave.Error, EOFError):
        raise HTTPException(status_code=400, detail="El audio no es un WAV valido.")
    if formato != (16000, 1, 2):
        raise HTTPException(status_code=400, detail="Se espera WAV PCM 16 kHz mono 16 bit.")
    return np.frombuffer(pcm, dtype=np.int16).astype(np.float32) / 32768.0


def _transcribir(audio):
    try:
        modelo = _modelo_whisper()
    except ImportError:
        raise HTTPException(status_code=503, detail="Dictado no disponible: falta instalar faster-whisper.")
    segmentos, _ = modelo.transcribe(_leer_wav(audio), language="es", vad_filter=True)
    return " ".join(s.text.strip() for s in segmentos).strip()


@app.post("/transcribir", dependencies=[Depends(verificar_acceso)])
async def transcribir(request: Request):
    """Cuerpo crudo audio/wav (sin multipart) -> {"texto": "..."} en espanol."""
    audio = await request.body()
    if not audio:
        raise HTTPException(status_code=400, detail="No se recibio audio.")
    if len(audio) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="Audio demasiado largo.")
    texto = await run_in_threadpool(_transcribir, audio)
    return {"texto": texto}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=False)

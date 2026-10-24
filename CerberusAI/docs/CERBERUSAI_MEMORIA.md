---
title: "CerberusAI — Documentación técnica y experimental"
subtitle: "Módulo de Inteligencia Artificial del proyecto CerberusIDE-IA"
author: "Proyecto CerberusIDE-IA"
date: "2026-10-11"
lang: es
---

# 1. Resumen ejecutivo

CerberusAI es el módulo de Inteligencia Artificial del proyecto **CerberusIDE-IA**. Su objetivo
es especializar un modelo de lenguaje local para que genere, explique y corrija código en
**Cerberus**, un lenguaje de pseudocódigo educativo en español, y exponerlo mediante una API
consumible desde el IDE JavaFX o desde un cliente remoto.

El módulo se apoya en cuatro pilares:

1. **Un oráculo** construido con el propio compilador Cerberus (`audit.CompiladorCLI`), que
   determina de forma objetiva si un fragmento de código compila.
2. **Un dataset validado** contra ese compilador (`cerberus_dataset_v2.jsonl`, 93 entradas).
3. **Un fine-tuning QLoRA 4-bit** de `Qwen/Qwen2.5-Coder-7B-Instruct`, ejecutado en WSL2 sobre
   una GPU RTX 3060 de 12 GB.
4. **Una API FastAPI** que reenvía peticiones a **Ollama** y valida el código generado con el
   oráculo, más un **cliente y panel de chat** para el IDE.

El resultado experimental registrado es: en una muestra de 9 tareas de generación, el modelo base
`qwen2.5-coder:7b` compiló 4/9 (44 %) y el modelo ajustado `cerberusai` compiló 8/9 (89 %), con
un exact-match de 3/9 (33 %). Esta cifra es una **prueba de concepto** y no una conclusión
estadística (véase §11.5).

**Estados usados en este documento:**

- **[IMP]** implementado y verificado con evidencia en el repositorio.
- **[PAR]** implementado parcialmente.
- **[PLAN]** planificado/documentado, sin ejecución verificada.
- **[NOV]** afirmado en la documentación del proyecto pero no verificado independientemente aquí.

# 2. Introducción y contexto

## 2.1 El compilador Cerberus

CerberusIDE incluye un compilador escrito en Java 24 con tres analizadores:

- **Léxico** (`AnalizadorLexico`): tokenizador basado en expresiones regulares.
- **Sintáctico** (`AnalizadorSintactico`): parser descendente recursivo que, además, realiza la
  validación semántica durante el parseo (tipos, scopes, llamadas).
- **Semántico** (`AnalizadorSemantico`): resuelve tipos (`Tipo`), compatibilidad (`compatible`),
  scopes (`buscarIde`/`existeEnScope`) y mapea tokens a tipos.

El compilador es un **analizador**, no un intérprete: no ejecuta el programa, solo comprueba que
sea válido. Esta propiedad es decisiva para la IA: la única señal automática de corrección
disponible es *¿compila o no?*.

## 2.2 Por qué una IA especializada

El modelo base (Qwen2.5-Coder) sabe programar en lenguajes populares, pero no conoce la sintaxis
concreta de Cerberus (por ejemplo, que `clase Nombre` exige paréntesis o que `para` exige un tipo
en el contador). El fine-tuning con ejemplos validados adapta el modelo a esa sintaxis.

## 2.3 Alcance del lenguaje (v1)

El compilador v1 implementa: declaraciones de tipos básicos, asignación simple y compuesta,
aritmética, operadores relacionales y lógicos, `si/sino`, `mientras`, `hacer-mientras`, `para`,
`segun`, `imprimir`, funciones con tipo de retorno opcional, parámetros, `retornar` y llamadas.
Quedan **fuera de v1** (rechazo explícito): POO (`nuevo`, `este`, `publico`, `privado`, acceso
`x.y`), arreglos y `nulo`. Esta delimitación condiciona el dataset y la evaluación.

# 3. Evolución del módulo y cronología

La evolución se reconstruye a partir de commits verificables (`git log`, rama `main`).

| Fecha | Commit | Hito |
|---|---|---|
| 2026-05-23/24 | `b26587b`…`b7cb6d1`, `0396d89`…`ef4c877` | Desarrollo del compilador CerberusIDE (léxico, sintáctico, documentador, semántica, GUI). |
| 2026-10-03 | `c09262f`, `7cbaab0`, `0123314` | Organización del repositorio; `AGENTS.md`; dataset v1 y documentación inicial. |
| 2026-10-04 | `dd18a0e` | Configuración de IntelliJ IDEA. |
| 2026-10-06 | `57b408a`, `1c949d2`, `4df5961`, `8a88de9`, `a461525`, `d0dcd44`, `47207bd` | Auditoría del compilador; scopes, tipos, booleanos, asignación compuesta, `segun`; cierre de gramáticas v1 (ALTA/MEDIA/BAJA). |
| 2026-10-09 | `6b61781` | Plan de implementación de CerberusAI (`CERBERUSIA_IMPLEMENTATION.md`). |
| 2026-10-10 | `1070bea`, `b8fb54d`, `5cd73d1`, `a34fd96`, `b86a262` | Dataset y preprocesado; scripts QLoRA y exportación; harness de evaluación y oráculo; backend FastAPI. |
| 2026-10-11 | `a24566d`, `0d00fa2` | Controlador de chat y scripts de ajuste; actualización de README. |

**Fases del plan** (`CERBERUSIA_IMPLEMENTATION.md`):

| Fase | Entregable | Estado observado |
|---|---|---|
| 1 | Oráculo `audit.CompiladorCLI` + `audit.Evaluador` | HECHO [IMP] |
| 2 | Datos `to_chat.py` + `split.py` → train/eval | HECHO [IMP] |
| 3 | Fine-tuning QLoRA, merge y GGUF, Ollama | HECHO [IMP] |
| 4 | Harness de evaluación base vs fine-tuned | HECHO [IMP] |
| 5 | API FastAPI + validación con oráculo | HECHO [IMP] |
| 6 | Chat en el IDE (JavaFX) | HECHO [IMP] (integrado en `principal.fxml`, URL configurable desde Opciones) |
| 7 | Expansión (más datos, corrección, dominio estable) | FUTURO [PLAN] |

# 4. Arquitectura general

## 4.1 Arquitectura de ejecución (inferencia)

```text
[Chat del IDE (JavaFX) / cliente remoto]
        │  HTTP POST /chat
        ▼
[API FastAPI (Windows)]
        ▲  Cloudflare quick tunnel (HTTPS) ── [cliente remoto]
        │  HTTP local
        ▼
[Ollama] ── modelo `cerberusai` (Qwen2.5-Coder-7B + LoRA → GGUF Q8_0)
```

- El modelo (GPU) reside siempre en la máquina local; el túnel solo expone la API.
- El oráculo (`audit.CompiladorCLI`) **no** participa del runtime de inferencia; se usa para
  validar la salida en la propia API (§9) y para evaluar (§11).

## 4.2 Flujo de entrenamiento y evaluación

```text
cerberus_dataset_v2.jsonl (93)
        │ to_chat.py  (ChatML: messages + text)
        ▼
     chat_all.jsonl (93)
        │ split.py  (estratificado, seed 42)
        ├── train.jsonl (74)
        └── eval.jsonl  (19)
        │ train_qlora.py  (QLoRA 4-bit, WSL2/GPU)
        ▼
    lora-adapter/
        │ export_gguf.py  (merge fp16 en CPU)
        ▼
    merged-model/
        │ convert_hf_to_gguf.py + llama-quantize
        ▼
cerberusai-f16.gguf → cerberusai-q8_0.gguf
        │ ollama create cerberusai
        ▼
    modelo servido
        │ eval_harness.py / run_eval.py (oráculo)
        ▼
resultados_base_vs_ft.txt
```

# 5. El compilador como oráculo

## 5.1 Componentes

- `src/main/java/audit/Evaluador.java`: evaluador headless compartido. Encapsula lexer + parser y
  devuelve un `Resultado` con `aceptado`, `error`, `linea`, `columna` y `lexema`.
- `src/main/java/audit/CompiladorCLI.java`: interfaz de línea de comandos. Lee un archivo o
  `stdin`, evalua el código y escribe en `stdout` un objeto JSON:

```json
{"aceptado": false, "error": "...", "linea": 3, "columna": 9, "lexema": "entero"}
```

- Sale con código `0` si acepta y `1` si rechaza.
- El ruido de depuración del parser se silencia durante la evaluación (`OutputStream.nullOutputStream()`)
  y el JSON se emite en **UTF-8 explícito** mediante `FileDescriptor.out`, para que sea parseable
  independientemente de la codificación de consola de Windows.
- `AnalizadorSintactico.getUltimoError()` expone el `ErrorSintactico` (fila/columna/lexema) que
  origina la ubicación estructurada del JSON.

## 5.2 Uso

```cmd
mvnw.cmd compile
java -cp target\classes audit.CompiladorCLI tests\valid\01_hola_mundo.txt
```

El harness de evaluación y la API invocan este CLI por subproceso. Es la fuente de verdad objetiva
del sistema.

# 6. Dataset

## 6.1 Dataset v1

`CerberusAI/dataset/cerberus_dataset_v1.jsonl` contiene 65 entradas derivadas de la documentación
original. Incluye ejemplos inválidos contra el compilador real (por ejemplo, `para(i = 0; ...)`
sin tipo, `salir` sin `;`, `clase programa {` sin paréntesis, arreglos y POO fuera de v1).

## 6.2 Dataset v2 (validado)

`CerberusAI/dataset/cerberus_dataset_v2.jsonl` contiene **93 entradas** con los campos
`instruction`, `response`, `category` y `source`. Se distribuyen en:

| Categoría | Entradas | Contenido |
|---|---|---|
| `generation` | 45 | Programas Cerberus completos |
| `explanation` | 14 | Explicación de construcciones |
| `syntax` | 12 | Preguntas de sintaxis |
| `errors_semantics` | 14 | Errores y sus mensajes |
| `compiler_concepts` | 8 | Conceptos del compilador |
| **Total** | **93** | |

**Validación.** Los 45 programas de `generation` se extrajeron y ejecutaron contra el compilador
real: **45/45 compilan**. El resto son respuestas textuales.

## 6.3 Preparación (`to_chat.py`, `split.py`)

- `to_chat.py` convierte cada entrada a formato de chat de Qwen2.5. Genera dos representaciones:
  - `messages`: `[system, user, assistant]`.
  - `text`: cadena renderizada en ChatML con `<|im_start|>rol ... <|im_end|>`.
- El prompt de sistema (`system_prompt.txt`) describe los tres roles (generar, explicar, corregir)
  y las reglas del lenguaje (clase con paréntesis, `para` con tipo, `salir;`, ausencia de POO,
  arreglos y `nulo`).
- `split.py` realiza una partición **estratificada por categoría**, **reproducible** (semilla 42,
  ratio 0.2). Resultado: **74 train / 19 eval**.

| Categoría | Total | Train | Eval |
|---|---|---|---|
| `compiler_concepts` | 8 | 6 | 2 |
| `errors_semantics` | 14 | 11 | 3 |
| `explanation` | 14 | 11 | 3 |
| `generation` | 45 | 36 | 9 |
| `syntax` | 12 | 10 | 2 |
| **Total** | **93** | **74** | **19** |

# 7. Fine-tuning QLoRA

## 7.1 Fundamentos

- **SFT (Supervised Fine-Tuning).** Entrenamiento supervisado sobre pares entrada/salida. En este
  caso, el modelo aprende a producir la respuesta del asistente dado el `system` y el `user`.
  La función de pérdida es la **entropía cruzada de modelo causal** (predicción del siguiente
  token), que es la que aplica `SFTTrainer` por defecto; no se sustituye por una función custom.
- **LoRA (Low-Rank Adaptation).** En lugar de actualizar los ~7 000 millones de parámetros del
  modelo base, se congelan y se entrenan matrices de bajo rango ("adaptadores") en determinadas
  capas. Reduce drásticamente el coste de entrenamiento y el tamaño del artefacto resultante.
- **QLoRA.** Combina LoRA con la **cuantización del modelo base a 4 bits**. El modelo se carga en
  formato NF4 (*NormalFloat4*) con doble cuantización; los adaptadores se entrenan en mayor
  precisión (bf16). Así un modelo de 7B cabe en 12 GB de VRAM.
- **Cuantización.** Reducción de la precisión numérica de los pesos (de 16/32 bits a 4/8 bits).
  Disminuye memoria y acelera la inferencia a cambio de una pequeña pérdida de calidad.
- **GGUF.** Formato de archivo binario de llama.cpp/Ollama que empaqueta pesos cuantizados y
  metadatos (incluida la plantilla de chat) para su ejecución eficiente.

## 7.2 Configuración del entrenamiento (`train_qlora.py`)

Estos son los **valores configurados** (defaults del script; el experimento usó los mismos):

| Elemento | Valor |
|---|---|
| Modelo base | `Qwen/Qwen2.5-Coder-7B-Instruct` |
| Cuantización | 4-bit, NF4, doble cuantización, `compute_dtype=bfloat16` |
| LoRA `r` / `alpha` / `dropout` | 16 / 32 / 0.05 |
| Módulos objetivo | `q_proj`, `k_proj`, `v_proj`, `o_proj`, `gate_proj`, `up_proj`, `down_proj` |
| `task_type` | `CAUSAL_LM` |
| Épocas | 5 |
| Batch por dispositivo | 2 |
| Acumulación de gradientes | 4 (batch efectivo 8) |
| Learning rate | 2e-4 |
| Scheduler | `cosine` con `warmup_steps=3` |
| Optimizador | `paged_adamw_8bit` |
| Precisión | `bf16=True` |
| Gradient checkpointing | activado |
| Campo de texto | `dataset_text_field="text"` |
| Longitud máxima | 2048 (`max_length`) |
| `packing` | `False` |
| Semilla | 42 |

> Nota de compatibilidad: el script se adaptó a **Transformers 5.19 y TRL 1.15**. En esta
> combinación, `warmup_ratio` ya no existe (se usa `warmup_steps`) y `max_seq_length` se denomina
> `max_length`; el tokenizador se pasa como `processing_class`.

## 7.3 Ejecución real (evidencia)

La evidencia proviene de `lora-adapter/checkpoint-50/trainer_state.json`, `finetune/train.log`,
`lora-adapter/adapter_config.json` y `lora-adapter/README.md`.

| Métrica / parámetro | Valor real |
|---|---|
| `global_step` / `max_steps` | 50 / 50 |
| `num_train_epochs` | 5 |
| `train_batch_size` | 2 |
| Learning rate observado | 2.0e-4 → 2.2e-7 (cosine) |
| `train_loss` del último paso | 0.0517 |
| `train_loss` promedio de la época 5 (`TrainOutput`) | **0.4042** |
| `eval_loss` final | **0.0827** |
| `mean_token_accuracy` (train / eval) | 0.9866 / 0.9816 |
| `train_runtime` | 711.6 s (~11.9 min) |
| `total_flos` | 9.92e15 |
| PEFT / TRL / Transformers / PyTorch / Datasets | 0.21.2 / 1.15.0 / 5.19.0 / 2.14.1 / 5.1.0 |

**Precisión terminológica importante.** El valor "loss 0.404" citado en `RUTA.md` y en el README
es el **promedio del loss de la época 5** (`train_loss` del `TrainOutput`), no el loss del último
paso (0.0517) ni el `eval_loss` (0.0827). La memoria debe citar los tres valores con su definición.

## 7.4 Entorno de ejecución

- El entrenamiento se ejecutó en **WSL2** (Linux + CUDA), no en Windows nativo, porque
  `bitsandbytes` no soporta oficialmente Windows.
- Se documenta un problema real de entorno: con **Python 3.14** sin `python3.14-dev`, el kernel
  Triton de TRL 1.x compila un helper con gcc y falla por `Python.h` ausente. El workaround
  registrado en `finetune/README.md` es descargar los `.deb` de `python3.14-dev`, extraerlos en
  `~/pyheaders` y exportar `CPATH` antes de entrenar.
- El hardware (RTX 3060 12 GB, 32 GB DDR4, Ryzen 7 5700X) figura en la documentación del proyecto
  **[NOV]** (no se verificó desde el repositorio).

# 8. Exportación, cuantización e inferencia

## 8.1 Merge del adaptador

`export_gguf.py`:

1. Carga el modelo base en **fp16 sobre CPU** (`device_map="cpu"`), porque el modelo completo en
   fp16 no cabe en la VRAM de 12 GB.
2. Aplica el adaptador con `PeftModel.from_pretrained` y ejecuta `merge_and_unload()`.
3. Guarda el modelo fusionado en `merged-model/` con `safe_serialization=True`.

Evidencia (`finetune/export.log`): carga de pesos en 339 shards, fusión y escritura del modelo
(~88 s). El pico de RAM ronda los **28 GB** (advertencia documentada).

## 8.2 Conversión a GGUF y cuantización

Con llama.cpp:

```bash
python llama.cpp/convert_hf_to_gguf.py merged-model --outfile cerberusai-f16.gguf --outtype f16
llama.cpp/build/bin/llama-quantize cerberusai-f16.gguf cerberusai-q8_0.gguf Q8_0
```

Se obtuvo `cerberusai-f16.gguf` y `cerberusai-q8_0.gguf` (≈7.5 GB, según `RUTA.md`).

## 8.3 Registro en Ollama

`finetune/Modelfile`:

```text
FROM ./cerberusai-q8_0.gguf

PARAMETER temperature 0.2
PARAMETER top_p 0.9
PARAMETER repeat_penalty 1.1
PARAMETER num_ctx 4096
```

```bash
ollama create cerberusai -f Modelfile
```

El prompt de sistema se envía desde la API (`system_prompt.txt`); no se fija en el `Modelfile`.

# 9. API FastAPI

## 9.1 Componentes y arranque

`CerberusAI/api/main.py` define una aplicación FastAPI (`CerberusAI API`, versión 0.1.0).

```bash
cd CerberusAI/api
pip install -r requirements-api.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

Dependencias: `fastapi`, `uvicorn[standard]`, `requests`.

## 9.2 Endpoints y contratos

**`GET /health`**

```json
{"status": "ok", "modelo": "cerberusai", "ollama": "http://localhost:11434"}
```

**`POST /chat`**

Petición (`ChatRequest`):

```json
{"role": "generar", "prompt": "imprime los numeros del 1 al 5 con un ciclo para"}
```

`role` admite `generar`, `explicar` y `corregir` (por defecto `generar`).

Respuesta (`ChatResponse`):

```json
{
  "texto": "...",
  "es_codigo": true,
  "codigo": "clase Programa(){ ... }",
  "compila": true,
  "error_compilacion": null
}
```

## 9.3 Comportamiento

1. Se aplica un prefijo según el rol (`ROLES`) a la petición.
2. Se envía a Ollama `POST /api/chat` (no streaming, `temperature=0.2`) con el `system` del
   `system_prompt.txt`.
3. Se extrae el código de la respuesta: primer bloque entre *fences* de Markdown o, si no hay,
   el texto completo si contiene una declaración `clase Nombre()`.
4. Para `generar` y `corregir`, el código se valida con `audit.CompiladorCLI` (subproceso Java,
   timeout 60 s). Para `explicar`, `compila` y `error_compilacion` son `null`.

## 9.4 Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL de Ollama |
| `MODEL` | `cerberusai` | Modelo servido |
| `SYSTEM_PROMPT_PATH` | `../finetune/system_prompt.txt` | Prompt de sistema |
| `CLASSPATH` | `../../target/classes` | Classpath del oráculo |
| `JAVA` | `java` | Ejecutable de Java |
| `COMPILER` | `audit.CompiladorCLI` | Clase del oráculo |
| `TIMEOUT` | `120` | Timeout (s) de la llamada a Ollama |
| `CERBERUSAI_API_KEY` | vacío | Clave exigida en `X-API-Key`; vacío = sin autenticación |
| `CERBERUSAI_RATE_LIMIT` | `20` | Peticiones por IP y ventana; `0` desactiva |
| `CERBERUSAI_RATE_WINDOW` | `60` | Ventana del rate limit, en segundos |

## 9.5 Errores

- `401` si `CERBERUSAI_API_KEY` está definida y `X-API-Key` falta o no coincide.
- `429` con `Retry-After` cuando se supera el rate limit por IP.
- `502` si Ollama no responde o devuelve error HTTP (`HTTPException` con `detail`).
- Si la validación con el oráculo falla por indisponibilidad (Java/classpath), la API **degrada**
  a `compila=null` sin romper la respuesta.

## 9.6 Exposición remota y acceso desde cualquier red

El acceso remoto se automatiza con `CerberusAI/api/tunel_rapido.cmd` (envoltorio de
`tunel_rapido.ps1`). El script:

1. Reutiliza `CERBERUSAI_API_KEY` o genera una clave aleatoria en `api_key.txt` (gitignored).
2. Arranca uvicorn en `127.0.0.1:8000` si la API no está activa.
3. Lanza `cloudflared tunnel --url http://127.0.0.1:8000 --no-autoupdate`.
4. Extrae la URL `https://...trycloudflare.com` (temporal, cambia en cada arranque), la imprime y
   la guarda en `url_tunel.txt` (gitignored).
5. Mantiene API y túnel activos hasta `Ctrl+C`.

La API exige `X-API-Key` cuando `CERBERUSAI_API_KEY` está definida y aplica un rate limit por IP
(`CERBERUSAI_RATE_LIMIT` = 20 peticiones, `CERBERUSAI_RATE_WINDOW` = 60 s por defecto). Detrás del
túnel usa `CF-Connecting-IP` (con `X-Forwarded-For` como respaldo). El cliente toma la URL de
**Opciones → Configurar API de CerberusAI...** (Preferences; `CERBERUSAI_API_URL` como fallback)
y envía la clave mediante `CERBERUSAI_API_KEY`.

# 10. Integración con el IDE

## 10.1 Cliente HTTP

`src/main/java/cliente/ClienteCerberusAI.java`:

- Java puro (sin JavaFX), `HttpClient` + Jackson.
- Fuerza **HTTP/1.1** (`HttpClient.Version.HTTP_1_1`).
- `URL_POR_DEFECTO = http://localhost:8000`; `TIEMPO_LIMITE_POR_DEFECTO = 180 s`;
  conexión `10 s`.
- `CERBERUSAI_API_URL` aporta la URL por defecto; la preferencia guardada desde
  **Opciones → Configurar API de CerberusAI...** tiene prioridad.
- `CERBERUSAI_API_KEY` aporta la clave; `chat()` y `ping()` envían `X-API-Key`.
- Records `Solicitud(String role, String prompt)` y
  `Respuesta(texto, es_codigo, codigo, compila, error_compilacion)`.
- Métodos `chat(rol, prompt)` y `ping()`; en error HTTP extrae el campo `detail` de FastAPI y
  añade el `Retry-After` cuando la respuesta es `429`.
- `module-info.java` requiere `java.net.http`, `java.prefs` y `com.fasterxml.jackson.databind`, y abre `cliente`
  a Jackson.

## 10.2 Panel de chat

- `src/main/resources/ui/chat_cerberus.fxml` + `src/main/java/ui/ChatCerberusController.java`.
- Componente **autocontenido**: no conoce el editor. El contenedor conecta un handler:

```java
chatCerberusController.setInsertarHandler(codigo -> txtCodigo.setText(codigo));
```

- Roles en un `ComboBox` (Generar, Explicar, Corregir), prompt, botón ENVIAR y botón
  **INSERTAR EN EDITOR** (deshabilitado si no hay código o no hay handler).
- La URL de la API **no se muestra** en el chat: se configura en
  `Opciones → Configurar API de CerberusAI...` y se persiste con `java.util.prefs.Preferences`
  (`cerberusai.api.url`); `CERBERUSAI_API_URL` actúa como fallback.
- La llamada se ejecuta en un `Task` de JavaFX para no bloquear la UI; el estado muestra si el
  código compila (`compila`).
- `main/PreviewChat.java` es un lanzador de desarrollo que muestra solo el panel junto a un editor
  simulado; incluye un *smoke test* sin ventana con `CERBERUS_PREVIEW_CHECK=1`.

## 10.3 Estado de integración

El componente está **integrado en la ventana principal** (`ui/principal.fxml`) mediante
`<fx:include source="chat_cerberus.fxml" fx:id="chatCerberus"/>` y el campo
`chatCerberusController` del `InterfazPrincipalController`. El panel incluye la cabecera de
CERBERUSai y el botón **INSERTAR EN EDITOR** escribe en `txtCodigo`.

# 11. Evaluación experimental

## 11.1 Harness

`CerberusAI/eval/eval_harness.py`:

- Carga tareas de un dataset (por defecto `eval.jsonl`, categoría `generation`).
- Genera con Ollama (`POST /api/chat`, `temperature=0.0`).
- Extrae el código (fences o programa completo).
- Lo valida con `audit.CompiladorCLI` (subproceso Java, UTF-8).
- Métricas: **tasa de compilación** (primaria), **exact-match normalizado** (secundaria) y
  **desglose de errores**.
- Solo usa la biblioteca estándar de Python.

`CerberusAI/eval/run_eval.py` ejecuta el harness sobre dos modelos y compara.

## 11.2 Diseño experimental

- Conjunto de evaluación: las **9 tareas `generation`** del split de evaluación (`eval.jsonl`),
  generadas con semilla 42 y estratificación por categoría.
- Modelos comparados: `qwen2.5-coder:7b` (base, Ollama) y `cerberusai` (fine-tuned).
- Mismo `system_prompt`, misma temperatura (0.0) y mismo oráculo para ambos.

## 11.3 Resultados registrados

Evidencia: `CerberusAI/eval/resultados_base_vs_ft.txt`.

| Modelo | Compilación | Exact-match |
|---|---|---|
| `qwen2.5-coder:7b` | 4/9 (44 %) | 0/9 (0 %) |
| `cerberusai` | 8/9 (89 %) | 3/9 (33 %) |

Mejora en tasa de compilación: **+44 puntos porcentuales**.

Errores típicos del modelo base: uso de una función inexistente (`Funcion no encontrada: leer`),
uso de palabras reservadas como identificadores, ausencia de `principal` y construcciones no
reconocidas. El fine-tuned reduce esos fallos a un único caso (`No se reconoce la instruccion`).

## 11.4 Verificación del compilador (independiente del modelo)

- `audit.TestSuite`: **99/99**.
- `audit.CompilerProbe`: **48/48**.

## 11.5 Limitaciones experimentales

1. **Tamaño muestral**: 9 tareas de generación; insuficiente para conclusiones estadísticas.
2. **Una sola semilla**: no hay repeticiones ni intervalos de confianza.
3. **Exact-match frágil**: normaliza espacios, pero el modelo elige su propio formato y nombres;
   la métrica primaria es la compilación.
4. **Sin evaluación de explicación/corrección**: no hay intérprete que permita comparar salidas;
   solo se mide compilación de código.
5. **Posible ambigüedad del modelo base**: el plan menciona `qwen2.5-coder:7b-instruct`, mientras
   los resultados usan `qwen2.5-coder:7b`. Debe confirmarse que es el mismo modelo.
6. **Sesgo de dominio**: el dataset es pequeño (93 entradas) y de una sola fuente/documentación.

# 12. Instalación y operación

## 12.1 Requisitos

- Windows + JDK 24 (compilador y IDE).
- Python 3.14 (venv en `CerberusAI/.venv` para la API).
- WSL2 con CUDA (fine-tuning).
- Ollama.

## 12.2 Compilador

```cmd
mvnw.cmd clean compile
java -cp target\classes audit.TestSuite
java -cp target\classes audit.CompilerProbe
java -cp target\classes audit.CompiladorCLI tests\valid\01_hola_mundo.txt
```

## 12.3 Datos y fine-tuning (WSL2)

```bash
cd CerberusAI/finetune
python to_chat.py --input ../dataset/cerberus_dataset_v2.jsonl --output chat_all.jsonl
python split.py --input chat_all.jsonl --out-dir . --eval-ratio 0.2 --seed 42
python train_qlora.py --check
python train_qlora.py --epochs 5
python export_gguf.py
# convertir a GGUF y cuantizar (llama.cpp)
ollama create cerberusai -f Modelfile
```

## 12.4 API y túnel

```powershell
cd CerberusAI\api
.\tunel_rapido.cmd
```

El script arranca uvicorn en `127.0.0.1:8000`, genera/reutiliza `api_key.txt`, lanza
`cloudflared` y escribe la URL pública en `url_tunel.txt`. Para un arranque manual equivalente:

```powershell
cd CerberusAI
$env:CERBERUSAI_API_KEY = "una-clave-larga-y-aleatoria"
.\.venv\Scripts\python.exe -m uvicorn api.main:app --host 127.0.0.1 --port 8000
cloudflared tunnel --url http://localhost:8000
```

## 12.5 Mantenimiento

- Actualizar `.gitignore` para artefactos pesados (`lora-adapter/`, `merged-model/`, `*.gguf`,
  `*.log`, `.venv/`, `__pycache__/`, `*.pyc`).
- Re-ejecutar el harness tras cambios de modelo; regenerar el split con semilla fija.
- Reentrenar si cambia el compilador o el dataset; versionar el `adapter_config.json`.

# 13. Seguridad

## 13.1 Protecciones existentes

- El código generado **no se ejecuta**: el oráculo solo lo analiza (lexer/parser). No hay ejecución
  arbitraria de código del modelo.
- La API valida el código con el oráculo antes de marcarlo como insertable.
- El `Modelfile` fija parámetros conservadores; el prompt de sistema restringe el dominio.
- El modelo y los datos permanecen locales.
- Si `CERBERUSAI_API_KEY` está definida, la API exige `X-API-Key` en todos los endpoints.
- Rate limit por IP en memoria (`CERBERUSAI_RATE_LIMIT`/`CERBERUSAI_RATE_WINDOW`); detrás del túnel
  se identifica al cliente con `CF-Connecting-IP`/`X-Forwarded-For`.
- El script del túnel genera una clave aleatoria y la guarda en `api_key.txt` (gitignored).

## 13.2 Riesgos

1. **Clave filtrada o compartida**: el quick tunnel publica la API en Internet; quien tenga la URL
   y la `X-API-Key` puede invocarla (abuso, coste de GPU, denegación de servicio).
2. **Rate limit en memoria**: se reinicia con el proceso y no se comparte entre varios workers; no
   sustituye a Cloudflare Access ni a cuotas por usuario.
3. **Sin CORS configurado** (no afecta al cliente Java, pero limita un futuro cliente web).
4. **Prompt injection**: un usuario podría intentar que el modelo ignore el sistema; el impacto se
   limita a la respuesta (no se ejecuta código).
5. **Subproceso del oráculo**: la API invoca `java` con el `CLASSPATH` configurado; un classpath
   malicioso podría cargar clases no previstas. Debe mantenerse bajo control del operador.
6. **Dependencias**: `pip install` sin versiones fijas puede introducir cambios incompatibles.
7. **Clave en disco**: `api_key.txt` es un secreto local en texto plano; no debe versionarse ni
   compartirse.

## 13.3 Mejoras recomendadas

- **Cloudflare Access** o una capa de identidad para el túnel (además de `X-API-Key`).
- Cuotas por usuario, límites distribuidos y timeouts estrictos.
- Rotar `CERBERUSAI_API_KEY` y no reutilizarla entre equipos.
- Fijar versiones en los `requirements` y verificar hashes.
- Endurecer el arranque del subproceso (classpath absoluto y de confianza).
- CORS explícito si se añade cliente web.
- Registro de peticiones sin datos sensibles.

# 14. Decisiones de diseño, alternativas y trabajo futuro

## 14.1 Decisiones

- **Modelo local (Ollama)**: privacidad y control; el modelo vive en la máquina con GPU.
- **QLoRA 4-bit**: única opción viable en 12 GB de VRAM para un 7B.
- **Oráculo con el compilador real**: evita métricas subjetivas para código.
- **ChatML pre-renderizado**: simplicidad y compatibilidad con `dataset_text_field`.
- **API intermedia**: desacopla el IDE (Java) del ecosistema Python/IA.
- **Quick tunnel**: acceso remoto sin abrir puertos ni contratar infraestructura.

## 14.2 Alternativas consideradas o posibles

- Fine-tuning completo (no viable por VRAM) o LoRA en precisión completa.
- Otros modelos base (p. ej. variantes más pequeñas) para acelerar iteración.
- Inferencia directa con Transformers en lugar de Ollama (más control, más complejidad).
- Despliegue de la API en AWS con túnel inverso (descartado por simplicidad).

## 14.3 Trabajo futuro

1. **Ampliar el dataset** (más generación y ejemplos de corrección de código roto).
2. **Evaluar explicación/corrección** (LLM-as-judge u otra métrica).
3. **Mejorar el chat** con historial/persistencia de conversación (la integración base ya está hecha).
4. **Dominio estable** y **Cloudflare Access** para el túnel (la autenticación por API Key ya está implementada).
5. **Repetir experimentos** con varias semillas y un conjunto mayor.
6. **Aumento de datos** para robustez.

# 15. Referencias

Las siguientes son fuentes reales recomendadas; las URLs deben verificarse antes de publicar.

1. Hu, E. et al. *LoRA: Low-Rank Adaptation of Large Language Models*. arXiv:2106.09685.
2. Dettmers, T. et al. *QLoRA: Efficient Finetuning of Quantized LLMs*. arXiv:2305.14314.
3. Hugging Face. *TRL: Transformer Reinforcement Learning* (documentación del proyecto).
4. Qwen Team. *Qwen2.5-Coder* (model card en Hugging Face).
5. Ollama. Documentación oficial de la API y de `Modelfile`.
6. llama.cpp. Documentación de conversión a GGUF y cuantización.
7. FastAPI. Documentación oficial.
8. Hugging Face. *PEFT* (Parameter-Efficient Fine-Tuning).
9. bitsandbytes. Documentación oficial (cuantización 4/8 bits).

---

## Anexo A. Trazabilidad de evidencia

| Afirmación | Evidencia |
|---|---|
| QLoRA 4-bit NF4/bf16/doble cuantización | `finetune/train_qlora.py`, líneas 117–122 |
| LoRA r=16, α=32, dropout 0.05, 7 módulos | `train_qlora.py` 128–136; `lora-adapter/adapter_config.json` |
| 5 epochs, 50 pasos, batch 2, grad-accum 4, lr 2e-4 | `train_qlora.py` 141–162; `trainer_state.json` |
| Loss promedio época 5 = 0.4042 | `finetune/train.log` (`train_loss: 0.4042`) |
| Loss final del paso = 0.0517 | `trainer_state.json` (step 50) |
| eval_loss final = 0.0827 | `trainer_state.json` |
| Runtime ~11.9 min | `train.log` (`train_runtime: 711.6`) |
| Merge fp16 CPU, 339 shards | `finetune/export.log` |
| GGUF Q8_0 ≈7.5 GB | `RUTA.md` |
| Eval 44 % → 89 %, exact 33 % | `eval/resultados_base_vs_ft.txt` |
| API `/health`, `/chat`, validación | `api/main.py` |
| Cliente HTTP/1.1 + Jackson | `cliente/ClienteCerberusAI.java` |
| Chat integrado con insertar en editor | `ui/ChatCerberusController.java`; `ui/chat_cerberus.fxml`; `ui/principal.fxml` |
| Suite 99/99 y sonda 48/48 | `audit.TestSuite`, `audit.CompilerProbe` |

## Anexo B. Nota de reproducibilidad documental

Este documento se genera a partir de `CerberusAI/docs/CERBERUSAI_MEMORIA.md` mediante Pandoc
(3.9). La fuente Markdown es la única fuente de verdad; el `.docx` se regenera con:

```bash
python -c "import pypandoc; pypandoc.convert_file('CERBERUSAI_MEMORIA.md','docx',outputfile='CERBERUSAI_MEMORIA.docx',extra_args=['--toc','--toc-depth=2','--number-sections'])"
```

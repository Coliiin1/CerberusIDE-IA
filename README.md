# CerberusIDE & CerberusAI

**CerberusIDE** es un Entorno de Desarrollo Integrado (IDE) y analizador para un lenguaje de pseudocódigo en español llamado **Cerberus**. Está construido en **Java 24** con **JavaFX** y una arquitectura desacoplada para el motor de análisis y compilación.

El repositorio incluye además el módulo **CerberusAI**: el pipeline completo de Inteligencia Artificial del lenguaje — dataset validado contra el compilador, fine-tuning QLoRA de un modelo local (Qwen2.5-Coder-7B), evaluación automática, una API (FastAPI + Ollama) y un componente de chat para el IDE.

---

## Características principales

- **Editor de código**:
  - Numeración reactiva de líneas sincronizada con el scroll.
  - Temas visuales dinámicos (**Modo Oscuro** por defecto y **Modo Claro**).
- **Flujo de análisis por fases**:
  - **Analizador léxico**: tokenizador por expresiones regulares y normalización léxica, con tabla de tokens.
  - **Analizador sintáctico**: parser descendente recursivo con pilas de scopes; valida balanceo de delimitadores (`()`, `{}`, `[]`), declaraciones, tipos, funciones (parámetros y tipo de retorno), llamadas, estructuras de control y asignaciones compuestas. Los errores son tipados (`ErrorSintactico`) con línea, columna y lexema.
  - **Analizador semántico**: `AnalizadorSemantico` + `Tipo` resuelven identificadores en el scope visible y validan compatibilidad de tipos; la UI muestra el resultado real del análisis.
  - **Documentador automático**: bitácora en lenguaje natural de clases, funciones y estructuras creadas.
- **Gestión de archivos**: creación, apertura y persistencia directa en formato `.txt`.
- **Chat CerberusAI integrado**: panel de chat con roles **generar / explicar / corregir** que consume la API, con botón "Insertar en editor" mediante callback. Está integrado en la ventana principal (`ui/principal.fxml` + `ui.ChatCerberusController`), con la URL de la API configurable desde **Opciones → Configurar API de CerberusAI...** (no se muestra en el chat); `main.PreviewChat` sigue como lanzador de desarrollo.

---

## El Lenguaje Cerberus

Cerberus es un lenguaje imperativo y estructurado con palabras reservadas íntegramente en español.

### Palabras Reservadas y Tipos

| Categoría | Palabras Clave |
| :--- | :--- |
| **Estructura** | `clase`, `principal`, `funcion`, `retornar` |
| **Tipos de Datos** | `entero`, `real`, `cadena`, `caracter`, `booleano`, `vacio`, `nulo` |
| **Estructuras de Control** | `si`, `sino`, `para`, `mientras`, `hacer`, `segun`, `caso`, `predeterminado`, `salir` |
| **Entrada / Salida** | `imprimir` |
| **Literales Booleanos** | `verdadero`, `falso` |
| **Operadores y Asignación** | `+`, `-`, `*`, `/`, `%`, `++`, `--`, `+=`, `-=`, `*=`, `/=`, `=`, `==`, `!=`, `<`, `>`, `<=`, `>=`, `&`, `|`, `!` |

### Alcance v1

- `nulo` está **reservado pero no implementado** (no hay tipos por referencia); `vacio` es el tipo de retorno sin valor.
- **POO** (`nuevo`, `este`, `publico`, `privado`, acceso a miembros `x.y`) y **arreglos** se rechazan explícitamente (`POO no implementada en v1` / `Arreglos no implementados en v1`).
- El compilador real es la fuente de verdad: ver [`CerberusAI_COMPILER_AUDIT.md`](CerberusAI_COMPILER_AUDIT.md) y [`CerberusAI/knowledge/language_spec.md`](CerberusAI/knowledge/language_spec.md).

### Estructura de un Programa

Un programa Cerberus se compone de variables globales opcionales, una definición de clase y un bloque de funciones donde la función `principal()` es obligatoria.

```cerberus
clase MiPrograma() {
    funcion entero doble(entero n) {
        retornar n * 2;
    }

    funcion principal() {
        entero edad = 20;
        cadena saludo = "Hola Cerberus";

        si (edad >= 18) {
            imprimir(saludo + " - Mayor de edad");
        } sino {
            imprimir("Menor de edad");
        }

        para (entero i = 0; i < 5; i++) {
            imprimir("Doble de " + i + ": " + doble(i));
        }
    }
}
```

---

## Arquitectura del Proyecto

El proyecto es modular (JPMS, `module-info.java`) y mantiene la lógica del compilador aislada de la interfaz gráfica mediante la interfaz funcional `Util.Reporter`:

```
CerberusIDE-IA/
├── CerberusAI/                        # Módulo de Inteligencia Artificial (ver su README)
│   ├── dataset/
│   │   ├── cerberus_dataset_v1.jsonl  # Dataset original (incluye entradas inválidas)
│   │   └── cerberus_dataset_v2.jsonl  # Dataset validado contra el compilador (93 entradas)
│   ├── knowledge/
│   │   └── language_spec.md           # Especificación del lenguaje (reconciliada)
│   ├── finetune/                      # Fases 2–3: datos, QLoRA, export GGUF (ver README)
│   ├── eval/                          # Fase 4: harness y resultados base vs fine-tuned
│   ├── api/                           # Fase 5: API FastAPI sobre Ollama (ver README)
│   └── README.md
├── src/main/
│   ├── java/
│   │   ├── module-info.java           # JPMS: JavaFX, java.net.http, Jackson
│   │   ├── AnalizadorLexico/          # Tokenizador (AnalizadorLexico, Tokens, Token, Identificadores)
│   │   ├── AnalizadorSintactico/      # Parser + Documentador + ErrorSintactico
│   │   ├── AnalizadorSemantico/       # Validación semántica (AnalizadorSemantico, Tipo)
│   │   ├── Archivos/                  # Persistencia de archivos de texto (Archivo)
│   │   ├── Util/                      # Reporter (desacople UI ↔ compilador)
│   │   ├── audit/                     # Herramientas headless: CompilerProbe, Evaluador, CompiladorCLI, TestSuite
│   │   ├── cliente/                   # ClienteCerberusAI (HTTP + Jackson, Java puro)
│   │   ├── main/                      # Main, CerberusApp y PreviewChat (lanzador dev)
│   │   └── ui/                        # InterfazPrincipalController, ChatCerberusController
│   └── resources/
│       ├── Imagenes/                  # Logotipos e íconos de la aplicación
│       └── ui/                        # principal.fxml, chat_cerberus.fxml, estilos.css
├── Code Examples/                     # Programas Cerberus de ejemplo (.txt)
├── tests/                             # Suite del compilador (valid/ invalid/ expected/)
├── pom.xml                            # Maven (Java 24, OpenJFX 24, Jackson)
├── mvnw / mvnw.cmd                    # Maven Wrapper
├── run.cmd                            # Script de ejecución rápida en Windows (local, no versionado)
├── RUTA.md                            # Bitácora viva del proyecto
├── CERBERUSIA_IMPLEMENTATION.md       # Plan del módulo de IA
├── CerberusAI_COMPILER_AUDIT.md       # Auditoría real del compilador
└── GramaticasAttendance.md            # Gramáticas pendientes / fuera de v1
```

`module-info.java` declara `requires java.net.http` y `requires com.fasterxml.jackson.databind`, con `opens cliente to com.fasterxml.jackson.databind` (Jackson bajo JPMS).

---

## Requisitos del Sistema

- **Java Development Kit (JDK)**: Versión **24** o superior.
- **JavaFX SDK**: Versión **24.0.2** (gestionado automáticamente vía dependencias de Maven).
- **Maven**: 3.9+ (no requiere instalación global previa gracias al Maven Wrapper incluido).

Módulo CerberusAI (opcional, solo para la IA):

- **Ollama** con el modelo `cerberusai` registrado (ver [`CerberusAI/finetune/README.md`](CerberusAI/finetune/README.md)).
- **Python** con el venv de la API (`CerberusAI/.venv`) y `CerberusAI/api/requirements-api.txt`.
- **cloudflared** (opcional) para exponer la API por un quick tunnel de Cloudflare.
- Re-entrenar el modelo requiere **WSL2 + CUDA** (la inferencia/servicio corre en Windows).

---

## Compilación y Ejecución

### Opción 1: Ejecutar con Maven Wrapper (Recomendado)

En sistemas Windows:
```cmd
mvnw.cmd clean javafx:run
```

En sistemas Linux / macOS:
```bash
./mvnw clean javafx:run
```

### Opción 2: Empaquetar el proyecto

Para compilar y empaquetar el binario en la carpeta `target/`:
```cmd
mvnw.cmd clean package
```

### Opción 3: Script local de Windows (`run.cmd`)

Si se cuenta con un SDK de JavaFX descargado localmente, se puede utilizar el script `run.cmd` (archivo local, no versionado):
1. Configurar la variable `PATH_TO_FX` con la ruta local a la carpeta `lib` del SDK de JavaFX dentro de `run.cmd`.
2. Ejecutar:
```cmd
run.cmd
```

### Tests y herramientas headless

```cmd
mvnw.cmd compile
java -cp target\classes audit.TestSuite
java -cp target\classes audit.CompilerProbe
java -cp target\classes audit.CompiladorCLI tests\valid\01_hola_mundo.txt
```

- `audit.TestSuite`: suite del compilador (99 casos; exit code 0 = todo OK).
- `audit.CompilerProbe`: sonda accept/reject de fragmentos.
- `audit.CompiladorCLI`: oráculo headless (imprime JSON `{aceptado, error, linea, columna, lexema}`).

### Preview del chat (desarrollo)

```powershell
.\mvnw.cmd javafx:run "-Dmain.class=main.PreviewChat"

# Smoke sin ventana (requiere la API arriba):
$env:CERBERUS_PREVIEW_CHECK="1"; .\mvnw.cmd javafx:run "-Dmain.class=main.PreviewChat"
```

### API y evaluación (módulo AI)

```cmd
cd CerberusAI\api
..\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

```cmd
REM Acceso remoto: arranca la API y el quick tunnel, con API Key y rate limit
cd CerberusAI\api
tunel_rapido.cmd
```

```cmd
py CerberusAI\eval\run_eval.py --base-model qwen2.5-coder:7b
```

Detalle en [`CerberusAI/api/README.md`](CerberusAI/api/README.md) y [`CerberusAI/finetune/README.md`](CerberusAI/finetune/README.md).

---

## Módulo CerberusAI (resumen)

| Fase | Entregable | Estado |
| :--- | :--- | :--- |
| 1. Oráculo | `audit/CompiladorCLI` + `audit/Evaluador` (validación headless) | HECHO |
| 2. Datos | `to_chat.py` / `split.py` → `train.jsonl` (74) y `eval.jsonl` (19) | HECHO |
| 3. Fine-tuning | QLoRA 4-bit de Qwen2.5-Coder-7B (loss 0.404) → GGUF Q8_0 → Ollama `cerberusai` | HECHO |
| 4. Evaluación | 44% → **89%** de compilación (exact-match 33%) — `eval/resultados_base_vs_ft.txt` | HECHO |
| 5. API | FastAPI `GET /health` y `POST /chat` (roles + validación con el oráculo) + acceso remoto con `X-API-Key` y rate limit por IP | HECHO |
| 6. Chat IDE | Chat integrado en `principal.fxml` + `PreviewChat` dev; URL de API desde Opciones | HECHO |
| 7. Expansión | Ampliar dataset (generación + corrección), re-entrenar, dominio estable | FUTURO |

- **Dataset v2**: 93 entradas; los 45 programas de categoría `generation` compilan contra el compilador.
- **Exposición remota**: `CerberusAI/api/tunel_rapido.cmd` arranca la API local y un quick tunnel de Cloudflare; exige `X-API-Key` (`CERBERUSAI_API_KEY`) y limita peticiones por IP. La URL cambia en cada arranque; se configura en **Opciones → Configurar API de CerberusAI...** (Preferences) o con `CERBERUSAI_API_URL`, y la clave se toma de `CERBERUSAI_API_KEY`.
- Detalles completos en [`CerberusAI/README.md`](CerberusAI/README.md).

---

## Archivos de Prueba

En la carpeta [`Code Examples/`](Code%20Examples/) hay programas de ejemplo listos para cargar
en el IDE o probar en el analizador:
- [`prueba 1.txt`](Code%20Examples/prueba%201.txt): Ejemplo exhaustivo con variables, condicionales (`si`/`sino`), ciclos (`para`, `mientras`, `hacer mientras`), selección (`segun`/`caso`) e impresiones.
- [`cicloPara.txt`](Code%20Examples/cicloPara.txt): Ejemplo enfocado en variables globales y ciclos `para`.
- [`imprimir.txt`](Code%20Examples/imprimir.txt): Ejemplo de sintaxis básica de impresión.

Además, [`tests/`](tests/) contiene la suite headless del compilador: `valid/` (deben aceptarse), `invalid/` (deben rechazarse) y `expected/invalid/` (subcadena de error esperada).

---

## Documentación

- [`RUTA.md`](RUTA.md) — bitácora viva del avance (pasos y entregables).
- [`CERBERUSIA_IMPLEMENTATION.md`](CERBERUSIA_IMPLEMENTATION.md) — plan de implementación del módulo de IA (Fases 0–7).
- [`CerberusAI_COMPILER_AUDIT.md`](CerberusAI_COMPILER_AUDIT.md) — auditoría de lo que el compilador acepta realmente.
- [`GramaticasAttendance.md`](GramaticasAttendance.md) — gramáticas incompletas o fuera de v1, con prioridades.
- [`CerberusAI/knowledge/language_spec.md`](CerberusAI/knowledge/language_spec.md) — especificación consolidada del lenguaje.
- [`CerberusAI/README.md`](CerberusAI/README.md) — hub del módulo de IA.
- [`CerberusAI/finetune/README.md`](CerberusAI/finetune/README.md) — datos y fine-tuning QLoRA (Fases 2–3).
- [`CerberusAI/api/README.md`](CerberusAI/api/README.md) — API FastAPI, configuración y túnel.

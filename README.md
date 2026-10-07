# CerberusIDE & CerberusAI

**CerberusIDE** es un Entorno de Desarrollo Integrado (IDE) y analizador para un lenguaje de pseudocódigo en español llamado **Cerberus**. Está construido en **Java 24** utilizando **JavaFX** para su interfaz gráfica y una arquitectura desacoplada para el motor de análisis y compilación.

El repositorio incluye además el módulo **CerberusAI**, que provee conjuntos de datos preparados para el entrenamiento y fine-tuning de modelos de Inteligencia Artificial orientados a la generación y análisis de código en el lenguaje Cerberus.

---

## Características Principales

- **Editor de Código Intuitivo**:
  - Numeración reactiva de líneas sincronizada con el scroll.
  - Soporte de temas visuales dinámicos (**Modo Oscuro** por defecto y **Modo Claro**).
- **Flujo de Análisis por Fases**:
  - **Analizador Léxico**: Reconocimiento de tokens basado en expresiones regulares y normalización léxica, con visualización de la lista de tokens extraídos.
  - **Analizador Sintáctico**: Validador sintáctico descendente recursivo con comprobación estricta de balanceo de delimitadores (`()`, `{}`, `[]`) mediante pila.
  - **Analizador Semántico**: Estructura base para validación de tipos e identificadores en la tabla de símbolos.
  - **Documentador Automático**: Genera una bitácora en lenguaje natural describiendo las clases, funciones y estructuras creadas durante el análisis.
- **Gestión de Archivos**:
  - Creación de nuevos archivos, apertura y persistencia directa en formato `.txt`.
- **Módulo CerberusAI**:
  - Dataset en formato JSON Lines (`.jsonl`) con pares de instrucción-código para alimentar modelos de lenguaje.

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

### Estructura de un Programa

Un programa Cerberus se compone de variables globales opcionales, una definición de clase y un bloque de funciones donde la función `principal()` es obligatoria.

```cerberus
clase MiPrograma() {
    funcion principal() {
        entero edad = 20;
        real promedio = 9.5;
        cadena saludo = "Hola Cerberus";
        
        si (edad >= 18) {
            imprimir(saludo + " - Mayor de edad");
        } sino {
            imprimir("Menor de edad");
        }

        para (entero i = 0; i < 5; i++) {
            imprimir("Iteracion: " + i);
        }
    }
}
```

---

## Arquitectura del Proyecto

El proyecto está diseñado de forma modular, manteniendo la lógica del compilador aislada de la interfaz gráfica mediante la interfaz funcional `Util.Reporter`:

```
CerberusIDE-IA/
├── CerberusAI/                        # Módulo de Inteligencia Artificial
│   ├── dataset/
│   │   └── cerberus_dataset_v1.jsonl  # Dataset de instrucciones y respuestas
│   └── README.md
├── src/main/
│   ├── java/
│   │   ├── AnalizadorLexico/          # Tokenizador y definiciones de tokens
│   │   │   ├── AnalizadorLexico.java
│   │   │   ├── Identificadores.java
│   │   │   ├── Token.java
│   │   │   └── Tokens.java
│   │   ├── AnalizadorSintactico/      # Parser descendente recursivo y documentador
│   │   │   ├── AnalizadorSintactico.java
│   │   │   ├── Documentador.java
│   │   │   └── ErrorSintactico.java
│   │   ├── AnalizadorSemantico/       # Verificación semántica y tabla de tipos
│   │   │   └── AnalizadorSemantico.java
│   │   ├── Archivos/                  # Persistencia y lectura de archivos de texto
│   │   │   └── Archivo.java
│   │   ├── main/                      # Punto de entrada y aplicación JavaFX
│   │   │   ├── CerberusApp.java
│   │   │   └── Main.java
│   │   ├── ui/                        # Controlador de la interfaz gráfica
│   │   │   └── InterfazPrincipalController.java
│   │   └── Util/                      # Utilerías e interfaces de reporte desacopladas
│   │       └── Reporter.java
│   └── resources/
│       ├── Imagenes/                  # Logotipos e íconos de la aplicación
│       └── ui/                        # Vistas FXML y hojas de estilo CSS
│           ├── estilos.css
│           └── principal.fxml
├── pom.xml                            # Configuración Maven (Java 24, OpenJFX 24)
├── mvnw / mvnw.cmd                    # Maven Wrapper
└── run.cmd                            # Script de ejecución rápida en Windows con SDK local
```

---

## Requisitos del Sistema

- **Java Development Kit (JDK)**: Versión **24** o superior.
- **JavaFX SDK**: Versión **24.0.2** (gestionado automáticamente vía dependencias de Maven).
- **Maven**: 3.9+ (no requiere instalación global previa gracias al Maven Wrapper incluido).

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

Si se cuenta con un SDK de JavaFX descargado localmente, se puede utilizar el script [`run.cmd`](run.cmd):
1. Configurar la variable `PATH_TO_FX` con la ruta local a la carpeta `lib` del SDK de JavaFX dentro de `run.cmd`.
2. Ejecutar:
```cmd
run.cmd
```

---

## Archivos de Prueba

En la raíz del proyecto se incluyen ejemplos listos para cargar en el IDE o probar en el analizador:
- [`prueba 1.txt`](prueba%201.txt): Ejemplo exhaustivo con variables, condicionales (`si`/`sino`), ciclos (`para`, `mientras`, `hacer mientras`), selección (`segun`/`caso`) e impresiones.
- [`cicloPara.txt`](cicloPara.txt): Ejemplo enfocado en variables globales y ciclos `para`.
- [`imprimir.txt`](imprimir.txt): Ejemplo de sintaxis básica de impresión.

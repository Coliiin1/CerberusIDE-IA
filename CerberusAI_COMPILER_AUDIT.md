# CerberusAI — Auditoría del compilador

Fecha de auditoría: 2026-10-04
Proyecto auditado: CerberusIDE-IA
Fuentes: código fuente Java (`AnalizadorLexico`, `AnalizadorSintactico`, `AnalizadorSemantico`), `CerberusAI/knowledge/language_spec.md`, `CerberusAI/dataset/cerberus_dataset_v1.jsonl` y `Documentación CerberusIDE.pdf`.

## Objetivo

Comparar la documentación de Cerberus con la implementación Java real del compilador para evitar entrenar al modelo con sintaxis que el compilador no acepta.

## Metodología

1. **Análisis estático** del lexer, parser y analizador semántico.
2. **Verificación dinámica** con una sonda headless (`src/main/java/audit/CompilerProbe.java`) que ejecuta snippets reales contra el compilador (sin JavaFX) y registra aceptación/rechazo y el mensaje de error.

Ejecutar la sonda:

```cmd
mvnw.cmd compile
java -cp target\classes audit.CompilerProbe
```

> Nota: la sonda no usa JavaFX, por lo que no necesita `--module-path`. El `module-info.java` se ignora al ejecutar en modo classpath.

## Componentes encontrados

- `src/main/java/AnalizadorLexico/AnalizadorLexico.java`
- `src/main/java/AnalizadorLexico/Tokens.java`
- `src/main/java/AnalizadorLexico/Token.java`
- `src/main/java/AnalizadorLexico/Identificadores.java`
- `src/main/java/AnalizadorSintactico/AnalizadorSintactico.java`
- `src/main/java/AnalizadorSintactico/ErrorSintactico.java`
- `src/main/java/AnalizadorSintactico/Documentador.java`
- `src/main/java/AnalizadorSemantico/AnalizadorSemantico.java`
- JavaFX IDE en `ui/`, `main/`, `Archivos/` y recursos FXML/CSS.
- `CerberusAI/dataset/cerberus_dataset_v1.jsonl`
- `CerberusAI/knowledge/language_spec.md`
- Sonda de auditoría `src/main/java/audit/CompilerProbe.java`

## Estados usados

- `IMPLEMENTED`: confirmado en el código fuente y verificado con la sonda.
- `PARTIAL`: existe soporte, pero es incompleto o inconsistente.
- `DOCUMENTED`: aparece en documentación, pero no se confirmó implementación suficiente.
- `CONFLICT`: documentación y código presentan reglas distintas.
- `PENDING`: no existe evidencia suficiente para definir la regla.

## Hallazgos

### Léxico

Implementados en `AnalizadorLexico.analizador()`:

`clase`, `nuevo`, `este`, `publico`, `privado`, `si`, `sino`, `segun`, `caso`, `salir`, `para`, `mientras`, `verdadero`, `falso`, `funcion`, `retornar`, `imprimir`, `entero`, `real`, `caracter`, `cadena`, `booleano`, `nulo`, `hacer`, `vacio`, `principal`.

Operadores/tokenización presentes:

`+ - * / %`, `=`, `++ --`, `*= /=`, `== != < > <= >=`, `& | !`, delimitadores `() {} [] ; :`, identificadores, enteros, reales, cadenas y caracteres.

**Operadores compuestos (`+=`, `-=`, `*=`, `/=`):** `ROTO/CONFLICT`. El `switch` de `analizador()` solo tiene `case "*="` (MUL_VARIABLE) y `case "/="` (DIV_VARIABLE); no existen `case "+="` ni `case "-="`. Además, `seccionarCadena()` intenta colapsar `+ =` → `+=`, `* =` → `*=`, `/ =` → `/=` (nunca `- =`), pero por el espaciado generado (`+`→` + `, luego `=`→` = `) el patrón con un solo espacio **no coincide**. La sonda confirma que los cuatro (`+=`, `-=`, `*=`, `/=`) son rechazados (error `Se esperaba PUNTO_COMA y se encontró OPERADOR_*`). En ningún caso se usan en la gramática del parser.

**Operador `!` (NEGAR):** tokenizado pero inutilizado. `expresionLogica()` solo consume `&` (AND) y `|` (OR). La sonda confirma que `si(!x)` falla con `Factor invalido`.

### Tipos

- `entero`: IMPLEMENTED (declaración, inicialización y aritmética).
- `real`: IMPLEMENTED.
- `cadena`: IMPLEMENTED para asignación simple e impresión.
- `caracter`: IMPLEMENTED para asignación simple e impresión.
- `booleano`: PARTIAL/CONFLICT. `booleano x;` se acepta, pero `booleano x = verdadero;` falla con `TIPO INVALIDO` porque `asignacion()` no tiene caso para `PALABRA_RESERVADA_BOO`. `AnalizadorSemantico.retornarTipo()` tampoco contempla booleano.
- `nulo` y `vacio`: DOCUMENTED/PARTIAL. Reconocidos léxicamente, sin semántica completa. `entero x = nulo;` falla con `Factor invalido`.

**Falta de validación de tipos (nuevo):** `entero x = 9.5;` es **aceptado** y trunca silenciosamente a `9` (`asignacion()` hace `(int) valorEntero`). Esto contradice `language_spec` (§16.2/16.3) y el dataset (#46), que lo catalogan como error de tipo. No existe verificación de compatibilidad de tipos real.

### Clase y programa

El parser exige:

```cerberus
clase Nombre() {
    ...
}
```

`programa()` procesa globales, `inicio()`, funciones y finalmente `}`. Se comprueba que exista una función principal y se rechaza más de una.

Estado: IMPLEMENTED, con una sintaxis concreta que debe considerarse normativa para la IA mientras no se modifique el parser. `clase Nombre {` (sin paréntesis) es rechazado (`Se esperaba PARENTESIS_ABRE y se encontró LLAVE_ABRE`).

### Funciones

Se reconoce `funcion principal() { ... }` y funciones comunes sin parámetros `funcion nombre() { ... }`.

Estado: IMPLEMENTED para funciones sin parámetros y sin retorno funcional completo. `retornar` está tokenizado, pero no hay soporte sintáctico: la sonda confirma que `retornar 1;` falla (`no se ha puesto el token` → AssertionError).

### Variables y asignación

Declaraciones aceptadas: `entero x;`, `real x;`, `cadena x;`, `caracter x;`, `booleano x;`, con inicialización opcional mediante `=`. Las asignaciones numéricas pasan por `expresionAritmetica()`. Las de cadena/carácter aceptan sus tokens. Las booleanas no están implementadas.

### `imprimir`

Acepta identificadores, cadenas, caracteres, números enteros y reales. Permite concatenación mediante `+` entre expresiones imprimibles. Estado: IMPLEMENTED con ese conjunto.

### `si / sino`

Implementado. `si` exige expresión lógica y bloque entre llaves. `sino` permite bloque o `sino si`. Estado: IMPLEMENTED.

### `mientras`

`mientras()` consume `mientras ( condicion )`; el bloque `{ ... }` se consume desde `instruccion()`. Estado: IMPLEMENTED, con responsabilidad dividida entre ambos métodos (cubrir con pruebas antes de considerarlo estable).

### `hacer ... mientras`

`hacer { ... } mientras ( condicion );`. Estado: IMPLEMENTED.

### `para`

Implementación específica y restringida. Exige:

```cerberus
para(entero i = 0; i < 10; i++) {
    ...
}
```

La inicialización requiere literalmente `entero`; `para(i = 0; ...)` es rechazado (`Se esperaba PALABRA_RESERVADA_ENT`). El valor inicial es entero, la condición compara mediante operador relacional con entero y el incremento exige `++`. Estado: PARTIAL. No debe describirse como un `para` general.

### `segun`

El selector debe ser un identificador. Cada `caso` debe usar el tipo retornado por el selector (`retornarTipo`): selector `cadena` → `caso "..."`; selector `entero` → `caso NUM`. El bloque continúa hasta `salir` y después exige `;`.

No hay procesamiento de `predeterminado`: la sonda confirma que `predeterminado:` es rechazado (`Se esperaba LLAVE_CIERRA`). `salir` sin `;` también es rechazado (`Se esperaba PUNTO_COMA`).

Estado: PARTIAL/CONFLICT si la documentación promete `predeterminado`.

### Expresiones

Existe evaluación de expresiones aritméticas con precedencia (`expresionAritmetica()`, `termino()`, `factor()`), con `+ - * / %`, números, identificadores y paréntesis.

Las expresiones lógicas se construyen a partir de condiciones y operadores `&` y `|`, con operadores relacionales `> < >= <= == !=`. `!` no se usa.

**`factor()` exige identificadores numéricos:** hace `Double.parseDouble(ide.getValor())`. Usar una variable `cadena`/`caracter` en aritmética lanza `NumberFormatException` (`For input string: ""hola""`). La aritmética es solo numérica en la práctica.

Estado: IMPLEMENTED para el subconjunto anterior; PARTIAL para lógica completa (sin `!`, sin booleano desnudo como condición).

### Tabla de símbolos / semántica

El parser mantiene `ArrayList<Identificadores> tabla` y usa `AnalizadorSemantico` para buscar identificadores, obtener información, detectar referencias inexistentes y asociar tipo/valor.

- `AnalizadorSemantico` es un **stub real**: solo implementa `retornarTipo()`, `buscar()`, `buscarIde()` y `verificarIdentificador()`. La UI hardcodea "AÚN NO IMPLEMENTADO" (y luego lo sobreescribe si el sintáctico pasa). No hay validación de tipos real, redeclaración por scope ni detección de múltiples `principal` por vía semántica (esas comprobaciones viven en el parser).
- **Scopes implementados:** el parser mantiene una pila de scopes (`global` → función → bloque) y `buscar()`/`buscarIde()` resuelven al identificador visible más interno. Se permite *shadowing* (una variable local puede ocultar una global o de un bloque externo). La redeclaración solo se rechaza dentro del **mismo** scope (`YA EXISTE ESE IDENTIFICADOR`). Verificado por la suite (`17_shadowing_global_local`, `18_shadowing_bloque`, `16_redeclaracion_scope`).
- **`generarTabla()` del lexer es código muerto:** reporta `"MAMO"` y tiene la inserción comentada; la tabla real la construye el parser en `agregarTabla()`.

Estado: `IMPLEMENTED` para scopes (global/función/bloque con shadowing); `PARTIAL` para la validación de tipos, que sigue sin implementarse.

### POO

Los tokens `clase`, `nuevo`, `este`, `publico`, `privado` existen. El parser implementa la construcción de clase y funciones, pero no hay implementación completa de objetos, instanciación, atributos, métodos con parámetros, herencia ni acceso público/privado. `nuevo` en contexto de expresión falla (`Factor invalido`).

Estado: DOCUMENTED/PARTIAL según característica.

## Evidencia dinámica (resumen)

Resultados de `audit.CompilerProbe` (32/33 casos coinciden; la única discrepancia es un hallazgo, no un error de la sonda):

| Snippet | ¿Aceptado? | Error observado |
|---|---|---|
| `clase Test(){ funcion principal(){ imprimir("Hola"); } }` | Sí | — |
| `clase Test{ ... }` (sin paréntesis) | No | `Se esperaba PARENTESIS_ABRE y se encontró LLAVE_ABRE` |
| `clase Test(){ }` (sin `principal`) | No | `No se encontro una funcion principal` |
| `entero x = 5;` / `entero x;` | Sí | — |
| `real x = 2.5;` / `cadena x = "hola";` / `caracter x = 'a';` | Sí | — |
| `booleano x;` | Sí | — |
| `booleano x = verdadero;` | No | `TIPO INVALIDO` |
| `entero x = 9.5;` | **Sí (trunca a 9)** | — |
| `para(entero i = 0; i < 10; i++){...}` | Sí | — |
| `para(i = 0; ...)` | No | `Se esperaba PALABRA_RESERVADA_ENT` |
| `segun` con `caso` + `salir;` | Sí | — |
| `segun` con `predeterminado:` | No | `Se esperaba LLAVE_CIERRA` |
| `segun` con `salir` (sin `;`) | No | `Se esperaba PUNTO_COMA` |
| `retornar 1;` | No | `no se ha puesto el token` |
| `x += 2;` / `x -= 2;` / `x *= 2;` / `x /= 2;` | No | `Se esperaba PUNTO_COMA y se encontró OPERADOR_*` |
| `si(!x){...}` | No | `Factor invalido` |
| `entero y = 1 + x;` (x cadena) | No | `For input string: ""hola""` |
| `entero x = nulo;` | No | `Factor invalido` |
| shadowing: `x` global + `x` local | Sí | — (scopes implementados) |
| redeclaración `x` en el **mismo** scope | No | `YA EXISTE ESE IDENTIFICADOR: x` |

## Tabla de conflictos spec vs compilador

| Característica | `language_spec` / PDF | Compilador real | Estado |
|---|---|---|---|
| `clase Nombre {` | ejemplos sin paréntesis | exige `clase Nombre()` | CONFLICT |
| `booleano activo = verdadero;` | documentado | solo `booleano x;` | CONFLICT |
| `entero x = 9.5;` | error de tipo | aceptado (trunca) | CONFLICT |
| `para(i = 0; ...)` | documentado | exige `para(entero i = 0; ...)` | CONFLICT |
| `segun` con `predeterminado` | documentado | no implementado | CONFLICT |
| `salir` | ejemplos sin `;` | exige `salir;` | CONFLICT |
| `+=`, `-=` | documentados | no implementados | CONFLICT |
| `!` (negación lógica) | documentado | tokenizado, no usado | DOCUMENTED/PARTIAL |
| parámetros de función | PENDING | no implementados | PENDING |
| `retornar` | documentado | solo tokenizado | DOCUMENTED/PARTIAL |
| arreglos (`entero [] a;`) | mencionados | no implementados | PENDING |

## Impacto en el dataset v1

Entradas de `cerberus_dataset_v1.jsonl` inválidas contra el compilador real:

- **#12** `para(i = 0; i < 10; i++)` → falta `entero`.
- **#15** `segun` con `predeterminado:` y `salir` sin `;`.
- **#16** `entero [] arreglo1;` → arreglos no implementados.
- **#19** `clase programa{` → falta `()`.
- **#20** `clase MiPrograma() { instrucciones }` → placeholder no ejecutable.
- **#5** `booleano activo = verdadero;` → inicialización booleana no soportada.
- **#46** afirma que `entero edad = 9.5;` es error → en realidad el compilador lo acepta (trunca).

## Riesgos para el dataset de IA

1. No usar la documentación como única fuente de verdad.
2. No generar ejemplos de POO avanzada hasta confirmar implementación.
3. No entrenar booleanos como si fueran completamente funcionales.
4. No generar `para` genérico.
5. No generar `predeterminado` hasta confirmar su soporte real.
6. No asumir parámetros o retornos en funciones.
7. No asumir operadores compuestos (`+=`, `-=`, `*=`, `/=`) ni `!`.
8. No asumir validación de tipos: el compilador actualmente acepta `entero x = 9.5;` (trunca).
9. Mantener una lista explícita de características futuras.

## Próximo paso técnico

1. Convertir estos hallazgos en una `language_spec.md` canónica.
2. Crear una suite `tests/valid/` y `tests/invalid/` con programas Cerberus pequeños.
3. Ejecutar esos programas contra el compilador y registrar resultados reales.
4. Corregir o decidir los conflictos del lenguaje.
5. Generar `cerberus_dataset_v2.jsonl` únicamente con sintaxis validada.
6. Después elegir el modelo base y diseñar fine-tuning/RAG.

# CerberusAI — Auditoría del compilador

Fecha de auditoría inicial: 2026-10-04
Fecha de re-auditoría (compilador v1 completo): 2026-10-06
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
- `ACOTADO`: fuera del alcance v1; el parser lo rechaza de forma explícita y documentada.

## Hallazgos

### Léxico

Implementados en `AnalizadorLexico.analizador()`:

`clase`, `nuevo`, `este`, `publico`, `privado`, `si`, `sino`, `segun`, `caso`, `predeterminado`, `salir`, `para`, `mientras`, `verdadero`, `falso`, `funcion`, `retornar`, `imprimir`, `entero`, `real`, `caracter`, `cadena`, `booleano`, `nulo`, `hacer`, `vacio`, `principal`.

Operadores/tokenización presentes:

`+ - * / %`, `=`, `++ --`, `+= -= *= /=`, `== != < > <= >=`, `& | !`, delimitadores `() {} [] ; :`, identificadores, enteros, reales, cadenas y caracteres.

**Operadores compuestos (`+=`, `-=`, `*=`, `/=`):** implementados. El lexer colapsa correctamente los cuatro (`seccionarCadena()` con las pautas de doble espacio) y `analizador()` mapea `+=`→`MAS_VARIABLE`, `-=`→`MENOS_VARIABLE`, `*=`→`MUL_VARIABLE`, `/=`→`DIV_VARIABLE`. El parser los acepta vía `asignacionCompuesta()` solo sobre `entero`/`real`; sobre otros tipos o con RHS real hacia `entero` → `TIPO INCOMPATIBLE`.

**Operador `!` (NEGAR):** implementado en `terminoLogico()`: negación de un término (`!activo`, `!!a`), de un grupo `!( ... )` (`!(x > 5)`, `!(a & b)`), y en el RHS de asignación booleana (`booleano b = !a;`). `!` sobre un valor no booleano (`si(!x)` con `x` entero) se rechaza (`Operador relacional inválido`).

### Errores

Implementado: todo error del parser se lanza como `ErrorSintactico` (con fila/columna/lexema) mediante los helpers `error(mensaje)`/`error(mensaje, token)`; `analizar()` lo captura y reporta `OCURRIO UN ERROR: <mensaje>` seguido de `En la linea: N`, `Columna: N`, `Lexema: X`. Se conservaron los textos previos, por lo que los esperados en `tests/expected/invalid/*` siguen coincidiendo. Evidencia: inválidos `34`–`35` comprueban `En la linea: N`. Estado: IMPLEMENTED.

### Tipos

- `entero`: IMPLEMENTED (declaración, inicialización y aritmética).
- `real`: IMPLEMENTED.
- `cadena`: IMPLEMENTED para asignación simple e impresión.
- `caracter`: IMPLEMENTED para asignación simple e impresión.
- `booleano`: IMPLEMENTED. `booleano x;` y `booleano x = verdadero;`/`= falso;` se aceptan; RHS admite también una variable booleana. El booleano participa como condición (`si(activo)`, `si(activo == verdadero)`) y en `&`/`|`. No participa en aritmética (`entero y = b + 1;` → `TIPO INCOMPATIBLE`).
- `vacio`: IMPLEMENTED como tipo de retorno de función (retorno sin valor). `nulo`: reservado / fuera de v1 (sin tipos por referencia, sin semántica); `entero x = nulo;` falla con `Factor invalido` y no genera ejemplos en el dataset v2.

**Validación de tipos (implementada):** las expresiones llevan tipo (entero/real) y las asignaciones se validan: `entero x = 9.5;` es **rechazado** (`TIPO INCOMPATIBLE: no se puede asignar REAL a ENTERO`). `real` acepta entero y real; `cadena`/`caracter`/`booleano` aceptan literales o variables del mismo tipo.

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

Se reconoce `funcion principal()` y funciones comunes `funcion [<tipo>] nombre(<parametros>) { ... }` con tipo de retorno opcional (`entero`/`real`/`cadena`/`caracter`/`booleano`/`vacio`) y parámetros `[<tipo> <id> { , <tipo> <id> }]` (tipos sin `vacio`).

Estado: IMPLEMENTED para funciones con tipo de retorno, parámetros, `retornar` y **llamadas**. `recolectarFirmas()` registra nombre, tipo de retorno y tipos de parámetros en un pre-escaneo global (permite llamadas hacia adelante); `llamadaFuncion()` valida existencia (`Funcion no encontrada: X`), aridad y tipos de argumentos, y devuelve el tipo de retorno. Las llamadas se aceptan como sentencia (`f();`) y como expresión (asignación, `imprimir`, condición booleana `si(f())`); el valor retornado **no se evalúa** (placeholder tipado). Los parámetros se registran en el scope de la función con un valor placeholder (no reciben valores reales). `principal` no es llamable.

### Variables y asignación

Declaraciones aceptadas: `entero x;`, `real x;`, `cadena x;`, `caracter x;`, `booleano x;`, con inicialización opcional mediante `=`. Las asignaciones numéricas pasan por `expresionAritmetica()`; cadena/carácter/booleano aceptan literal o variable del mismo tipo.

### `imprimir`

Acepta identificadores, cadenas, caracteres, números enteros y reales, booleanos (`verdadero`/`falso`) y llamadas a función. Permite encadenar expresiones imprimibles con `+` o `,`. Estado: IMPLEMENTED con ese conjunto.

### `si / sino`

Implementado. `si` exige expresión lógica y bloque entre llaves. `sino` permite bloque o `sino si`. Estado: IMPLEMENTED.

### `mientras`

`mientras()` consume `mientras ( condicion )`; el bloque `{ ... }` se consume desde `instruccion()`. Estado: IMPLEMENTED (cubierto por la suite: `13_mientras`, `14_hacer_mientras`).

### `hacer ... mientras`

`hacer { ... } mientras ( condicion );`. Estado: IMPLEMENTED.

### `para`

Implementación generalizada:

```cerberus
para(entero i = 1 + 1; i < 10 * 2; i++) {
    ...
}
para(entero i = 10; i > 0; i--) {
    ...
}
```

El contador es `entero` o `real`; la inicialización y la condición son expresiones aritméticas generales; el incremento es `++` o `--`. El contador se registra en el scope del `para` (visible en el cuerpo). `para(i = 0; ...)` (sin tipo) se rechaza (`El contador del para debe ser entero o real`). Estado: IMPLEMENTED.

### `segun`

El selector debe ser un identificador. Cada `caso` debe usar el tipo retornado por el selector (`retornarTipo`): selector `cadena` → `caso "..."`; selector `entero` → `caso NUM`. El bloque continúa hasta `salir` y después exige `;`.

`predeterminado` está implementado como caso por defecto opcional, **solo al final** del `segun`, y exige `salir;`. En cualquier otra posición se rechaza (`Se esperaba LLAVE_CIERRA`). `salir` sin `;` se rechaza (`Se esperaba PUNTO_COMA`).

Estado: IMPLEMENTED.

### Expresiones

Existe evaluación de expresiones aritméticas con precedencia (`expresionAritmetica()`, `termino()`, `factor()`), con `+ - * / %`, números, identificadores y paréntesis.

Las expresiones lógicas se construyen a partir de términos y operadores `&` y `|`, con operadores relacionales `> < >= <= == !=` y negación `!` (`terminoLogico()`).

**`factor()` tipado:** identifica el tipo de cada operando. Un identificador `cadena`/`caracter` en aritmética se rechaza con `TIPO INCOMPATIBLE: la variable X no es numerica` (ya no lanza `NumberFormatException`).

**Llamadas en expresiones:** `factor()` (y los RHS de cadena/carácter/booleano y `imprimir`) aceptan `f(args)` como operando; se valida el tipo de retorno y se usa un placeholder (valor no evaluado).

Estado: IMPLEMENTED para el subconjunto anterior (booleanos desnudos, comparaciones `==`/`!=`, `&`/`|`, negación `!` y llamadas como operandos).

### Tabla de símbolos / semántica

El parser mantiene `ArrayList<Identificadores> tabla` y usa `AnalizadorSemantico` para buscar identificadores, obtener información, detectar referencias inexistentes y asociar tipo/valor.

- `AnalizadorSemantico` implementa tipo (`Tipo`), compatibilidad de asignación, búsqueda por scope y `existeEnScope()`. El parser usa expresiones tipadas (`Expresion`) y valida asignaciones, además de condiciones booleanas (`condicion()`/`primarioBooleano()`). `&`/`|` corregidos. La UI (`InterfazPrincipalController`) muestra el resultado real del análisis (ya no hay texto fijo de "no implementado").
- **Scopes implementados:** el parser mantiene una pila de scopes (`global` → función → bloque) y `buscar()`/`buscarIde()` resuelven al identificador visible más interno. Se permite *shadowing* (una variable local puede ocultar una global o de un bloque externo). La redeclaración solo se rechaza dentro del **mismo** scope (`YA EXISTE ESE IDENTIFICADOR`). Verificado por la suite (`17_shadowing_global_local`, `18_shadowing_bloque`, `16_redeclaracion_scope`).
- **`generarTabla()` del lexer es código muerto:** reporta `"MAMO"` y tiene la inserción comentada; la tabla real la construye el parser en `agregarTabla()`.

Estado: `IMPLEMENTED` para scopes (global/función/bloque con shadowing), validación de tipos (entero/real/cadena/caracter) y booleanos (inicialización y condiciones).

### POO

Los tokens `clase`, `nuevo`, `este`, `publico`, `privado` existen. El parser implementa la construcción de clase y funciones, pero **no** objetos, instanciación, atributos, métodos con parámetros, herencia ni acceso público/privado (fuera de v1). Estas construcciones se rechazan de forma explícita: `nuevo`/`este`/`publico`/`privado` → `POO no implementada en v1: <lexema>`; el acceso a miembro `x.y` → `POO no implementada en v1: acceso a miembro`. Evidencia: inválidos `15` y `43`.

Estado: ACOTADO (fuera de v1) con rechazo explícito.

## Evidencia dinámica (resumen)

Resultados de `audit.CompilerProbe` (48/48 casos coinciden con lo esperado):

| Snippet | ¿Aceptado? | Error observado |
|---|---|---|
| `clase Test(){ funcion principal(){ imprimir("Hola"); } }` | Sí | — |
| `clase Test{ ... }` (sin paréntesis) | No | `Se esperaba PARENTESIS_ABRE y se encontró LLAVE_ABRE` |
| `clase Test(){ }` (sin `principal`) | No | `No se encontro una funcion principal` |
| `entero x = 5;` / `entero x;` | Sí | — |
| `real x = 2.5;` / `cadena x = "hola";` / `caracter x = 'a';` | Sí | — |
| `booleano x;` | Sí | — |
| `booleano x = verdadero;` | Sí | — |
| `si(activo)` / `si(activo == verdadero)` | Sí | — |
| `si(edad >= 18 & activo == verdadero)` | Sí | — |
| `entero x = 9.5;` | No | `TIPO INCOMPATIBLE: no se puede asignar REAL a ENTERO` |
| `para(entero i = 0; i < 10; i++){...}` | Sí | — |
| `para(entero i = 10; i > 0; i--){...}` | Sí | — |
| `para(i = 0; ...)` (sin tipo) | No | `El contador del para debe ser entero o real` |
| `segun` con `caso` + `salir;` | Sí | — |
| `segun` con `predeterminado:` (al final) | Sí | — |
| `segun` con `predeterminado` no último | No | `Se esperaba LLAVE_CIERRA` |
| `segun` con `salir` (sin `;`) | No | `Se esperaba PUNTO_COMA` |
| `retornar <expr>;` en `funcion <tipo>` | Sí | — (valida el tipo) |
| `retornar;` en `funcion vacio` | Sí | — |
| `retornar 1;` en función sin tipo (void) | No | `TIPO INCOMPATIBLE` |
| `retornar 2.5;` en `funcion entero` | No | `TIPO INCOMPATIBLE` |
| `retornar;` en `funcion entero` | No | `TIPO INCOMPATIBLE` |
| `x += 2;` / `x -= 2;` / `x *= 2;` / `x /= 2;` (x numérico) | Sí | — (asignación compuesta) |
| compuesto sobre `cadena`/`booleano` o `entero += real` | No | `TIPO INCOMPATIBLE` |
| `si(!x){...}` (x entero) | No | `Operador relacional inválido` |
| `si(!activo)`, `si(!!a)`, `si(!(x > 5))`, `booleano b = !a;` | Sí | — (negación implementada) |
| `entero y = 1 + x;` (x cadena) | No | `TIPO INCOMPATIBLE: la variable x no es numerica` |
| `entero x = nulo;` | No | `Factor invalido` |
| shadowing: `x` global + `x` local | Sí | — (scopes implementados) |
| redeclaración `x` en el **mismo** scope | No | `YA EXISTE ESE IDENTIFICADOR: x` |
| llamada a función void (`f();`) | Sí | — |
| llamada con argumentos (`suma(1, 2);`) | Sí | — |
| llamada como valor (`entero y = f(1, 2);`) | Sí | — (placeholder) |
| llamada booleana en `si(activo())` | Sí | — |
| llamada a función inexistente | No | `Funcion no encontrada: X` |
| llamada con aridad incorrecta | No | `La funcion X espera N argumentos...` |
| `imprimir(x, 2, "z");` (con `,`) e `imprimir(verdadero);` | Sí | — |
| `entero [] a;` | No | `Arreglos no implementados en v1` |
| `publico entero x;` | No | `POO no implementada en v1: publico` |
| `entero x = nuevo entero;` | No | `POO no implementada en v1: nuevo` |

## Tabla de conflictos spec vs compilador

| Característica | `language_spec` / PDF | Compilador real | Estado |
|---|---|---|---|
| `clase Nombre {` | ejemplos sin paréntesis | exige `clase Nombre()` | CONFLICT |
| `booleano activo = verdadero;` | documentado | implementado | `IMPLEMENTED` |
| `entero x = 9.5;` | error de tipo | rechazado | `IMPLEMENTED` |
| `para(i = 0; ...)` | documentado | exige `para(entero i = 0; ...)` | CONFLICT |
| `segun` con `predeterminado` | documentado | implementado (solo al final) | `IMPLEMENTED` |
| `salir` | ejemplos sin `;` | exige `salir;` | CONFLICT |
| `+=`, `-=`, `*=`, `/=` | implementados (solo numérico) | `IMPLEMENTED` |
| `!` (negación lógica) | documentado | implementado | `IMPLEMENTED` |
| parámetros de función | PENDING | implementados (placeholder, sin valores reales) | `IMPLEMENTED` |
| llamada a funciones | documentado | implementada (sentencia y expresión) | `IMPLEMENTED` |
| `retornar` + tipo de retorno | documentado | implementado | `IMPLEMENTED` |
| arreglos (`entero [] a;`) | mencionados | rechazados (`Arreglos no implementados en v1`) | ACOTADO |

## Impacto en el dataset v1

Entradas de `cerberus_dataset_v1.jsonl` inválidas contra el compilador real:

- **#12** `para(i = 0; i < 10; i++)` → falta `entero`.
- **#15** `segun` con `predeterminado:` (ahora válido) pero `salir` sin `;` → sigue inválido por el `;`.
- **#16** `entero [] arreglo1;` → arreglos no implementados.
- **#19** `clase programa{` → falta `()`.
- **#20** `clase MiPrograma() { instrucciones }` → placeholder no ejecutable.
- **#5** `booleano activo = verdadero;` → ahora **válido** (inicialización booleana implementada).
- **#46** afirma que `entero edad = 9.5;` es error → ahora **coincide** con el compilador (lo rechaza).

## Riesgos para el dataset de IA

1. No usar la documentación como única fuente de verdad.
2. No generar ejemplos de POO avanzada hasta confirmar implementación.
3. Booleanos ya funcionales (inicialización y condiciones); no usarlos en aritmética.
4. El `para` es generalizado (expresiones, `real`, `++`/`--`) pero exige tipo en el contador (`entero`/`real`).
5. `predeterminado` implementado (solo al final del `segun`, con `salir;`).
6. Llamadas a funciones implementadas (sentencia y expresión); el valor retornado no se evalúa (placeholder tipado).
7. Asignación compuesta (`+=`, `-=`, `*=`, `/=`) implementada solo para `entero`/`real`.
8. `--`, negación `!`, `retornar`, parámetros y llamadas implementados; el paso de valores en llamadas no se evalúa.
9. Mantener una lista explícita de características futuras.

## Próximo paso técnico

Estado (2026-10-06): auditoría y suite ya ejecutadas y verdes (`audit.TestSuite` 99/99, `audit.CompilerProbe` 48/48); el compilador v1 está cerrado (ALTA + MEDIA + BAJA acotada).

1. Generar `cerberus_dataset_v2.jsonl` únicamente con sintaxis validada (sin POO, arreglos ni `nulo`).
2. Elegir el modelo base y diseñar fine-tuning/RAG.
3. Servicio Python/FastAPI e integración con el IDE JavaFX.

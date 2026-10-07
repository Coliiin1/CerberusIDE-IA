# GramaticasAttendance.md — Gramáticas incompletas del compilador Cerberus

**Propósito:** inventario de las gramáticas del compilador Cerberus que aún **no están implementadas** o están **incompletas**, con la producción BNF propuesta para cada una. Sirve como guía de implementación para dejar el compilador completo y estable **antes** de generar el dataset v2 y entrenar la IA.

**Método de detección:** auditoría estática del código fuente + sonda dinámica `audit.CompilerProbe` + suite `audit.TestSuite`. Ver `CerberusAI_COMPILER_AUDIT.md` para el estado confirmado de cada característica.

**Leyenda de prioridad:**

| Prioridad | Significado |
|---|---|
| `ALTA` | Core semántico. Bloquea ejemplos correctos en el dataset (booleanos, tipos, funciones, control). |
| `MEDIA` | Mejora de consistencia/robustez. No bloquea, pero conviene antes de entrenar. |
| `BAJA / ACOTAR` | POO avanzada o estructuras de datos. Decidir si entra en v1 o se marca como futura. |

---

## 1. Baseline — gramáticas YA implementadas

Checklist de lo que funciona hoy (verificado con la suite, 99/99 casos):

- `clase Nombre() { ... }` (paréntesis obligatorios).
- Declaraciones `entero / real / cadena / caracter / booleano` con inicialización opcional; condiciones booleanas (desnudas y comparaciones) y `&`/`|`.
- Variables globales antes de `clase`.
- Asignación directa numérica (`x = expr;`).
- `imprimir(expr ( ( + | , ) expr)*);` con `IDENTIFICADOR`, `TIPO_CADENA`, `TIPO_CARACTER`, `NUMERO_ENTERO`, `NUMERO_REAL`, `verdadero`/`falso` y llamadas.
- `si (expr) { ... } [ sino { ... } | sino si(...) ]`.
- `mientras(expr) { ... }`.
- `hacer { ... } mientras(expr);`.
- `segun(id) { caso <tipo>: ... salir; [predeterminado: ... salir;] }`.
- `para(entero i = 0; i < 10; i++) { ... }` y su forma generalizada (`real`, expresiones, `++`/`--`).
- Expresiones aritméticas `+ - * / %` con precedencia y paréntesis.
- Condiciones `exprAritmetica opRelacional exprAritmetica` con `&` y `|`.
- Funciones `funcion [<tipo>] nombre(<parametros>) { ... }` (tipo de retorno, parámetros y `retornar` implementados) y `principal()`.
- Llamadas a funciones como sentencia (`f();`) y como expresión (`entero y = f(1,2);`, `imprimir(f(1));`, `si(f())`), con verificación de existencia, aridad y tipos (valor no evaluado).

---

## 2. Gramáticas incompletas (con BNF propuesto)

### 2.1 Parámetros de funciones — `ALTA` — **HECHO**

**Estado:** `HECHO`. `parametros()` en `funcionComun()` parsea `(<TIPO> id { , <TIPO> id })` y `agregarParametro()` los registra en el scope de la función (rechaza duplicados). Nuevo token `COMA`. `principal()` sigue sin parámetros. Nota: no hay llamada a funciones (2.13), así que los parámetros no reciben valores. Verificado por `44`–`46`, inválidos `31`–`32` y la sonda.

<details>
<summary>Descripción original del hueco</summary>

- **Dónde faltaba:** `funcionComun()` y `principal()` exigían paréntesis vacíos.
- **Qué faltaba:** lista de parámetros y registro en la tabla con su scope.
- **BNF:** `<PARAMETRO> ::= <TIPO> IDENTIFICADOR`.

</details>

---

### 2.2 Tipo de retorno de función — `ALTA` — **HECHO**

**Estado:** `HECHO`. `funcionComun()` lee un tipo de retorno opcional tras `funcion` (`leerTipoRetorno()`): `entero`/`real`/`cadena`/`caracter`/`booleano`/`vacio`; `vacio` o su ausencia = void. Verificado por `39`–`43`, inválidos `28`–`30` y la sonda.

<details>
<summary>Descripción original del hueco</summary>

- **Dónde faltaba:** `funcionComun()` no leía un tipo antes del identificador.
- **BNF:** `<TIPO_RETORNO> ::= "entero" | "real" | "cadena" | "caracter" | "booleano" | "vacio"`.
- **Forma final:** `funcion <TIPO_RETORNO>? IDENTIFICADOR ( ) { ... }`.

</details>

---

### 2.3 `retornar` — `ALTA` — **HECHO**

**Estado:** `HECHO`. `instruccion()` tiene `case PALABRA_RESERVADA_RET`; `retornar()` implementa `retornar [<expr>];` y valida contra el tipo de retorno (`tipoRetornoActual`): `retornar 2.5;` en `funcion entero` → `TIPO INCOMPATIBLE`; `retornar 1;` en función void → `TIPO INCOMPATIBLE`; `retornar;` en función tipada → `TIPO INCOMPATIBLE`. Verificado por `39`–`43` e inválidos `28`–`30`.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `PALABRA_RESERVADA_RET` (lexer OK).
- **Dónde faltaba:** `instruccion()` no tenía `case` para `RET`.
- **BNF:** `<RETORNAR> ::= "retornar" [ <EXPRESION> ] ";"`.

</details>

---

### 2.4 Inicialización booleana — `ALTA` — **HECHO**

**Estado:** `HECHO`. `asignacion()` tiene case `PALABRA_RESERVADA_BOO`; RHS admite `verdadero`/`falso` o variable booleana (valida con `sem.compatible`). Verificado por `23_booleano_init`, `26_booleano_desde_variable`.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `PALABRA_RESERVADA_BOO`, `PALABRA_RESERVADA_VER`, `PALABRA_RESERVADA_FAL` (lexer OK).
- **Dónde falta:** `AnalizadorSintactico.asignacion()` — el `switch` cubría `ENT`, `REA`, `CAD`, `CAR` pero no `BOO`.

</details>

---

### 2.5 Booleano como condición — `ALTA` — **HECHO**

**Estado:** `HECHO`. `condicion()` acepta booleano desnudo (`si(activo)`, `si(verdadero)`) y comparaciones booleanas (`activo == verdadero`, `activo != falso`). Además se corrigió `expresionLogica()` (el `while` no releía el token), por lo que `&`/`|` funcionan. Verificado por `24_booleano_condicion`, `25_booleano_comparacion`, `27_logico_and`.

<details>
<summary>Descripción original del hueco</summary>

- **Dónde falta:** `condicion()`/`factor()` exigían `exprAritmetica opRelacional exprAritmetica`; `si(verdadero)`/`si(activo)` fallaban.
- **BNF objetivo:** `<CONDICION> ::= <EXPRESION_LOGICA_PRIMARIA> [ ("=="|"!=") <EXPRESION_LOGICA_PRIMARIA> ] | <EXPRESION_ARITMETICA> opRelacional <EXPRESION_ARITMETICA>`.

</details>

---

### 2.6 Negación lógica `!` — `ALTA` — **HECHO**

**Estado:** `HECHO`. `terminoLogico()` implementa `!` sobre un término (`!activo`, `!!a`) y sobre un grupo `!( <expresionLogica> )` (`!(x > 5)`, `!(a & b)`); combinable con `&`/`|`. `expresionBooleana()` también acepta `!` en el RHS (`booleano b = !a;`). `!` sobre un no booleano se rechaza. Verificado por `28`–`33` y por la sonda.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `NEGAR` (lexer OK).
- **Dónde falta:** `expresionLogica()` solo consumía `&`/`|`.

</details>

---

### 2.7 Operadores de asignación compuesta `+= -= *= /=` — `ALTA` — **HECHO**

**Estado:** `HECHO`. Tokens `MAS_VARIABLE`/`MENOS_VARIABLE` (renombrados desde `INC_VARIABLE`/`DEC_VARIABLE`) + `MUL_VARIABLE`/`DIV_VARIABLE`. `seccionarCadena()` colapsa los cuatro (pautas de doble espacio) y `analizador()` los mapea. `asignacionCompuesta()` en el parser los aplica solo a `entero`/`real`; otros tipos o `entero += real` → `TIPO INCOMPATIBLE`. Verificado por `34`–`37`, inválidos `23`–`25` y la sonda.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `MUL_VARIABLE`, `DIV_VARIABLE` existían; `INC_VARIABLE`/`DEC_VARIABLE` sin uso; `+=`/`-=` no tokenizados.
- **Dónde faltaba:** lexer (`seccionarCadena()`/`analizador()`) y parser (sin producción).

</details>

---

### 2.8 Decremento `--` en `para` — `MEDIA` — **HECHO**

**Estado:** `HECHO` (con 6.9). El token `DECREMENTO` existía en el enum, pero el **lexer no lo generaba**: `seccionarCadena()` expandía `-` antes de colapsar `--`, así que `--` quedaba como dos `OPERADOR_RESTA`. Se corrigió el colapso a `" -  - " → " -- "` (análogo a `" +  + " → " ++ "`), y `para()` acepta `++` o `--`. Verificado por `48` y la sonda.

<details>
<summary>Descripción original (incompleta: omitía el bug del lexer)</summary>

- **Token:** `DECREMENTO` (el enum y el `case "--"` existen, pero el lexer no producía el token).
- **Dónde faltaba:** lexer (`seccionarCadena()`) y parser (`para()`).

</details>

---

### 2.9 `para` generalizado — `ALTA` — **HECHO**

**Estado:** `HECHO`. `para()` ahora acepta `<entero|real> id = <exprAritmetica> ; <exprAritmetica> opRelacional <exprAritmetica> ; id (++|--)`. El contador se registra en el scope del `para` (visible en el cuerpo) y `entero` con inicialización real se rechaza. Verificado por `10`, `47`–`50`, inválidos `04`/`33`, `22` y la sonda.

<details>
<summary>Descripción original + BNF corregida</summary>

- **Dónde faltaba:** `para()` restringido a `para(entero i = N; i <rel> M; i++)`.
- **BNF original (con `;` de más):** `<PARA> ::= "para" "(" <DECLARACION_PARA> <CONDICION_PARA> ";" <INCREMENTO_PARA> ")" ...`
- **BNF corregida:**

```text
<PARA> ::= "para" "(" <DECLARACION_PARA> ";" <CONDICION> ";" <INCREMENTO_PARA> ")" "{" <INSTRUCCIONES> "}"

<DECLARACION_PARA> ::= ( "entero" | "real" ) IDENTIFICADOR "=" <EXPRESION_ARITMETICA>

<CONDICION> ::= <EXPRESION_ARITMETICA> <OPERADOR_RELACIONAL> <EXPRESION_ARITMETICA>

<INCREMENTO_PARA> ::= IDENTIFICADOR ( "++" | "--" )
```

</details>

---

### 2.10 `segun` con `predeterminado` — `ALTA` — **HECHO**

**Estado:** `HECHO`. `predeterminado` es palabra reservada (`PALABRA_RESERVADA_PRE`); `segun()` llama a `predeterminado()` (caso por defecto, solo al final, opcional) y comparte `cuerpoCaso()` con `casos()`. Exige `salir;`; en otra posición se rechaza (`Se esperaba LLAVE_CIERRA`). Verificado por `38`, inválidos `26`–`27` y la sonda.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `predeterminado` no era palabra reservada (se tokenizaba como `IDENTIFICADOR`).
- **Dónde faltaba:** lexer y `segun()` (solo iteraba sobre `caso`).

</details>

---

### 2.11 Validación de tipos (semántica) — `ALTA` — **HECHO**

**Estado:** `HECHO` (entero/real/cadena/caracter). `Tipo` enum + `compatible()` en `AnalizadorSemantico`; expresiones tipadas (`Expresion`) en el parser. Reglas: `entero←entero`; `real←entero|real`; `cadena←cadena`; `caracter←caracter`. `entero x = 9.5;` → `TIPO INCOMPATIBLE`. RHS de `cadena`/`caracter` acepta variables del mismo tipo. Verificado por la suite (`17`–`21`, `13`). (Booleanos y retorno tipado se completaron en 2.4/2.5 y 2.2/2.3.)

<details>
<summary>Descripción original del hueco</summary>

- **Dónde falta:** `AnalizadorSemantico` era un stub; `asignacion()` truncaba con `(int)` sin comprobar tipos.
- **Qué falta:** reglas de compatibilidad de tipos (asignación, operadores, condiciones, retorno).
- Reglas propuestas (parcialmente cubiertas): 1) `entero` no acepta reales; 2) `+ - * / %` solo numéricos; 3) condiciones booleanas (pendiente 2.5); 4) `retornar` (pendiente 2.3); 5) no redeclarar en el mismo scope (HECHO en 2.12).

</details>

---

### 2.12 Scopes reales — `ALTA` — **HECHO**

**Estado:** `HECHO`. El parser mantiene una pila de scopes (`global` → función → bloque); `AnalizadorSemantico.buscarIde()` resuelve al identificador visible más interno y `existeEnScope()` detecta redeclaración solo en el mismo scope. Se permite *shadowing*. Verificado por la suite (`17_shadowing_global_local`, `18_shadowing_bloque`).

<details>
<summary>Descripción original del hueco</summary>

- **Dónde falta:** `AnalizadorSemantico.buscar()`/`buscarIde()` recorren la tabla plana por nombre; `scope` se guarda en `Identificadores` pero no se usa en la búsqueda.
- **Qué falta:** búsqueda por scope anidado (global → función → bloque), permitiendo shadowing.

</details>

---

### 2.13 Llamada a funciones — `MEDIA` — **HECHO**

**Estado:** `HECHO`. `recolectarFirmas()` registra las funciones (nombre, tipo de retorno y tipos de parámetros) en un pre-escaneo global, lo que permite llamadas hacia adelante. `llamadaFuncion()` valida existencia (`Funcion no encontrada: X`), aridad (`La funcion X espera N argumentos...`) y tipo de cada argumento. Las llamadas se aceptan como sentencia y como expresión (valor no evaluado: placeholder tipado) en asignaciones, `imprimir` y condiciones booleanas (`si(f())`). `principal` no es llamable. Verificado por válidos `51`–`58`, inválidos `36`–`41` y la sonda.

<details>
<summary>Descripción original + BNF implementada</summary>

- **Dónde faltaba:** no existía producción de invocación de una función definida.
- **Qué faltaba:** gramática de llamada y verificación de que la función existe.

```text
<LLAMADA_FUNCION> ::= IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")" ";"

<LLAMADA_EXPR>    ::= IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")"

<ARGUMENTOS>      ::= <EXPRESION> { "," <EXPRESION> }
```

</details>

---

### 2.14 `imprimir` con varios argumentos — `MEDIA` — **HECHO**

**Estado:** `HECHO`. `imprimir()` encadena argumentos con `,` además de `+`, y `expresionImprimible()` admite además `verdadero`/`falso` y llamadas a función (de 2.13). Una coma colgante (`imprimir("a", );`) es rechazada (`NO SE RECONOCE EL TIPO`). Verificado por válidos `59`–`62`, inválido `42` y la sonda.

<details>
<summary>Descripción original + BNF implementada</summary>

- **Dónde faltaba:** `AnalizadorSintactico.imprimir()` solo concatenaba con `+`.
- **Qué faltaba:** aceptar separador `,` además de `+`.

```text
<IMPRIMIR> ::= "imprimir" "(" <EXPRESION_IMPRIMIBLE> { ( "," | "+" ) <EXPRESION_IMPRIMIBLE> } ")" ";"

<EXPRESION_IMPRIMIBLE> ::= IDENTIFICADOR | TIPO_CADENA | TIPO_CARACTER
                         | NUMERO_ENTERO | NUMERO_REAL | "verdadero" | "falso"
                         | <LLAMADA_EXPR>
```

</details>

---

### 2.15 `nulo` y `vacio` — `MEDIA` — **HECHO (acotado)**

**Estado:** `HECHO (acotado)`. `vacio` ya está implementado como tipo de retorno de función (2.2). `nulo` queda **reservado / fuera de v1**: sin tipos por referencia, no se implementa semántica y `entero x = nulo;` sigue siendo rechazado (`Factor invalido`); no genera ejemplos en el dataset v2. Verificado por el inválido `14_nulo_como_valor`.

<details>
<summary>Descripción original del hueco</summary>

- **Token:** `PALABRA_RESERVADA_NUL`, `PALABRA_RESERVADA_VAC` (lexer OK).
- **Dónde faltaba:** sin semántica; `entero x = nulo;` falla (`Factor invalido`).
- **Qué faltaba:** definir su uso (asignación de `nulo` a tipos por referencia; `vacio` como tipo de retorno de función sin valor).

</details>

---

### 2.16 Errores tipados (`ErrorSintactico`) — `MEDIA` — **HECHO**

**Estado:** `HECHO`. Todo error del parser se lanza como `ErrorSintactico` (fila/columna/lexema) mediante los helpers `error(mensaje)` / `error(mensaje, token)`; `analizar()` captura `ErrorSintactico` y reporta `OCURRIO UN ERROR: <mensaje>` + línea/columna/lexema. Se conservaron los textos para no romper `tests/expected/invalid/*`. Verificado por inválidos `34`–`35` (comprueban `En la linea: N`).

<details>
<summary>Descripción original del hueco</summary>

- **Dónde faltaba:** `AnalizadorSintactico.ErrorSintactico` existe pero **no se usa**; los errores son `RuntimeException`/`AssertionError`.
- **Qué faltaba:** unificar el reporte de errores con línea/columna/lexema.

</details>

---

### 2.17 POO: `nuevo`, `este`, `publico`, `privado` — `BAJA / ACOTAR` — **HECHO (acotado)**

**Estado:** `HECHO (acotado)`. Fuera de v1 (ver sección 3). Las construcciones POO se rechazan de forma **explícita y determinista**: `nuevo`/`este`/`publico`/`privado` → `POO no implementada en v1: <lexema>` (en instrucción y en expresión para `nuevo`), y el acceso a miembro `x.y` → `POO no implementada en v1: acceso a miembro`. Verificado por inválido `15` (actualizado) y `43`, y la sonda.

<details>
<summary>Descripción original + BNF propuesta (no implementada)</summary>

- **Token:** todos existen (`PALABRA_RESERVADA_NUE`, `_EST`, `_PUB`, `_PRI`).
- **Dónde faltaba:** no hay gramática de objetos, atributos, métodos con parámetros, herencia ni control de acceso.
- **Qué faltaba:** decidir alcance. Ver sección 3.

BNF propuesto (si entrara en v1, **no implementado**):

```text
<ATRIBUTO> ::= ( "publico" | "privado" ) <TIPO> IDENTIFICADOR ";"

<INSTANCIA> ::= <IDENTIFICADOR_CLASE> IDENTIFICADOR "=" "nuevo" <IDENTIFICADOR_CLASE> "(" ")" ";"

<ACCESO_MIEMBRO> ::= IDENTIFICADOR "." IDENTIFICADOR
```

</details>

---

### 2.18 Arreglos — `BAJA / ACOTAR` — **HECHO (acotado)**

**Estado:** `HECHO (acotado)`. Fuera de v1 (ver sección 3). La declaración `entero [] a;` se rechaza con `Arreglos no implementados en v1`. Verificado por inválido `44` y la sonda.

<details>
<summary>Descripción original + BNF propuesto (no implementado)</summary>

- **Token:** `CORCHETE_ABRE`, `CORCHETE_CIERRA` (lexer OK).
- **Dónde faltaba:** sin producción de arreglo; `entero [] a;` es rechazado.
- **Qué faltaba:** decidir alcance.

BNF propuesto (si entrara en v1, **no implementado**):

```text
<DECLARACION_ARREGLO> ::= <TIPO> "[" "]" IDENTIFICADOR [ "=" "[" { <VALOR> } "]" ] ";"

<ACCESO_ARREGLO> ::= IDENTIFICADOR "[" <EXPRESION_ARITMETICA> "]"
```

</details>

---

## 3. Decisiones de alcance (confirmadas)

Para no bloquear el entrenamiento de la IA, se acota **fuera del alcance v1**:

| Característica | Propuesta |
|---|---|
| Herencia | Fuera de v1 (documentar como futura). |
| Constructores | Fuera de v1 (documentar como futura). |
| Arreglos completos | Fuera de v1 (o solo sintaxis mínima). |
| `publico`/`privado` como modificadores reales | Fuera de v1 (solo tokens). |
| `nulo` | Fuera de v1 (reservado; sin tipos por referencia). `vacio` sí se implementa como retorno (2.2). |

Estas se marcarán como `PENDING`/`DOCUMENTED` en `language_spec.md` y **no** generarán ejemplos en el dataset v2.

---

## 4. Resumen de prioridades

| # | Feature | Prioridad | Estado |
|---|---|---|---|
| 2.1 | Parámetros de funciones | ALTA | HECHO |
| 2.2 | Tipo de retorno | ALTA | HECHO |
| 2.3 | `retornar` | ALTA | HECHO |
| 2.4 | Inicialización booleana | ALTA | HECHO |
| 2.5 | Booleano como condición | ALTA | HECHO |
| 2.6 | Negación `!` | ALTA | HECHO |
| 2.7 | Asignación compuesta `+= -= *= /=` | ALTA | HECHO |
| 2.9 | `para` generalizado | ALTA | HECHO |
| 2.10 | `segun` con `predeterminado` | ALTA | HECHO |
| 2.11 | Validación de tipos | ALTA | HECHO |
| 2.12 | Scopes reales | ALTA | HECHO |
| 2.8 | Decremento `--` | MEDIA | HECHO |
| 2.13 | Llamada a funciones | MEDIA | HECHO |
| 2.14 | `imprimir` multi-argumento | MEDIA | HECHO |
| 2.15 | `nulo`/`vacio` | MEDIA | HECHO (acotado) |
| 2.16 | Errores tipados | MEDIA | HECHO |
| 2.17 | POO (`nuevo`, `este`, `publico`, `privado`) | BAJA / ACOTAR | ACOTADO (fuera de v1) |
| 2.18 | Arreglos | BAJA / ACOTAR | ACOTADO (fuera de v1) |

> Los ítems MEDIA (2.13–2.16) no se implementan en un orden arbitrario: siguen la secuencia de la sección 5.

---

## 5. Plan de desarrollo — prioridad MEDIA (secuencia ordenada)

**Propósito:** ordenar los ítems MEDIA (2.13–2.16) por dependencias, igual que la prioridad ALTA (paso 6 de `RUTA.md`, ítems 6.1–6.9). Cada sub-paso se cierra cuando: (a) el código compila, (b) `TestSuite` y `CompilerProbe` pasan, (c) se añaden casos en `tests/` y en la sonda, y (d) se actualizan `CerberusAI_COMPILER_AUDIT.md`, `language_spec.md`, esta guía y `RUTA.md`.

**Decisiones de alcance fijadas:**

- M1 (2.16): estandarizar los errores del parser con `ErrorSintactico` (fila/columna/lexema).
- M2 (2.13): llamadas como **sentencia y expresión**; se valida existencia, aridad y tipos; el valor retornado no se evalúa (placeholder tipado).
- M3 (2.14): `imprimir` acepta `,` y además booleanos y llamadas como argumentos imprimibles.
- M4 (2.15): `vacio` ya está HECHO (2.2); `nulo` se documenta como **reservado / fuera de v1** (no se implementa; sin POO no hay tipos por referencia).

**Secuencia:**

| Sub-paso | Feature | Ref. | Depende de | Estado |
|---|---|---|---|---|
| M1 | Errores tipados (`ErrorSintactico`) | 2.16 | — | HECHO |
| M2 | Llamada a funciones | 2.13 | M1 | HECHO |
| M3 | `imprimir` multi-argumento | 2.14 | M2 | HECHO |
| M4 | `nulo` / `vacio` (alcance) | 2.15 | — | HECHO |

**Fase de pruebas (idéntica a la prioridad ALTA):** tras cada cambio de código se ejecuta `mvnw.cmd -q clean compile`, se añaden casos a `tests/valid/`, `tests/invalid/` y (opcional) `tests/expected/invalid/`, se actualiza `audit.CompilerProbe` y se corren `java -cp target\classes audit.TestSuite` y `java -cp target\classes audit.CompilerProbe`, que deben quedar verdes antes de cerrar el sub-paso.

---

### M1 — Errores tipados (ref. 2.16)

- **Objetivo:** todo error del parser sale como `ErrorSintactico` con línea, columna y lexema.
- **Cambios:**
  - `AnalizadorSintactico`: helpers `error(String mensaje)` y `error(String mensaje, Token tok)` que localizan el token actual (con guarda de límites) y devuelven `new ErrorSintactico(mensaje, fila, columna, lexema)`.
  - Reemplazar los `throw new RuntimeException(...)` / `throw new AssertionError(...)` del parser (incluido `match()`) por `throw error(...)`.
  - Conservar el texto actual de los mensajes: `tests/expected/invalid/*` compara subcadenas (`Factor invalido`, `El contador del para debe ser entero o real`, etc.).
- **BNF/gramática:** sin cambios (infraestructura).
- **Pruebas:** casos inválidos nuevos que verifiquen que el mensaje incluye línea/columna.
- **Criterio de aceptación:** `TestSuite` y `CompilerProbe` siguen verdes; `ErrorSintactico` es el tipo lanzado.
- **Docs:** `CerberusAI_COMPILER_AUDIT.md` (sección de errores), `language_spec.md`, `2.16 → HECHO`, `RUTA.md`.

### M2 — Llamada a funciones (ref. 2.13)

- **Objetivo:** invocar funciones definidas y usarlas como sentencia y como expresión.
- **Cambios:**
  - Nueva clase `Firma` (nombre, `Tipo` retorno, `List<Tipo>` parámetros) y `Map<String,Firma> firmas` en `AnalizadorSintactico`.
  - `recolectarFirmas()`: pre-escaneo de `tokensDetectados` para poblar `firmas` (permite referencias hacia adelante desde `principal`).
  - `instruccion()` `case IDENTIFICADOR`: si el token siguiente es `PARENTESIS_ABRE` → llamada; si es `=`/compuestos → `asignacionDirecta()`.
  - `llamadaFuncion(...)`: `IDENTIFICADOR "(" [<ARGUMENTOS>] ")"`; verifica firma existente, aridad y tipo de cada argumento (reusa `expresionAritmetica` / `expresionCadena` / `expresionCaracter` / `expresionBooleana` según el parámetro).
  - Llamadas como expresión en `factor()`, `expresionCadena`, `expresionCaracter`, `expresionBooleana` y `expresionImprimible`: devuelven un placeholder tipado (entero → `Expresion(0,false)`, real → `Expresion(0,true)`, etc.); usar un retorno `vacio` como valor → error.
  - Rechazar la llamada a `principal`.
- **BNF:**

```text
<LLAMADA_FUNCION> ::= IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")" ";"

<LLAMADA_EXPR>    ::= IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")"

<ARGUMENTOS>      ::= <EXPRESION> { "," <EXPRESION> }
```

- **Pruebas:** válidos (llamada void; con argumentos; en RHS `entero y = f(1, 2);`; dentro de `imprimir`; función que llama a otra); inválidos (función inexistente, aridad incorrecta, tipo de argumento incorrecto, usar retorno `vacio` como valor, asignar retorno `cadena` a `entero`).
- **Criterio de aceptación:** suite + sonda verdes.
- **Docs:** `CerberusAI_COMPILER_AUDIT.md`, `language_spec.md` (§funciones), `2.13 → HECHO`, `RUTA.md`.

### M3 — `imprimir` multi-argumento (ref. 2.14)

- **Objetivo:** aceptar `,` como separador y admitir booleanos y llamadas como argumentos.
- **Cambios:**
  - `imprimir()`: encadenar con `COMA` además de `OPERADOR_SUMA`.
  - `expresionImprimible()`: añadir `PALABRA_RESERVADA_VER` / `PALABRA_RESERVADA_FAL` y llamadas a función (de M2).
- **BNF:**

```text
<IMPRIMIR> ::= "imprimir" "(" <EXPRESION_IMPRIMIBLE> { ( "," | "+" ) <EXPRESION_IMPRIMIBLE> } ")" ";"

<EXPRESION_IMPRIMIBLE> ::= IDENTIFICADOR | TIPO_CADENA | TIPO_CARACTER
                         | NUMERO_ENTERO | NUMERO_REAL | "verdadero" | "falso"
                         | <LLAMADA_EXPR>
```

- **Pruebas:** válidos `imprimir("a", 2);`, `imprimir(x, y, "z");`, `imprimir(verdadero);`, `imprimir(f(1));`; inválido `imprimir("a", );`.
- **Criterio de aceptación:** suite + sonda verdes.
- **Docs:** `CerberusAI_COMPILER_AUDIT.md`, `language_spec.md`, `2.14 → HECHO`, `RUTA.md`.

### M4 — `nulo` / `vacio` (ref. 2.15)

- **Objetivo:** cerrar el alcance de `nulo` / `vacio`.
- **Cambios:** `vacio` ya está HECHO (2.2); **`nulo` se documenta como reservado / fuera de v1** (sin tipos por referencia). No se implementa semántica.
- **BNF/gramática:** sin cambios.
- **Pruebas:** mantener/ajustar el inválido `14_nulo_como_valor` (`Factor invalido`); no se generan ejemplos en el dataset v2.
- **Criterio de aceptación:** suite + sonda verdes.
- **Docs:** `language_spec.md` (marcar `nulo` `DOCUMENTED/PENDING`), `CerberusAI_COMPILER_AUDIT.md`, `2.15 → HECHO (acotado)`, `RUTA.md`.

---

## 6. Siguientes pasos

1. Decisiones de alcance confirmadas (secciones 3 y 5).
2. Ejecutar la secuencia MEDIA (sección 5, M1–M4) validando cada feature con `audit.TestSuite` y `audit.CompilerProbe`.
3. Re-auditar y actualizar `CerberusAI_COMPILER_AUDIT.md` y `language_spec.md`.
4. Generar `cerberus_dataset_v2.jsonl` únicamente con sintaxis implementada y verificada.

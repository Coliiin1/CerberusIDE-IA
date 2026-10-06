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

Checklist de lo que funciona hoy (verificado con la suite, 32/32 casos):

- `clase Nombre() { ... }` (paréntesis obligatorios).
- Declaraciones `entero / real / cadena / caracter` con inicialización opcional; `booleano` solo declaración.
- Variables globales antes de `clase`.
- Asignación directa numérica (`x = expr;`).
- `imprimir(expr ( + expr)*);` con `IDENTIFICADOR`, `TIPO_CADENA`, `TIPO_CARACTER`, `NUMERO_ENTERO`, `NUMERO_REAL`.
- `si (expr) { ... } [ sino { ... } | sino si(...) ]`.
- `mientras(expr) { ... }`.
- `hacer { ... } mientras(expr);`.
- `segun(id) { caso <tipo>: ... salir; }` (sin `predeterminado`).
- `para(entero i = N; i <rel> M; i++) { ... }` (forma restringida).
- Expresiones aritméticas `+ - * / %` con precedencia y paréntesis.
- Condiciones `exprAritmetica opRelacional exprAritmetica` con `&` y `|`.
- Funciones `funcion nombre() { ... }` y `principal()` (sin parámetros ni retorno).

---

## 2. Gramáticas incompletas (con BNF propuesto)

### 2.1 Parámetros de funciones — `ALTA`

- **Token:** no aplica (los identificadores y tipos ya existen).
- **Dónde falta:** `AnalizadorSintactico.funcionComun()` y `principal()` exigen `match(PARENTESIS_ABRE); match(PARENTESIS_CIERRA);` (paréntesis siempre vacíos).
- **Qué falta:** producción de lista de parámetros y registro de cada parámetro en la tabla con su scope.

**BNF propuesto:**

```text
<FUNCION_COMUN> ::= [ <TIPO_RETORNO> ] IDENTIFICADOR "(" [ <PARAMETROS> ] ")" "{" <INSTRUCCIONES> "}"

<PARAMETROS> ::= <PARAMETRO> { "," <PARAMETRO> }

<PARAMETRO> ::= <TIPO> IDENTIFICADOR
```

---

### 2.2 Tipo de retorno de función — `ALTA`

- **Dónde falta:** `funcionComun()` no lee un tipo antes del identificador.
- **Qué falta:** token `<TIPO>` opcional al inicio de la función y compatibilidad con `retornar`.

**BNF propuesto:**

```text
<TIPO_RETORNO> ::= "entero" | "real" | "cadena" | "caracter" | "booleano" | "vacio"
```

---

### 2.3 `retornar` — `ALTA`

- **Token:** `PALABRA_RESERVADA_RET` (lexer OK, `AnalizadorLexico.analizador()`).
- **Dónde falta:** `AnalizadorSintactico.instruccion()` no tiene `case PALABRA_RESERVADA_RET`.
- **Qué falta:** producción de retorno y validación contra el tipo de retorno de la función.

**BNF propuesto:**

```text
<RETORNAR> ::= "retornar" [ <EXPRESION> ] ";"
```

---

### 2.4 Inicialización booleana — `ALTA`

- **Token:** `PALABRA_RESERVADA_BOO`, `PALABRA_RESERVADA_VER`, `PALABRA_RESERVADA_FAL` (lexer OK).
- **Dónde falta:** `AnalizadorSintactico.asignacion()` — el `switch` cubre `ENT`, `REA`, `CAD`, `CAR` pero **no** `BOO`.
- **Qué falta:** aceptar `verdadero`/`falso` como valor y almacenarlo.

**BNF propuesto:**

```text
<VALOR_BOOLEANO> ::= "verdadero" | "falso"

<DECLARACION> ::= "booleano" IDENTIFICADOR [ "=" <VALOR_BOOLEANO> ] ";"
```

---

### 2.5 Booleano como condición — `ALTA`

- **Dónde falta:** `AnalizadorSintactico.condicion()` y `factor()`. Hoy toda condición exige `exprAritmetica opRelacional exprAritmetica`; `si(verdadero)` o `si(activo)` fallan (`Factor invalido`).
- **Qué falta:** permitir una expresión lógica primaria (valor booleano o identificador booleano) como condición completa.

**BNF propuesto:**

```text
<CONDICION> ::= <EXPRESION_LOGICA_PRIMARIA> [ <OPERADOR_RELACIONAL> <EXPRESION_ARITMETICA> ]

<EXPRESION_LOGICA_PRIMARIA> ::= <EXPRESION_ARITMETICA>
                               | <VALOR_BOOLEANO>
                               | IDENTIFICADOR   // de tipo booleano
```

---

### 2.6 Negación lógica `!` — `ALTA`

- **Token:** `NEGAR` (lexer OK).
- **Dónde falta:** `AnalizadorSintactico.expresionLogica()` solo consume `&` (AND) y `|` (OR).
- **Qué falta:** factor lógico con `!`.

**BNF propuesto:**

```text
<EXPRESION_LOGICA> ::= <CONDICION> { ( "&" | "|" ) <CONDICION> }

<FACTOR_LOGICO> ::= "!" <FACTOR_LOGICO>
                  | <CONDICION>
```

---

### 2.7 Operadores de asignación compuesta `+= -= *= /=` — `ALTA`

- **Token:** `MUL_VARIABLE`, `DIV_VARIABLE` existen en `Tokens`; `INC_VARIABLE`, `DEC_VARIABLE` declarados pero sin uso. `+=` y `-=` **no** están tokenizados.
- **Dónde falta:**
  - Lexer: `AnalizadorLexico.seccionarCadena()` no colapsa correctamente `+=`/`-=` y `analizador()` no tiene cases.
  - Parser: no existe producción de asignación compuesta.
- **Qué falta:** tokens y gramática completos.

**BNF propuesto:**

```text
<ASIGNACION_COMPUESTA> ::= IDENTIFICADOR ( "+=" | "-=" | "*=" | "/=" ) <EXPRESION_ARITMETICA> ";"
```

> Nota: reutilizar `INC_VARIABLE`/`DEC_VARIABLE` para `+=`/`-=` y `MUL_VARIABLE`/`DIV_VARIABLE` para `*=`/`/=`.

---

### 2.8 Decremento `--` en `para` — `MEDIA`

- **Token:** `DECREMENTO` (lexer OK).
- **Dónde falta:** `AnalizadorSintactico.para()` hace `match(INCREMENTO)` fijo.
- **Qué falta:** aceptar `++` o `--`.

**BNF propuesto:**

```text
<INCREMENTO_PARA> ::= IDENTIFICADOR ( "++" | "--" )
```

---

### 2.9 `para` generalizado — `ALTA`

- **Dónde falta:** `AnalizadorSintactico.para()` restringido a `para(entero i = N; i <rel> M; i++)`.
- **Qué falta:** inicialización con cualquier tipo/expresión, condición con expresiones, incremento/decremento general.

**BNF propuesto:**

```text
<PARA> ::= "para" "(" <DECLARACION_PARA> <CONDICION_PARA> ";" <INCREMENTO_PARA> ")" "{" <INSTRUCCIONES> "}"

<DECLARACION_PARA> ::= <TIPO> IDENTIFICADOR "=" <EXPRESION_ARITMETICA> ";"

<CONDICION_PARA> ::= <EXPRESION_ARITMETICA> <OPERADOR_RELACIONAL> <EXPRESION_ARITMETICA> ";"
```

---

### 2.10 `segun` con `predeterminado` — `ALTA`

- **Token:** `predeterminado` **no** es palabra reservada (hoy se tokeniza como `IDENTIFICADOR`).
- **Dónde falta:** lexer (nueva palabra reservada) y `AnalizadorSintactico.segun()` (solo itera sobre `caso`).
- **Qué falta:** case `predeterminado` opcional al final.

**BNF propuesto:**

```text
<SEGUN> ::= "segun" "(" IDENTIFICADOR ")" "{" { <CASO> } [ <PREDETERMINADO> ] "}"

<PREDETERMINADO> ::= "predeterminado" ":" <INSTRUCCIONES> "salir" ";"
```

---

### 2.11 Validación de tipos (semántica) — `ALTA`

- **Dónde falta:** `AnalizadorSemantico` es un stub (`retornarTipo`, `buscar`, `buscarIde`, `verificarIdentificador`). `asignacion()` trunca con `(int)` sin comprobar tipos.
- **Qué falta:** reglas de compatibilidad de tipos (asignación, operadores, condiciones, retorno). No es BNF sino semántica.

**Reglas mínimas propuestas:**

1. `entero` no acepta literales reales sin conversión explícita (hoy trunca: `entero x = 9.5;`).
2. `+ - * / %` solo sobre `entero`/`real`.
3. Condiciones y `&`/`|`/`!` solo sobre booleanos.
4. `retornar` debe coincidir con el tipo de retorno declarado.
5. No redeclarar en el mismo scope (hoy la búsqueda es global, ver 2.12).

---

### 2.12 Scopes reales — `ALTA` — **HECHO**

**Estado:** `HECHO`. El parser mantiene una pila de scopes (`global` → función → bloque); `AnalizadorSemantico.buscarIde()` resuelve al identificador visible más interno y `existeEnScope()` detecta redeclaración solo en el mismo scope. Se permite *shadowing*. Verificado por la suite (`17_shadowing_global_local`, `18_shadowing_bloque`).

<details>
<summary>Descripción original del hueco</summary>

- **Dónde falta:** `AnalizadorSemantico.buscar()`/`buscarIde()` recorren la tabla plana por nombre; `scope` se guarda en `Identificadores` pero no se usa en la búsqueda.
- **Qué falta:** búsqueda por scope anidado (global → función → bloque), permitiendo shadowing.

</details>

---

### 2.13 Llamada a funciones — `MEDIA`

- **Dónde falta:** no existe producción de invocación de una función definida.
- **Qué falta:** gramática de llamada y verificación de que la función existe.

**BNF propuesto:**

```text
<LLAMADA_FUNCION> ::= IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")" ";"

<ARGUMENTOS> ::= <EXPRESION> { "," <EXPRESION> }
```

---

### 2.14 `imprimir` con varios argumentos — `MEDIA`

- **Dónde falta:** `AnalizadorSintactico.imprimir()` solo concatena con `+`.
- **Qué falta:** aceptar separador `,` además de `+`.

**BNF propuesto:**

```text
<IMPRIMIR> ::= "imprimir" "(" <EXPRESION_IMPRIMIBLE> { ( "," | "+" ) <EXPRESION_IMPRIMIBLE> } ")" ";"
```

---

### 2.15 `nulo` y `vacio` — `MEDIA`

- **Token:** `PALABRA_RESERVADA_NUL`, `PALABRA_RESERVADA_VAC` (lexer OK).
- **Dónde falta:** sin semántica; `entero x = nulo;` falla (`Factor invalido`).
- **Qué falta:** definir su uso (asignación de `nulo` a tipos por referencia; `vacio` como tipo de retorno de función sin valor).

---

### 2.16 Errores tipados (`ErrorSintactico`) — `MEDIA`

- **Dónde falta:** `AnalizadorSintactico.ErrorSintactico` existe pero **no se usa**; los errores son `RuntimeException`/`AssertionError`.
- **Qué falta:** unificar el reporte de errores con línea/columna/lexema.

---

### 2.17 POO: `nuevo`, `este`, `publico`, `privado` — `BAJA / ACOTAR`

- **Token:** todos existen (`PALABRA_RESERVADA_NUE`, `_EST`, `_PUB`, `_PRI`).
- **Dónde falta:** no hay gramática de objetos, atributos, métodos con parámetros, herencia ni control de acceso.
- **Qué falta:** decidir alcance. Ver sección 3.

**BNP propuesto (mínimo, si entra en v1):**

```text
<ATRIBUTO> ::= ( "publico" | "privado" ) <TIPO> IDENTIFICADOR ";"

<INSTANCIA> ::= <IDENTIFICADOR_CLASE> IDENTIFICADOR "=" "nuevo" <IDENTIFICADOR_CLASE> "(" ")" ";"

<ACCESO_MIEMBRO> ::= IDENTIFICADOR "." IDENTIFICADOR
```

---

### 2.18 Arreglos — `BAJA / ACOTAR`

- **Token:** `CORCHETE_ABRE`, `CORCHETE_CIERRA` (lexer OK).
- **Dónde falta:** sin producción de arreglo; `entero [] a;` es rechazado.
- **Qué falta:** decidir alcance.

**BNF propuesto (si entra en v1):**

```text
<DECLARACION_ARREGLO> ::= <TIPO> "[" "]" IDENTIFICADOR [ "=" "[" { <VALOR> } "]" ] ";"

<ACCESO_ARREGLO> ::= IDENTIFICADOR "[" <EXPRESION_ARITMETICA> "]"
```

---

## 3. Decisiones de alcance (PENDIENTES de confirmar)

Para no bloquear el entrenamiento de la IA, se propone **acotar fuera del alcance v1**:

| Característica | Propuesta |
|---|---|
| Herencia | Fuera de v1 (documentar como futura). |
| Constructores | Fuera de v1 (documentar como futura). |
| Arreglos completos | Fuera de v1 (o solo sintaxis mínima). |
| `publico`/`privado` como modificadores reales | Fuera de v1 (solo tokens). |

Estas se marcarían como `PENDING`/`DOCUMENTED` en `language_spec.md` y **no** generarían ejemplos en el dataset v2.

---

## 4. Resumen de prioridades

| # | Feature | Prioridad | Estado |
|---|---|---|---|
| 2.1 | Parámetros de funciones | ALTA | PENDIENTE |
| 2.2 | Tipo de retorno | ALTA | PENDIENTE |
| 2.3 | `retornar` | ALTA | PENDIENTE |
| 2.4 | Inicialización booleana | ALTA | PENDIENTE |
| 2.5 | Booleano como condición | ALTA | PENDIENTE |
| 2.6 | Negación `!` | ALTA | PENDIENTE |
| 2.7 | Asignación compuesta `+= -= *= /=` | ALTA | PENDIENTE |
| 2.9 | `para` generalizado | ALTA | PENDIENTE |
| 2.10 | `segun` con `predeterminado` | ALTA | PENDIENTE |
| 2.11 | Validación de tipos | ALTA | PENDIENTE |
| 2.12 | Scopes reales | ALTA | HECHO |
| 2.8 | Decremento `--` | MEDIA | PENDIENTE |
| 2.13 | Llamada a funciones | MEDIA | PENDIENTE |
| 2.14 | `imprimir` multi-argumento | MEDIA | PENDIENTE |
| 2.15 | `nulo`/`vacio` | MEDIA | PENDIENTE |
| 2.16 | Errores tipados | MEDIA | PENDIENTE |
| 2.17 | POO (`nuevo`, `este`, `publico`, `privado`) | BAJA / ACOTAR | PENDIENTE |
| 2.18 | Arreglos | BAJA / ACOTAR | PENDIENTE |

---

## 5. Siguientes pasos

1. Confirmar las decisiones de alcance (sección 3).
2. Implementar por prioridad (ALTA → MEDIA), validando cada feature con `audit.CompilerProbe` y `audit.TestSuite`.
3. Re-auditar y actualizar `CerberusAI_COMPILER_AUDIT.md` y `language_spec.md`.
4. Generar `cerberus_dataset_v2.jsonl` únicamente con sintaxis implementada y verificada.

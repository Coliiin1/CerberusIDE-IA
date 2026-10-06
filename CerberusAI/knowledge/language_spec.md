# Cerberus Language Specification

**Nombre del lenguaje:** Cerberus  
**Documento:** `language_spec.md`  
**Propósito:** especificación técnica de referencia para el lenguaje Cerberus y para herramientas que analicen, generen, expliquen o corrijan código Cerberus.  
**Fuente principal:** *Documentación CerberusIDE.pdf*  
**Estado:** especificación consolidada y **reconciliada con la implementación real del compilador** (ver `CerberusAI_COMPILER_AUDIT.md`, raíz del repositorio).

> **Regla fundamental:** este documento no debe inventar características de Cerberus. Cuando la documentación no define algo con suficiente claridad, se marca como `PENDING` o `PARTIAL`.
>
> **Autoridad:** cuando la documentación (PDF) y el compilador real difieren, el compilador real es la fuente de verdad. Los conflictos detectados se marcan como `CONFLICT` y se listan en la sección 26. La auditoría `CerberusAI_COMPILER_AUDIT.md` registra la verificación dinámica (sonda `audit.CompilerProbe`) de cada característica.

---

## 1. Identidad del lenguaje

### 1.1 Descripción

Cerberus es un lenguaje de programación educativo, escrito en español, diseñado para facilitar el aprendizaje de programación y de programación orientada a objetos.

Su sintaxis toma como referencia conceptos y formas de escritura presentes en Java, pero adapta los elementos principales al español y busca una sintaxis más sencilla y comprensible para estudiantes con conocimientos básicos de programación.

### 1.2 Paradigma

- Programación orientada a objetos.
- Soporte documentado para clases, objetos, atributos, métodos, constructores e herencia.
- Soporte de estructuras de control imperativas.

**Estado:** `DEFINED` para el objetivo y paradigma general.  
**Nota:** varias características de POO están documentadas conceptualmente, pero su gramática completa no está definida en la documentación disponible. Ver sección 10.

### 1.3 Ejecución

La documentación describe Cerberus como un lenguaje compilado y presenta un compilador escrito en Java.

**Estado:** `DEFINED` como diseño documentado.

### 1.4 Programa mínimo documentado

```cerberus
clase programa(){
    funcion principal(){
        imprimir("Hola mundo");
    }
}
```

Este ejemplo representa la estructura básica para un programa Cerberus.

> **Nota (reconciliación):** el compilador **exige** los paréntesis en la declaración de clase: `clase programa()`. La forma `clase programa {` (sin paréntesis) es rechazada (`Se esperaba PARENTESIS_ABRE`). Estado: `IMPLEMENTED`.

---

## 2. Convenciones de esta especificación

Cada característica utiliza uno de los siguientes estados:

| Estado | Significado |
|---|---|
| `DEFINED` | La documentación define la regla de manera suficiente. |
| `IMPLEMENTED` | La documentación confirma que existe una implementación en el compilador. |
| `PARTIAL` | Existe la característica, pero faltan detalles formales o hay información incompleta. |
| `DOCUMENTED` | La característica aparece en la documentación, pero no se ha confirmado aquí que la implementación actual coincida exactamente. |
| `PENDING` | La característica requiere una decisión o definición posterior. |
| `CONFLICT` | La documentación contiene información contradictoria que no debe resolverse silenciosamente. |

### 2.1 Regla de autoridad

Para el desarrollo del lenguaje:

1. Esta especificación representa la intención técnica consolidada.
2. La implementación real del compilador determina qué acepta actualmente el compilador.
3. La documentación original determina qué características fueron propuestas o documentadas.
4. Si documentación y compilador difieren, el conflicto debe registrarse y no ocultarse.
5. Una IA no debe presentar una característica `PENDING`, `PARTIAL`, `DOCUMENTED` o `CONFLICT` como si fuera una característica completamente definida.

---

## 3. Reglas léxicas

## 3.1 Alfabeto

La documentación define un alfabeto que incluye:

- letras minúsculas `a-z`
- letras mayúsculas `A-Z`
- dígitos `0-9`
- `{ }`
- `( )`
- `[ ]`
- `.`
- `;`
- `:`
- `"`
- `'`
- `+ - * / %`
- `= < > ! & |`
- `_`
- `,`

**Estado:** `DOCUMENTED`.

## 3.2 Sensibilidad a mayúsculas y minúsculas

Cerberus distingue entre mayúsculas y minúsculas.

Por ejemplo:

```cerberus
clase programa(){}
```

no debe considerarse equivalente a:

```cerberus
Clase programa(){}
```

**Estado:** `DEFINED`.

## 3.3 Identificadores

La documentación proporciona la expresión regular conceptual:

```text
[a-zA-Z]([a-zA-Z0-9_])*
```

Un identificador:

- debe comenzar con una letra;
- puede continuar con letras;
- puede contener números después del primer carácter;
- puede contener `_` después del primer carácter;
- no puede ser una palabra reservada;
- distingue mayúsculas y minúsculas.

### Ejemplos válidos

```cerberus
x
edad
nombre1
numero_total
Promedio
```

### Ejemplos inválidos

```cerberus
1edad
123
nombre-
```

**Estado:** `DEFINED`.

> La expresión regular aparece en la documentación con comas tipográficas dentro de los conjuntos. Para uso formal se normaliza aquí a `[a-zA-Z]([a-zA-Z0-9_])*`, que expresa la descripción textual proporcionada.

---

## 4. Palabras reservadas

Las palabras reservadas documentadas son:

### 4.1 POO

| Palabra | Función |
|---|---|
| `clase` | Define una clase. |
| `nuevo` | Indica la instanciación de un objeto. |
| `este` | Hace referencia al objeto/atributo de la clase actual. |

### 4.2 Control de acceso

| Palabra | Función |
|---|---|
| `publico` | Permite acceso desde cualquier parte. |
| `privado` | Restringe el acceso a la misma clase. |

### 4.3 Condicionales y selección

`si`, `sino`, `segun`, `caso`, `predeterminado`, `salir`

### 4.4 Ciclos

`para`, `mientras`, `hacer`

### 4.5 Literales booleanos

`verdadero`, `falso`

### 4.6 Funciones

`funcion`, `principal`, `retornar`, `imprimir`

### 4.7 Tipos

`entero`, `real`, `caracter`, `cadena`, `booleano`, `nulo`, `vacio`

**Estado:** `DEFINED` como conjunto documentado. La implementación exacta de cada palabra debe verificarse contra el lexer actual.

---

## 5. Tipos de datos

Los tipos documentados son:

| Tipo | Descripción | Estado |
|---|---|---|
| `entero` | Valores numéricos enteros. | `DEFINED` |
| `real` | Valores numéricos reales. | `DEFINED` |
| `caracter` | Valores de carácter. | `DEFINED` |
| `cadena` | Texto. | `DEFINED` |
| `booleano` | Valor lógico. | `DEFINED` |
| `nulo` | Literal/tipo asociado a ausencia de valor. | `PARTIAL` |
| `vacio` | Tipo/valor asociado a ausencia de retorno. | `PARTIAL` |
| arreglos | Estructuras de múltiples elementos. | `PARTIAL` |

### 5.1 Declaraciones básicas

```cerberus
entero edad;
real promedio = 9.5;
cadena nombre = "Carlos";
```

La documentación formaliza la forma general de declaración como:

```text
<TIPO> IDENTIFICADOR "=" <VALOR> ";"
```

**Reconciliación con el compilador:**

| Declaración | ¿Acepta el compilador? | Estado |
|---|---|---|
| `entero x;` / `entero x = 5;` | Sí | `IMPLEMENTED` |
| `real x;` / `real x = 9.5;` | Sí | `IMPLEMENTED` |
| `cadena x;` / `cadena x = "hola";` | Sí | `IMPLEMENTED` |
| `caracter x;` / `caracter x = 'a';` | Sí | `IMPLEMENTED` |
| `booleano x;` | Sí | `IMPLEMENTED` |
| `booleano x = verdadero;` | Sí | `IMPLEMENTED` |
| `entero x = 9.5;` | **No** (`TIPO INCOMPATIBLE`) | `IMPLEMENTED` |
| `real r = 5;` (entero→real) | Sí (ensanche) | `IMPLEMENTED` |
| `cadena b = a;` / `caracter c = d;` | Sí | `IMPLEMENTED` |

### 5.2 Valores documentados

```text
NUMERO_ENTERO
NUMERO_REAL
TIPO_CADENA
TIPO_CARACTER
verdadero
falso
```

### 5.3 Booleanos

`verdadero` representa un valor booleano verdadero y `falso` un valor booleano falso.

La documentación también describe `verdadero` como equivalente a un 1 lógico y `falso` como equivalente a un 0 lógico.

**Reconciliación (implementado):** `booleano x = verdadero;`/`= falso;` se aceptan, y el RHS admite una variable booleana. El booleano participa como condición desnuda (`si(activo)`) y en comparaciones (`activo == verdadero`, `activo != falso`) y en `&`/`|`. No participa en aritmética. Estado: `IMPLEMENTED`.

---

## 6. Variables y asignaciones

### 6.1 Declaración

Forma documentada:

```cerberus
entero edad = 20;
```

También se documenta la declaración sin valor inicial:

```cerberus
entero edad;
```

**Estado:** `IMPLEMENTED`. El parser acepta tanto `entero edad = 20;` como `entero edad;` (la inicialización es opcional en todos los tipos, incluido `booleano`).

### 6.2 Asignación

La documentación utiliza expresiones de asignación como:

```cerberus
resultado = (a + b) * c;
```

**Estado:** `IMPLEMENTED` para asignación de variables ya declaradas, con la restricción de que la expresión asignada a un `entero`/`real` es aritmética numérica (ver sección 8).

### 6.3 Redeclaración

El análisis semántico documentado detecta la redeclaración de variables.

**Reconciliación:** se detecta la redeclaración **dentro del mismo scope** (global, función o bloque). Se permite *shadowing*: una variable local puede ocultar una global o de un bloque externo.

**Estado:** `IMPLEMENTED` (scopes global/función/bloque con shadowing).

### 6.4 Identificador no declarado

El analizador semántico detecta el uso de identificadores no declarados.

Ejemplo de error documentado:

```text
NO SE HA DECLARADO EL IDENTIFICADOR: x
```

**Estado:** `IMPLEMENTED` según la documentación.

---

## 7. Operadores

### 7.1 Aritméticos

| Operador | Tipo |
|---|---|
| `+` | Binario |
| `-` | Binario |
| `*` | Binario |
| `/` | Binario |
| `%` | Binario |
| `++` | Unario |
| `--` | Unario |
| `+=` | Asignación compuesta |
| `-=` | Asignación compuesta |
| `*=` | Asignación compuesta |
| `/=` | Asignación compuesta |

La documentación indica que los operadores aritméticos utilizan operandos `entero` y `real` y producen un resultado numérico.

**Reconciliación:** implementados en el parser `+`, `-`, `*`, `/`, `%`. `++` se usa únicamente como incremento en `para`. Los operadores compuestos (`+=`, `-=`, `*=`, `/=`) y `--` **no están implementados**: el lexer no los tokeniza correctamente y el parser los rechaza. Estado: `IMPLEMENTED` para `+ - * / %` y `++` (en `para`); `CONFLICT`/no implementado para `+=`, `-=`, `*=`, `/=`, `--`.

### 7.2 Relacionales

```text
==
!=
<
>
<=
>=
```

Los operadores relacionales producen un resultado booleano.

### 7.3 Lógicos

```text
&
|
```

El parser solo implementa `&` (AND) y `|` (OR) en `expresionLogica()`.

La documentación también lista `!` dentro del conjunto de operadores, pero el parser **no lo consume** en ninguna producción: `si(!x)` es rechazado (`Factor invalido`).

**Estado:** `IMPLEMENTED` para `&` y `|`; `DOCUMENTED/PARTIAL` para `!` (tokenizado pero no usado).

### 7.4 Precedencia

La documentación indica que las expresiones aritméticas siguen precedencia matemática mediante la separación entre:

- expresiones;
- términos;
- factores.

No se establece en el documento una tabla completa y formal de precedencia para todos los operadores.

**Estado:** `PARTIAL`.

---

## 8. Expresiones

### 8.1 Expresiones aritméticas

Las expresiones aritméticas permiten combinar valores numéricos mediante operadores aritméticos.

Ejemplo:

```cerberus
resultado = (a + b) * c;
```

La documentación estructura estas expresiones mediante:

```text
EXPRESION_ARITMETICA
TERMINO
FACTOR
```

### 8.2 Factor

Producción documentada:

```text
<FACTOR> ::= IDENTIFICADOR
           | NUMERO_ENTERO
           | NUMERO_REAL
           | "(" <EXPRESION_ARITMETICA> ")"
```

### 8.3 Expresiones lógicas

Las condiciones pueden utilizar operadores relacionales y operadores lógicos.

Ejemplo documentado:

```cerberus
si (edad >= 18 & activo == verdadero) {
    imprimir("Acceso permitido");
}
```

**Reconciliación (implementado):** se soportan condiciones booleanas desnudas (`si(activo)`, `si(verdadero)`) y comparaciones booleanas (`activo == verdadero`, `activo != falso`), además de la condición numérica `exprAritmetica opRelacional exprAritmetica`. Los operadores `&`/`|` combinan condiciones. El ejemplo documentado `edad >= 18 & activo == verdadero` **ya compila** con `activo` de tipo `booleano`:

```cerberus
entero edad = 20;
booleano activo = verdadero;
si (edad >= 18 & activo == verdadero) {
    imprimir("Acceso permitido");
}
```

Falta el operador `!` (ver 6.4).

### 8.4 Condición

Producción documentada:

```text
<CONDICION> ::= <EXPRESION_ARITMETICA>
                <OPERADOR_RELACIONAL>
                <EXPRESION_ARITMETICA>
```

### 8.5 Operador relacional

```text
<OPERADOR_RELACIONAL> ::= ">"
                         | "<"
                         | ">="
                         | "<="
                         | "=="
                         | "!="
```

---

## 9. Impresión

La palabra reservada `imprimir` permite mostrar información en la consola.

Ejemplos documentados:

```cerberus
imprimir("Hola mundo");
imprimir(edad);
imprimir(10);
imprimir('A');
```

También se documenta la posibilidad de concatenar expresiones imprimibles mediante `+`.

### Gramática documentada

```text
<IMPRIMIR> ::= "imprimir" "(" <EXPRESION_IMPRIMIBLE> ")" ";"

<EXPRESION_IMPRIMIBLE> ::= <VALOR_IMPRIMIBLE>
                           <CONCATENACION>

<VALOR_IMPRIMIBLE> ::= IDENTIFICADOR
                      | TIPO_CADENA
                      | TIPO_CARACTER
                      | NUMERO_ENTERO
                      | NUMERO_REAL
```

La producción de `<CONCATENACION>` no aparece completa en el texto disponible.

**Estado:** `PARTIAL` para la gramática completa.

---

## 10. Control de flujo

## 10.1 Condicional `si`

Permite ejecutar un bloque cuando una expresión lógica es verdadera.

```cerberus
si (edad >= 18) {
    imprimir("Mayor de edad");
}
```

### `sino`

```cerberus
si (calificacion >= 70) {
    imprimir("Aprobado");
} sino {
    imprimir("Reprobado");
}
```

Se documentan también condicionales anidados.

### Gramática

```text
<SI> ::= "si" "(" <EXPRESION_LOGICA> ")" "{"
         <INSTRUCCIONES>
         "}"
         <SINO> | ε

<SINO> ::= "sino" "{" <INSTRUCCIONES> "}"
         | <SI>
```

**Estado:** `DEFINED`.

## 10.2 Ciclo `para`

Permite ejecutar instrucciones repetidamente mediante inicialización, condición e incremento/decremento.

Ejemplo (forma que realmente acepta el compilador):

```cerberus
para(entero i = 0; i < 10; i++) {
    imprimir(i);
}
```

**Reconciliación:** la implementación es muy restringida. Exige literalmente:

```text
para( entero IDENTIFICADOR = NUMERO_ENTERO ; IDENTIFICADOR OPERADOR_RELACIONAL NUMERO_ENTERO ; IDENTIFICADOR ++ ) { ... }
```

- La inicialización requiere la palabra `entero`.
- El valor inicial y el límite son enteros literales.
- El incremento es `++` sobre el identificador.
- `para(i = 0; ...)` es rechazado (`Se esperaba PALABRA_RESERVADA_ENT`).

Se documenta el operador relacional específico para `para`:

```text
<OPERADOR_RELACIONAL_PARA> ::= "<"
                              | ">"
                              | "<="
                              | ">="
```

La producción completa de `<PARA>` aparece en la documentación, pero su representación textual presenta detalles de formato ambiguos.

**Estado:** `PARTIAL` para la producción formal completa; comportamiento general `IMPLEMENTED` solo en su forma restringida.

## 10.3 Ciclo `mientras`

Ejecuta el bloque mientras la condición sea verdadera.

```cerberus
mientras(x < 5) {
    imprimir(x);
}
```

La condición se evalúa antes de cada iteración.

**Estado:** `DEFINED`.

## 10.4 Ciclo `hacer mientras`

Ejecuta el bloque al menos una vez y evalúa la condición al final.

```cerberus
hacer {
    imprimir(contador);
} mientras(contador < 10);
```

**Estado:** `DEFINED`.

## 10.5 Selección `segun`

Permite seleccionar diferentes bloques dependiendo del valor de una variable.

Forma que realmente acepta el compilador:

```cerberus
segun(opcion) {
    caso 1:
        imprimir("Uno");
        salir;
    caso 2:
        imprimir("Dos");
        salir;
}
```

Elementos implementados:

- `segun`
- `caso`
- `salir` (obligatorio `;` al final)

**Reconciliación:** `predeterminado` **no está implementado** (`Se esperaba LLAVE_CIERRA`). `salir` exige `;` (`Se esperaba PUNTO_COMA`). El selector debe ser un identificador; cada `caso` debe usar un literal del mismo tipo que el selector (`retornarTipo`): selector `entero` → `caso 1`; selector `cadena` → `caso "..."`.

**Estado:** `IMPLEMENTED` para `segun`/`caso`/`salir;`; `CONFLICT` para `predeterminado` (documentado pero no implementado).

---

## 11. Funciones

La documentación utiliza la palabra reservada `funcion` y distingue una función `principal` de funciones comunes.

### 11.1 Función principal

Producción documentada:

```text
<PRINCIPAL> ::= "principal" "(" ")" "{" <INSTRUCCIONES> "}"
```

Ejemplo:

```cerberus
funcion principal() {
    imprimir("Hola mundo");
}
```

### 11.2 Función común

Producción documentada:

```text
<FUNCION_COMUN> ::= IDENTIFICADOR "(" ")" "{" <INSTRUCCIONES> "}"
```

La documentación también describe `retornar` como mecanismo para devolver un valor desde una función o método.

### 11.3 Parámetros

**Reconciliación:** el compilador **no implementa parámetros de funciones**. La única forma aceptada es `funcion nombre() { ... }` con paréntesis vacíos.

**Estado:** `PENDING` (documentación no define gramática; compilador no lo implementa).

### 11.4 Tipos de retorno

**Reconciliación:** el compilador **no implementa tipos de retorno**. Las funciones se declaran solo como `funcion nombre() { ... }` (sin tipo de retorno).

**Estado:** `PENDING`/no implementado.

### 11.5 `retornar`

Conceptualmente devuelve el valor de una función o método.

**Reconciliación:** `retornar` está tokenizado (`PALABRA_RESERVADA_RET`) pero **no tiene gramática**: el parser lo rechaza (`no se ha puesto el token`).

Ejemplo documentado conceptualmente (no compila en el compilador actual):

```cerberus
publico entero uno() {
    retornar 1;
}
```

La capitalización mostrada en algunos ejemplos de la documentación no debe tomarse como sintaxis alternativa: Cerberus es sensible a mayúsculas y minúsculas.

**Estado:** `DOCUMENTED/PARTIAL` (tokenizado pero no implementado).

---

## 12. Programación orientada a objetos

> **Reconciliación general:** el compilador solo implementa la declaración de clases (`clase Nombre() { ... }`) y funciones. Los tokens `nuevo`, `este`, `publico`, `privado` están **tokenizados pero no tienen gramática**. No hay objetos, instanciación, atributos, métodos con parámetros, constructores, herencia ni control de acceso real. Estado global: `DOCUMENTED/PARTIAL`.

## 12.1 Clases

La palabra `clase` define una clase.

Ejemplo (forma que acepta el compilador):

```cerberus
clase hola(){}
```

El programa mínimo utiliza:

```cerberus
clase programa(){
    funcion principal() {
        imprimir("Hola mundo");
    }
}
```

## 12.2 Objetos

La palabra `nuevo` se documenta para indicar la instanciación de un objeto.

Ejemplo conceptual documentado:

```cerberus
hola mensaje = nuevo hola();
```

## 12.3 `este`

`este` se documenta como referencia al identificador utilizado como atributo de la clase actual.

Ejemplo:

```cerberus
este.nombre = nombre;
```

## 12.4 Control de acceso

```cerberus
publico nombre;
privado apellido;
```

- `publico`: accesible desde cualquier parte.
- `privado`: accesible desde la misma clase.

## 12.5 Constructores

La documentación menciona constructores como parte del paradigma orientado a objetos, pero no proporciona una gramática formal completa para ellos.

**Estado:** `PENDING`.

## 12.6 Métodos

Los métodos forman parte del modelo orientado a objetos documentado, pero su gramática completa no está especificada en el material disponible.

**Estado:** `PARTIAL`.

## 12.7 Herencia

La herencia se menciona como parte de la programación orientada a objetos de Cerberus, pero no se proporciona una sintaxis formal completa en la documentación disponible.

**Estado:** `PENDING`.

---

## 13. Gramática formal consolidada

La siguiente sección reproduce las producciones que aparecen explícitamente en la documentación, sin inventar las producciones que no están completas.

### 13.1 Instrucciones

```text
<INSTRUCCIONES> ::= <INSTRUCCION>
                  | <INSTRUCCIONES>
                  | ε

<INSTRUCCION> ::= <DECLARACION>
                | <FUNCIONES>
                | <IMPRIMIR>
                | <SI>
                | <PARA>
                | <HACER_MIENTRAS>
                | <SEGUN>
```

### 13.2 Declaración

```text
<DECLARACION> ::= <TIPO> IDENTIFICADOR [ "=" <VALOR> ] ";"

<TIPO> ::= "entero"
         | "real"
         | "cadena"
         | "caracter"
         | "booleano"

<VALOR> ::= NUMERO_ENTERO
          | NUMERO_REAL
          | TIPO_CADENA
          | TIPO_CARACTER
```

> **Reconciliación:** la inicialización es opcional. `<VALOR>` admite también `verdadero`/`falso` para `booleano` (implementado). Un `entero` **no** acepta `NUMERO_REAL` (rechazado con `TIPO INCOMPATIBLE`); `real` sí acepta entero (ensanche).

### 13.3 `si`

```text
<SI> ::= "si" "(" <EXPRESION_LOGICA> ")" "{"
         <INSTRUCCIONES>
         "}"
         <SINO> | ε

<SINO> ::= "sino" "{" <INSTRUCCIONES> "}"
         | <SI>
```

### 13.4 Condiciones

```text
<EXPRESION_LOGICA> ::= <CONDICION> <OPERADOR_LOGICO>

<CONDICION> ::= <EXPRESION_ARITMETICA>
                <OPERADOR_RELACIONAL>
                <EXPRESION_ARITMETICA>

<OPERADOR_RELACIONAL> ::= ">"
                         | "<"
                         | ">="
                         | "<="
                         | "=="
                         | "!="
```

La producción de `<EXPRESION_LOGICA>` mostrada en la documentación es incompleta para representar todas las combinaciones de condiciones y operadores lógicos. Por ello se conserva como `DOCUMENTED/PARTIAL`.

### 13.5 Factor

```text
<FACTOR> ::= IDENTIFICADOR
           | NUMERO_ENTERO
           | NUMERO_REAL
           | "(" <EXPRESION_ARITMETICA> ")"
```

### 13.6 Programa y funciones

```text
<PROGRAMA> ::= [ <GLOBALES> ] <INICIO> <FUNCIONES> "}"

<INICIO> ::= "clase" IDENTIFICADOR "(" ")" "{"

<FUNCIONES> ::= { <FUNCION> }
<FUNCION>  ::= "funcion" ( <PRINCIPAL> | <FUNCION_COMUN> )

<PRINCIPAL>     ::= "principal" "(" ")" "{" <INSTRUCCIONES> "}"
<FUNCION_COMUN> ::= IDENTIFICADOR "(" ")" "{" <INSTRUCCIONES> "}"
```

> **Reconciliación:** la clase exige `clase Nombre()` (con paréntesis). Las funciones exigen la palabra `funcion` delante y no admiten parámetros ni tipo de retorno. Se exige exactamente una función `principal`.

---

## 14. Tokens del compilador

La documentación proporciona nombres de tokens como los siguientes:

### 14.1 Palabras reservadas

```text
PALABRA_RESERVADA_CLA
PALABRA_RESERVADA_NUE
PALABRA_RESERVADA_EST
PALABRA_RESERVADA_PUB
PALABRA_RESERVADA_PRI
PALABRA_RESERVADA_SI
PALABRA_RESERVADA_SIN
PALABRA_RESERVADA_SEG
PALABRA_RESERVADA_CAS
PALABRA_RESERVADA_SAL
PALABRA_RESERVADA_PAR
PALABRA_RESERVADA_MIE
PALABRA_RESERVADA_HAC
PALABRA_RESERVADA_VER
PALABRA_RESERVADA_FAL
PALABRA_RESERVADA_FUN
PALABRA_RESERVADA_RET
PALABRA_RESERVADA_IMP
PALABRA_RESERVADA_ENT
PALABRA_RESERVADA_REA
PALABRA_RESERVADA_CAR
PALABRA_RESERVADA_CAD
PALABRA_RESERVADA_BOO
PALABRA_RESERVADA_NUL
PALABRA_RESERVADA_VAC
PALABRA_RESERVADA_PRIN
```

### 14.2 Delimitadores

```text
DOS_PUNTOS
PUNTO_COMA
PARENTESIS_ABRE
PARENTESIS_CIERRA
LLAVE_ABRE
LLAVE_CIERRA
CORCHETE_ABRE
CORCHETE_CIERRA
```

### 14.3 Literales

```text
NUMERO_ENTERO
NUMERO_REAL
TIPO_CADENA
TIPO_CARACTER
```

### 14.4 Operadores

Los tokens exactos de cada operador deben conservar los nombres definidos por la implementación del lexer.

**Estado:** `PARTIAL` hasta validar la enumeración completa de `Tokens` del código fuente del compilador.

---

## 15. Análisis sintáctico

El parser de Cerberus valida las estructuras gramaticales mediante consumo de tokens.

La documentación indica el uso de un método `match()` para verificar que el token actual sea el esperado.

Ejemplo conceptual:

```text
match(PARENTESIS_CIERRA)
```

Si el token no coincide, se produce un error similar a:

```text
Se esperaba PARENTESIS_CIERRA y se encontró LLAVE_ABRE
```

El mensaje puede incluir:

- token esperado;
- token encontrado;
- línea del error;
- lexema involucrado.

### 15.1 Balanceo de símbolos

La implementación documentada utiliza una pila para comprobar:

- `()`
- `{}`
- `[]`

Ejemplo de error documentado:

```text
Hubo un problema con LLAVE_ABRE
```

### 15.2 Estructuras validadas

Se documentan métodos de validación para:

- `si()`
- `para()`
- `mientras()`
- `segun()`
- `imprimir()`
- `declaracion()`

---

## 16. Análisis semántico

El análisis semántico comprueba que estructuras sintácticamente correctas tengan significado válido dentro de las reglas de Cerberus.

La documentación menciona una implementación mediante traducción dirigida por la sintaxis y una tabla de símbolos.

> **Reconciliación:** `AnalizadorSemantico` es un **stub real**: solo implementa `retornarTipo()`, `buscar()`, `buscarIde()` y `verificarIdentificador()`. La interfaz gráfica muestra "AÚN NO IMPLEMENTADO". Las comprobaciones de identificadores/redeclaración viven en el parser (`agregarTabla()` y `asignacionDirecta()`), no en un analizador semántico completo.

### 16.1 Tabla de símbolos

La tabla se documenta conceptualmente como:

```java
ArrayList<Identificadores> tabla
```

El componente `AnalizadorSemantico` utiliza esta información para resolver identificadores y sus tipos.

### 16.2 Validaciones documentadas

Las siguientes comprobaciones existen **en el parser**, no en un analizador semántico completo:

1. identificadores no declarados (en `asignacionDirecta()` y `factor()`);
2. redeclaración de variables en el mismo scope y *shadowing* con scopes anidados (en `agregarTabla()` y `AnalizadorSemantico`, con pila de scopes global/función/bloque);
3. ausencia de la función `principal` (en `funciones()`);
4. múltiples funciones `principal` (en `principal()`);
5. tipos inválidos en asignaciones: **no implementado** (ver 16.3).

### 16.3 Compatibilidad de tipos

La documentación establece que el análisis semántico verifica compatibilidad de tipos.

Ejemplo conceptual de error:

```text
no se debe intentar sumar un texto con un número
```

**Reconciliación (implementado):** las expresiones llevan tipo. Reglas: `entero←entero`; `real←entero|real`; `cadena←cadena`; `caracter←caracter`; `booleano←booleano`. `entero x = 9.5;` se rechaza (`TIPO INCOMPATIBLE`); sumar un texto con un número se rechaza con `TIPO INCOMPATIBLE`. Las condiciones booleanas desnudas y `&`/`|` están implementadas; falta `!` (6.4).

**Estado:** `IMPLEMENTED` (entero/real/cadena/caracter/booleano).

---

## 17. Sistema de errores

### 17.1 Error sintáctico

Ocurre cuando la secuencia de tokens no coincide con la gramática esperada.

Ejemplo:

```text
Se esperaba PARENTESIS_CIERRA y se encontró LLAVE_ABRE
```

### 17.2 Error semántico

Ocurre cuando el código tiene una estructura válida, pero viola una regla semántica.

Ejemplo:

```text
NO SE HA DECLARADO EL IDENTIFICADOR: x
```

### 17.3 Excepciones

La documentación indica el uso de:

```java
RuntimeException
AssertionError
```

para detener el análisis ante inconsistencias.

### 17.4 Finalización del análisis

El método principal `analizar()` encapsula el proceso en un bloque `try-catch`. Cuando ocurre un error:

1. se captura la excepción;
2. se muestra el mensaje mediante `JOptionPane`;
3. se marca el análisis como inválido;
4. se evita continuar con resultados inconsistentes.

---

## 18. Ejemplos oficiales de referencia

Estos ejemplos deben considerarse ejemplos canónicos para generación y explicación hasta que exista una especificación posterior que los modifique.

> **Reconciliación:** algunos ejemplos de la documentación no compilan con el compilador actual. A continuación se muestran corregidos (con paréntesis en `clase`, `entero` en `para`, `salir;` y sin `predeterminado` en `segun`, y sin inicialización booleana).

### 18.1 Hola mundo

```cerberus
clase programa(){
    funcion principal() {
        imprimir("Hola mundo");
    }
}
```

### 18.2 Variables

```cerberus
entero edad = 20;
real promedio = 9.5;
cadena nombre = "Carlos";
```

### 18.3 Condicional

```cerberus
si (edad >= 18) {
    imprimir("Mayor de edad");
} sino {
    imprimir("Menor de edad");
}
```

### 18.4 `para`

```cerberus
para(entero i = 0; i < 10; i++) {
    imprimir(i);
}
```

### 18.5 `mientras`

```cerberus
mientras(x < 5) {
    imprimir(x);
}
```

### 18.6 `hacer mientras`

```cerberus
hacer {
    imprimir(contador);
} mientras(contador < 10);
```

### 18.7 `segun`

```cerberus
segun(opcion) {
    caso 1:
        imprimir("Uno");
        salir;
    caso 2:
        imprimir("Dos");
        salir;
}
```

### 18.8 Expresión aritmética

```cerberus
resultado = (a + b) * c;
```

### 18.9 Expresión lógica

```cerberus
entero edad = 20;
booleano activo = verdadero;
si (edad >= 18 & activo == verdadero) {
    imprimir("Acceso permitido");
}
```

> **Reconciliación:** implementado. La forma documentada `activo == verdadero` compila con `activo` de tipo `booleano`.

---

## 19. Ejemplos inválidos y corrección

### 19.1 Identificador no declarado

Código:

```cerberus
imprimir(x);
```

Problema:

```text
NO SE HA DECLARADO EL IDENTIFICADOR: x
```

Corrección mínima:

```cerberus
entero x = 10;
imprimir(x);
```

### 19.2 Uso de palabra reservada como identificador

```cerberus
entero si = 10;
```

No es válido porque `si` es una palabra reservada.

### 19.3 Mayúsculas/minúsculas

```cerberus
Clase programa(){}
```

No debe tratarse como equivalente a:

```cerberus
clase programa(){}
```

### 19.4 Paréntesis incorrectos

Ejemplo conceptual:

```cerberus
si (edad >= 18 {
    imprimir("Mayor");
}
```

Debe detectarse la ausencia del `)` esperado.

---

## 20. Reglas para generación de código mediante IA

Una IA que genere Cerberus debe seguir estas reglas.

### 20.1 Reglas obligatorias

1. Utilizar únicamente palabras reservadas definidas por esta especificación.
2. Respetar mayúsculas y minúsculas.
3. No introducir sintaxis de Java, C, C++, Python u otro lenguaje como si fuera Cerberus.
4. Utilizar los tipos documentados.
5. Respetar la gramática formal cuando esté disponible.
6. No inventar parámetros, herencia, constructores u otras características que estén marcadas como `PENDING`.
7. Si una solicitud requiere una característica no definida, indicarlo explícitamente.
8. Preferir ejemplos simples y educativos.
9. Mantener la intención del usuario cuando se corrija código.
10. No afirmar que un código compila si la característica utilizada no está confirmada.
11. Escribir siempre la clase con paréntesis: `clase Nombre()`.
12. En `para`, incluir la palabra `entero`: `para(entero i = 0; i < 10; i++)`.
13. En `segun`, no usar `predeterminado` y terminar cada `caso` con `salir;`.
14. Las variables `booleano` se inicializan con `verdadero`/`falso` y se usan en condiciones; no se usan en aritmética.
15. No usar operadores compuestos (`+=`, `-=`, `*=`, `/=`), `--` ni `!`.
16. No declarar `entero` con literales o expresiones reales: el compilador lo rechaza (`TIPO INCOMPATIBLE`).

### 20.2 Comparaciones con Java

La similitud con Java puede utilizarse para explicar conceptos, pero Java no es una fuente válida para completar automáticamente la sintaxis de Cerberus.

Ejemplo incorrecto de razonamiento:

> "Como Java permite X, Cerberus también lo permite."

Ese razonamiento no es válido salvo que X esté definido en esta especificación o confirmado en el compilador.

### 20.3 Generación segura

Cuando una característica sea `PENDING`:

```text
La especificación actual de Cerberus no define formalmente esta característica.
```

La IA debe evitar generar código inventado como si fuera oficial.

---

## 21. Reglas para explicación de código

Cuando la IA explique código Cerberus debe:

1. identificar la estructura general;
2. identificar clases, funciones y bloques;
3. identificar declaraciones y tipos;
4. explicar operadores;
5. explicar condiciones y ciclos;
6. identificar posibles errores sintácticos;
7. identificar posibles errores semánticos documentados;
8. diferenciar entre comportamiento confirmado y comportamiento no definido;
9. utilizar terminología de Cerberus;
10. evitar atribuir a Cerberus reglas que solamente existen en Java u otros lenguajes.

La explicación debe ser adecuada al propósito educativo de Cerberus.

---

## 22. Reglas para corrección de código

Cuando una IA corrija código Cerberus:

1. conservar la intención original;
2. realizar el cambio mínimo necesario;
3. señalar qué estaba mal;
4. explicar por qué estaba mal;
5. proporcionar el código corregido;
6. utilizar únicamente reglas definidas;
7. no introducir características pendientes para resolver un problema;
8. distinguir errores sintácticos de errores semánticos.

Formato recomendado:

```text
Error:
<descripción>

Causa:
<regla de Cerberus violada>

Corrección:
<código>
```

---

## 23. Reglas para el modelo de IA de Cerberus

Esta sección está destinada a los sistemas de IA que utilicen esta especificación como fuente de conocimiento.

### 23.1 Prioridad de conocimiento

Orden recomendado:

```text
1. language_spec.md
2. implementación confirmada del compilador
3. ejemplos oficiales
4. dataset de entrenamiento
5. conocimiento general de programación
```

El conocimiento general nunca debe sobrescribir una regla específica de Cerberus.

### 23.2 Separación entre conocimiento y entrenamiento

`language_spec.md` es conocimiento estructurado.

Los archivos `.jsonl` contienen ejemplos de entrenamiento.

No se debe utilizar el dataset como sustituto de la especificación.

### 23.3 Manejo de incertidumbre

Si existe incertidumbre:

```text
No está definido en la especificación actual de Cerberus.
```

Si existe una contradicción:

```text
La documentación presenta información contradictoria sobre esta característica.
```

La IA debe solicitar o utilizar la implementación del compilador para resolverla cuando esté disponible.

---

## 24. Documentador automático

La documentación de Cerberus describe una funcionalidad llamada `Documentador`, capaz de convertir instrucciones del código en descripciones en lenguaje natural.

Ejemplo documentado:

```cerberus
entero numero1 = 10;
```

Descripción:

```text
Línea 05: Declaración de una variable de tipo entero llamada numero1 con valor inicial 10.
```

Esta funcionalidad puede utilizarse como referencia para el componente de explicación de CerberusAI.

---

## 25. Características pendientes

Las siguientes características requieren mayor definición antes de utilizarlas como reglas absolutas de generación:

| Característica | Estado | Motivo |
|---|---|---|
| Parámetros de funciones | `PENDING` | No existe gramática ni implementación. |
| Tipos de retorno completos | `PENDING` | No implementados en el compilador. |
| Constructores | `PENDING` | Se mencionan dentro de POO, sin gramática ni implementación. |
| Herencia | `PENDING` | Se menciona conceptualmente, sin sintaxis ni implementación. |
| Arrays | `PENDING` | No implementados (`entero [] a;` es rechazado). |
| `nulo` | `PARTIAL` | Tokenizado, sin semántica; `entero x = nulo;` falla (`Factor invalido`). |
| `vacio` | `PARTIAL` | Tokenizado, sin semántica. |
| Operador `!` | `PARTIAL` | Tokenizado pero no usado en el parser (`si(!x)` falla). |
| Operadores compuestos (`+=`, `-=`, `*=`, `/=`), `--` | `PARTIAL`/no implementado | No tokenizados/usados correctamente; rechazados. |
| Precedencia completa | `PARTIAL` | Se describe precedencia aritmética, pero no existe tabla completa. |
| Inicialización booleana | `IMPLEMENTED` | `booleano x = verdadero;` y variables booleanas; condiciones booleanas. |
| Validación de tipos | `IMPLEMENTED` | `entero x = 9.5;` rechazado (`TIPO INCOMPATIBLE`); `booleano` pendiente. |
| Scopes | `IMPLEMENTED` | Pila global/función/bloque; shadowing permitido, redeclaración solo en el mismo scope. |
| `retornar` | `PARTIAL` | Tokenizado, sin gramática. |
| `predeterminado` en `segun` | `PARTIAL`/no implementado | Documentado pero rechazado. |
| Gramática completa de `para` | `PARTIAL` | Implementación restringida a `para(entero i = N; i <rel> M; i++)`. |
| Gramática completa de `segun` | `PARTIAL` | Sin `predeterminado`; `salir` exige `;`. |

---

## 26. Conflictos y observaciones de la documentación

Esta sección es deliberadamente conservadora. Los conflictos no deben resolverse silenciosamente.

### 26.1 Declaración con o sin inicialización

La documentación presenta ejemplos como:

```cerberus
entero edad;
```

pero la producción formal disponible de `<DECLARACION>` es:

```text
<DECLARACION> ::= <TIPO> IDENTIFICADOR "=" <VALOR> ";"
```

**Resolución:** el parser acepta ambas formas (`entero edad;` y `entero edad = 20;`), también para `booleano`.

**Estado:** `IMPLEMENTED` (la inicialización es opcional).

### 26.2 Capitalización de palabras reservadas

La especificación textual establece sensibilidad a mayúsculas y minúsculas, pero algunos ejemplos de las tablas utilizan formas capitalizadas como `Si`, `Hacer`, `Imprimir` o `Retornar`.

La forma canónica debe considerarse la minúscula indicada en la lista de palabras reservadas:

```text
si
sino
hacer
imprimir
retornar
```

Los ejemplos capitalizados no deben interpretarse como sintaxis adicional.

### 26.3 Gramática incompleta

Algunas producciones de diagramas sintácticos aparecen parcialmente representadas en el texto extraído de la documentación. No deben completarse mediante suposiciones de Java, C o cualquier otro lenguaje.

### 26.4 AST

La documentación explica el concepto de AST y su utilidad, pero también describe la implementación de Cerberus como traducción dirigida por la sintaxis mediante una tabla de símbolos.

Esto no implica necesariamente que el compilador actual mantenga un AST completo persistente.

**Estado:** `DOCUMENTED` como concepto; implementación de AST completo: `PENDING` hasta verificar el código fuente.

### 26.5 Conflictos confirmados con el compilador real

Verificación dinámica con `audit.CompilerProbe` (ver `CerberusAI_COMPILER_AUDIT.md`):

| Documentación | Compilador real | Estado |
|---|---|---|
| `clase programa {` | exige `clase programa()` | `CONFLICT` |
| `booleano activo = verdadero;` | implementado | `IMPLEMENTED` |
| `entero x = 9.5;` es error de tipo | rechazado | `IMPLEMENTED` |
| `para(i = 0; ...)` | exige `para(entero i = 0; ...)` | `CONFLICT` |
| `segun` con `predeterminado` | no implementado | `CONFLICT` |
| `salir` sin `;` | exige `salir;` | `CONFLICT` |
| `+=`, `-=`, `*=`, `/=` | no implementados | `CONFLICT` |
| `!` (negación) | tokenizado, no usado | `CONFLICT`/`PARTIAL` |
| `si (... & activo == verdadero)` | implementado (booleano) | `IMPLEMENTED` |

---

## 27. Guía de versionado de la especificación

Cada modificación importante debe registrar:

```text
Versión:
Fecha:
Cambio:
Motivo:
Fuente:
Estado:
```

Ejemplo:

```text
Versión: 0.2
Fecha: YYYY-MM-DD
Cambio: Se definió la sintaxis de parámetros de funciones.
Motivo: Actualización del compilador.
Fuente: Código del parser.
Estado: IMPLEMENTED
```

---

## 28. Regla final de consistencia

Antes de considerar una característica de Cerberus como oficial, debe poder responderse afirmativamente a las siguientes preguntas:

- ¿Está definida en la especificación?
- ¿La sintaxis está clara?
- ¿Su semántica está definida?
- ¿Existen ejemplos válidos?
- ¿Existen ejemplos inválidos cuando son necesarios?
- ¿El compilador actual la acepta de acuerdo con la especificación?
- ¿El sistema de IA puede reconocerla sin depender de reglas de otro lenguaje?

Si alguna respuesta es negativa, la característica debe permanecer marcada como `PARTIAL`, `PENDING`, `DOCUMENTED` o `CONFLICT`, según corresponda.

---

## 29. Fuente documental

Este documento fue construido a partir de:

**Documentación CerberusIDE.pdf**

La especificación no pretende sustituir la implementación del compilador. Su función es organizar y normalizar la información disponible para que el compilador, el IDE y el sistema CerberusAI puedan compartir una fuente de conocimiento coherente.

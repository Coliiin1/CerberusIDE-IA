#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generar_datasets.py -- Genera y valida los datasets v3 de CerberusAI (Fase 7).

Construye, por plantillas y de forma reproducible, tres datasets:
  1. codigo valido           -> cerberus_dataset_v3_valido.jsonl              (generation)
  2. errores semanticos       -> cerberus_dataset_v3_errores_semanticos.jsonl  (correction_semantics)
  3. errores sintacticos      -> cerberus_dataset_v3_errores_sintacticos.jsonl (correction_syntax)

y un cuarto archivo de "fallos de validacion" (programas pensados como validos que
NO pasaron los analizadores -> se agregan al dataset de errores, como pide el flujo):

  4. cerberus_dataset_v3_errores_validacion.jsonl

Todo se valida contra el oraculo real del compilador (audit.CompiladorCLI), que ejecuta
lexer + parser + analisis semantico. Solo se publican ejemplos comprobados.

Uso (desde la raiz del proyecto, tras `mvnw.cmd compile`):
    py CerberusAI\\dataset\\generar_datasets.py
    py CerberusAI\\dataset\\generar_datasets.py --out-dir CerberusAI\\dataset

Solo usa la biblioteca estandar de Python.
"""
import argparse
import json
import os
import subprocess
import sys
import tempfile

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.normpath(os.path.join(AQUI, "..", ".."))

COMPILER = "audit.CompiladorCLI"
SOURCE = "compilador_v1_validado_v3"

JAVA = os.environ.get("JAVA", "java")
CLASSPATH = os.path.join(RAIZ, "target", "classes")

# ---------------------------------------------------------------------------
# Oraculo (con cache por texto de codigo)
# ---------------------------------------------------------------------------
_cache = {}


def compilar(codigo):
    if codigo in _cache:
        return _cache[codigo]
    fd, ruta = tempfile.mkstemp(suffix=".cer")
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        f.write(codigo)
    try:
        proc = subprocess.run(
            [JAVA, "-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8",
             "-cp", CLASSPATH, COMPILER, ruta],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60,
        )
        lineas = (proc.stdout or "").strip().splitlines()
        linea = lineas[-1] if lineas else ""
        try:
            res = json.loads(linea)
        except json.JSONDecodeError:
            res = {"aceptado": False, "error": "no-JSON: " + (linea or (proc.stderr or ""))[:200]}
    except Exception as e:
        res = {"aceptado": False, "error": "oraculo: %s" % e}
    finally:
        try:
            os.remove(ruta)
        except OSError:
            pass
    _cache[codigo] = res
    return res


def acepta(codigo):
    return bool(compilar(codigo).get("aceptado"))


# ---------------------------------------------------------------------------
# Constructores de programas
# ---------------------------------------------------------------------------
def _indent(texto, n=4):
    pad = " " * n
    return "\n".join((pad + l if l else l) for l in texto.split("\n"))


def prog(cuerpo, globales="", funciones="", clase="Programa"):
    out = []
    if globales:
        out.append(globales.strip())
    out.append("clase %s(){" % clase)
    out.append("    funcion principal(){")
    out.append(_indent(cuerpo.strip(), 8))
    out.append("    }")
    if funciones:
        out.append(_indent(funciones.strip(), 4))
    out.append("}")
    return "\n".join(out)


def fn(nombre, tipo, cuerpo, params=""):
    sig = "funcion " + (tipo + " " if tipo else "") + nombre + "(" + params + "){"
    return sig + "\n" + _indent(cuerpo.strip(), 4) + "\n}"


# ---------------------------------------------------------------------------
# Generacion VALIDA (category: generation)
# ---------------------------------------------------------------------------
def gen_validos():
    out = []

    # 1) Declaraciones con/sin inicializacion, varios nombres
    tipos = [
        ("entero", "20", "un numero entero"),
        ("real", "9.5", "un numero real"),
        ("cadena", '"Carlos"', "una cadena de texto"),
        ("caracter", "'C'", "un caracter"),
        ("booleano", "verdadero", "un valor booleano"),
    ]
    for tipo, lit, desc in tipos:
        for nombre in ["dato", "valor", "total", "numero"]:
            out.append(("Escribe un programa Cerberus que declare una variable de tipo %s llamada %s con valor %s y la imprima."
                        % (tipo, nombre, lit), prog("%s %s = %s;\nimprimir(%s);" % (tipo, nombre, lit, nombre))))
            out.append(("Escribe un programa Cerberus que declare una variable de tipo %s llamada %s sin valor inicial, le asigne %s y la imprima."
                        % (tipo, nombre, lit), prog("%s %s;\n%s = %s;\nimprimir(%s);" % (tipo, nombre, nombre, lit, nombre))))

    # 2) Aritmetica entera
    pares = [("2", "3"), ("10", "4"), ("6", "7"), ("8", "2"), ("10", "3"), ("15", "5"),
             ("7", "2"), ("9", "4"), ("12", "5"), ("20", "6"), ("11", "3"), ("14", "7"),
             ("100", "25"), ("30", "4"), ("17", "9"), ("24", "8"),
             ("5", "5"), ("18", "3"), ("21", "7"), ("16", "4"), ("13", "6"), ("19", "9"),
             ("22", "11"), ("33", "3"), ("44", "4"), ("50", "10"), ("27", "9"), ("36", "6"), ("48", "8")]
    for op, desc in [("+", "sume"), ("-", "reste"), ("*", "multiplique"), ("/", "divida"), ("%", "calcule el residuo (modulo)")]:
        for va, vb in pares:
            cuerpo = "entero a = %s;\nentero b = %s;\nentero resultado = a %s b;\nimprimir(resultado);" % (va, vb, op)
            out.append(("Escribe un programa Cerberus que %s dos numeros enteros (%s y %s) y lo imprima." % (desc, va, vb), prog(cuerpo)))

    # 3) Expresiones con parentesis
    for expr, va, vb, vc in [
        ("(a + b) * c", "2", "3", "4"),
        ("a + b * c", "2", "3", "4"),
        ("(a - b) / c", "10", "4", "2"),
        ("a * (b + c)", "3", "4", "5"),
        ("(a + b) % c", "9", "3", "4"),
        ("(a * b) + (a * c)", "2", "3", "4"),
        ("(a + b) - (c * 2)", "5", "3", "4"),
        ("a + b + c", "1", "2", "3"),
    ]:
        cuerpo = "entero a = %s;\nentero b = %s;\nentero c = %s;\nentero r = %s;\nimprimir(r);" % (va, vb, vc, expr)
        out.append(("Escribe un programa Cerberus que calcule la expresion %s con a=%s, b=%s y c=%s y la imprima." % (expr, va, vb, vc), prog(cuerpo)))

    # 4) Aritmetica real
    for op in ["+", "-", "*", "/"]:
        for va, vb in [("1.5", "2.5"), ("3.0", "2.0"), ("9.0", "2.0"), ("4.5", "1.5"),
                       ("10.0", "4.0"), ("2.25", "3.75"), ("5.5", "2.5"), ("7.0", "3.5"),
                       ("12.5", "2.5"), ("8.0", "4.0")]:
            cuerpo = "real a = %s;\nreal b = %s;\nreal r = a %s b;\nimprimir(r);" % (va, vb, op)
            out.append(("Escribe un programa Cerberus que calcule a %s b con reales %s y %s y lo imprima." % (op, va, vb), prog(cuerpo)))

    # 5) Asignacion compuesta
    for signo, tipo, ini in [("+=", "entero", "5"), ("-=", "entero", "10"), ("*=", "entero", "4"), ("/=", "real", "9.0")]:
        for valor in ["2", "3", "5"]:
            cuerpo = "%s x = %s;\nx %s %s;\nimprimir(x);" % (tipo, ini, signo, valor)
            out.append(("Escribe un programa Cerberus que declare una variable %s y le aplique la asignacion compuesta %s con %s y la imprima."
                        % (tipo, signo, valor), prog(cuerpo)))

    # 6) Ensanche entero -> real
    for a in ["3", "7", "15", "42"]:
        out.append(("Escribe un programa Cerberus que declare un entero a=%s y asigne su valor a un real r (ensanche) y lo imprima." % a,
                    prog("entero a = %s;\nreal r = a;\nimprimir(r);" % a)))

    # 7) Condicionales
    out.append(('Escribe un programa Cerberus que imprima Mayor de edad si la edad es mayor o igual a 18 y Menor en caso contrario.',
                prog('entero edad = 20;\nsi (edad >= 18) {\n    imprimir("Mayor de edad");\n} sino {\n    imprimir("Menor de edad");\n}')))
    out.append(('Escribe un programa Cerberus con si, sino si y sino que clasifique una calificacion en Sobresaliente, Aprobado o Reprobado.',
                prog('entero calificacion = 85;\nsi (calificacion >= 90) {\n    imprimir("Sobresaliente");\n} sino si (calificacion >= 70) {\n    imprimir("Aprobado");\n} sino {\n    imprimir("Reprobado");\n}')))
    out.append(('Escribe un programa Cerberus con un si anidado que clasifique un entero en Positivo o Cero.',
                prog('entero n = 3;\nsi (n > 0) {\n    imprimir("Positivo");\n} sino {\n    imprimir("Cero");\n}')))
    for limite in ["10", "20", "30", "50"]:
        out.append(("Escribe un programa Cerberus que imprima Suficiente si una nota es mayor o igual a %s." % limite,
                    prog('entero nota = 15;\nsi (nota >= %s) {\n    imprimir("Suficiente");\n}' % limite)))

    # 8) Booleanos
    out.append(('Escribe un programa Cerberus que imprima Activo cuando la variable booleana activo sea verdadera (condicion desnuda).',
                prog('booleano activo = verdadero;\nsi (activo) {\n    imprimir("Activo");\n}')))
    out.append(('Escribe un programa Cerberus que compare una variable booleana contra el literal verdadero con ==.',
                prog('booleano activo = falso;\nsi (activo == verdadero) {\n    imprimir("Activo");\n} sino {\n    imprimir("Inactivo");\n}')))
    out.append(('Escribe un programa Cerberus que compare una variable booleana contra falso con !=.',
                prog('booleano activo = falso;\nsi (activo != falso) {\n    imprimir("Activo");\n} sino {\n    imprimir("Inactivo");\n}')))
    out.append(('Escribe un programa Cerberus que use la negacion logica ! sobre una variable booleana.',
                prog('booleano activo = falso;\nsi (!activo) {\n    imprimir("Inactivo");\n}')))
    out.append(('Escribe un programa Cerberus que use doble negacion !! sobre una variable booleana.',
                prog('booleano activo = verdadero;\nsi (!!activo) {\n    imprimir("Activo");\n}')))
    out.append(('Escribe un programa Cerberus que use negacion agrupada con parentesis, por ejemplo !(a > 10).',
                prog('entero a = 5;\nsi (!(a > 10)) {\n    imprimir("No es mayor");\n}')))
    out.append(('Escribe un programa Cerberus que combine dos condiciones con el operador logico &.',
                prog('entero edad = 20;\nbooleano activo = verdadero;\nsi (edad >= 18 & activo == verdadero) {\n    imprimir("Acceso permitido");\n}')))
    out.append(('Escribe un programa Cerberus que combine dos condiciones con el operador logico |.',
                prog('entero a = 1;\nsi (a > 5 | a < 3) {\n    imprimir("Cumple");\n}')))
    out.append(('Escribe un programa Cerberus que asigne la negacion de una variable booleana a otra variable booleana.',
                prog('booleano a = falso;\nbooleano b = !a;\nimprimir(b);')))
    out.append(('Escribe un programa Cerberus que use la negacion de una comparacion entre parentesis.',
                prog('entero x = 5;\nsi (!(x == 10)) {\n    imprimir("No es diez");\n}')))
    out.append(('Escribe un programa Cerberus que imprima Activo o Inactivo segun un booleano con si y sino.',
                prog('booleano activo = verdadero;\nsi (activo) {\n    imprimir("Activo");\n} sino {\n    imprimir("Inactivo");\n}')))

    # 9) Ciclos
    out.append(("Escribe un programa Cerberus con un ciclo mientras que imprima x y lo incremente mientras x sea menor que 3.",
                prog("entero x = 0;\nmientras (x < 3) {\n    imprimir(x);\n    x = x + 1;\n}")))
    for tope in ["3", "5", "8", "10"]:
        out.append(("Escribe un programa Cerberus con un ciclo mientras que cuente de 0 hasta antes de %s." % tope,
                    prog("entero x = 0;\nmientras (x < %s) {\n    imprimir(x);\n    x = x + 1;\n}" % tope)))
    out.append(("Escribe un programa Cerberus con un ciclo hacer-mientras que imprima contador mientras sea menor que 3.",
                prog("entero contador = 0;\nhacer {\n    imprimir(contador);\n    contador = contador + 1;\n} mientras (contador < 3);")))
    for tope in ["3", "4", "6", "9"]:
        out.append(("Escribe un programa Cerberus con un ciclo hacer-mientras que cuente de 0 hasta antes de %s." % tope,
                    prog("entero n = 0;\nhacer {\n    imprimir(n);\n    n = n + 1;\n} mientras (n < %s);" % tope)))
    for tope in ["5", "8", "10", "12"]:
        out.append(("Escribe un programa Cerberus con un ciclo para con contador entero i de 0 a menos de %s con incremento ++." % tope,
                    prog("para (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope)))
    out.append(("Escribe un programa Cerberus con un ciclo para con decremento -- que cuente de 5 a 1.",
                prog("para (entero i = 5; i > 0; i--) {\n    imprimir(i);\n}")))
    out.append(("Escribe un programa Cerberus con un ciclo para con decremento -- que cuente de 10 a 1.",
                prog("para (entero i = 10; i > 0; i--) {\n    imprimir(i);\n}")))
    out.append(("Escribe un programa Cerberus con un ciclo para con contador real i de 0.0 a menos de 1.0.",
                prog("para (real i = 0.0; i < 1.0; i++) {\n    imprimir(i);\n}")))
    out.append(("Escribe un programa Cerberus con un ciclo para cuya inicializacion y condicion usen expresiones aritmeticas (1 + 1 y n * 2).",
                prog("entero n = 5;\npara (entero i = 1 + 1; i < n * 2; i++) {\n    imprimir(i);\n}")))

    # 10) segun
    out.append(('Escribe un programa Cerberus con una seleccion segun sobre un entero opcion con casos 1 y 2.',
                prog('entero opcion = 2;\nsegun (opcion) {\n    caso 1:\n        imprimir("Uno");\n        salir;\n    caso 2:\n        imprimir("Dos");\n        salir;\n}')))
    out.append(('Escribe un programa Cerberus con una seleccion segun sobre un entero con tres casos.',
                prog('entero opcion = 3;\nsegun (opcion) {\n    caso 1:\n        imprimir("Uno");\n        salir;\n    caso 2:\n        imprimir("Dos");\n        salir;\n    caso 3:\n        imprimir("Tres");\n        salir;\n}')))
    out.append(('Escribe un programa Cerberus con una seleccion segun sobre una cadena con un caso y un predeterminado.',
                prog('cadena opcion = "x";\nsegun (opcion) {\n    caso "a":\n        imprimir("A");\n        salir;\n    predeterminado:\n        imprimir("Otro");\n        salir;\n}')))
    out.append(('Escribe un programa Cerberus con una seleccion segun sobre un entero con predeterminado.',
                prog('entero opcion = 5;\nsegun (opcion) {\n    caso 1:\n        imprimir("Uno");\n        salir;\n    predeterminado:\n        imprimir("Otro");\n        salir;\n}')))
    out.append(('Escribe un programa Cerberus con una seleccion segun sobre una cadena con dos casos.',
                prog('cadena opcion = "b";\nsegun (opcion) {\n    caso "a":\n        imprimir("A");\n        salir;\n    caso "b":\n        imprimir("B");\n        salir;\n}')))

    # 11) imprimir
    out.append(('Escribe un programa Cerberus que imprima varios argumentos separados por coma.',
                prog('entero a = 1;\nentero b = 2;\nimprimir(a, b, "z");')))
    out.append(('Escribe un programa Cerberus que concatene texto y variable con el operador + dentro de imprimir.',
                prog('cadena nombre = "Ana";\nimprimir("Hola " + nombre);')))
    out.append(("Escribe un programa Cerberus que imprima los literales booleanos verdadero y falso.",
                prog("imprimir(verdadero);\nimprimir(falso);")))
    out.append(('Escribe un programa Cerberus que imprima un entero, un real y una cadena en una sola llamada.',
                prog('entero a = 1;\nreal b = 2.5;\nimprimir(a, b, "texto");')))
    out.append(('Escribe un programa Cerberus que imprima una cadena y un entero separados por coma.',
                prog('cadena t = "total";\nentero n = 3;\nimprimir(t, n);')))
    out.append(("Escribe un programa Cerberus que imprima un caracter y un numero.",
                prog("caracter c = 'A';\nimprimir(c, 10);")))

    # 12) Funciones
    out.append(("Escribe un programa Cerberus con una funcion saludar sin retorno y llamala desde principal.",
                prog("saludar();", funciones=fn("saludar", "", 'imprimir("Hola");'))))
    out.append(("Escribe un programa Cerberus con una funcion sin retorno que imprima y termine con la instruccion retornar;.",
                prog("saludar();", funciones=fn("saludar", "", 'imprimir("Hola");\nretornar;'))))
    for nombre, tipo, params, expr, args, desc in [
        ("sumar", "entero", "entero a, entero b", "a + b", "3, 4", "sume dos enteros"),
        ("restar", "entero", "entero a, entero b", "a - b", "10, 4", "reste dos enteros"),
        ("multiplicar", "entero", "entero a, entero b", "a * b", "6, 7", "multiplique dos enteros"),
        ("doble", "entero", "entero n", "n + n", "5", "devuelva el doble de un entero"),
        ("promedio", "real", "real a, real b", "(a + b) / 2.0", "1.0, 2.0", "promedie dos reales"),
        ("duplicar", "real", "real r", "r * 2.0", "3.5", "duplique un real"),
        ("triple", "entero", "entero n", "n * 3", "4", "devuelva el triple de un entero"),
        ("sumarTres", "entero", "entero a, entero b", "a + b + 1", "2, 3", "sume dos enteros mas uno"),
    ]:
        out.append(("Escribe un programa Cerberus con una funcion %s que %s y usala para asignar una variable." % (tipo, desc),
                    prog("imprimir(%s(%s));" % (nombre, args), funciones=fn(nombre, tipo, "retornar %s;" % expr, params))))

    # funciones con variantes de nombre (para mas cobertura)
    for prefijo, tipo, params, expr, args in [
        ("calc", "entero", "entero a, entero b", "a + b", "5, 6"),
        ("operar", "entero", "entero a, entero b", "a * b", "3, 4"),
        ("promediar", "real", "real x, real y", "(x + y) / 2.0", "2.0, 4.0"),
    ]:
        for sufijo in ["A", "B", "C"]:
            nombre = prefijo + sufijo
            out.append(("Escribe un programa Cerberus con una funcion %s que reciba parametros y retorne un resultado y usala en una asignacion." % tipo,
                        prog("%s r = %s(%s);\nimprimir(r);" % (tipo, nombre, args),
                             funciones=fn(nombre, tipo, "retornar %s;" % expr, params))))
    out.append(('Escribe un programa Cerberus con una funcion cadena saludo(nombre) que retorne el parametro.',
                prog('cadena s = saludo("Ana");\nimprimir(s);', funciones=fn("saludo", "cadena", "retornar nombre;", "cadena nombre"))))
    out.append(('Escribe un programa Cerberus con una funcion cadena que retorne una cadena literal.',
                prog('cadena s = etiqueta();\nimprimir(s);', funciones=fn("etiqueta", "cadena", 'retornar "hola";'))))
    out.append(("Escribe un programa Cerberus con una funcion caracter inicial() que retorne el literal 'X'.",
                prog("caracter c = inicial();\nimprimir(c);", funciones=fn("inicial", "caracter", "retornar 'X';"))))
    out.append(("Escribe un programa Cerberus con una funcion booleana activo() que retorne verdadero y usala como condicion de un si.",
                prog('si (activo()) {\n    imprimir("Activo");\n}', funciones=fn("activo", "booleano", "retornar verdadero;"))))
    out.append(("Escribe un programa Cerberus con una funcion booleana que retorne falso y usala como condicion.",
                prog('si (!inactivo()) {\n    imprimir("No inactivo");\n}', funciones=fn("inactivo", "booleano", "retornar falso;"))))
    out.append(("Escribe un programa Cerberus con una funcion entera doble que llame a otra funcion suma para retornar su resultado.",
                prog("imprimir(doble(5));", funciones=fn("doble", "entero", "retornar suma(n, n);", "entero n") + "\n" + fn("suma", "entero", "retornar a + b;", "entero a, entero b"))))
    out.append(("Escribe un programa Cerberus con una funcion factorial recursiva para un entero n.",
                prog("imprimir(factorial(3));", funciones=fn("factorial", "entero", "si (n <= 1) {\n    retornar 1;\n}\nretornar n * factorial(n - 1);", "entero n"))))
    out.append(("Escribe un programa Cerberus con una funcion void que recibe un entero y lo imprime.",
                prog("mostrar(7);", funciones=fn("mostrar", "", "imprimir(n);", "entero n"))))
    out.append(("Escribe un programa Cerberus con una funcion void que recibe una cadena y la imprime.",
                prog('mostrar("hola");', funciones=fn("mostrar", "", "imprimir(t);", "cadena t"))))
    out.append(("Escribe un programa Cerberus con una funcion entera que recibe dos enteros y se usa dentro de imprimir.",
                prog("imprimir(sumar(1, 2));", funciones=fn("sumar", "entero", "retornar a + b;", "entero a, entero b"))))

    # 13) Globales y shadowing
    out.append(("Escribe un programa Cerberus con una variable global entera antes de la clase y usala en principal.",
                prog("imprimir(contador);", globales="entero contador = 10;")))
    out.append(("Escribe un programa Cerberus donde una variable local en principal oculte (shadowing) una variable global del mismo nombre.",
                prog("entero x = 2;\nimprimir(x);", globales="entero x = 1;")))
    out.append(("Escribe un programa Cerberus con una variable global real usada en un calculo.",
                prog("real r = base * 2.0;\nimprimir(r);", globales="real base = 3.5;")))
    out.append(("Escribe un programa Cerberus con una variable global cadena impresa desde principal.",
                prog('imprimir(mensaje);', globales='cadena mensaje = "hola";')))
    out.append(("Escribe un programa Cerberus con una variable global booleana usada como condicion.",
                prog('si (activo) {\n    imprimir("Activo");\n}', globales="booleano activo = verdadero;")))

    # 14) Composiciones
    out.append(("Escribe un programa Cerberus que sume los numeros del 1 al 5 usando un ciclo mientras y un acumulador.",
                prog("entero i = 1;\nentero suma = 0;\nmientras (i <= 5) {\n    suma = suma + i;\n    i = i + 1;\n}\nimprimir(suma);")))
    out.append(("Escribe un programa Cerberus que use un si dentro de un ciclo para imprimir solo los numeros pares de 1 a 5.",
                prog('para (entero i = 1; i <= 5; i++) {\n    si (i % 2 == 0) {\n        imprimir(i);\n    }\n}')))
    out.append(("Escribe un programa Cerberus con una funcion entera maximo(a, b) que retorne el mayor usando un condicional.",
                prog("imprimir(maximo(7, 3));", funciones=fn("maximo", "entero", "si (a > b) {\n    retornar a;\n}\nretornar b;", "entero a, entero b"))))
    out.append(("Escribe un programa Cerberus con variables globales y una funcion que las use.",
                prog("imprimir(doble());", globales="entero base = 4;", funciones=fn("doble", "entero", "retornar base * 2;"))))
    out.append(("Escribe un programa Cerberus que cuente cuantos numeros de 1 a 5 son mayores que 2.",
                prog('entero c = 0;\npara (entero i = 1; i <= 5; i++) {\n    si (i > 2) {\n        c = c + 1;\n    }\n}\nimprimir(c);')))
    out.append(("Escribe un programa Cerberus con un ciclo mientras que sume los pares menores que 10.",
                prog('entero i = 0;\nentero s = 0;\nmientras (i < 10) {\n    si (i % 2 == 0) {\n        s = s + i;\n    }\n    i = i + 1;\n}\nimprimir(s);')))
    out.append(("Escribe un programa Cerberus con una funcion entera que retorne el mayor de dos numeros.",
                prog("entero m = mayor(5, 9);\nimprimir(m);", funciones=fn("mayor", "entero", "si (a > b) {\n    retornar a;\n}\nretornar b;", "entero a, entero b"))))

    # 15) Mas declaraciones con distintos nombres (combinatorio)
    for tipo, lit in [("entero", "25"), ("real", "3.14"), ('cadena', '"test"'), ("caracter", "'Z'"), ("booleano", "falso")]:
        for nombre in ["n1", "n2", "r", "s", "c"]:
            out.append(("Escribe un programa Cerberus que declare una variable %s llamada %s con valor %s y la imprima."
                        % (tipo, nombre, lit), prog("%s %s = %s;\nimprimir(%s);" % (tipo, nombre, lit, nombre))))

    # 16) Mas composiciones de control
    out.append(("Escribe un programa Cerberus con un ciclo para anidado dentro de otro ciclo para.",
                prog("para (entero i = 0; i < 2; i++) {\n    para (entero j = 0; j < 2; j++) {\n        imprimir(i, j);\n    }\n}")))
    out.append(("Escribe un programa Cerberus con un si dentro de un ciclo mientras.",
                prog('entero i = 0;\nmientras (i < 5) {\n    si (i == 3) {\n        imprimir("tres");\n    }\n    i = i + 1;\n}')))
    out.append(("Escribe un programa Cerberus con una funcion que use un ciclo para acumular una suma.",
                prog("imprimir(sumaHasta(4));", funciones=fn("sumaHasta", "entero", "entero s = 0;\npara (entero i = 1; i <= n; i++) {\n    s = s + i;\n}\nretornar s;", "entero n"))))
    out.append(("Escribe un programa Cerberus que imprima los numeros del 1 al 3 con un ciclo hacer-mientras.",
                prog("entero i = 1;\nhacer {\n    imprimir(i);\n    i = i + 1;\n} mientras (i <= 3);")))
    out.append(("Escribe un programa Cerberus con una funcion booleana que indique si un numero es par.",
                prog('si (esPar(4)) {\n    imprimir("par");\n}', funciones=fn("esPar", "booleano", "entero resto = n % 2;\nsi (resto == 0) {\n    retornar verdadero;\n}\nretornar falso;", "entero n"))))

    # 17) aritmetica entera con otros identificadores
    for op, desc in [("+", "sume"), ("-", "reste"), ("*", "multiplique"), ("/", "divida"), ("%", "calcule el residuo")]:
        for vm, vk in [("3", "2"), ("12", "4"), ("9", "3"), ("20", "5"), ("7", "3"), ("16", "4"), ("25", "5"), ("11", "2")]:
            cuerpo = "entero m = %s;\nentero k = %s;\nentero r = m %s k;\nimprimir(r);" % (vm, vk, op)
            out.append(("Escribe un programa Cerberus que %s dos enteros (%s y %s) usando variables m y k y lo imprima." % (desc, vm, vk), prog(cuerpo)))

    # 18) mas composiciones y funciones
    out.append(("Escribe un programa Cerberus con una funcion void que llame a otra funcion.",
                prog("saludar();", funciones=fn("saludar", "", "despedir();") + "\n" + fn("despedir", "", 'imprimir("adios");'))))
    out.append(("Escribe un programa Cerberus que use una funcion entera dentro de una expresion aritmetica.",
                prog("entero y = suma(2, 3) + 1;\nimprimir(y);", funciones=fn("suma", "entero", "retornar a + b;", "entero a, entero b"))))
    out.append(("Escribe un programa Cerberus que imprima el resultado de una funcion dentro de imprimir.",
                prog("imprimir(doble(4));", funciones=fn("doble", "entero", "retornar n + n;", "entero n"))))
    out.append(("Escribe un programa Cerberus con un ciclo mientras que cuente de 1 a 4.",
                prog("entero i = 1;\nmientras (i <= 4) {\n    imprimir(i);\n    i = i + 1;\n}")))
    out.append(("Escribe un programa Cerberus con un ciclo para que recorra de 3 a 1 con decremento.",
                prog("para (entero i = 3; i > 0; i--) {\n    imprimir(i);\n}")))
    out.append(("Escribe un programa Cerberus con una seleccion segun sobre un entero con cuatro casos.",
                prog('entero opcion = 4;\nsegun (opcion) {\n    caso 1:\n        imprimir("Uno");\n        salir;\n    caso 2:\n        imprimir("Dos");\n        salir;\n    caso 3:\n        imprimir("Tres");\n        salir;\n    caso 4:\n        imprimir("Cuatro");\n        salir;\n}')))
    out.append(("Escribe un programa Cerberus con variables globales enteras usadas en aritmetica.",
                prog("entero r = g1 + g2;\nimprimir(r);", globales="entero g1 = 3;\nentero g2 = 4;")))
    out.append(("Escribe un programa Cerberus que imprima una cadena concatenada con un numero dentro de imprimir.",
                prog('entero n = 7;\nimprimir("El numero es " + n);')))
    out.append(("Escribe un programa Cerberus con una funcion real que retorne un entero (ensanche).",
                prog("real r = f();\nimprimir(r);", funciones=fn("f", "real", "retornar 3;"))))
    out.append(("Escribe un programa Cerberus que declare un real a partir de una expresion entera.",
                prog("entero a = 2;\nentero b = 3;\nreal r = a + b;\nimprimir(r);")))

    # 19) mas funciones con variantes de argumentos
    for args in [("suma", "1, 2"), ("suma", "3, 4"), ("resta", "10, 3"), ("multiplica", "2, 5")]:
        nombre, lit = args
        out.append(("Escribe un programa Cerberus que llame a una funcion entera %s con argumentos %s y use su resultado." % (nombre, lit),
                    prog("imprimir(%s(%s));" % (nombre, lit),
                         funciones=fn(nombre, "entero", "retornar a;", "entero a, entero b"))))

    # 20) mas booleanos
    out.append(("Escribe un programa Cerberus que combine tres condiciones con &.",
                prog('entero a = 1;\nbooleano b = verdadero;\nsi (a > 0 & b == verdadero & a < 10) {\n    imprimir("ok");\n}')))
    out.append(("Escribe un programa Cerberus que use | entre dos comparaciones.",
                prog('entero a = 5;\nsi (a == 5 | a == 6) {\n    imprimir("ok");\n}')))
    out.append(("Escribe un programa Cerberus que niegue una comparacion de igualdad.",
                prog('entero x = 3;\nsi (!(x == 4)) {\n    imprimir("no es cuatro");\n}')))

    # 21) mas ciclos y seleccion
    out.append(("Escribe un programa Cerberus con un ciclo para con contador real que cuente de 1.0 a 3.0.",
                prog("para (real i = 1.0; i < 3.0; i++) {\n    imprimir(i);\n}")))
    out.append(("Escribe un programa Cerberus con una seleccion segun sobre una cadena con tres casos.",
                prog('cadena x = "b";\nsegun (x) {\n    caso "a":\n        imprimir("A");\n        salir;\n    caso "b":\n        imprimir("B");\n        salir;\n    caso "c":\n        imprimir("C");\n        salir;\n}')))

    # 22) aritmetica con identificadores p y q
    for op, desc in [("+", "sume"), ("-", "reste"), ("*", "multiplique"), ("/", "divida"), ("%", "calcule el residuo")]:
        for vp, vq in [("4", "2"), ("15", "3"), ("21", "7"), ("18", "6"), ("24", "8"), ("30", "10")]:
            cuerpo = "entero p = %s;\nentero q = %s;\nentero r = p %s q;\nimprimir(r);" % (vp, vq, op)
            out.append(("Escribe un programa Cerberus que %s dos enteros (%s y %s) usando p y q." % (desc, vp, vq), prog(cuerpo)))

    # 23) funciones void con distintos parametros
    for params, arg, desc in [
        ("entero n", "3", "entero"),
        ('cadena s', '"hola"', "cadena"),
        ("real r", "2.5", "real"),
        ("caracter c", "'a'", "caracter"),
        ("booleano b", "verdadero", "booleano"),
    ]:
        out.append(("Escribe un programa Cerberus con una funcion void que reciba un parametro %s y lo imprima." % desc,
                    prog("muestra(%s);" % arg, funciones=fn("muestra", "", "imprimir(%s);" % params.split(" ")[1], params))))

    # 24) mas funciones con retorno tipado y variantes de nombre
    for sufijo in ["X", "Y", "Z"]:
        nombre = "calc" + sufijo
        out.append(("Escribe un programa Cerberus con una funcion entera %s que retorne la suma de dos parametros." % nombre,
                    prog("entero r = %s(2, 3);\nimprimir(r);" % nombre, funciones=fn(nombre, "entero", "retornar a + b;", "entero a, entero b"))))
        out.append(("Escribe un programa Cerberus con una funcion real %s que retorne el producto de dos parametros." % nombre,
                    prog("real r = %s(2.0, 3.0);\nimprimir(r);" % nombre, funciones=fn(nombre, "real", "retornar a * b;", "real a, real b"))))

    # 25) mas imprimir con concatenacion
    for nombre in ["nombre", "ciudad", "pais"]:
        out.append(("Escribe un programa Cerberus que concatene una cadena con la variable %s en imprimir." % nombre,
                    prog('cadena %s = "x";\nimprimir("Valor: " + %s);' % (nombre, nombre))))

    # 26) mas globales usadas por funciones
    for nombre, op in [("base", "* 2"), ("inicial", "+ 1")]:
        out.append(("Escribe un programa Cerberus con una variable global %s usada por una funcion." % nombre,
                    prog("imprimir(f());", globales="entero %s = 5;" % nombre,
                         funciones=fn("f", "entero", "retornar %s %s;" % (nombre, op)))))

    # 27) declarar, reasignar e imprimir (combinatorio)
    for nombre in ["x", "a", "b", "n", "total", "dato"]:
        out.append(("Escribe un programa Cerberus que declare un entero %s, le reasigne otro valor y lo imprima." % nombre,
                    prog("entero %s = 1;\n%s = %s + 10;\nimprimir(%s);" % (nombre, nombre, nombre, nombre))))

    # 28) mas composiciones con ciclos y condicionales
    out.append(("Escribe un programa Cerberus que use un ciclo mientras dentro de una funcion para acumular valores.",
                prog("imprimir(suma(4));", funciones=fn("suma", "entero", "entero i = 1;\nentero s = 0;\nmientras (i <= n) {\n    s = s + i;\n    i = i + 1;\n}\nretornar s;", "entero n"))))
    out.append(("Escribe un programa Cerberus que imprima solo los impares de 1 a 6 con un para y un si.",
                prog('para (entero i = 1; i <= 6; i++) {\n    si (i % 2 != 0) {\n        imprimir(i);\n    }\n}')))
    out.append(("Escribe un programa Cerberus con una funcion que combine dos condiciones booleanas.",
                prog('si (valida(2)) {\n    imprimir("valido");\n}',
                     funciones=fn("valida", "booleano", "si (n > 0) {\n    retornar verdadero;\n}\nretornar falso;", "entero n"))))

    return out


# ---------------------------------------------------------------------------
# Errores SEMANTICOS (roto -> corregido). category: correction_semantics
# ---------------------------------------------------------------------------
def gen_semanticos():
    pares = []
    nombres = ["x", "dato", "valor", "n", "a", "b", "num", "total"]

    # tipo incompatible en inicializacion
    for nombre in nombres:
        for real_lit, int_lit in [("9.5", "9"), ("2.5", "2"), ("0.5", "0"), ("7.0", "7")]:
            pares.append((prog("entero %s = %s;" % (nombre, real_lit)), prog("entero %s = %s;" % (nombre, int_lit))))
        pares.append((prog('real %s = "hola";' % nombre), prog("real %s = 2.5;" % nombre)))
        pares.append((prog('real %s = "x";' % nombre), prog("real %s = 1.0;" % nombre)))
        for ent, cad in [("5", '"5"'), ("10", '"10"')]:
            pares.append((prog("cadena %s = %s;" % (nombre, ent)), prog("cadena %s = %s;" % (nombre, cad))))
        pares.append((prog('caracter %s = "a";' % nombre), prog("caracter %s = 'a';" % nombre)))
        pares.append((prog('caracter %s = "z";' % nombre), prog("caracter %s = 'z';" % nombre)))
        for ent, boo in [("1", "verdadero"), ("0", "falso"), ("2", "verdadero")]:
            pares.append((prog("booleano %s = %s;" % (nombre, ent)), prog("booleano %s = %s;" % (nombre, boo))))
        pares.append((prog('booleano %s = "si";' % nombre), prog("booleano %s = verdadero;" % nombre)))

    # reasignacion incompatible
    for nombre in nombres:
        pares.append((prog("entero %s = 5;\n%s = 9.5;" % (nombre, nombre)), prog("entero %s = 5;\n%s = 9;" % (nombre, nombre))))
        pares.append((prog("entero %s = 5;\n%s = %s + 1.0;" % (nombre, nombre, nombre)), prog("entero %s = 5;\n%s = %s + 1;" % (nombre, nombre, nombre))))
        pares.append((prog('cadena %s = "a";\n%s = 5;' % (nombre, nombre)), prog('cadena %s = "a";\n%s = "b";' % (nombre, nombre))))

    # identificador no declarado (en aritmetica/asignacion/condicion, que si lo validan)
    for nombre in nombres:
        pares.append((prog("entero z = %s + 1;" % nombre), prog("entero %s = 2;\nentero z = %s + 1;" % (nombre, nombre))))
        pares.append((prog("%s = %s + 1;" % (nombre, nombre)), prog("entero %s = 2;\n%s = %s + 1;" % (nombre, nombre, nombre))))
        pares.append((prog("si (%s > 0) {\n    imprimir(%s);\n}" % (nombre, nombre)), prog("entero %s = 3;\nsi (%s > 0) {\n    imprimir(%s);\n}" % (nombre, nombre, nombre))))

    # redeclaracion en el mismo scope
    for nombre in nombres:
        pares.append((prog("entero %s = 1;\nentero %s = 2;" % (nombre, nombre)), prog("entero %s = 1;\n%s = 2;" % (nombre, nombre))))

    # variable no numerica en aritmetica
    pares.append((prog('cadena x = "hola";\nentero y = 1 + x;'), prog("entero x = 2;\nentero y = 1 + x;")))
    pares.append((prog("caracter c = 'a';\nentero y = c * 2;"), prog("entero c = 2;\nentero y = c * 2;")))
    pares.append((prog("booleano b = verdadero;\nentero y = b + 1;"), prog("entero b = 1;\nentero y = b + 1;")))
    pares.append((prog('cadena s = "abc";\nentero y = s - 1;'), prog("entero s = 5;\nentero y = s - 1;")))

    # asignacion compuesta sobre tipos no numericos o entero += real
    pares.append((prog('cadena s = "a";\ns += "b";'), prog("entero s = 1;\ns += 2;")))
    pares.append((prog("booleano b = verdadero;\nb += falso;"), prog("entero b = 1;\nb += 2;")))
    for nombre in nombres:
        pares.append((prog("entero %s = 5;\n%s += 1.5;" % (nombre, nombre)), prog("entero %s = 5;\n%s += 1;" % (nombre, nombre))))

    # negacion sobre no booleano
    pares.append((prog('entero x = 5;\nsi (!x) {\n    imprimir("x");\n}'), prog('booleano x = falso;\nsi (!x) {\n    imprimir("x");\n}')))
    pares.append((prog('real r = 1.0;\nsi (!r) {\n    imprimir("r");\n}'), prog('booleano r = falso;\nsi (!r) {\n    imprimir("r");\n}')))

    # nulo como valor (fuera de v1)
    pares.append((prog("entero x = nulo;"), prog("entero x = 0;")))

    # booleano desde no booleano
    pares.append((prog("booleano b = 5;"), prog("booleano b = verdadero;")))
    pares.append((prog("entero x = 3;\nbooleano b = x;"), prog("entero x = 3;\nentero b = x;")))
    pares.append((prog('booleano b = "verdadero";'), prog("booleano b = verdadero;")))

    # llamadas a funciones: aridad, tipo, existencia, principal
    suma2 = fn("suma", "", "imprimir(a);", "entero a, entero b")
    pares.append((prog("suma(1);", funciones=suma2), prog("suma(1, 2);", funciones=suma2)))
    pares.append((prog("suma(1, 2, 3);", funciones=suma2), prog("suma(1, 2);", funciones=suma2)))
    saludo = fn("saludo", "", "imprimir(nombre);", "cadena nombre")
    pares.append((prog("saludo(5);", funciones=saludo), prog('saludo("hola");', funciones=saludo)))
    pares.append((prog('saludo(5, 6);', funciones=saludo), prog('saludo("hola");', funciones=saludo)))
    doble = fn("doble", "", "imprimir(n);", "entero n")
    pares.append((prog("doble();", funciones=doble), prog("doble(4);", funciones=doble)))
    pares.append((prog('doble("x");', funciones=doble), prog("doble(4);", funciones=doble)))
    pares.append((prog("f();"), prog("f();", funciones=fn("f", "", 'imprimir("x");'))))
    pares.append((prog("principal();"), prog('imprimir("ok");')))

    # retornar incompatible
    pares.append((prog("f();", funciones=fn("f", "", "retornar 1;")), prog("f();", funciones=fn("f", "", "retornar;"))))
    pares.append((prog("f();", funciones=fn("f", "entero", "retornar;")), prog("f();", funciones=fn("f", "entero", "retornar 1;"))))
    pares.append((prog("f();", funciones=fn("f", "entero", "retornar 2.5;")), prog("f();", funciones=fn("f", "entero", "retornar 2;"))))
    pares.append((prog("f();", funciones=fn("f", "entero", 'retornar "hola";')), prog("f();", funciones=fn("f", "entero", "retornar 1;"))))
    pares.append((prog("f();", funciones=fn("f", "real", 'retornar "hola";')), prog("f();", funciones=fn("f", "real", "retornar 1.0;"))))
    pares.append((prog("f();", funciones=fn("f", "cadena", "retornar 5;")), prog("f();", funciones=fn("f", "cadena", 'retornar "x";'))))
    pares.append((prog("f();", funciones=fn("f", "booleano", "retornar 1;")), prog("f();", funciones=fn("f", "booleano", "retornar verdadero;"))))

    # retorno de funcion vacia usado como valor
    for nombre in ["f", "g"]:
        pares.append((prog("entero y = %s();" % nombre, funciones=fn(nombre, "", 'imprimir("x");')),
                      prog("%s();" % nombre, funciones=fn(nombre, "", 'imprimir("x");'))))

    # parametro duplicado
    pares.append((prog("suma(1, 2);", funciones=fn("suma", "", "imprimir(a);", "entero a, entero a")),
                  prog("suma(1, 2);", funciones=fn("suma", "", "imprimir(a);", "entero a, entero b"))))
    pares.append((prog("f(1, 2);", funciones=fn("f", "entero", "retornar a;", "entero a, entero a")),
                  prog("f(1, 2);", funciones=fn("f", "entero", "retornar a;", "entero a, entero b"))))

    # mas aridad incorrecta con distintas funciones
    for nombre, params, ok_args in [
        ("restar", "entero a, entero b", "5, 2"),
        ("multiplicar", "entero a, entero b", "3, 4"),
        ("promedio", "real a, real b", "1.0, 2.0"),
    ]:
        base = fn(nombre, "entero" if nombre != "promedio" else "real", "retornar a;", params)
        pares.append((prog("%s(%s" % (nombre, ""), funciones=base), prog("%s(%s);" % (nombre, ok_args), funciones=base)))
        pares.append((prog("%s(1);" % nombre, funciones=base), prog("%s(%s);" % (nombre, ok_args), funciones=base)))
        pares.append((prog("%s(%s, 99);" % (nombre, ok_args), funciones=base), prog("%s(%s);" % (nombre, ok_args), funciones=base)))

    # mas "vacia usada como valor" y "retornar valor en vacia"
    for nombre in ["f", "g", "h"]:
        pares.append((prog("entero y = %s();" % nombre, funciones=fn(nombre, "", 'imprimir("x");')),
                      prog("%s();" % nombre, funciones=fn(nombre, "", 'imprimir("x");'))))
        pares.append((prog("%s();" % nombre, funciones=fn(nombre, "", "retornar 1;")),
                      prog("%s();" % nombre, funciones=fn(nombre, "", "retornar;"))))

    # asignacion desde variable de tipo incorrecto
    for nombre in nombres:
        pares.append((prog("entero a = 3;\ncadena %s = a;" % nombre), prog('cadena %s = "3";' % nombre)))
        pares.append((prog("entero a = 3;\ncaracter %s = a;" % nombre), prog("caracter %s = 'a';" % nombre)))
        pares.append((prog('cadena a = "x";\nentero %s = a;' % nombre), prog("entero %s = 5;" % nombre)))
        pares.append((prog("entero a = 3;\nbooleano %s = a;" % nombre), prog("booleano %s = verdadero;" % nombre)))

    # retornar variable de tipo incorrecto
    pares.append((prog("f();", funciones=fn("f", "entero", 'cadena s = "x";\nretornar s;')),
                  prog("f();", funciones=fn("f", "entero", "retornar 1;"))))
    pares.append((prog("f();", funciones=fn("f", "booleano", "entero n = 1;\nretornar n;")),
                  prog("f();", funciones=fn("f", "booleano", "retornar verdadero;"))))

    # expresiones reales asignadas a entero
    for nombre in nombres:
        pares.append((prog("entero %s = 3.0 + 1.0;" % nombre), prog("entero %s = 4;" % nombre)))
        pares.append((prog("entero %s = 10.0 / 2.0;" % nombre), prog("entero %s = 5;" % nombre)))

    # mas variable no numerica en aritmetica
    for nombre in ["x", "dato", "a"]:
        pares.append((prog('cadena %s = "hola";\nentero y = 1 + %s;' % (nombre, nombre)),
                      prog("entero %s = 2;\nentero y = 1 + %s;" % (nombre, nombre))))
        pares.append((prog('cadena %s = "hola";\nentero y = %s - 1;' % (nombre, nombre)),
                      prog("entero %s = 5;\nentero y = %s - 1;" % (nombre, nombre))))

    # redeclaracion con distinto tipo en el mismo scope
    for nombre in nombres:
        pares.append((prog("entero %s = 1;\nreal %s = 2.0;" % (nombre, nombre)),
                      prog("entero %s = 1;\n%s = 2;" % (nombre, nombre))))

    # llamada a funcion con argumento de tipo incorrecto (combinatorio)
    fn_entero = fn("calcula", "entero", "retornar n;", "entero n")
    fn_cadena = fn("imprime", "", "imprimir(t);", "cadena t")
    fn_real = fn("promedia", "real", "retornar r;", "real r")
    for nombre in ["a", "b", "n"]:
        pares.append((prog("calcula(1.5);", funciones=fn_entero), prog("calcula(1);", funciones=fn_entero)))
        pares.append((prog('imprime(5);', funciones=fn_cadena), prog('imprime("hola");', funciones=fn_cadena)))
        pares.append((prog('promedia("x");', funciones=fn_real), prog("promedia(1.0);", funciones=fn_real)))
        pares.append((prog("entero %s = calcula(2.0);" % nombre, funciones=fn_entero),
                      prog("entero %s = calcula(2);" % nombre, funciones=fn_entero)))

    # compuesto sobre caracter
    pares.append((prog("caracter c = 'a';\nc += 'b';"), prog("entero c = 1;\nc += 2;")))
    pares.append((prog("caracter c = 'a';\nc -= 'b';"), prog("entero c = 5;\nc -= 2;")))

    # condicion o ciclo con variable no declarada
    for nombre in ["x", "dato", "a"]:
        pares.append((prog("mientras (%s < 3) {\n    imprimir(%s);\n}" % (nombre, nombre)),
                      prog("entero %s = 0;\nmientras (%s < 3) {\n    imprimir(%s);\n}" % (nombre, nombre, nombre))))
        pares.append((prog("para (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % nombre),
                      prog("entero %s = 5;\npara (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % (nombre, nombre))))
        pares.append((prog("hacer {\n    imprimir(%s);\n} mientras (%s < 3);" % (nombre, nombre)),
                      prog("entero %s = 0;\nhacer {\n    imprimir(%s);\n} mientras (%s < 3);" % (nombre, nombre, nombre))))

    # expresiones numericas incompatibles
    for nombre in ["x", "dato", "n", "total"]:
        pares.append((prog("entero %s = 1 + 2.0;" % nombre), prog("entero %s = 3;" % nombre)))
        pares.append((prog("entero %s = 2.5 * 2;" % nombre), prog("entero %s = 5;" % nombre)))

    # cadena/caracter/booleano en aritmetica (mas)
    for nombre in ["s", "t", "dato"]:
        pares.append((prog('cadena %s = "a";\nentero y = %s + %s;' % (nombre, nombre, nombre)),
                      prog("entero %s = 1;\nentero y = %s + %s;" % (nombre, nombre, nombre))))
        pares.append((prog('cadena %s = "a";\nentero y = 2 * %s;' % (nombre, nombre)),
                      prog("entero %s = 2;\nentero y = 2 * %s;" % (nombre, nombre))))

    # compuesto sobre booleano/cadena (mas nombres)
    for nombre in ["b", "flag", "dato"]:
        pares.append((prog("booleano %s = verdadero;\n%s += falso;" % (nombre, nombre)),
                      prog("entero %s = 1;\n%s += 2;" % (nombre, nombre))))
        pares.append((prog('cadena %s = "a";\n%s *= 2;' % (nombre, nombre)),
                      prog("entero %s = 2;\n%s *= 2;" % (nombre, nombre))))

    # mas aridad incorrecta
    for nombre, params, ok_args in [("multiplicar", "entero a, entero b", "3, 4"), ("dividir", "entero a, entero b", "8, 2")]:
        base = fn(nombre, "entero", "retornar a;", params)
        pares.append((prog("%s(1);" % nombre, funciones=base), prog("%s(%s);" % (nombre, ok_args), funciones=base)))
        pares.append((prog("%s();" % nombre, funciones=base), prog("%s(%s);" % (nombre, ok_args), funciones=base)))

    # mas tipos incompatibles en inicializacion
    for nombre in ["x", "dato", "num"]:
        pares.append((prog("real %s = 'a';" % nombre), prog("real %s = 1.0;" % nombre)))
        pares.append((prog("real %s = verdadero;" % nombre), prog("real %s = 1.0;" % nombre)))
        pares.append((prog("cadena %s = 'a';" % nombre), prog('cadena %s = "a";' % nombre)))
        pares.append((prog("caracter %s = 5;" % nombre), prog("caracter %s = 'a';" % nombre)))

    return pares


# ---------------------------------------------------------------------------
# Errores SINTACTICOS (roto -> corregido). category: correction_syntax
# ---------------------------------------------------------------------------
def gen_sintacticos():
    pares = []

    # clase sin parentesis (varias variantes)
    for clase in ["Programa", "Test", "Demo"]:
        pares.append(("clase %s{\n    funcion principal(){\n    }\n}" % clase,
                      "clase %s(){\n    funcion principal(){\n    }\n}" % clase))
        pares.append(("clase %s{\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}" % clase,
                      "clase %s(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}" % clase))

    # falta punto y coma (varias sentencias)
    for stmt, fijo in [
        ('imprimir("Hola")', 'imprimir("Hola");'),
        ("entero x = 5", "entero x = 5;"),
        ("real r = 2.5", "real r = 2.5;"),
        ('cadena s = "hola"', 'cadena s = "hola";'),
        ("caracter c = 'a'", "caracter c = 'a';"),
        ("booleano b = verdadero", "booleano b = verdadero;"),
        ("entero x = 0\nx = x + 1", "entero x = 0;\nx = x + 1;"),
        ("entero a = 2\nentero b = 3", "entero a = 2;\nentero b = 3;"),
        ("imprimir(10)", "imprimir(10);"),
        ('imprimir("hola", "mundo")', 'imprimir("hola", "mundo");'),
    ]:
        pares.append((prog(stmt), prog(fijo)))

    # sin principal
    pares.append(("clase Programa(){\n}", "clase Programa(){\n    funcion principal(){\n        imprimir(\"Hola\");\n    }\n}"))

    # para sin tipo (varias variantes)
    for tope in ["5", "8", "10", "3"]:
        pares.append((prog("para (i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope),
                      prog("para (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope)))

    # salir sin punto y coma
    for lit, txt in [("1", "uno"), ("2", "dos"), ("3", "tres")]:
        pares.append((prog('entero x = 1;\nsegun (x) {\n    caso %s:\n        imprimir("%s");\n        salir\n}' % (lit, txt)),
                      prog('entero x = 1;\nsegun (x) {\n    caso %s:\n        imprimir("%s");\n        salir;\n}' % (lit, txt))))

    # imprimir con coma colgante
    pares.append((prog('imprimir("a", );'), prog('imprimir("a");')))

    # falta parentesis de cierre en si
    pares.append((prog('entero x = 0;\nsi (x > 0 {\n    imprimir("x");\n}'),
                  prog('entero x = 0;\nsi (x > 0) {\n    imprimir("x");\n}')))

    # hacer sin mientras
    pares.append((prog("entero x = 0;\nhacer {\n    x = x + 1;\n}"),
                  prog("entero x = 0;\nhacer {\n    x = x + 1;\n} mientras (x < 3);")))

    # palabra reservada capitalizada
    pares.append((prog('Imprimir("Hola");'), prog('imprimir("Hola");')))
    pares.append(("Clase Programa(){\n    funcion principal(){\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n    }\n}"))
    pares.append(("Clase Demo(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}",
                  "clase Demo(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # sino si sin parentesis de cierre
    pares.append((prog('entero x = 0;\nsi (x == 0) {\n    imprimir("cero");\n} sino si (x == 1 {\n    imprimir("uno");\n}'),
                  prog('entero x = 0;\nsi (x == 0) {\n    imprimir("cero");\n} sino si (x == 1) {\n    imprimir("uno");\n}')))

    # predeterminado fuera de posicion (no al final)
    pares.append((prog('entero x = 1;\nsegun (x) {\n    predeterminado:\n        imprimir("otro");\n        salir;\n    caso 1:\n        imprimir("uno");\n        salir;\n}'),
                  prog('entero x = 1;\nsegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n    predeterminado:\n        imprimir("otro");\n        salir;\n}')))

    # llamada sin punto y coma (con funcion definida)
    pares.append((prog("saludar()", funciones=fn("saludar", "", 'imprimir("hola");')),
                  prog("saludar();", funciones=fn("saludar", "", 'imprimir("hola");'))))

    # retornar sin punto y coma
    pares.append((prog("f();", funciones=fn("f", "entero", "retornar 1")),
                  prog("f();", funciones=fn("f", "entero", "retornar 1;"))))

    # llave de cierre faltante en la clase
    pares.append(("clase Programa(){\n    funcion principal(){\n        entero x = 1;\n    }\n",
                  "clase Programa(){\n    funcion principal(){\n        entero x = 1;\n    }\n}"))

    # declaracion sin tipo
    for nombre in ["x", "n", "a", "b", "num", "total"]:
        pares.append((prog("%s = 5;" % nombre), prog("entero %s = 5;" % nombre)))

    # se esperaba parentesis de cierre en imprimir
    pares.append((prog('imprimir("hola";'), prog('imprimir("hola");')))
    pares.append((prog('imprimir(10;'), prog('imprimir(10);')))

    # funcion sin llave de apertura del cuerpo
    pares.append((prog("saludar();", funciones="funcion saludar()\n    imprimir(\"hola\");\n}"),
                  prog("saludar();", funciones=fn("saludar", "", 'imprimir("hola");'))))

    # mientras sin parentesis
    pares.append((prog("entero x = 0;\nmientras x < 3 {\n    x = x + 1;\n}"),
                  prog("entero x = 0;\nmientras (x < 3) {\n    x = x + 1;\n}")))

    # si sin llaves de apertura
    pares.append((prog('entero x = 1;\nsi (x > 0)\n    imprimir("x");\n}'),
                  prog('entero x = 1;\nsi (x > 0) {\n    imprimir("x");\n}')))

    # llave de cierre faltante al final de la clase
    pares.append(("clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # segun sin parentesis
    pares.append((prog('entero x = 1;\nsegun x {\n    caso 1:\n        imprimir("uno");\n        salir;\n}'),
                  prog('entero x = 1;\nsegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n}')))

    # caso sin dos puntos
    pares.append((prog('entero x = 1;\nsegun (x) {\n    caso 1\n        imprimir("uno");\n        salir;\n}'),
                  prog('entero x = 1;\nsegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n}')))

    # falta la palabra funcion en la definicion
    pares.append(("clase Programa(){\n    principal(){\n        imprimir(\"hola\");\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # mas variantes: falta punto y coma en declaraciones y asignaciones
    for nombre in ["x", "a", "n", "total"]:
        pares.append((prog("entero %s = 5\nimprimir(%s)" % (nombre, nombre)),
                      prog("entero %s = 5;\nimprimir(%s);" % (nombre, nombre))))
        pares.append((prog("entero %s = 5\n%s = %s + 1" % (nombre, nombre, nombre)),
                      prog("entero %s = 5;\n%s = %s + 1;" % (nombre, nombre, nombre))))

    # parentesis de cierre faltante en distintas estructuras
    for tope in ["5", "8"]:
        pares.append((prog("entero i = 0;\nmientras (i < %s {\n    i = i + 1;\n}" % tope),
                      prog("entero i = 0;\nmientras (i < %s) {\n    i = i + 1;\n}" % tope)))
        pares.append((prog("para (entero i = 0; i < %s; i++ {\n    imprimir(i);\n}" % tope),
                      prog("para (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope)))

    # palabras reservadas capitalizadas (mas)
    pares.append((prog('entero x = 1;\nSi (x > 0) {\n    imprimir("x");\n}'),
                  prog('entero x = 1;\nsi (x > 0) {\n    imprimir("x");\n}')))
    pares.append((prog('entero x = 0;\nMientras (x < 3) {\n    x = x + 1;\n}'),
                  prog('entero x = 0;\nmientras (x < 3) {\n    x = x + 1;\n}')))
    pares.append(("clase Programa(){\n    Funcion principal(){\n        imprimir(\"hola\");\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # sin principal (mas variantes)
    pares.append(("clase Programa(){\n    funcion ayuda(){\n        imprimir(\"hola\");\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # gran expansion: falta de punto y coma en muchas sentencias
    for cuerpo in [
        "entero x = 1",
        "real r = 2.5",
        'cadena s = "hola"',
        "caracter c = 'a'",
        "booleano b = verdadero",
        "imprimir(1)",
        'imprimir("a", "b")',
        "imprimir(verdadero)",
        "imprimir('c')",
        "entero a = 2",
        "entero b = 3",
    ]:
        pares.append((prog(cuerpo), prog(cuerpo + ";")))
    for nombre in ["x", "n", "a"]:
        pares.append((prog("entero %s = 5\n%s += 1" % (nombre, nombre)),
                      prog("entero %s = 5;\n%s += 1;" % (nombre, nombre))))
        pares.append((prog("entero %s = 5\n%s = %s * 2" % (nombre, nombre, nombre)),
                      prog("entero %s = 5;\n%s = %s * 2;" % (nombre, nombre, nombre))))

    # para sin punto y coma interno
    pares.append((prog("para (entero i = 0 i < 5; i++) {\n    imprimir(i);\n}"),
                  prog("para (entero i = 0; i < 5; i++) {\n    imprimir(i);\n}")))

    # segun sin selector
    pares.append((prog('segun () {\n    caso 1:\n        imprimir("uno");\n        salir;\n}'),
                  prog('entero x = 1;\nsegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n}')))

    # imprimir sin parentesis de apertura
    pares.append((prog('imprimir "hola";'), prog('imprimir("hola");')))

    # funcion principal sin parentesis
    pares.append(("clase Programa(){\n    funcion principal{\n        imprimir(\"hola\");\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # extra llave de cierre
    pares.append(("clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}\n}",
                  "clase Programa(){\n    funcion principal(){\n        imprimir(\"hola\");\n    }\n}"))

    # sino sin si previo
    pares.append(("clase Programa(){\n    funcion principal(){\n        sino {\n            imprimir(\"x\");\n        }\n    }\n}",
                  "clase Programa(){\n    funcion principal(){\n        entero x = 1;\n        si (x > 0) {\n            imprimir(\"x\");\n        } sino {\n            imprimir(\"x\");\n        }\n    }\n}"))

    # declaraciones sin punto y coma (combinatorio)
    for tipo, lit in [("entero", "5"), ("entero", "10"), ("real", "2.5"), ("real", "1.0"),
                      ('cadena', '"hola"'), ('cadena', '"mundo"'), ("caracter", "'a'"), ("caracter", "'b'"),
                      ("booleano", "verdadero"), ("booleano", "falso")]:
        for nombre in ["x", "y", "dato", "num"]:
            pares.append((prog("%s %s = %s" % (tipo, nombre, lit)),
                          prog("%s %s = %s;" % (tipo, nombre, lit))))

    # clase sin parentesis (mas nombres)
    for clase in ["Programa", "Test", "Demo", "Hola", "MiPrograma"]:
        pares.append(("clase %s{\n    funcion principal(){\n    }\n}" % clase,
                      "clase %s(){\n    funcion principal(){\n    }\n}" % clase))

    # imprimir con coma colgante (mas)
    for txt in ["a", "hola", "x"]:
        pares.append((prog('imprimir("%s", );' % txt), prog('imprimir("%s");' % txt)))

    # capitalizacion de otras palabras reservadas
    pares.append((prog('entero x = 0;\nPara (entero i = 0; i < 5; i++) {\n    imprimir(i);\n}'),
                  prog('entero x = 0;\npara (entero i = 0; i < 5; i++) {\n    imprimir(i);\n}')))
    pares.append((prog('entero x = 1;\nSegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n}'),
                  prog('entero x = 1;\nsegun (x) {\n    caso 1:\n        imprimir("uno");\n        salir;\n}')))

    # retornar sin punto y coma (mas)
    for tipo in ["entero", "real", "cadena", "booleano"]:
        lit = {"entero": "1", "real": "1.0", "cadena": '"x"', "booleano": "verdadero"}[tipo]
        pares.append((prog("f();", funciones=fn("f", tipo, "retornar %s" % lit)),
                      prog("f();", funciones=fn("f", tipo, "retornar %s;" % lit))))

    # asignaciones aritmeticas sin punto y coma (combinatorio)
    for op in ["+", "-", "*", "/"]:
        for nombre in ["x", "a", "n"]:
            pares.append((prog("entero %s = 5\n%s = %s %s 2" % (nombre, nombre, nombre, op)),
                          prog("entero %s = 5;\n%s = %s %s 2;" % (nombre, nombre, nombre, op))))

    # si sin parentesis de cierre (mas)
    for v in ["0", "5", "10"]:
        pares.append((prog("entero x = %s;\nsi (x > 0 {\n    imprimir(\"x\");\n}" % v),
                      prog("entero x = %s;\nsi (x > 0) {\n    imprimir(\"x\");\n}" % v)))

    # hacer sin mientras (mas)
    for nombre in ["x", "n"]:
        pares.append((prog("entero %s = 0;\nhacer {\n    %s = %s + 1;\n}" % (nombre, nombre, nombre)),
                      prog("entero %s = 0;\nhacer {\n    %s = %s + 1;\n} mientras (%s < 3);" % (nombre, nombre, nombre, nombre))))

    # mas: imprimir y asignaciones sin punto y coma (combinatorio)
    for nombre in ["x", "a", "b", "n"]:
        pares.append((prog("entero %s = 5\nimprimir(%s)" % (nombre, nombre)),
                      prog("entero %s = 5;\nimprimir(%s);" % (nombre, nombre))))
        pares.append((prog("entero %s = 5\n%s = %s + 3" % (nombre, nombre, nombre)),
                      prog("entero %s = 5;\n%s = %s + 3;" % (nombre, nombre, nombre))))

    # segun con predeterminado no al final (mas)
    pares.append((prog('cadena x = "z";\nsegun (x) {\n    predeterminado:\n        imprimir("otro");\n        salir;\n    caso "z":\n        imprimir("z");\n        salir;\n}'),
                  prog('cadena x = "z";\nsegun (x) {\n    caso "z":\n        imprimir("z");\n        salir;\n    predeterminado:\n        imprimir("otro");\n        salir;\n}')))

    # para sin tipo (mas contadores)
    for nombre in ["i", "j", "k"]:
        pares.append((prog("para (%s = 0; %s < 5; %s++) {\n    imprimir(%s);\n}" % (nombre, nombre, nombre, nombre)),
                      prog("para (entero %s = 0; %s < 5; %s++) {\n    imprimir(%s);\n}" % (nombre, nombre, nombre, nombre))))

    # mas matrices de errores: asignaciones sin punto y coma
    for op in ["+", "-", "*"]:
        for nombre in ["x", "a", "b", "n", "total"]:
            pares.append((prog("entero %s = 5\n%s = %s %s 2" % (nombre, nombre, nombre, op)),
                          prog("entero %s = 5;\n%s = %s %s 2;" % (nombre, nombre, nombre, op))))

    # mientras sin parentesis (mas)
    for nombre in ["x", "n", "i"]:
        pares.append((prog("entero %s = 0;\nmientras %s < 3 {\n    %s = %s + 1;\n}" % (nombre, nombre, nombre, nombre)),
                      prog("entero %s = 0;\nmientras (%s < 3) {\n    %s = %s + 1;\n}" % (nombre, nombre, nombre, nombre))))

    # imprimir sin parentesis de cierre (mas)
    for arg in ["5", '"hola"', "verdadero", "'c'"]:
        pares.append((prog("imprimir(%s;" % arg), prog("imprimir(%s);" % arg)))

    # si sin llaves de apertura (mas)
    for nombre in ["x", "a", "n"]:
        pares.append((prog("entero %s = 1;\nsi (%s > 0)\n    imprimir(%s);\n}" % (nombre, nombre, nombre)),
                      prog("entero %s = 1;\nsi (%s > 0) {\n    imprimir(%s);\n}" % (nombre, nombre, nombre))))

    # caso sin dos puntos (mas)
    for lit, txt in [("2", "dos"), ("3", "tres")]:
        pares.append((prog('entero x = 1;\nsegun (x) {\n    caso %s\n        imprimir("%s");\n        salir;\n}' % (lit, txt)),
                      prog('entero x = 1;\nsegun (x) {\n    caso %s:\n        imprimir("%s");\n        salir;\n}' % (lit, txt))))

    # declaracion sin tipo (mas)
    for nombre in ["x", "y", "z", "dato"]:
        pares.append((prog("%s = 10;" % nombre), prog("entero %s = 10;" % nombre)))

    # ultimas matrices: asignaciones sin punto y coma (division y modulo)
    for op in ["/", "%"]:
        for nombre in ["x", "a", "b", "n", "total"]:
            pares.append((prog("entero %s = 8\n%s = %s %s 2" % (nombre, nombre, nombre, op)),
                          prog("entero %s = 8;\n%s = %s %s 2;" % (nombre, nombre, nombre, op))))

    # para sin tipo (mas valores de condicion)
    for tope in ["4", "6", "7"]:
        pares.append((prog("para (i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope),
                      prog("para (entero i = 0; i < %s; i++) {\n    imprimir(i);\n}" % tope)))

    # salir sin punto y coma (mas)
    for lit, txt in [("4", "cuatro"), ("5", "cinco")]:
        pares.append((prog('entero x = 1;\nsegun (x) {\n    caso %s:\n        imprimir("%s");\n        salir\n}' % (lit, txt)),
                      prog('entero x = 1;\nsegun (x) {\n    caso %s:\n        imprimir("%s");\n        salir;\n}' % (lit, txt))))

    # si sin parentesis de cierre (mas valores)
    for v in ["1", "2", "3"]:
        pares.append((prog("entero x = %s;\nsi (x == %s {\n    imprimir(\"x\");\n}" % (v, v)),
                      prog("entero x = %s;\nsi (x == %s) {\n    imprimir(\"x\");\n}" % (v, v))))

    # extra: faltan parentesis en segun (mas) y sin principal (mas)
    pares.append((prog('cadena x = "a";\nsegun x {\n    caso "a":\n        imprimir("A");\n        salir;\n}'),
                  prog('cadena x = "a";\nsegun (x) {\n    caso "a":\n        imprimir("A");\n        salir;\n}')))
    pares.append(("clase Demo(){\n    funcion principal(){\n        imprimir(\"x\");\n    }\n    funcion principal(){\n        imprimir(\"y\");\n    }\n}",
                  "clase Demo(){\n    funcion principal(){\n        imprimir(\"x\");\n    }\n}"))
    pares.append((prog('entero x = ;'), prog('entero x = 5;')))

    return pares


# ---------------------------------------------------------------------------
# Escritura de salida
# ---------------------------------------------------------------------------
def escribir_jsonl(ruta, filas):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        for obj in filas:
            f.write(json.dumps(obj, ensure_ascii=False) + "\n")


def _dedup(filas, key="instruction"):
    vistos = set()
    out = []
    for f in filas:
        k = f[key]
        if k in vistos:
            continue
        vistos.add(k)
        out.append(f)
    return out


def main():
    ap = argparse.ArgumentParser(description="Genera y valida los datasets v3 de CerberusAI.")
    ap.add_argument("--out-dir", default=AQUI, help="carpeta donde escribir los .jsonl.")
    ap.add_argument("--classpath", default=os.path.join(RAIZ, "target", "classes"))
    ap.add_argument("--java", default=os.environ.get("JAVA", "java"))
    ap.add_argument("--prefix", default="cerberus_dataset_v3", help="prefijo de los archivos de salida.")
    args = ap.parse_args()

    global JAVA, CLASSPATH
    JAVA = args.java
    CLASSPATH = args.classpath

    if not os.path.isdir(CLASSPATH):
        print("No se encontro el classpath '%s'. Ejecuta antes `mvnw.cmd compile`." % CLASSPATH)
        return 1

    valido_raw = gen_validos()
    sem_raw = gen_semanticos()
    sin_raw = gen_sintacticos()

    validos, fallos_val = [], []
    for inst, codigo in valido_raw:
        if acepta(codigo):
            validos.append({"instruction": inst, "response": codigo, "category": "generation", "source": SOURCE})
        else:
            fallos_val.append({"instruction": inst, "response": codigo, "category": "errors_validacion",
                               "source": SOURCE, "error": compilar(codigo).get("error")})

    def validar_pares(pares, categoria):
        ok, descartados, inesperados = [], 0, 0
        for roto, corregido in pares:
            if roto is None or corregido is None:
                descartados += 1
                continue
            if acepta(roto):
                inesperados += 1
                continue
            if not acepta(corregido):
                descartados += 1
                continue
            ok.append({"instruction": "Corrige el siguiente codigo Cerberus y devuelve una version que compile:\n" + roto,
                       "response": corregido, "category": categoria, "source": SOURCE})
        return ok, descartados, inesperados

    sem_ok, sem_descartados, sem_inesperados = validar_pares(sem_raw, "correction_semantics")
    sin_ok, sin_descartados, sin_inesperados = validar_pares(sin_raw, "correction_syntax")

    validos = _dedup(validos)
    fallos_val = _dedup(fallos_val)
    sem_ok = _dedup(sem_ok)
    sin_ok = _dedup(sin_ok)

    archivos = {
        args.prefix + "_valido.jsonl": validos,
        args.prefix + "_errores_semanticos.jsonl": sem_ok,
        args.prefix + "_errores_sintacticos.jsonl": sin_ok,
        args.prefix + "_errores_validacion.jsonl": fallos_val,
    }
    for nombre, filas in archivos.items():
        escribir_jsonl(os.path.join(args.out_dir, nombre), filas)

    total = len(validos) + len(sem_ok) + len(sin_ok) + len(fallos_val)
    print("== Datasets v3 generados y validados contra el oraculo ==")
    print("validos (generation):             %5d" % len(validos))
    print("errores semanticos (correction):  %5d" % len(sem_ok))
    print("errores sintacticos (correction): %5d" % len(sin_ok))
    print("fallos de validacion (errores):   %5d" % len(fallos_val))
    print("descartados sem/sin: %d/%d, inesperados (rotos que compilan): %d/%d"
          % (sem_descartados, sin_descartados, sem_inesperados, sin_inesperados))
    print("TOTAL publicado: %d" % total)
    if fallos_val:
        print()
        print("Fallos de validacion (programas 'validos' que NO compilan):")
        for f in fallos_val:
            print("  - %s" % (f["error"] or "(sin error)"))
    return 0


if __name__ == "__main__":
    sys.exit(main())

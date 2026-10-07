package audit;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;
import Util.Reporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Sonda headless del compilador Cerberus.
 *
 * Ejecuta cada snippet a traves del analizador lexico y sintactico (sin JavaFX)
 * y registra si el compilador lo acepta o lo rechaza, junto con el error
 * producido. Sirve como evidencia dinamica para la auditoria del compilador.
 *
 * Ejecucion (desde la raiz del proyecto, tras `mvnw.cmd compile`):
 *   java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml -cp target\classes audit.CompilerProbe
 */
public class CompilerProbe {

    private static final class Caso {
        final String nombre;
        final String codigo;
        final boolean seEsperaAceptar;

        Caso(String nombre, String codigo, boolean seEsperaAceptar) {
            this.nombre = nombre;
            this.codigo = codigo;
            this.seEsperaAceptar = seEsperaAceptar;
        }
    }

    private static final class Resultado {
        boolean balanceado;
        boolean analizo;
        boolean resultado;
        boolean excepcion;
        String error;

        boolean aceptado() {
            return balanceado && analizo && resultado && !excepcion;
        }
    }

    private static final class ReporterAcumulador implements Reporter {
        final List<String> mensajes = new ArrayList<>();

        @Override
        public void reportar(String mensaje) {
            mensajes.add(mensaje);
        }
    }

    private static String prog(String cuerpo) {
        return "clase Test(){\nfuncion principal(){\n" + cuerpo + "\n}\n}";
    }

    private static String progConGlobales(String globales, String cuerpo) {
        return globales + "\nclase Test(){\nfuncion principal(){\n" + cuerpo + "\n}\n}";
    }

    private static String progConFunciones(String cuerpoPrincipal, String funciones) {
        return "clase Test(){\nfuncion principal(){\n" + cuerpoPrincipal + "\n}\n" + funciones + "\n}";
    }

    private static Resultado evaluar(String codigo) {
        Resultado res = new Resultado();
        try {
            AnalizadorLexico lex = new AnalizadorLexico();
            ReporterAcumulador rep = new ReporterAcumulador();
            lex.setReporter(rep::reportar);

            lex.seccionarCadena(codigo);
            AnalizadorSintactico sin = new AnalizadorSintactico(lex.getTokensDetectados(), rep::reportar);

            res.balanceado = sin.comprobarParentesis();
            res.analizo = sin.analizar();
            res.resultado = sin.getResultado();
            res.error = String.join(" | ", rep.mensajes);
        } catch (Throwable t) {
            res.excepcion = true;
            res.error = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
        return res;
    }

    public static void main(String[] args) {
        List<Caso> casos = new ArrayList<>();

        casos.add(new Caso("programa minimo valido",
                prog("imprimir(\"Hola\");"), true));

        casos.add(new Caso("clase sin parentesis (clase Test{ ... })",
                "clase Test{\nfuncion principal(){\n}\n}", false));

        casos.add(new Caso("clase sin funcion principal",
                "clase Test(){\n}", false));

        casos.add(new Caso("entero con inicializacion",
                prog("entero x = 5;"), true));

        casos.add(new Caso("entero sin inicializacion",
                prog("entero x;"), true));

        casos.add(new Caso("real con inicializacion",
                prog("real x = 2.5;"), true));

        casos.add(new Caso("cadena con inicializacion",
                prog("cadena x = \"hola\";"), true));

        casos.add(new Caso("caracter con inicializacion",
                prog("caracter x = 'a';"), true));

        casos.add(new Caso("booleano sin inicializacion",
                prog("booleano x;"), true));

        casos.add(new Caso("booleano con inicializacion",
                prog("booleano x = verdadero;"), true));

        casos.add(new Caso("negacion booleana valida",
                prog("booleano a = falso;\nsi(!a){\n}"), true));

        casos.add(new Caso("asignacion directa numerica",
                prog("entero x = 5;\nx = 10;"), true));

        casos.add(new Caso("variable global antes de clase",
                progConGlobales("entero a = 2;", "imprimir(a);"), true));

        casos.add(new Caso("para valido (entero i)",
                prog("para(entero i = 0; i < 10; i++){\n imprimir(i);\n}"), true));

        casos.add(new Caso("para con decremento",
                prog("para(entero i = 10; i > 0; i--){\n imprimir(i);\n}"), true));

        casos.add(new Caso("para sin palabra entero",
                prog("para(i = 0; i < 10; i++){\n}"), false));

        casos.add(new Caso("si / sino valido",
                prog("entero x = 5;\nsi(x == 5){\n imprimir(\"si\");\n} sino {\n imprimir(\"no\");\n}"), true));

        casos.add(new Caso("si anidado (sino si)",
                prog("entero x = 5;\nsi(x == 5){\n} sino si(x == 6){\n}"), true));

        casos.add(new Caso("mientras valido",
                prog("entero x = 0;\nmientras(x < 5){\n x = x + 1;\n}"), true));

        casos.add(new Caso("hacer-mientras valido",
                prog("entero x = 0;\nhacer{\n x = x + 1;\n} mientras(x < 5);"), true));

        casos.add(new Caso("segun/caso valido (cadena)",
                prog("cadena x = \"hola\";\nsegun(x){\n caso \"hola\":\n  imprimir(\"ok\");\n salir;\n}"), true));

        casos.add(new Caso("segun con predeterminado",
                prog("entero x = 1;\nsegun(x){\n caso 1:\n  imprimir(\"uno\");\n salir;\n predeterminado:\n  imprimir(\"otro\");\n salir;\n}"), true));

        casos.add(new Caso("segun con salir sin punto y coma",
                prog("entero x = 1;\nsegun(x){\n caso 1:\n  imprimir(\"uno\");\n salir\n}"), false));

        casos.add(new Caso("funcion comun sin parametros",
                progConFunciones("imprimir(\"hola\");", "funcion f(){\n entero y = 1;\n}"), true));

        casos.add(new Caso("funcion con parametros",
                progConFunciones("", "funcion suma(entero a, entero b){\n imprimir(a);\n}"), true));

        casos.add(new Caso("retornar valor en funcion sin tipo (invalida)",
                progConFunciones("", "funcion f(){\n retornar 1;\n}"), false));

        casos.add(new Caso("retornar en funcion entero (valida)",
                progConFunciones("", "funcion entero f(){\n retornar 1;\n}"), true));

        casos.add(new Caso("operador compuesto +=",
                prog("entero x = 5;\nx += 2;"), true));

        casos.add(new Caso("operador compuesto -=",
                prog("entero x = 5;\nx -= 2;"), true));

        casos.add(new Caso("operador compuesto *=",
                prog("entero x = 5;\nx *= 2;"), true));

        casos.add(new Caso("operador compuesto /=",
                prog("entero x = 5;\nx /= 2;"), true));

        casos.add(new Caso("negacion de entero (invalida)",
                prog("entero x = 5;\nsi(!x){\n}"), false));

        casos.add(new Caso("aritmetica con variable cadena",
                prog("cadena x = \"hola\";\nentero y = 1 + x;"), false));

        casos.add(new Caso("declaracion entero con valor real",
                prog("entero x = 9.5;"), false));

        casos.add(new Caso("nulo como valor",
                prog("entero x = nulo;"), false));

        casos.add(new Caso("nuevo (POO, tokenizado sin gramatica)",
                prog("entero x = nuevo entero;"), false));

        casos.add(new Caso("shadowing entre scopes (global y local)",
                progConGlobales("entero x = 1;", "entero x = 2;"), true));

        casos.add(new Caso("llamada a funcion void (sentencia)",
                progConFunciones("saluda();", "funcion saluda(){\n imprimir(\"hola\");\n}"), true));

        casos.add(new Caso("llamada con argumentos",
                progConFunciones("suma(1, 2);", "funcion suma(entero a, entero b){\n imprimir(a);\n}"), true));

        casos.add(new Caso("llamada como valor de asignacion",
                progConFunciones("entero y = f(1, 2);", "funcion entero f(entero a, entero b){\n retornar a;\n}"), true));

        casos.add(new Caso("llamada booleana en condicion",
                progConFunciones("si(activo()){\n imprimir(\"si\");\n}", "funcion booleano activo(){\n retornar verdadero;\n}"), true));

        casos.add(new Caso("llamada a funcion inexistente",
                progConFunciones("f();", ""), false));

        casos.add(new Caso("llamada con aridad incorrecta",
                progConFunciones("suma(1);", "funcion suma(entero a, entero b){\n imprimir(a);\n}"), false));

        casos.add(new Caso("llamada void usada como valor",
                progConFunciones("entero y = f();", "funcion f(){\n imprimir(\"x\");\n}"), false));

        casos.add(new Caso("imprimir con varios argumentos separados por coma",
                prog("entero x = 1;\nimprimir(x, 2, \"z\");"), true));

        casos.add(new Caso("imprimir booleano",
                prog("imprimir(verdadero);\nimprimir(falso);"), true));

        casos.add(new Caso("arreglos (no implementados en v1)",
                prog("entero [] a;"), false));

        casos.add(new Caso("modificador publico (POO no implementada en v1)",
                prog("publico entero x;"), false));

        int aciertos = 0;
        int fallos = 0;

        System.out.println("NOMBRE | ESPERADO | ACEPTA | VEREDICTO | ERROR");
        System.out.println("-------|----------|--------|-----------|------");
        for (Caso c : casos) {
            Resultado r = evaluar(c.codigo);
            boolean acepta = r.aceptado();
            boolean veredicto = (acepta == c.seEsperaAceptar);
            if (veredicto) {
                aciertos++;
            } else {
                fallos++;
            }
            String error = (r.error == null || r.error.isEmpty()) ? "-" : r.error;
            System.out.println(c.nombre + " | " + c.seEsperaAceptar + " | " + acepta
                    + " | " + (veredicto ? "OK" : "DISCREPA") + " | " + error);
        }

        System.out.println();
        System.out.println("Resumen: " + aciertos + " casos coinciden con lo esperado, "
                + fallos + " discrepancias, " + casos.size() + " casos totales.");
    }
}

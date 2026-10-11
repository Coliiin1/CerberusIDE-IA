package audit;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;
import AnalizadorSintactico.ErrorSintactico;
import Util.Reporter;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluador headless del compilador Cerberus.
 *
 * Centraliza la logica que antes estaba duplicada en {@link CompilerProbe} y
 * {@link TestSuite}: ejecuta el lexer y el parser sin JavaFX y devuelve un
 * resultado estructurado (aceptado + mensaje + ubicacion del error).
 */
public final class Evaluador {

    /** Resultado de evaluar un fragmento de codigo. */
    public static final class Resultado {
        public final boolean aceptado;
        public final String error;
        public final int linea;
        public final int columna;
        public final String lexema;

        Resultado(boolean aceptado, String error, int linea, int columna, String lexema) {
            this.aceptado = aceptado;
            this.error = error;
            this.linea = linea;
            this.columna = columna;
            this.lexema = lexema;
        }
    }

    private static final class ReporterAcumulador implements Reporter {
        final List<String> mensajes = new ArrayList<>();

        @Override
        public void reportar(String mensaje) {
            mensajes.add(mensaje);
        }
    }

    private Evaluador() {
    }

    public static Resultado evaluar(String codigo) {
        try {
            AnalizadorLexico lex = new AnalizadorLexico();
            ReporterAcumulador rep = new ReporterAcumulador();
            lex.setReporter(rep::reportar);

            lex.seccionarCadena(codigo);
            AnalizadorSintactico sin = new AnalizadorSintactico(lex.getTokensDetectados(), rep::reportar);

            boolean balanceado = sin.comprobarParentesis();
            boolean analizo = sin.analizar();
            boolean resultado = sin.getResultado();
            boolean aceptado = balanceado && analizo && resultado;

            if (aceptado) {
                return new Resultado(true, null, -1, -1, null);
            }
            String error = String.join(" | ", rep.mensajes);
            ErrorSintactico e = sin.getUltimoError();
            if (e != null) {
                return new Resultado(false, error, e.getFila(), e.getColumna(), e.getLexema());
            }
            return new Resultado(false, error, -1, -1, null);
        } catch (Throwable t) {
            return new Resultado(false, t.getClass().getSimpleName() + ": " + t.getMessage(), -1, -1, null);
        }
    }

    /** Atajo: true si el codigo compila. */
    public static boolean compila(String codigo) {
        return evaluar(codigo).aceptado;
    }
}

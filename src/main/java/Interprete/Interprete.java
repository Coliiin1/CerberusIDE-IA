package Interprete;

import AnalizadorLexico.Token;
import AnalizadorLexico.Tokens;
import static AnalizadorLexico.Tokens.*;
import AnalizadorSemantico.Tipo;
import Util.Reporter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interprete de Cerberus que recorre la lista de tokens (sin AST).
 *
 * Precondicion: el programa ya fue ACEPTADO por {@code AnalizadorSintactico};
 * por eso casi no valida sintaxis ni tipos, solo errores de ejecucion
 * (division entre cero, variable sin valor, limites de pasos/profundidad/salida).
 * Cada {@code imprimir} emite una linea por el {@link Reporter} de salida.
 */
public class Interprete {

    public static final int MAX_PASOS = 1_000_000;
    public static final int MAX_PROFUNDIDAD = 1_000;
    public static final int MAX_LINEAS = 10_000;

    /** Error en tiempo de ejecucion (con la linea del token donde ocurrio). */
    public static final class ErrorEjecucion extends RuntimeException {
        private final int linea;

        ErrorEjecucion(String mensaje, int linea) {
            super(mensaje);
            this.linea = linea;
        }

        public int getLinea() {
            return linea;
        }
    }

    private static final class Variable {
        final Tipo tipo;
        Object valor;

        Variable(Tipo tipo, Object valor) {
            this.tipo = tipo;
            this.valor = valor;
        }
    }

    private static final class Funcion {
        final Tipo retorno; // null = vacio
        final List<Tipo> tiposParam = new ArrayList<>();
        final List<String> nombresParam = new ArrayList<>();
        int cuerpo; // posicion de la "{" del cuerpo

        Funcion(Tipo retorno) {
            this.retorno = retorno;
        }
    }

    /** Salida no local de "retornar". */
    private static final class Retorno extends RuntimeException {
        final Object valor;

        Retorno(Object valor) {
            super(null, null, false, false);
            this.valor = valor;
        }
    }

    private final List<Token> t;
    private final Reporter salida;
    private int pos;
    private int pasos;
    private int lineas;
    private int profundidad;
    private final Map<String, Variable> globales = new HashMap<>();
    private Deque<Map<String, Variable>> marco = new ArrayDeque<>();
    private Tipo retornoActual;
    private final Map<String, Funcion> funciones = new HashMap<>();
    private int principal = -1;

    public Interprete(List<Token> tokens, Reporter salida) {
        this.t = tokens;
        this.salida = salida;
    }

    /** Ejecuta globales y luego el cuerpo de principal. */
    public void ejecutar() {
        recolectarFunciones();
        pos = 0;
        while (tipoDeclaracion(tipo()) != null) {
            declaracion();
        }
        if (principal < 0) {
            throw new ErrorEjecucion("No se encontro la funcion principal", -1);
        }
        pos = principal;
        retornoActual = null;
        try {
            bloque();
        } catch (Retorno r) {
            // retornar; en principal termina el programa
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Tokens tipo() {
        return t.get(pos).getTipo();
    }

    private Tokens tipo(int i) {
        return i < t.size() ? t.get(i).getTipo() : null;
    }

    private Token consumir() {
        return t.get(pos++);
    }

    private ErrorEjecucion error(String mensaje) {
        Token tok = t.get(Math.min(pos, t.size() - 1));
        return new ErrorEjecucion(mensaje, tok.getLinea());
    }

    private void paso() {
        if (++pasos > MAX_PASOS) {
            throw error("Se supero el limite de " + MAX_PASOS + " pasos (posible ciclo infinito)");
        }
    }

    private static Tipo tipoDeclaracion(Tokens tk) {
        if (tk == null) return null;
        switch (tk) {
            case PALABRA_RESERVADA_ENT: return Tipo.ENTERO;
            case PALABRA_RESERVADA_REA: return Tipo.REAL;
            case PALABRA_RESERVADA_CAD: return Tipo.CADENA;
            case PALABRA_RESERVADA_CAR: return Tipo.CARACTER;
            case PALABRA_RESERVADA_BOO: return Tipo.BOOLEANO;
            default: return null;
        }
    }

    /** Desde una "{" salta hasta despues de su "}". */
    private void saltarBloque() {
        int nivel = 0;
        do {
            Tokens tk = consumir().getTipo();
            if (tk == LLAVE_ABRE) nivel++;
            else if (tk == LLAVE_CIERRA) nivel--;
        } while (nivel > 0);
    }

    /** Desde un "(" salta hasta despues de su ")". */
    private void saltarParentesis() {
        int nivel = 0;
        do {
            Tokens tk = consumir().getTipo();
            if (tk == PARENTESIS_ABRE) nivel++;
            else if (tk == PARENTESIS_CIERRA) nivel--;
        } while (nivel > 0);
    }

    // ---------------------------------------------------------------- funciones

    private void recolectarFunciones() {
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).getTipo() != PALABRA_RESERVADA_FUN) continue;
            int j = i + 1;
            if (tipo(j) == PALABRA_RESERVADA_PRIN) {
                // funcion principal ( ) {
                principal = j + 3;
                continue;
            }
            Tipo retorno = tipoDeclaracion(tipo(j));
            if (retorno != null || tipo(j) == PALABRA_RESERVADA_VAC) j++;
            if (tipo(j) != IDENTIFICADOR || tipo(j + 1) != PARENTESIS_ABRE) continue;
            String nombre = t.get(j).getLexema();
            Funcion f = new Funcion(retorno);
            j += 2;
            while (tipo(j) != PARENTESIS_CIERRA) {
                if (tipo(j) == COMA) { j++; continue; }
                f.tiposParam.add(tipoDeclaracion(tipo(j)));
                f.nombresParam.add(t.get(j + 1).getLexema());
                j += 2;
            }
            f.cuerpo = j + 1;
            funciones.put(nombre, f);
        }
    }

    private boolean esLlamada() {
        return tipo() == IDENTIFICADOR && tipo(pos + 1) == PARENTESIS_ABRE
                && funciones.containsKey(t.get(pos).getLexema());
    }

    /** IDENTIFICADOR "(" args ")": ejecuta la funcion y devuelve su valor (null si es vacio). */
    private Object llamada() {
        Token nombre = consumir();
        Funcion f = funciones.get(nombre.getLexema());
        consumir(); // (
        Map<String, Variable> params = new HashMap<>();
        for (int k = 0; k < f.tiposParam.size(); k++) {
            if (k > 0) consumir(); // ,
            Tipo tp = f.tiposParam.get(k);
            params.put(f.nombresParam.get(k), new Variable(tp, valor(tp)));
        }
        consumir(); // )
        if (++profundidad > MAX_PROFUNDIDAD) {
            throw new ErrorEjecucion("Se supero la profundidad maxima de llamadas ("
                    + MAX_PROFUNDIDAD + ")", nombre.getLinea());
        }
        Deque<Map<String, Variable>> marcoAnterior = marco;
        Tipo retornoAnterior = retornoActual;
        int vuelta = pos;
        marco = new ArrayDeque<>();
        marco.push(params);
        retornoActual = f.retorno;
        pos = f.cuerpo;
        Object resultado = null;
        try {
            bloque();
        } catch (Retorno r) {
            resultado = r.valor;
        } finally {
            marco = marcoAnterior;
            retornoActual = retornoAnterior;
            pos = vuelta;
            profundidad--;
        }
        return resultado;
    }

    // ---------------------------------------------------------------- variables

    private Variable buscar(String nombre) {
        for (Map<String, Variable> m : marco) {
            Variable v = m.get(nombre);
            if (v != null) return v;
        }
        return globales.get(nombre);
    }

    private Object leer(Token tok) {
        Variable v = buscar(tok.getLexema());
        if (v == null) {
            throw new ErrorEjecucion("Variable no encontrada: " + tok.getLexema(), tok.getLinea());
        }
        if (v.valor == null) {
            throw new ErrorEjecucion("Variable sin valor: " + tok.getLexema(), tok.getLinea());
        }
        return v.valor;
    }

    private void definir(String nombre, Tipo tipo, Object valor) {
        Map<String, Variable> ambito = marco.isEmpty() ? globales : marco.peek();
        ambito.put(nombre, new Variable(tipo, valor));
    }

    /** Ajusta un valor numerico al tipo destino (entero/real). */
    private static Object convertir(Tipo tipo, Object valor) {
        if (tipo == Tipo.REAL && valor instanceof Integer) return ((Integer) valor).doubleValue();
        if (tipo == Tipo.ENTERO && valor instanceof Double) return ((Double) valor).intValue();
        return valor;
    }

    // ---------------------------------------------------------------- sentencias

    /** "{" instrucciones "}" con su propio ambito. */
    private void bloque() {
        consumir(); // {
        marco.push(new HashMap<>());
        try {
            while (tipo() != LLAVE_CIERRA) {
                instruccion();
            }
            consumir(); // }
        } finally {
            marco.pop();
        }
    }

    private void instruccion() {
        paso();
        Tokens tk = tipo();
        if (tipoDeclaracion(tk) != null) {
            declaracion();
            return;
        }
        switch (tk) {
            case PALABRA_RESERVADA_IMP: imprimir(); break;
            case PALABRA_RESERVADA_SI: si(); break;
            case PALABRA_RESERVADA_MIE: mientras(); break;
            case PALABRA_RESERVADA_HAC: hacer(); break;
            case PALABRA_RESERVADA_PAR: para(); break;
            case PALABRA_RESERVADA_SEG: segun(); break;
            case PALABRA_RESERVADA_RET: retornar(); break;
            case PALABRA_RESERVADA_FUN: saltarFunciones(); break;
            case IDENTIFICADOR: asignacionOLlamada(); break;
            default:
                throw error("Instruccion no ejecutable: " + t.get(pos).getLexema());
        }
    }

    /** Definiciones de funcion anidadas: se registraron en el pre-escaneo, aqui solo se saltan. */
    private void saltarFunciones() {
        while (tipo() == PALABRA_RESERVADA_FUN) {
            consumir();
            while (tipo() != PARENTESIS_ABRE) consumir();
            saltarParentesis();
            saltarBloque();
        }
    }

    private void declaracion() {
        Tipo tp = tipoDeclaracion(consumir().getTipo());
        String nombre = consumir().getLexema();
        Object valor = null;
        if (tipo() == ASIGNACION) {
            consumir();
            valor = valor(tp);
        }
        consumir(); // ;
        definir(nombre, tp, valor);
    }

    private void asignacionOLlamada() {
        if (esLlamada()) {
            llamada();
            consumir(); // ;
            return;
        }
        Token nombre = consumir();
        Variable v = buscar(nombre.getLexema());
        if (v == null) {
            throw new ErrorEjecucion("Variable no encontrada: " + nombre.getLexema(), nombre.getLinea());
        }
        Tokens op = tipo();
        if (op == ASIGNACION) {
            consumir();
            v.valor = valor(v.tipo);
        } else if (op == MAS_VARIABLE || op == MENOS_VARIABLE || op == MUL_VARIABLE || op == DIV_VARIABLE) {
            consumir();
            Object base = leer(nombre);
            Object derecha = aritmetica();
            Tokens bin = op == MAS_VARIABLE ? OPERADOR_SUMA
                    : op == MENOS_VARIABLE ? OPERADOR_RESTA
                    : op == MUL_VARIABLE ? OPERADOR_MULTIPLICAR : OPERADOR_DIVISION;
            v.valor = convertir(v.tipo, operar(bin, base, derecha, nombre));
        }
        consumir(); // ;
    }

    private void imprimir() {
        consumir(); // imprimir
        consumir(); // (
        StringBuilder sb = new StringBuilder(texto(imprimible()));
        while (tipo() == OPERADOR_SUMA || tipo() == COMA) {
            consumir();
            sb.append(texto(imprimible()));
        }
        consumir(); // )
        consumir(); // ;
        if (++lineas > MAX_LINEAS) {
            throw error("Se supero el limite de " + MAX_LINEAS + " lineas de salida");
        }
        salida.reportar(sb.toString());
    }

    private Object imprimible() {
        if (esLlamada()) return llamada();
        Token tok = consumir();
        switch (tok.getTipo()) {
            case IDENTIFICADOR: return leer(tok);
            default: return literal(tok);
        }
    }

    private void si() {
        consumir(); // si
        consumir(); // (
        boolean c = logica();
        consumir(); // )
        if (c) {
            bloque();
            while (tipo() == PALABRA_RESERVADA_SIN) {
                consumir();
                if (tipo() == PALABRA_RESERVADA_SI) {
                    consumir();
                    saltarParentesis();
                }
                saltarBloque();
            }
            return;
        }
        saltarBloque();
        if (tipo() == PALABRA_RESERVADA_SIN) {
            consumir();
            if (tipo() == PALABRA_RESERVADA_SI) {
                si();
            } else {
                bloque();
            }
        }
    }

    private void mientras() {
        consumir(); // mientras
        int inicio = pos;
        while (true) {
            pos = inicio;
            consumir(); // (
            boolean c = logica();
            consumir(); // )
            if (!c) {
                saltarBloque();
                return;
            }
            bloque();
            paso();
        }
    }

    private void hacer() {
        consumir(); // hacer
        int inicio = pos;
        while (true) {
            pos = inicio;
            bloque();
            consumir(); // mientras
            consumir(); // (
            boolean c = logica();
            consumir(); // )
            if (!c) {
                consumir(); // ;
                return;
            }
            paso();
        }
    }

    private void para() {
        consumir(); // para
        consumir(); // (
        marco.push(new HashMap<>());
        try {
            declaracion(); // incluye el ";"
            int condicion = pos;
            while (true) {
                pos = condicion;
                boolean c = comparacion();
                consumir(); // ;
                Token contador = consumir();
                boolean incremento = consumir().getTipo() == INCREMENTO;
                consumir(); // )
                if (!c) {
                    saltarBloque();
                    return;
                }
                bloque();
                Variable v = buscar(contador.getLexema());
                v.valor = convertir(v.tipo, operar(incremento ? OPERADOR_SUMA : OPERADOR_RESTA,
                        leer(contador), 1, contador));
                paso();
            }
        } finally {
            marco.pop();
        }
    }

    private void segun() {
        consumir(); // segun
        consumir(); // (
        Object valor = leer(consumir());
        consumir(); // )
        consumir(); // {
        marco.push(new HashMap<>());
        try {
            boolean ejecutado = false;
            while (tipo() == PALABRA_RESERVADA_CAS || tipo() == PALABRA_RESERVADA_PRE) {
                boolean coincide;
                if (consumir().getTipo() == PALABRA_RESERVADA_CAS) {
                    coincide = iguales(valor, literal(consumir()));
                } else {
                    coincide = true; // predeterminado (siempre el ultimo)
                }
                consumir(); // :
                if (coincide && !ejecutado) {
                    ejecutado = true;
                    while (tipo() != PALABRA_RESERVADA_SAL) instruccion();
                } else {
                    saltarCaso();
                }
                consumir(); // salir
                consumir(); // ;
            }
            consumir(); // }
        } finally {
            marco.pop();
        }
    }

    /** Salta instrucciones hasta el "salir" del mismo nivel. */
    private void saltarCaso() {
        int nivel = 0;
        while (nivel > 0 || tipo() != PALABRA_RESERVADA_SAL) {
            Tokens tk = consumir().getTipo();
            if (tk == LLAVE_ABRE) nivel++;
            else if (tk == LLAVE_CIERRA) nivel--;
        }
    }

    private void retornar() {
        consumir(); // retornar
        Object v = null;
        if (tipo() != PUNTO_COMA) {
            v = valor(retornoActual);
        }
        consumir(); // ;
        throw new Retorno(v);
    }

    // ---------------------------------------------------------------- expresiones

    /** Lado derecho segun el tipo destino. */
    private Object valor(Tipo tp) {
        switch (tp) {
            case ENTERO:
            case REAL:
                return convertir(tp, aritmetica());
            case BOOLEANO:
                return booleano();
            default: // CADENA, CARACTER: literal, variable o llamada
                if (esLlamada()) return llamada();
                Token tok = consumir();
                return tok.getTipo() == IDENTIFICADOR ? leer(tok) : literal(tok);
        }
    }

    /** Asignacion a booleano: llamada | "!" b | "(" b ")" | verdadero/falso | variable. */
    private boolean booleano() {
        if (esLlamada()) return (Boolean) llamada();
        Token tok = consumir();
        switch (tok.getTipo()) {
            case NEGAR: return !booleano();
            case PARENTESIS_ABRE:
                boolean b = booleano();
                consumir(); // )
                return b;
            case IDENTIFICADOR: return (Boolean) leer(tok);
            default: return tok.getTipo() == PALABRA_RESERVADA_VER;
        }
    }

    /** Condicion: termino { ("&"|"|") termino }, asociativo a la izquierda. */
    private boolean logica() {
        boolean r = terminoLogico();
        while (tipo() == AND || tipo() == OR) {
            boolean y = consumir().getTipo() == AND;
            boolean d = terminoLogico();
            r = y ? (r && d) : (r || d);
        }
        return r;
    }

    private boolean terminoLogico() {
        if (tipo() == NEGAR) {
            consumir();
            if (tipo() == PARENTESIS_ABRE) {
                consumir();
                boolean r = logica();
                consumir(); // )
                return !r;
            }
            return !terminoLogico();
        }
        return comparacion();
    }

    private boolean esPrimarioBooleano() {
        Tokens tk = tipo();
        if (tk == PALABRA_RESERVADA_VER || tk == PALABRA_RESERVADA_FAL) return true;
        if (tk != IDENTIFICADOR) return false;
        if (esLlamada()) return funciones.get(t.get(pos).getLexema()).retorno == Tipo.BOOLEANO;
        Variable v = buscar(t.get(pos).getLexema());
        return v != null && v.tipo == Tipo.BOOLEANO;
    }

    private boolean primarioBooleano() {
        if (esLlamada()) return (Boolean) llamada();
        Token tok = consumir();
        if (tok.getTipo() == IDENTIFICADOR) return (Boolean) leer(tok);
        return tok.getTipo() == PALABRA_RESERVADA_VER;
    }

    /** Booleano [ ("=="|"!=") booleano ]  |  aritmetica op_relacional aritmetica. */
    private boolean comparacion() {
        if (esPrimarioBooleano()) {
            boolean a = primarioBooleano();
            if (tipo() == IGUAL || tipo() == DIFERENTE) {
                boolean igual = consumir().getTipo() == IGUAL;
                boolean b = primarioBooleano();
                return igual == (a == b);
            }
            return a;
        }
        double a = ((Number) aritmetica()).doubleValue();
        Tokens op = consumir().getTipo();
        double b = ((Number) aritmetica()).doubleValue();
        switch (op) {
            case MAYOR: return a > b;
            case MENOR: return a < b;
            case MAYOR_IGUAL: return a >= b;
            case MENOR_IGUAL: return a <= b;
            case IGUAL: return a == b;
            default: return a != b;
        }
    }

    private Object aritmetica() {
        Object r = termino();
        while (tipo() == OPERADOR_SUMA || tipo() == OPERADOR_RESTA) {
            Token op = consumir();
            r = operar(op.getTipo(), r, termino(), op);
        }
        return r;
    }

    private Object termino() {
        Object r = factor();
        while (tipo() == OPERADOR_MULTIPLICAR || tipo() == OPERADOR_DIVISION || tipo() == OPERADOR_MODULO) {
            Token op = consumir();
            r = operar(op.getTipo(), r, factor(), op);
        }
        return r;
    }

    private Object factor() {
        if (esLlamada()) return llamada();
        Token tok = consumir();
        switch (tok.getTipo()) {
            case PARENTESIS_ABRE:
                Object v = aritmetica();
                consumir(); // )
                return v;
            case IDENTIFICADOR: return leer(tok);
            default: return literal(tok);
        }
    }

    /** Entero op entero = entero (division truncada); si alguno es real, real. */
    private static Object operar(Tokens op, Object a, Object b, Token donde) {
        if (a instanceof Integer && b instanceof Integer) {
            int x = (Integer) a, y = (Integer) b;
            switch (op) {
                case OPERADOR_SUMA: return x + y;
                case OPERADOR_RESTA: return x - y;
                case OPERADOR_MULTIPLICAR: return x * y;
                default:
                    if (y == 0) throw new ErrorEjecucion("Division entre cero", donde.getLinea());
                    return op == OPERADOR_DIVISION ? x / y : x % y;
            }
        }
        double x = ((Number) a).doubleValue(), y = ((Number) b).doubleValue();
        switch (op) {
            case OPERADOR_SUMA: return x + y;
            case OPERADOR_RESTA: return x - y;
            case OPERADOR_MULTIPLICAR: return x * y;
            case OPERADOR_DIVISION: return x / y;
            default: return x % y;
        }
    }

    private Object literal(Token tok) {
        String lx = tok.getLexema();
        switch (tok.getTipo()) {
            case NUMERO_ENTERO: return Integer.parseInt(lx);
            case NUMERO_REAL: return Double.parseDouble(lx);
            case TIPO_CADENA: return lx.substring(1, lx.length() - 1);
            case TIPO_CARACTER: return lx.charAt(1);
            case PALABRA_RESERVADA_VER: return Boolean.TRUE;
            case PALABRA_RESERVADA_FAL: return Boolean.FALSE;
            default: throw new ErrorEjecucion("Valor no ejecutable: " + lx, tok.getLinea());
        }
    }

    private static boolean iguales(Object a, Object b) {
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private static String texto(Object v) {
        if (v instanceof Boolean) return (Boolean) v ? "verdadero" : "falso";
        return String.valueOf(v);
    }
}

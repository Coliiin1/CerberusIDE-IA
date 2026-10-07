package AnalizadorSintactico;

import AnalizadorLexico.Identificadores;
import AnalizadorLexico.Token;
import AnalizadorLexico.Tokens;
import static AnalizadorLexico.Tokens.*;
import AnalizadorSemantico.AnalizadorSemantico;
import AnalizadorSemantico.Tipo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import Util.Reporter;

public class AnalizadorSintactico {
    private Stack<Token> pilaParentesis;
    private List<Token> tokensDetectados;
    private ArrayList<Identificadores> tabla;
    private int posicion;
    private boolean resultado;
    private AnalizadorSemantico sem;
    private boolean existePrincipal = false;
    private final ArrayList<String> pilaScopes = new ArrayList<>();
    private int contadorBloques = 0;
    private Tipo tipoRetornoActual;
    private Documentador documentador;
    private Reporter reporter;
    private final Map<String, Firma> firmas = new HashMap<>();

    public AnalizadorSintactico(List<Token> tokensDetectados) {
        this(tokensDetectados, null);
    }

    public AnalizadorSintactico(List<Token> tokensDetectados, Reporter reporter) {
        this.pilaParentesis = new Stack<>();
        this.tokensDetectados = tokensDetectados;
        this.reporter = reporter;
        posicion=0;
        resultado=true;
        tabla=new ArrayList<>();
        sem=new AnalizadorSemantico();
        documentador=new Documentador();
    }

    /** Firma de una funcion: nombre, tipo de retorno (null = void) y tipos de parametros. */
    private static final class Firma {
        final String nombre;
        final Tipo retorno;
        final List<Tipo> parametros;

        Firma(String nombre, Tipo retorno, List<Tipo> parametros) {
            this.nombre = nombre;
            this.retorno = retorno;
            this.parametros = parametros;
        }
    }

    /** Valor de una expresion aritmetica con su tipo (entero o real). */
    private static final class Expresion {
        final double valor;
        final boolean esReal;

        Expresion(double valor, boolean esReal) {
            this.valor = valor;
            this.esReal = esReal;
        }
    }

    private void reportar(String mensaje) {
        if (reporter != null) {
            reporter.reportar(mensaje);
        }
    }

    /** Construye un ErrorSintactico apuntando al token actual. */
    private ErrorSintactico error(String mensaje) {
        return error(mensaje, tokenActual());
    }

    /** Construye un ErrorSintactico con el token indicado (fila/columna/lexema). */
    private ErrorSintactico error(String mensaje, Token tok) {
        if (tok == null) {
            return new ErrorSintactico(mensaje, -1, -1, null);
        }
        return new ErrorSintactico(mensaje, tok.getLinea(), tok.getColumna(), tok.getLexema());
    }

    /** Token actual, o el ultimo si la posicion se paso del final. */
    private Token tokenActual() {
        if (posicion >= 0 && posicion < tokensDetectados.size()) {
            return tokensDetectados.get(posicion);
        }
        if (!tokensDetectados.isEmpty()) {
            return tokensDetectados.get(tokensDetectados.size() - 1);
        }
        return null;
    }

    /**
     * Pre-escaneo de tokens para registrar las firmas de las funciones antes de
     * parsear los cuerpos, lo que permite llamadas hacia adelante desde principal.
     */
    private void recolectarFirmas() {
        firmas.clear();
        for (int i = 0; i < tokensDetectados.size(); i++) {
            if (tokensDetectados.get(i).getTipo() != PALABRA_RESERVADA_FUN) {
                continue;
            }
            int j = i + 1;
            if (j < tokensDetectados.size()
                    && tokensDetectados.get(j).getTipo() == PALABRA_RESERVADA_PRIN) {
                continue;
            }
            Tipo retorno = null;
            if (j < tokensDetectados.size()) {
                Tipo posible = sem.tipoDeToken(tokensDetectados.get(j).getTipo());
                if (posible != null) {
                    retorno = posible;
                    j++;
                } else if (tokensDetectados.get(j).getTipo() == PALABRA_RESERVADA_VAC) {
                    j++;
                }
            }
            if (j >= tokensDetectados.size()
                    || tokensDetectados.get(j).getTipo() != IDENTIFICADOR) {
                continue;
            }
            String nombre = tokensDetectados.get(j).getLexema();
            j++;
            if (j >= tokensDetectados.size()
                    || tokensDetectados.get(j).getTipo() != PARENTESIS_ABRE) {
                continue;
            }
            j++;
            List<Tipo> parametros = new ArrayList<>();
            while (j < tokensDetectados.size()
                    && tokensDetectados.get(j).getTipo() != PARENTESIS_CIERRA) {
                Tipo p = sem.tipoDeToken(tokensDetectados.get(j).getTipo());
                if (p != null) {
                    parametros.add(p);
                }
                j++;
            }
            firmas.put(nombre, new Firma(nombre, retorno, parametros));
        }
    }

    /** true si el token actual es una llamada: IDENTIFICADOR seguido de "(". */
    private boolean esLlamadaActual() {
        return posicion + 1 < tokensDetectados.size()
                && tokensDetectados.get(posicion).getTipo() == IDENTIFICADOR
                && tokensDetectados.get(posicion + 1).getTipo() == PARENTESIS_ABRE;
    }

    /**
     * Parsea IDENTIFICADOR "(" [ <ARGUMENTOS> ] ")" y devuelve el tipo de retorno
     * (null = void). Valida existencia, aridad y tipos de los argumentos.
     */
    private Tipo llamadaFuncion() {
        Token nombreToken = tokensDetectados.get(posicion);
        String nombre = nombreToken.getLexema();
        Firma firma = firmas.get(nombre);
        if (firma == null) {
            throw error("Funcion no encontrada: " + nombre, nombreToken);
        }
        posicion++;
        match(PARENTESIS_ABRE);
        List<Tipo> parametros = firma.parametros;
        int indice = 0;
        while (tokensDetectados.get(posicion).getTipo() != PARENTESIS_CIERRA) {
            if (indice >= parametros.size()) {
                throw error("La funcion " + nombre + " espera " + parametros.size()
                        + " argumentos y se recibieron mas");
            }
            argumento(parametros.get(indice), nombre, indice + 1);
            indice++;
            if (tokensDetectados.get(posicion).getTipo() == COMA) {
                posicion++;
            } else {
                break;
            }
        }
        match(PARENTESIS_CIERRA);
        if (indice != parametros.size()) {
            throw error("La funcion " + nombre + " espera " + parametros.size()
                    + " argumentos y se recibieron " + indice);
        }
        return firma.retorno;
    }

    /** Parsea un argumento segun el tipo esperado del parametro. */
    private void argumento(Tipo esperado, String funcion, int numero) {
        switch (esperado) {
            case ENTERO:
                Expresion entero = expresionAritmetica();
                if (entero.esReal) {
                    throw error("TIPO INCOMPATIBLE: argumento " + numero + " de "
                            + funcion + " esperaba ENTERO");
                }
                break;
            case REAL:
                expresionAritmetica();
                break;
            case CADENA:
                expresionCadena(funcion);
                break;
            case CARACTER:
                expresionCaracter(funcion);
                break;
            case BOOLEANO:
                expresionBooleana(funcion);
                break;
            default:
                throw error("TIPO DE PARAMETRO INVALIDO en " + funcion);
        }
    }

    /** Llamada usada como expresion numerica: exige retorno ENTERO/REAL. */
    private Expresion llamadaNumerica() {
        Token tok = tokensDetectados.get(posicion);
        Tipo retorno = llamadaFuncion();
        if (retorno == Tipo.ENTERO) {
            return new Expresion(0, false);
        }
        if (retorno == Tipo.REAL) {
            return new Expresion(0, true);
        }
        String desc = (retorno == null) ? "vacio" : retorno.toString();
        throw error("TIPO INCOMPATIBLE: la funcion " + tok.getLexema()
                + " retorna " + desc + " y se usa como valor numerico", tok);
    }

    /** Llamada usada como valor de un tipo concreto; devuelve un placeholder. */
    private String llamadaTipo(Tipo esperado, String contexto, String placeholder) {
        Token tok = tokensDetectados.get(posicion);
        Tipo retorno = llamadaFuncion();
        if (retorno != esperado) {
            String desc = (retorno == null) ? "vacio" : retorno.toString();
            throw error("TIPO INCOMPATIBLE: la funcion " + tok.getLexema()
                    + " retorna " + desc + " y no " + esperado + " en " + contexto, tok);
        }
        return placeholder;
    }

    private void entrarScope(String nombre) {
        pilaScopes.add(nombre);
    }

    private void salirScope() {
        if (!pilaScopes.isEmpty()) {
            pilaScopes.remove(pilaScopes.size() - 1);
        }
    }

    private String scopeActual() {
        return String.join("/", pilaScopes);
    }

    private void entrarBloque() {
        entrarScope("b" + (++contadorBloques));
    }

    private void salirBloque() {
        salirScope();
    }

    private boolean esGlobal() {
        return pilaScopes.size() == 1 && pilaScopes.get(0).equals("global");
    }
    
    
    public boolean comprobarParentesis(){
        Token tok;
        for(Token token: tokensDetectados){
            switch (token.getTipo()) {
                case PARENTESIS_ABRE:
                case CORCHETE_ABRE:
                case LLAVE_ABRE:
                    pilaParentesis.push(token);
                    break;
                case PARENTESIS_CIERRA:
                case CORCHETE_CIERRA:
                case LLAVE_CIERRA:
                    tok=pilaParentesis.peek();
                    if (tok.getTipo()==PARENTESIS_ABRE&&token.getTipo()==PARENTESIS_CIERRA || tok.getTipo()==CORCHETE_ABRE&&token.getTipo()==CORCHETE_CIERRA || tok.getTipo()==LLAVE_ABRE&&token.getTipo()==LLAVE_CIERRA) {
                        pilaParentesis.pop();
                    }else{
                        reportar("HAY UN PROBLEMA CON: "+tok);
                        return false;
                    }
                default:
            }
        }
        mostrarParentesis();
        if (pilaParentesis.size()==0) {
            return true;
        }else{
            reportar("hubo un problema con "+ pilaParentesis.peek().getTipo().name());
            return false;
        }
    }
    
    public String mostrarPeekParentesis(){
        Token token=pilaParentesis.peek();
        return token.getTipo().name();
    }
    
    public void mostrarParentesis(){
        for(Token token: pilaParentesis){
            System.out.println(token.getTipo().name());
        }
    }
    
    public boolean analizar(){
        try{
            recolectarFirmas();
            programa();
            return true;
        }catch(ErrorSintactico e){
            reportar("OCURRIO UN ERROR: "+e.getMessage()
                +"\nEn la linea: "+e.getFila()
                +"\nColumna: "+e.getColumna()
                +"\nLexema: "+e.getLexema());
        }catch(RuntimeException | AssertionError e){
            reportar("OCURRIO UN ERROR: "+e.getMessage());
        }
        falso();
        return false;
    }
    
    public boolean match(Tokens esperado){
        if (posicion>=tokensDetectados.size()) {
            System.out.println("ERROR");
            return false;
        }
        if (tokensDetectados.get(posicion).getTipo()==esperado) {
            posicion++;
        }else{
            System.out.println("SE ESPERABA "+esperado.name()+" Y SE ENCONTRO: "+tokensDetectados.get(posicion).getTipo().name());
            falso();
             throw error(
            "Se esperaba "
            + esperado.name()
            + " y se encontró "
            + tokensDetectados.get(posicion).getTipo().name()+"\nen la linea: "
            + tokensDetectados.get(posicion).getLinea()+"\nen: "
            + tokensDetectados.get(posicion).getLexema()
        );
        }
        return true;
    }
    
    public void mostrarEsperado(Tokens esperado){
        reportar("NO SE ENCONTRO EL: "+esperado.name()+"\nEn su lugar se encontro: "+tokensDetectados.get(posicion).getTipo().name());
    }
    //mis gramaticas 
    private void programa(){
        entrarScope("global");
        globales();
        inicio();
        funciones();
        match(LLAVE_CIERRA);
        if(posicion<tokensDetectados.size()){
            throw error("Codigo fuera de la clase");
        }
        salirScope();
    }

    private void inicio(){
        match(PALABRA_RESERVADA_CLA);
        match(IDENTIFICADOR);
        String clase=tokensDetectados.get(posicion-1).getLexema();
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA CLASE LLAMADA: "+clase);
    }
    private void globales(){
        while(true){
            switch((tokensDetectados.get(posicion).getTipo())){
                case PALABRA_RESERVADA_ENT:
                case PALABRA_RESERVADA_REA:
                case PALABRA_RESERVADA_CAD:
                case PALABRA_RESERVADA_CAR:
                case PALABRA_RESERVADA_BOO:
                    declaracion(tokensDetectados.get(posicion).getTipo());
                    break;
                default:
                    return;
            }
        }
    }
    private void funciones(){
        while(tokensDetectados.get(posicion).getTipo()== PALABRA_RESERVADA_FUN){
            match(PALABRA_RESERVADA_FUN);
            funcion();
        }
        if(!existePrincipal){
            throw error("No se encontro una funcion principal");
        }
    }
    private void funcion(){
        if(tokensDetectados.get(posicion).getTipo()== PALABRA_RESERVADA_PRIN){
            principal();
        }else{
            funcionComun();
        }
    }
    private void funcionComun(){        
        Tipo tipoRetorno = leerTipoRetorno();
        match(IDENTIFICADOR);
        String funcion=tokensDetectados.get(posicion-1).getLexema();
        entrarScope(funcion);
        parametros();
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA FUNCION LLAMADA: "+funcion);
        tipoRetornoActual = tipoRetorno;
        instrucciones();
        salirScope();
        tipoRetornoActual = null;
        match(LLAVE_CIERRA);
        
    }

    /** Parametros: "(" [ <TIPO> IDENTIFICADOR { "," <TIPO> IDENTIFICADOR } ] ")". */
    private void parametros(){
        match(PARENTESIS_ABRE);
        while (tokensDetectados.get(posicion).getTipo() != PARENTESIS_CIERRA) {
            Token tipoParam = tokensDetectados.get(posicion);
            switch (tipoParam.getTipo()) {
                case PALABRA_RESERVADA_ENT:
                case PALABRA_RESERVADA_REA:
                case PALABRA_RESERVADA_CAD:
                case PALABRA_RESERVADA_CAR:
                case PALABRA_RESERVADA_BOO:
                    posicion++;
                    break;
                default:
                    throw error("Se esperaba un tipo de parametro");
            }
            match(IDENTIFICADOR);
            String nombre = tokensDetectados.get(posicion-1).getLexema();
            agregarParametro(tipoParam.getTipo(), nombre);
            if (tokensDetectados.get(posicion).getTipo() == COMA) {
                posicion++;
            }
        }
        match(PARENTESIS_CIERRA);
    }

    private void agregarParametro(Tokens tipo, String nombre){
        if (sem.existeEnScope(tabla, nombre, scopeActual())) {
            throw error("YA EXISTE ESE IDENTIFICADOR: "+nombre);
        }
        Identificadores parametro = new Identificadores(tipo, nombre, scopeActual());
        parametro.setValor(valorPlaceholder(tipo));
        tabla.add(parametro);
    }

    /**
     * Valor placeholder para un parametro (no recibe valor real). Permite usarlo
     * en expresiones sin disparar "Variable sin valor".
     */
    private String valorPlaceholder(Tokens tipo){
        switch (tipo) {
            case PALABRA_RESERVADA_ENT: return "0";
            case PALABRA_RESERVADA_REA: return "0.0";
            case PALABRA_RESERVADA_BOO: return "verdadero";
            case PALABRA_RESERVADA_CAD:
            case PALABRA_RESERVADA_CAR: return "";
            default: return null;
        }
    }

    /** Lee el tipo de retorno opcional tras "funcion". Devuelve null si es void. */
    private Tipo leerTipoRetorno(){
        Token actual = tokensDetectados.get(posicion);
        switch (actual.getTipo()) {
            case PALABRA_RESERVADA_ENT:
            case PALABRA_RESERVADA_REA:
            case PALABRA_RESERVADA_CAD:
            case PALABRA_RESERVADA_CAR:
            case PALABRA_RESERVADA_BOO:
                posicion++;
                return sem.tipoDeToken(actual.getTipo());
            case PALABRA_RESERVADA_VAC:
                posicion++;
                return null;
            default:
                return null;
        }
    }

    private void principal(){
        if(existePrincipal){
            throw error("Ya existe una funcion principal");
        }
        existePrincipal = true;
        match(PALABRA_RESERVADA_PRIN);
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA LA FUNCION PRINCIPAL DEL CODIGO");
        tipoRetornoActual = null;
        entrarScope("principal");
        instrucciones();
        salirScope();
        tipoRetornoActual = null;
        match(LLAVE_CIERRA);
        
    }
    
    private void instrucciones() {
        while(tokensDetectados.get(posicion).getTipo()!= LLAVE_CIERRA){
            instruccion();
        }
    }

    //cada pieza del codigo posible
    private void instruccion(){
        Token actual=tokensDetectados.get(posicion);
        switch (actual.getTipo()) {
            case PALABRA_RESERVADA_ENT: case PALABRA_RESERVADA_REA: case PALABRA_RESERVADA_CAD: case PALABRA_RESERVADA_CAR: case PALABRA_RESERVADA_BOO:
                declaracion(actual.getTipo());
                break;
            case PALABRA_RESERVADA_NUE:
            case PALABRA_RESERVADA_EST:
            case PALABRA_RESERVADA_PUB:
            case PALABRA_RESERVADA_PRI:
                throw error("POO no implementada en v1: " + actual.getLexema());
            case PALABRA_RESERVADA_FUN:
                funciones();
                break;
            case PALABRA_RESERVADA_PRIN:
                principal();
                break;
            case PALABRA_RESERVADA_IMP:
                imprimir();
                break;
            case PALABRA_RESERVADA_SI:
                si();
                break;
            case PALABRA_RESERVADA_PAR:
                para();
                break;
            case PARENTESIS_CIERRA:
                System.out.println("DETECTA EL PARETENTESIS QUE CIERRA");
                break;
            case PALABRA_RESERVADA_MIE:
                mientras();
                match(LLAVE_ABRE);
                entrarBloque();
                instruccionesSi(LLAVE_CIERRA);
                salirBloque();
                match(LLAVE_CIERRA);
                break;
            case PALABRA_RESERVADA_HAC:
                hacer();
                break;
            case PALABRA_RESERVADA_SEG:
                segun();
                break;
            case PALABRA_RESERVADA_RET:
                retornar();
                break;
            case IDENTIFICADOR:

                if (esLlamadaActual()) {
                    String nombreLlamada = tokensDetectados.get(posicion).getLexema();
                    llamadaFuncion();
                    match(PUNTO_COMA);
                    documentador.agregar(
                        obtenerLinea(tokensDetectados.get(posicion-1))
                        + "SE LLAMA A LA FUNCION: " + nombreLlamada
                    );
                    break;
                }

                if (posicion + 1 < tokensDetectados.size()
                        && tokensDetectados.get(posicion + 1).getTipo() == PUNTO) {
                    throw error("POO no implementada en v1: acceso a miembro");
                }

                String iden =
                    tokensDetectados.get(posicion).getLexema();

                asignacionDirecta();

                Identificadores variable =
                    sem.buscarIde(tabla, iden, scopeActual());

                match(PUNTO_COMA);

                if(variable != null){

                    documentador.agregar(
                        obtenerLinea(tokensDetectados.get(posicion-1))
                        + "SE ASIGNA EL VALOR: "
                        + variable.getValor()
                        + " A LA VARIABLE "
                        + variable.getIdentificador()
                    );

                }

                break;
            default:
                throw error("No se reconoce la instruccion");
        }
    }
    
    private void hacer(){
        match(PALABRA_RESERVADA_HAC);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CICLO HACER MIENTRAS QUE EVALUARA EL SIGUIENTE MIENTRAS ");
        entrarBloque();
        instruccionesSi(LLAVE_CIERRA);
        salirBloque();
        match(LLAVE_CIERRA);
        mientras();
        match(PUNTO_COMA);
        
    }
    
    private void declaracion(Tokens tipo){
        String identificador;
        if (tokensDetectados.get(posicion + 1).getTipo() == CORCHETE_ABRE) {
            throw error("Arreglos no implementados en v1");
        }
        agregarTabla();
        tipo();
        match(IDENTIFICADOR);
        identificador=tokensDetectados.get(posicion-1).getLexema();
        if (tokensDetectados.get(posicion).getTipo()==ASIGNACION) {
            asignacion(tokensDetectados.get(posicion-2).getTipo(),identificador);
        }
        match(PUNTO_COMA);
        Identificadores declarado = sem.buscarIde(tabla, identificador, scopeActual());
        if(esGlobal()){
            documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA VARIABLE GLOBAL LLAMADA: "+identificador+" DE TIPO: "+declarado.getTipo()
                    +" CON VALOR: "+declarado.getValor());
        }else{
            documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA VARIABLE LLAMADA: "+identificador+" DE TIPO: "+declarado.getTipo()
                    +" CON VALOR: "+declarado.getValor());
        }

    }
    
    private void asignacionDirecta(){
        match(IDENTIFICADOR);
        String nombre = tokensDetectados.get(posicion-1).getLexema();
        Token siguiente = tokensDetectados.get(posicion);
        if (siguiente.getTipo()==ASIGNACION) {
            if (!sem.buscar(tabla, nombre, scopeActual())) {
                throw error("NO SE ENCONTRO EL IDENTIFICADOR: "+nombre);
            }
            Tokens tok= sem.buscarIde(tabla, nombre, scopeActual()).getToken();
            asignacion(tok,nombre);
        } else if (siguiente.getTipo()==MAS_VARIABLE || siguiente.getTipo()==MENOS_VARIABLE
                || siguiente.getTipo()==MUL_VARIABLE || siguiente.getTipo()==DIV_VARIABLE) {
            asignacionCompuesta(nombre);
        }
    }

    /** Asignacion compuesta: IDENTIFICADOR ( "+=" | "-=" | "*=" | "/=" ) <EXPRESION_ARITMETICA> ";". */
    private void asignacionCompuesta(String identificador){
        Token operador = tokensDetectados.get(posicion);
        posicion++;
        Identificadores ide = sem.buscarIde(tabla, identificador, scopeActual());
        if (ide == null) {
            throw error("NO SE ENCONTRO EL IDENTIFICADOR: "+identificador);
        }
        Tipo tipo = sem.tipoDeIdentificador(ide);
        if (tipo != Tipo.ENTERO && tipo != Tipo.REAL) {
            throw error("TIPO INCOMPATIBLE: la asignacion compuesta solo aplica a ENTERO/REAL en "+identificador);
        }
        if (ide.getValor() == null) {
            throw error("Variable sin valor: "+identificador);
        }
        double base = Double.parseDouble(ide.getValor());
        Expresion valor = expresionAritmetica();
        if (tipo == Tipo.ENTERO && valor.esReal) {
            throw error("TIPO INCOMPATIBLE: no se puede asignar REAL a ENTERO en "+identificador);
        }
        double resultado;
        switch (operador.getTipo()) {
            case MAS_VARIABLE: resultado = base + valor.valor; break;
            case MENOS_VARIABLE: resultado = base - valor.valor; break;
            case MUL_VARIABLE: resultado = base * valor.valor; break;
            default: resultado = base / valor.valor; break;
        }
        if (tipo == Tipo.ENTERO) {
            asignarValor(identificador, String.valueOf((int)resultado));
        } else {
            asignarValor(identificador, String.valueOf(resultado));
        }
    }
    
    private void asignacion(Tokens variable,String identificador){
        match(ASIGNACION);
        switch (variable) {
            case PALABRA_RESERVADA_ENT:
                Expresion valorEntero = expresionAritmetica();
                if (valorEntero.esReal) {
                    throw error("TIPO INCOMPATIBLE: no se puede asignar REAL a ENTERO en "+identificador);
                }
                asignarValor(identificador,String.valueOf((int)valorEntero.valor));
                break;
            case PALABRA_RESERVADA_REA:
                Expresion valorReal = expresionAritmetica();
                asignarValor(identificador,String.valueOf(valorReal.valor));
                break;
            case PALABRA_RESERVADA_CAD:
                asignarValor(identificador,expresionCadena(identificador));
                break;
            case PALABRA_RESERVADA_CAR:
                asignarValor(identificador,expresionCaracter(identificador));
                break;
            case PALABRA_RESERVADA_BOO:
                asignarValor(identificador,expresionBooleana(identificador));
                break;
            default:
                throw error("TIPO INVALIDO");
        }
    }

    /** Lado derecho de una asignacion a cadena: literal, variable o llamada de tipo cadena. */
    private String expresionCadena(String identificador){
        Token actual = tokensDetectados.get(posicion);
        if (esLlamadaActual()) {
            return llamadaTipo(Tipo.CADENA, identificador, "");
        }
        if (actual.getTipo() == TIPO_CADENA) {
            posicion++;
            return actual.getLexema();
        }
        if (actual.getTipo() == IDENTIFICADOR) {
            posicion++;
            Identificadores ide = sem.buscarIde(tabla, actual.getLexema(), scopeActual());
            if (ide == null) {
                throw error("Variable no encontrada: "+ actual.getLexema());
            }
            if (sem.tipoDeIdentificador(ide) != Tipo.CADENA) {
                throw error("TIPO INCOMPATIBLE: no se puede asignar "+ide.getTipo()+" a CADENA en "+identificador);
            }
            return ide.getValor();
        }
        throw error("TIPO INCOMPATIBLE: se esperaba CADENA en "+identificador);
    }

    /** Lado derecho de una asignacion a caracter: literal, variable o llamada de tipo caracter. */
    private String expresionCaracter(String identificador){
        Token actual = tokensDetectados.get(posicion);
        if (esLlamadaActual()) {
            return llamadaTipo(Tipo.CARACTER, identificador, "");
        }
        if (actual.getTipo() == TIPO_CARACTER) {
            posicion++;
            return actual.getLexema();
        }
        if (actual.getTipo() == IDENTIFICADOR) {
            posicion++;
            Identificadores ide = sem.buscarIde(tabla, actual.getLexema(), scopeActual());
            if (ide == null) {
                throw error("Variable no encontrada: "+ actual.getLexema());
            }
            if (sem.tipoDeIdentificador(ide) != Tipo.CARACTER) {
                throw error("TIPO INCOMPATIBLE: no se puede asignar "+ide.getTipo()+" a CARACTER en "+identificador);
            }
            return ide.getValor();
        }
        throw error("TIPO INCOMPATIBLE: se esperaba CARACTER en "+identificador);
    }

    /** Lado derecho de una asignacion a booleano: verdadero/falso, variable, negacion o llamada. */
    private String expresionBooleana(String identificador){
        Token actual = tokensDetectados.get(posicion);
        if (esLlamadaActual()) {
            return llamadaTipo(Tipo.BOOLEANO, identificador, "verdadero");
        }
        if (actual.getTipo() == NEGAR) {
            posicion++;
            return negarBooleano(expresionBooleana(identificador));
        }
        if (actual.getTipo() == PARENTESIS_ABRE) {
            posicion++;
            String valor = expresionBooleana(identificador);
            match(PARENTESIS_CIERRA);
            return valor;
        }
        if (actual.getTipo() == PALABRA_RESERVADA_VER || actual.getTipo() == PALABRA_RESERVADA_FAL) {
            posicion++;
            return actual.getLexema();
        }
        if (actual.getTipo() == IDENTIFICADOR) {
            posicion++;
            Identificadores ide = sem.buscarIde(tabla, actual.getLexema(), scopeActual());
            if (ide == null) {
                throw error("Variable no encontrada: "+ actual.getLexema());
            }
            if (sem.tipoDeIdentificador(ide) != Tipo.BOOLEANO) {
                throw error("TIPO INCOMPATIBLE: no se puede asignar "+ide.getTipo()+" a BOOLEANO en "+identificador);
            }
            if (ide.getValor() == null) {
                throw error("Variable sin valor: "+ actual.getLexema());
            }
            return ide.getValor();
        }
        throw error("TIPO INCOMPATIBLE: se esperaba BOOLEANO en "+identificador);
    }

    private String negarBooleano(String valor){
        return "verdadero".equals(valor) ? "falso" : "verdadero";
    }
    
    private void tipo(){
        Token actual = tokensDetectados.get(posicion);
        switch (actual.getTipo()) {
            case PALABRA_RESERVADA_ENT: case PALABRA_RESERVADA_REA: case PALABRA_RESERVADA_CAD: case PALABRA_RESERVADA_CAR: case PALABRA_RESERVADA_BOO:
                posicion++;
                break;
            default:
                System.out.println("TIPO NO RECONOCIDO");
        }
    }
    public void asignarValor(String identificador, String valor){
        Identificadores iden = sem.buscarIde(tabla, identificador, scopeActual());
        if (iden != null) {
            iden.setValor(valor);
        }
    }
    
    
    public void imprimir(){
        String valor="";
        match(PALABRA_RESERVADA_IMP);
        match(PARENTESIS_ABRE);
        valor=expresionImprimible();
        while (tokensDetectados.get(posicion).getTipo()==OPERADOR_SUMA
                || tokensDetectados.get(posicion).getTipo()==COMA) {
            posicion++;
            valor=valor+expresionImprimible();
        }
        match(PARENTESIS_CIERRA);
        match(PUNTO_COMA);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE IMPRIME: "+valor);
    }
    private String expresionImprimible(){
        String resul="";
        if (esLlamadaActual()) {
            Token tok = tokensDetectados.get(posicion);
            Tipo retorno = llamadaFuncion();
            if (retorno == null) {
                throw error("TIPO INCOMPATIBLE: la funcion " + tok.getLexema()
                        + " no retorna un valor", tok);
            }
            return "";
        }
        switch (tokensDetectados.get(posicion).getTipo()) {
            case IDENTIFICADOR: case TIPO_CADENA: case TIPO_CARACTER: case NUMERO_REAL: case NUMERO_ENTERO:
            case PALABRA_RESERVADA_VER: case PALABRA_RESERVADA_FAL:
                resul=tokensDetectados.get(posicion).getLexema();
                posicion++;
                break;
            default:
                falso();
                throw error("NO SE RECONOCE EL TIPO: "+mostrarLineaError(tokensDetectados.get(posicion)));
        }
        return resul;
    }
    public void mientras(){
        match(PALABRA_RESERVADA_MIE);
        match(PARENTESIS_ABRE);
        int x=posicion;
        expresionLogica();
        match(PARENTESIS_CIERRA);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CICLO MIENTRAS QUE EVALUA LA EXPRESION lOGICA: "+recorrerGenerarCadena(x));
    }
    
    public void si(){
        match(PALABRA_RESERVADA_SI);
        match(PARENTESIS_ABRE);
        int x=posicion;
        expresionLogica();
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA CONDICION SI QUE EVALUA: "+recorrerGenerarCadena(x));
        entrarBloque();
        instruccionesSi(LLAVE_CIERRA);
        salirBloque();
        match(LLAVE_CIERRA);
        if(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_SIN){
            sino();
        }
    }
    
    private void sino(){
        match(PALABRA_RESERVADA_SIN);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA SALIDA EN CASO QUE EL SI RETORNE FALSO");
        switch (tokensDetectados.get(posicion).getTipo()) {
            case PALABRA_RESERVADA_SI:
                si();
                break;
            case LLAVE_ABRE:
                match(LLAVE_ABRE);
                entrarBloque();
                instruccionesSi(LLAVE_CIERRA);
                salirBloque();
                match(LLAVE_CIERRA);
                break;
            default:
                throw error("PROBELAMS EN IF");
        }
    }
    public void segun(){
        match(PALABRA_RESERVADA_SEG);
        match(PARENTESIS_ABRE);
        match(IDENTIFICADOR);
        String identificador=tokensDetectados.get(posicion-1).getLexema();
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN SEGUN QUE EJECUTARA UN BUCLE PARA EL VALOR DE: "+identificador);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        if (!sem.buscar(tabla, identificador, scopeActual())) {
            resultado=false;
            reportar("no se encontro el identificador "+identificador);
            return;
        }
        entrarBloque();
        while(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_CAS){
            casos(sem.retornarTipo(sem.buscarIde(tabla, identificador, scopeActual())));
        }
        if(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_PRE){
            predeterminado();
        }
        salirBloque();
        match(LLAVE_CIERRA);
    }
    
    public void casos(Tokens tipo){
        match(PALABRA_RESERVADA_CAS);
        match(tipo);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CASO PARA EL VALOR: "+tokensDetectados.get(posicion-1).getLexema());
        match(DOS_PUNTOS);
        cuerpoCaso();
    }

    /** Caso por defecto de un segun: solo puede ir al final. */
    private void predeterminado(){
        match(PALABRA_RESERVADA_PRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CASO PREDETERMINADO");
        match(DOS_PUNTOS);
        cuerpoCaso();
    }

    /** Instrucciones de un caso hasta "salir" ";". */
    private void cuerpoCaso(){
        while(tokensDetectados.get(posicion).getTipo()!=PALABRA_RESERVADA_SAL){
            instruccion();
        }
        match(PALABRA_RESERVADA_SAL);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE TERMINA EL CASO");
        match(PUNTO_COMA);
    }

    /** Sentencia "retornar" [ <expresion> ] ";". */
    private void retornar(){
        match(PALABRA_RESERVADA_RET);
        boolean sinValor = tokensDetectados.get(posicion).getTipo()==PUNTO_COMA;
        if (sinValor) {
            if (tipoRetornoActual != null) {
                throw error("TIPO INCOMPATIBLE: falta el valor de retorno en funcion "+tipoRetornoActual);
            }
            match(PUNTO_COMA);
            return;
        }
        if (tipoRetornoActual == null) {
            throw error("TIPO INCOMPATIBLE: la funcion no retorna valor");
        }
        switch (tipoRetornoActual) {
            case ENTERO:
                Expresion valorEntero = expresionAritmetica();
                if (valorEntero.esReal) {
                    throw error("TIPO INCOMPATIBLE: no se puede retornar REAL en funcion ENTERO");
                }
                break;
            case REAL:
                expresionAritmetica();
                break;
            case CADENA:
                expresionCadena("retorno");
                break;
            case CARACTER:
                expresionCaracter("retorno");
                break;
            case BOOLEANO:
                expresionBooleana("retorno");
                break;
            default:
                throw error("TIPO INCOMPATIBLE");
        }
        match(PUNTO_COMA);
    }
    public void para(){
        match(PALABRA_RESERVADA_PAR);
        match(PARENTESIS_ABRE);
        int x=posicion;
        entrarBloque();
        declaracionPara();
        expresionAritmetica();
        operadorRelacional();
        expresionAritmetica();
        match(PUNTO_COMA);
        match(IDENTIFICADOR);
        if (tokensDetectados.get(posicion).getTipo()==INCREMENTO) {
            posicion++;
        } else if (tokensDetectados.get(posicion).getTipo()==DECREMENTO) {
            posicion++;
        } else {
            throw error("Se esperaba ++ o -- en el para");
        }
        match(PARENTESIS_CIERRA );
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN BUCLE DE TIPO PARA CON LAS REGLAS: "+recorrerGenerarCadena(x));
        match(LLAVE_ABRE);
        entrarBloque();
        instruccionesSi(LLAVE_CIERRA);
        salirBloque();
        match(LLAVE_CIERRA);
        salirBloque();
    }

    /** Contador del para: <entero|real> IDENTIFICADOR "=" <EXPRESION_ARITMETICA> ";". */
    private void declaracionPara(){
        Token tipoToken = tokensDetectados.get(posicion);
        Tipo tipo;
        switch (tipoToken.getTipo()) {
            case PALABRA_RESERVADA_ENT:
                tipo = Tipo.ENTERO;
                break;
            case PALABRA_RESERVADA_REA:
                tipo = Tipo.REAL;
                break;
            default:
                throw error("El contador del para debe ser entero o real");
        }
        posicion++;
        match(IDENTIFICADOR);
        String nombre = tokensDetectados.get(posicion-1).getLexema();
        match(ASIGNACION);
        Expresion inicial = expresionAritmetica();
        if (tipo == Tipo.ENTERO && inicial.esReal) {
            throw error("TIPO INCOMPATIBLE: no se puede asignar REAL a ENTERO en "+nombre);
        }
        match(PUNTO_COMA);
        String valorInicial = (tipo == Tipo.ENTERO)
                ? String.valueOf((int)inicial.valor)
                : String.valueOf(inicial.valor);
        agregarVariable(tipoToken.getTipo(), nombre, valorInicial);
    }

    /** Registra una variable con valor inicial en el scope actual. */
    private void agregarVariable(Tokens tipo, String nombre, String valor){
        if (sem.existeEnScope(tabla, nombre, scopeActual())) {
            throw error("YA EXISTE ESE IDENTIFICADOR: "+nombre);
        }
        Identificadores id = new Identificadores(tipo, nombre, scopeActual());
        id.setValor(valor);
        tabla.add(id);
    }
    
    
    private void expresionLogica(){
        terminoLogico();
        while (tokensDetectados.get(posicion).getTipo()==AND || tokensDetectados.get(posicion).getTipo()==OR) {
            posicion++;
            terminoLogico();
        }
    }

    /** Termino logico: negacion (con o sin parentesis) o condicion. */
    private void terminoLogico(){
        if (tokensDetectados.get(posicion).getTipo()==NEGAR) {
            posicion++;
            if (tokensDetectados.get(posicion).getTipo()==PARENTESIS_ABRE) {
                match(PARENTESIS_ABRE);
                expresionLogica();
                match(PARENTESIS_CIERRA);
            } else {
                terminoLogico();
            }
            return;
        }
        condicion();
    }
    
    private void condicion(){
        if (esPrimarioBooleano()) {
            primarioBooleano();
            Token actual = tokensDetectados.get(posicion);
            if (actual.getTipo()==IGUAL || actual.getTipo()==DIFERENTE) {
                posicion++;
                primarioBooleano();
            }
            return;
        }
        expresionAritmetica();

        operadorRelacional();

        expresionAritmetica();
    }

    /** true si el token actual es verdadero/falso, un identificador booleano o una llamada booleana. */
    private boolean esPrimarioBooleano(){
        Token actual = tokensDetectados.get(posicion);
        if (actual.getTipo()==PALABRA_RESERVADA_VER || actual.getTipo()==PALABRA_RESERVADA_FAL) {
            return true;
        }
        if (actual.getTipo()==IDENTIFICADOR) {
            if (esLlamadaActual()) {
                Firma firma = firmas.get(actual.getLexema());
                return firma != null && firma.retorno == Tipo.BOOLEANO;
            }
            Identificadores ide = sem.buscarIde(tabla, actual.getLexema(), scopeActual());
            return ide != null && sem.tipoDeIdentificador(ide) == Tipo.BOOLEANO;
        }
        return false;
    }

    /** Consume verdadero/falso, un identificador booleano o una llamada booleana. */
    private void primarioBooleano(){
        Token actual = tokensDetectados.get(posicion);
        if (actual.getTipo()==PALABRA_RESERVADA_VER || actual.getTipo()==PALABRA_RESERVADA_FAL) {
            posicion++;
            return;
        }
        if (actual.getTipo()==IDENTIFICADOR) {
            if (esLlamadaActual()) {
                llamadaTipo(Tipo.BOOLEANO, "condicion", "verdadero");
                return;
            }
            Identificadores ide = sem.buscarIde(tabla, actual.getLexema(), scopeActual());
            if (ide == null) {
                throw error("Variable no encontrada: "+ actual.getLexema());
            }
            if (sem.tipoDeIdentificador(ide) != Tipo.BOOLEANO) {
                throw error("TIPO INCOMPATIBLE: "+actual.getLexema()+" no es booleano");
            }
            posicion++;
            return;
        }
        throw error("TIPO INCOMPATIBLE: se esperaba un valor booleano");
    }
    
    private void operadorRelacional(){
        Token actual = tokensDetectados.get(posicion);

    switch(actual.getTipo()){
        case MAYOR:
        case MENOR:
        case MAYOR_IGUAL:
        case MENOR_IGUAL:
        case IGUAL:
        case DIFERENTE:
            posicion++;
            break;
        default:
            throw error("Operador relacional inválido");
        }
    }
    private Expresion expresionAritmetica(){
        Expresion actual = termino();
        while(
            tokensDetectados.get(posicion).getTipo()==OPERADOR_SUMA ||tokensDetectados.get(posicion).getTipo()==OPERADOR_RESTA){
            Tokens operador =tokensDetectados.get(posicion).getTipo();
            posicion++;
            Expresion siguiente = termino();
            boolean esReal = actual.esReal || siguiente.esReal;
            double valor;
            if(operador == OPERADOR_SUMA){
                valor = actual.valor + siguiente.valor;
            }else{
                valor = actual.valor - siguiente.valor;
            }
            actual = new Expresion(valor, esReal);
        }
        return actual;
    }
    
    private Expresion termino(){
        Expresion actual = factor();
        while(
            tokensDetectados.get(posicion).getTipo()==OPERADOR_MULTIPLICAR ||tokensDetectados.get(posicion).getTipo()==OPERADOR_DIVISION ||tokensDetectados.get(posicion).getTipo()==OPERADOR_MODULO){
            Tokens operador =tokensDetectados.get(posicion).getTipo();
            posicion++;
            Expresion siguiente = factor();
            boolean esReal = actual.esReal || siguiente.esReal;
            double valor;
            switch(operador){
                case OPERADOR_MULTIPLICAR:
                    valor = actual.valor * siguiente.valor;
                    break;
                case OPERADOR_DIVISION:
                    valor = actual.valor / siguiente.valor;
                    break;
                default:
                    valor = actual.valor % siguiente.valor;
                    break;
            }
            actual = new Expresion(valor, esReal);
        }
        return actual;
    }
    
    private Expresion factor(){
        Token actual = tokensDetectados.get(posicion);
        switch(actual.getTipo()){
            case NUMERO_ENTERO:
                posicion++;
                return new Expresion(Double.parseDouble(actual.getLexema()), false);
            case NUMERO_REAL:
                posicion++;
                return new Expresion(Double.parseDouble(actual.getLexema()), true);
            case IDENTIFICADOR:
                if (esLlamadaActual()) {
                    return llamadaNumerica();
                }
                posicion++;
                Identificadores ide =
                sem.buscarIde(tabla, actual.getLexema(), scopeActual());
                if(ide == null){
                    throw error("Variable no encontrada: "+ actual.getLexema());
                }
                Tipo tipo = sem.tipoDeIdentificador(ide);
                if(tipo != Tipo.ENTERO && tipo != Tipo.REAL){
                    throw error("TIPO INCOMPATIBLE: la variable "+actual.getLexema()+" no es numerica");
                }
                if(ide.getValor() == null){
                    throw error("Variable sin valor: "+ actual.getLexema());
                }
                return new Expresion(Double.parseDouble(ide.getValor()), tipo == Tipo.REAL);
            case PARENTESIS_ABRE:
                match(PARENTESIS_ABRE);
                Expresion valor = expresionAritmetica();
                match(PARENTESIS_CIERRA);
                return valor;
            case PALABRA_RESERVADA_NUE:
                throw error("POO no implementada en v1: nuevo");
            default:
                throw error("Factor invalido");
        }
}
    
    
    //partes de funcionalidad de mi codigo 
    public void instruccionesSi(Tokens token){
        while(tokensDetectados.get(posicion).getTipo()!=token){
            instruccion();
        }
    }
    
    public boolean encontrarIdentificador(String nombreIdentificador){
        for(Identificadores ide: tabla){
            if (ide.getIdentificador().equals(nombreIdentificador)) {
                return true;
            }
        }
        return false;
    }
    public String imprimirTabla(){
        String resultado="";
        for(Identificadores iden:tabla){
            resultado+=iden.mostrarIdentificadorTerminal()+"\n";
        }
        return resultado;
    }

    public ArrayList<Identificadores> getTabla() {
        return tabla;
    }
    
    private void falso(){
        resultado=false;
    }
    
    private void verdadero(){
        resultado=true;
    }
    
    public boolean getResultado(){
        return resultado;
    }
    
    public Documentador getDocumentador(){
        return documentador;
    }
    
    private String mostrarLineaError(Token tok){
        String resultado="\nEn la linea: "+tok.getLinea()+"\nEn el: "+tok.getLexema();
        return resultado;
    }
    
    private void agregarTabla(){
        String nombre = tokensDetectados.get(posicion+1).getLexema();
        if (sem.existeEnScope(tabla, nombre, scopeActual())) {
            throw error("YA EXISTE ESE IDENTIFICADOR: "+nombre);
        }
        Tokens token;
        if (tokensDetectados.get(posicion+1).getTipo()==null) {
            reportar("OCURRIO UN ERROR");
            return;
        }
        if (tokensDetectados.get(posicion+1).getTipo()==IDENTIFICADOR) {
            token=tokensDetectados.get(posicion).getTipo();
            tabla.add(new Identificadores(token,nombre,scopeActual()));
        }else{
            reportar("NO SE PUDO AGREGAR A LA TABLA: "+tokensDetectados.get(posicion).getLinea()+" "+tokensDetectados.get(posicion).getLexema());
        }
    }
    
    private String obtenerLinea(Token tok){
        String res="LINEA ";
        res=res+tok.getLinea()+": ";
        return res;
    }
    
    private String obtenerOperacion(int x){
        String resul="";
        double numero=0;
        Stack<String> pila=new Stack<>();
        while (tokensDetectados.get(x).getTipo()!=PUNTO_COMA) {
            switch (tokensDetectados.get(x).getTipo()) {
                case NUMERO_ENTERO:
                    pila.add(tokensDetectados.get(x).getLexema());
                    numero+=Double.parseDouble(tokensDetectados.get(x).getLexema());
                    break;
                case NUMERO_REAL:
                    pila.add(tokensDetectados.get(x).getLexema());
                    numero+=Double.parseDouble(tokensDetectados.get(x).getLexema());
                    break;
                case OPERADOR_SUMA:
                    numero=Double.parseDouble(pila.peek());
                default:
                    throw error("Expresion no soportada");
            }
            x++;
        }
        return resul;
    }
    
    private String recorrerGenerarCadena(int x){
        String res="";
        while (tokensDetectados.get(x).getTipo()!=PARENTESIS_CIERRA) {
            res=res+tokensDetectados.get(x).getLexema();
            x++;
        }
        return res;
    }
}

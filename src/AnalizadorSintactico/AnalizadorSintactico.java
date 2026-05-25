/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorSintactico;

import AnalizadorLexico.Identificadores;
import AnalizadorLexico.Token;
import AnalizadorLexico.Tokens;
import static AnalizadorLexico.Tokens.*;
import AnalizadorSemantico.AnalizadorSemantico;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.swing.JOptionPane;

/**
 *
 * @author fabri
 */

public class AnalizadorSintactico {
    private Stack<Token> pilaParentesis;
    private List<Token> tokensDetectados;
    private ArrayList<Identificadores> tabla;
    private int posicion;
    private boolean resultado;
    private AnalizadorSemantico sem;
    private boolean existePrincipal = false;
    private String scope;
    private Documentador documentador;

    public AnalizadorSintactico( List<Token> tokensDetectados) {
        this.pilaParentesis = new Stack<>();
        this.tokensDetectados = tokensDetectados;
        posicion=0;
        resultado=true;
        tabla=new ArrayList<>();
        sem=new AnalizadorSemantico();
        documentador=new Documentador();
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
                        JOptionPane.showMessageDialog(null, "HAY UN PROBLEMA CON: "+tok, "ERROR", 0);
                        return false;
                    }
                default:
            }
        }
        mostrarParentesis();
        if (pilaParentesis.size()==0) {
            return true;
        }else{
            JOptionPane.showMessageDialog(null, "hubo un problema con "+ pilaParentesis.peek().getTipo().name(), "ERROR", 0);
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
            programa();
            return true;
        }catch(RuntimeException | AssertionError e){
            JOptionPane.showMessageDialog(null, "OCURRIO UN ERROR: "+e.getMessage(), "ERROR EN LA SINTAXIS", 0);
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
             throw new RuntimeException(
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
        JOptionPane.showMessageDialog(null,"NO SE ENCONTRO EL: "+esperado.name()+"\nEn su lugar se encontro: "+tokensDetectados.get(posicion).getTipo().name() ,"ERROR ANALIZADO SINTACTICO" , 0);
    }
    //mis gramaticas 
    private void programa(){
        globales();
        inicio();
        funciones();
        match(LLAVE_CIERRA);
        if(posicion<tokensDetectados.size()){
            throw new RuntimeException("Codigo fuera de la clase");
        }
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
        scope="global";
        while(true){
            switch((tokensDetectados.get(posicion).getTipo())){
                case PALABRA_RESERVADA_ENT:
                case PALABRA_RESERVADA_REA:
                case PALABRA_RESERVADA_CAD:
                case PALABRA_RESERVADA_CAR:
                case PALABRA_RESERVADA_BOO:
                    declaracion(tokensDetectados.get(posicion).getTipo(),scope);
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
            throw new RuntimeException("No se encontro una funcion principal");
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
        match(IDENTIFICADOR);
        String funcion=tokensDetectados.get(posicion-1).getLexema();
        scope=tokensDetectados.get(posicion-1).getLexema();
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA FUNCION LLAMADA: "+funcion);
        instrucciones();
        match(LLAVE_CIERRA);
        
    }
    private void principal(){
        if(existePrincipal){
            throw new RuntimeException("Ya existe una funcion principal");
        }
        scope="principal";
        existePrincipal = true;
        match(PALABRA_RESERVADA_PRIN);
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA LA FUNCION PRINCIPAL DEL CODIGO");
        instrucciones();
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
                declaracion(actual.getTipo(),scope);
                break;
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
                instruccionesSi(LLAVE_CIERRA);
                match(LLAVE_CIERRA);
                break;
            case PALABRA_RESERVADA_HAC:
                hacer();
                break;
            case PALABRA_RESERVADA_SEG:
                segun();
                break;
            case IDENTIFICADOR:

                String iden =
                    tokensDetectados.get(posicion).getLexema();

                asignacionDirecta();

                Identificadores variable =
                    sem.buscarIde(tabla, iden);

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
                System.out.println("no se ha puesto el token");
                throw new AssertionError();
        }
    }
    
    private void hacer(){
        match(PALABRA_RESERVADA_HAC);
        match(LLAVE_ABRE);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CICLO HACER MIENTRAS QUE EVALUARA EL SIGUIENTE MIENTRAS ");
        instruccionesSi(LLAVE_CIERRA);
        match(LLAVE_CIERRA);
        mientras();
        match(PUNTO_COMA);
        
    }
    
    private void declaracion(Tokens tipo, String scope){
        String identificador;
        agregarTabla();
        tipo();
        match(IDENTIFICADOR);
        identificador=tokensDetectados.get(posicion-1).getLexema();
        if (tokensDetectados.get(posicion).getTipo()==ASIGNACION) {
            asignacion(tokensDetectados.get(posicion-2).getTipo(),identificador);
        }
        match(PUNTO_COMA);
        if(scope=="global"){
            documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA VARIABLE GLOBAL LLAMADA: "+identificador+" DE TIPO: "+sem.buscarIde(tabla, identificador).getTipo()
                    +" CON VALOR: "+sem.buscarIde(tabla, identificador).getValor());
        }else{
            documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UNA VARIABLE LLAMADA: "+identificador+" DE TIPO: "+sem.buscarIde(tabla, identificador).getTipo()
                    +" CON VALOR: "+sem.buscarIde(tabla, identificador).getValor());
        }

    }
    
    private void asignacionDirecta(){
        match(IDENTIFICADOR);
        if (tokensDetectados.get(posicion).getTipo()==ASIGNACION) {
            if (!sem.buscar(tabla, tokensDetectados.get(posicion-1).getLexema())) {
                throw new AssertionError("NO SE ENCONTRO EL IDENTIFICADOR: "+tokensDetectados.get(posicion-1).getLexema());
            }
            Tokens tok= sem.buscarIde(tabla, tokensDetectados.get(posicion-1).getLexema()).getToken();
            asignacion(tok,tokensDetectados.get(posicion-1).getLexema());
        }
    }
    
    //aqui lo que hice fue cambiar como asiganamos retonando valores numericos no tome en cnta valores como strings y caracteres 
    private void asignacion(Tokens variable,String identificador){
        match(ASIGNACION);
        switch (variable) {
            case PALABRA_RESERVADA_ENT:
                double valorEntero =
                expresionAritmetica();
                asignarValor(identificador,String.valueOf((int)valorEntero));
                break;
            case PALABRA_RESERVADA_REA:
                double valorReal =
                expresionAritmetica();
                asignarValor(identificador,String.valueOf(valorReal));
                break;
            case PALABRA_RESERVADA_CAD:
                match(TIPO_CADENA);
                asignarValor(identificador,tokensDetectados.get(posicion-1).getLexema());
                break;
            case PALABRA_RESERVADA_CAR:
                match(TIPO_CARACTER);
                asignarValor(identificador,tokensDetectados.get(posicion-1).getLexema());
                break;
            default:
                throw new AssertionError("TIPO INVALIDO");
        }
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
        if (!tabla.isEmpty()) {
            for(Identificadores iden: tabla){
                if(iden.getIdentificador().equals(identificador)){
                    iden.setValor(valor);
                }
            }
        }
    }
    
    
    public void imprimir(){
        String valor="";
        match(PALABRA_RESERVADA_IMP);
        match(PARENTESIS_ABRE);
        valor=expresionImprimible();
        while (tokensDetectados.get(posicion).getTipo()==OPERADOR_SUMA) {
            posicion++;
            valor=valor+expresionImprimible();
        }
        match(PARENTESIS_CIERRA);
        match(PUNTO_COMA);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE IMPRIME: "+valor);
    }
    private String expresionImprimible(){
        String resul="";
        switch (tokensDetectados.get(posicion).getTipo()) {
            case IDENTIFICADOR: case TIPO_CADENA: case TIPO_CARACTER: case NUMERO_REAL: case NUMERO_ENTERO:
                resul=tokensDetectados.get(posicion).getLexema();
                posicion++;
                break;
            default:
                falso();
                throw new AssertionError("NO SE RECONOCE EL TIPO: "+mostrarLineaError(tokensDetectados.get(posicion)));
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
        instruccionesSi(LLAVE_CIERRA);
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
                instruccionesSi(LLAVE_CIERRA);
                match(LLAVE_CIERRA);
                break;
            default:
                throw new AssertionError("PROBELAMS EN IF");
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
        if (!sem.buscar(tabla, identificador)) {
            resultado=false;
            JOptionPane.showMessageDialog(null, "no se encontro el identificador "+identificador,"ERROR AL ENCONTRAR", 0);
            return;
        }
        while(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_CAS){
            casos(sem.retornarTipo(sem.buscarIde(tabla, identificador)));
        }
//        while(tokensDetectados.get(posicion)!=LLAVE_CIERRA){
//            casos(sem.retornarTipo(sem.buscarIde(tabla, identificador)));
//        }
        match(LLAVE_CIERRA);
    }
    
    public void casos(Tokens tipo){
        match(PALABRA_RESERVADA_CAS);
        match(tipo);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN CASO PARA EL VALOR: "+tokensDetectados.get(posicion-1).getLexema());
        match(DOS_PUNTOS);
        while(tokensDetectados.get(posicion).getTipo()!=PALABRA_RESERVADA_SAL){
            instruccion();
        }
        match(PALABRA_RESERVADA_SAL);
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE TERMINA EL CASO");
        match(PUNTO_COMA);
    }
    public void para(){
        match(PALABRA_RESERVADA_PAR);
        match(PARENTESIS_ABRE);
        int x=posicion;
        match(PALABRA_RESERVADA_ENT);
        String v=tokensDetectados.get(posicion).getLexema();
        match(IDENTIFICADOR);
        match(ASIGNACION);
        String valor=tokensDetectados.get(posicion).getLexema();
        match(NUMERO_ENTERO);
        asignarValor(v,valor);
        match(PUNTO_COMA);
        match(IDENTIFICADOR);
        operadorRelacional();
        match(NUMERO_ENTERO);
        match(PUNTO_COMA);
        match(IDENTIFICADOR);
        match(INCREMENTO);
        match(PARENTESIS_CIERRA );
        documentador.agregar(obtenerLinea(tokensDetectados.get(posicion-1))+"SE CREA UN BUCLE DE TIPO PARA CON LAS REGLAS: "+recorrerGenerarCadena(x));
        match(LLAVE_ABRE);
        instruccionesSi(LLAVE_CIERRA);
        match(LLAVE_CIERRA);
    }
    
    
    private void expresionLogica(){
        condicion();  
        Token actual=tokensDetectados.get(posicion);
        while (actual.getTipo()==AND||actual.getTipo()==OR) {
            posicion++;
            condicion();
        }
    }
    
    private void condicion(){
        expresionAritmetica();

        operadorRelacional();

        expresionAritmetica();
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
            throw new RuntimeException("Operador relacional inválido");
        }
    }
    private double expresionAritmetica(){
        double valor = termino();
        while(
            tokensDetectados.get(posicion).getTipo()==OPERADOR_SUMA ||tokensDetectados.get(posicion).getTipo()==OPERADOR_RESTA){
            Tokens operador =tokensDetectados.get(posicion).getTipo();
            posicion++;
            double valor2 = termino();
            if(operador == OPERADOR_SUMA){
                valor += valor2;
            }else{
                valor -= valor2;
            }
        }
        return valor;
    }
    
    private double termino(){
        double valor = factor();
        while(
            tokensDetectados.get(posicion).getTipo()==OPERADOR_MULTIPLICAR ||tokensDetectados.get(posicion).getTipo()==OPERADOR_DIVISION ||tokensDetectados.get(posicion).getTipo()==OPERADOR_MODULO){
            Tokens operador =tokensDetectados.get(posicion).getTipo();
            posicion++;
            double valor2 = factor();
            switch(operador){
                case OPERADOR_MULTIPLICAR:
                    valor *= valor2;
                    break;
                case OPERADOR_DIVISION:
                    valor /= valor2;
                    break;
                case OPERADOR_MODULO:
                    valor %= valor2;
                    break;
            }
        }
        return valor;
    }
    
    private double factor(){
        Token actual = tokensDetectados.get(posicion);
        switch(actual.getTipo()){
            case NUMERO_ENTERO:
                posicion++;
                return Double.parseDouble(actual.getLexema());
            case NUMERO_REAL:
                posicion++;
                return Double.parseDouble(actual.getLexema());
            case IDENTIFICADOR:
                posicion++;
                Identificadores ide =
                sem.buscarIde(tabla, actual.getLexema());
                if(ide == null){
                    throw new RuntimeException("Variable no encontrada: "+ actual.getLexema());}
                return Double.parseDouble(ide.getValor());
            case PARENTESIS_ABRE:
                match(PARENTESIS_ABRE);
                double valor = expresionAritmetica();
                match(PARENTESIS_CIERRA);
                return valor;
            default:
                throw new RuntimeException("Factor invalido");
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
        if (sem.buscar(tabla, tokensDetectados.get(posicion+1).getLexema())) {
            throw new AssertionError("YA EXISTE ESE IDENTIFICADOR: "+tokensDetectados.get(posicion+1).getLexema());
        }
        Tokens token;
        if (tokensDetectados.get(posicion+1).getTipo()==null) {
            JOptionPane.showMessageDialog(null, "OCURRIO UN ERROR", "ERROR", 0);
            return;
        }
        if (tokensDetectados.get(posicion+1).getTipo()==IDENTIFICADOR) {
            token=tokensDetectados.get(posicion).getTipo();
            tabla.add(new Identificadores(token,tokensDetectados.get(posicion+1).getLexema(),scope));
        }else{
            JOptionPane.showMessageDialog(null, "NO SE PUDO AGREGAR A LA TABLA: "+tokensDetectados.get(posicion).getLinea()+" "+tokensDetectados.get(posicion).getLexema(), "OCURRIO UN ERROR", 0);
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
                    throw new AssertionError();
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

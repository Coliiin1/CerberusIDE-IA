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

    public AnalizadorSintactico( List<Token> tokensDetectados) {
        this.pilaParentesis = new Stack<>();
        this.tokensDetectados = tokensDetectados;
        posicion=0;
        resultado=true;
        tabla=new ArrayList<>();
        sem=new AnalizadorSemantico();
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
        mostrarParentetis();
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
    
    public void mostrarParentetis(){
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
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
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
        scope=tokensDetectados.get(posicion-1).getLexema();
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
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
        instrucciones();
        match(LLAVE_CIERRA);
    }
    
    private void instrucciones() {
        while(tokensDetectados.get(posicion).getTipo()!= LLAVE_CIERRA){
            instruccion();
        }
    }

    //cada pieza de lcodigo posible
    private void instruccion(){
        Token actual=tokensDetectados.get(posicion);
        switch (actual.getTipo()) {
            case PALABRA_RESERVADA_ENT: case PALABRA_RESERVADA_REA: case PALABRA_RESERVADA_CAD: case PALABRA_RESERVADA_CAR: case PALABRA_RESERVADA_BOO:
                declaracion(actual.getTipo());
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
                match(PALABRA_RESERVADA_HAC);
                match(LLAVE_ABRE);
                instruccionesSi(LLAVE_CIERRA);
                match(LLAVE_CIERRA);
                mientras();
                match(PUNTO_COMA);
                break;
            case PALABRA_RESERVADA_SEG:
                segun();
                break;
            case IDENTIFICADOR:
                asignacionDirecta();
                match(PUNTO_COMA);
                break;
            default:
                System.out.println("no se ha puesto el token");
                throw new AssertionError();
        }
    }

    private void declaracion(Tokens tipo){
        String identificador;
        agregarTabla();
        tipo();
        match(IDENTIFICADOR);
        identificador=tokensDetectados.get(posicion-1).getLexema();
        
        if (tokensDetectados.get(posicion).getTipo()==ASIGNACION) {
            asignacion(tokensDetectados.get(posicion-2).getTipo());
        }
        match(PUNTO_COMA);
    }
    
    private void asignacionDirecta(){
        match(IDENTIFICADOR);
        if (tokensDetectados.get(posicion).getTipo()==ASIGNACION) {
            if (!sem.buscar(tabla, tokensDetectados.get(posicion-1).getLexema())) {
                throw new AssertionError("NO SE ENCONTRO EL IDENTIFICADOR: "+tokensDetectados.get(posicion-1).getLexema());
            }
            Tokens tok= sem.buscarIde(tabla, tokensDetectados.get(posicion-1).getLexema()).getToken();
            asignacion(tok);
        }
    }
    private void asignacion(Tokens variable){
        match(ASIGNACION);
        System.out.println("LLEGA A LA ASIGNACION");
        switch (variable) {
            case PALABRA_RESERVADA_ENT:
                match(NUMERO_ENTERO);
                break;
            case PALABRA_RESERVADA_REA:
                match(NUMERO_REAL);
                break;
            case PALABRA_RESERVADA_CAD:
                match(TIPO_CADENA);
                break;
            case PALABRA_RESERVADA_CAR:
                match(TIPO_CARACTER);
                break;
            case PALABRA_RESERVADA_BOO:
                if(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_VER){
                    match(PALABRA_RESERVADA_VER);
                }else if(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_FAL){
                    match(PALABRA_RESERVADA_FAL);
                }else{
                    throw new RuntimeException("Se esperaba verdadero o falso");
                }
                break;
            default:
                falso();
                throw new AssertionError("NO SE RECONOCIO EL TIPO "+tokensDetectados.get(posicion).getLinea());
        }
        
        asignarValor( tokensDetectados.get(posicion-3).getLexema(), tokensDetectados.get(posicion-1).getLexema());
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
        match(PALABRA_RESERVADA_IMP);
        match(PARENTESIS_ABRE);
        switch (tokensDetectados.get(posicion).getTipo()) {
            case IDENTIFICADOR: case TIPO_CADENA: case TIPO_CARACTER: case NUMERO_REAL: case NUMERO_ENTERO:
                posicion++;
                break;
            default:
                falso();
                throw new AssertionError();
        }
        match(PARENTESIS_CIERRA);
        match(PUNTO_COMA);
    }
    
    public void mientras(){
        match(PALABRA_RESERVADA_MIE);
        match(PARENTESIS_ABRE);
        expresionLogica();
        match(PARENTESIS_CIERRA);
    }
    
    public void si(){
        match(PALABRA_RESERVADA_SI);
        match(PARENTESIS_ABRE);
        expresionLogica();
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        instruccionesSi(LLAVE_CIERRA);
        match(LLAVE_CIERRA);
        if(tokensDetectados.get(posicion).getTipo()==PALABRA_RESERVADA_SIN){
            sino();
        }
    }
    
    private void sino(){
        match(PALABRA_RESERVADA_SIN);
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
        match(DOS_PUNTOS);
        while(tokensDetectados.get(posicion).getTipo()!=PALABRA_RESERVADA_SAL){
            instruccion();
        }
        match(PALABRA_RESERVADA_SAL);
        match(PUNTO_COMA);
    }
    public void para(){
        match(PALABRA_RESERVADA_PAR);
        match(PARENTESIS_ABRE);
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
    private void expresionAritmetica(){
        termino();
        while(tokensDetectados.get(posicion).getTipo()==OPERADOR_SUMA||tokensDetectados.get(posicion).getTipo()==OPERADOR_RESTA){
            posicion++;
            termino();
        }
    }
    
    private void termino(){
        factor();
        while(tokensDetectados.get(posicion).getTipo()==OPERADOR_MULTIPLICAR||tokensDetectados.get(posicion).getTipo()==OPERADOR_DIVISION||tokensDetectados.get(posicion).getTipo()==OPERADOR_MODULO){
            posicion++;
            factor();
        }
    }
    
    private void factor(){
        Token actual=tokensDetectados.get(posicion);
        switch (actual.getTipo()) {
            case IDENTIFICADOR:
                if (!sem.buscar(tabla, actual.getLexema())) {
                    throw new AssertionError("NO SE HA DECLARADO EL IDENTIFICADOR: "+actual.getLexema()+mostrarLineaError(actual));
                }
            case NUMERO_ENTERO: case NUMERO_REAL:
                posicion++;
                break;
            case PARENTESIS_ABRE:
                match(PARENTESIS_ABRE);
                expresionAritmetica();
                match(PARENTESIS_CIERRA);
                
                break;
            default:
                throw new AssertionError("Numero invalido");
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
}

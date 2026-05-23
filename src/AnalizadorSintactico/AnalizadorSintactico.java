/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorSintactico;

import AnalizadorLexico.Identificadores;
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
    private Stack<Tokens> pilaParentesis;
    private List<Tokens> tokensDetectados;
    private List<String> codigo;
    private ArrayList<Identificadores> tabla;
    private int posicion;
    private boolean resultado;
    private AnalizadorSemantico sem;

    public AnalizadorSintactico( List<Tokens> tokensDetectados, List<String> codigo,ArrayList<Identificadores> tabla) {
        this.pilaParentesis = new Stack<>();
        this.tokensDetectados = tokensDetectados;
        this.codigo = codigo;
        this.tabla=tabla;
        posicion=0;
        resultado=true;
        sem=new AnalizadorSemantico();
    }
    
    
    public boolean comprobarParentesis(){
        Tokens tok;
        for(Tokens token: tokensDetectados){
            switch (token) {
                case PARENTESIS_ABRE:
                case CORCHETE_ABRE:
                case LLAVE_ABRE:
                    pilaParentesis.push(token);
                    break;
                case PARENTESIS_CIERRA:
                case CORCHETE_CIERRA:
                case LLAVE_CIERRA:
                    tok=pilaParentesis.peek();
                    if (tok==PARENTESIS_ABRE&&token==PARENTESIS_CIERRA || tok==CORCHETE_ABRE&&token==CORCHETE_CIERRA || tok==LLAVE_ABRE&&token==LLAVE_CIERRA) {
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
            JOptionPane.showMessageDialog(null, "hubo un problema con "+ pilaParentesis.peek().name(), "ERROR", 0);
            return false;
        }
    }
    
    public String mostrarPeekParentesis(){
        Tokens token=pilaParentesis.peek();
        return token.name();
    }
    
    public void mostrarParentetis(){
        for(Tokens token: pilaParentesis){
            System.out.println(token.name());
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
        if (tokensDetectados.get(posicion)==esperado) {
            posicion++;
        }else{
            System.out.println("SE ESPERABA "+esperado.name()+" Y SE ENCONTRO: "+tokensDetectados.get(posicion).name());
            falso();
             throw new RuntimeException(
            "Se esperaba "
            + esperado.name()
            + " y se encontró "
            + tokensDetectados.get(posicion).name()
        );
        }
        return true;
    }
    
    public void mostrarEsperado(Tokens esperado){
        JOptionPane.showMessageDialog(null,"NO SE ENCONTRO EL: "+esperado.name()+"\nEn su lugar se encontro: "+tokensDetectados.get(posicion).name() ,"ERROR ANALIZADO SINTACTICO" , 0);
    }
    //mis gramaticas 
    private void programa(){
        inicio();
        instrucciones();
        match(LLAVE_CIERRA);
    }

    private void inicio(){
        match(PALABRA_RESERVADA_CLA);
        match(IDENTIFICADOR);
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
    }
    
    private void instrucciones() {
        while(posicion<tokensDetectados.size()-1){
            instruccion();
        }
    }
    
    //cada pieza de lcodigo posible
    private void instruccion(){
        Tokens actual=tokensDetectados.get(posicion);
        switch (actual) {
            case PALABRA_RESERVADA_ENT: case PALABRA_RESERVADA_REA: case PALABRA_RESERVADA_CAD: case PALABRA_RESERVADA_CAR: case PALABRA_RESERVADA_BOO:
                declaracion(actual);
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
            default:
                System.out.println("no se ha puesto el token");
                throw new AssertionError();
        }
    }
   
    private void declaracion(Tokens tipo){
        String identificador;
        tipo();
        match(IDENTIFICADOR);
        identificador=codigo.get(posicion-1);
        if (match(ASIGNACION)) {
            switch (tipo) {
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
                    if(tokensDetectados.get(posicion)==PALABRA_RESERVADA_VER){
                        match(PALABRA_RESERVADA_VER);
                    }else if(tokensDetectados.get(posicion)==PALABRA_RESERVADA_FAL){
                        match(PALABRA_RESERVADA_FAL);
                    }else{
                        throw new RuntimeException("Se esperaba verdadero o falso");
                    }
                    break;
                default:
                    falso();
                    throw new AssertionError();
            }
            asignarValor(identificador, codigo.get(posicion-1));
        }
        match(PUNTO_COMA);
    }
    
    private void asiganacion(){
        
    }
    
    private void tipo(){
        Tokens actual = tokensDetectados.get(posicion);
        switch (actual) {
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
        switch (tokensDetectados.get(posicion)) {
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
        
    }
    
    public void segun(){
        match(PALABRA_RESERVADA_SEG);
        match(PARENTESIS_ABRE);
        match(IDENTIFICADOR);
        String identificador=codigo.get(posicion-1);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        if (!sem.buscar(tabla, identificador)) {
            resultado=false;
            JOptionPane.showMessageDialog(null, "no se encontro el identificador "+identificador,"ERROR AL ENCONTRAR", 0);
            return;
        }   
        while(tokensDetectados.get(posicion)==PALABRA_RESERVADA_CAS){
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
        while(tokensDetectados.get(posicion)!=PALABRA_RESERVADA_SAL){
            instruccion();
        }
        match(PALABRA_RESERVADA_SAL);
        match(PUNTO_COMA);
    }
    public void para(){
        match(PALABRA_RESERVADA_PAR);
        match(PARENTESIS_ABRE);
        match(PALABRA_RESERVADA_ENT);
        match(IDENTIFICADOR);
        match(ASIGNACION);
        match(NUMERO_ENTERO);
        match(PUNTO_COMA);
        match(IDENTIFICADOR);
        switch (tokensDetectados.get(posicion)) {
            case MENOR:
                match(MENOR);
                break;
            case MAYOR:
                match(MAYOR);
                break;
            case MENOR_IGUAL:
                match(MENOR_IGUAL);
                break;
            case MAYOR_IGUAL:
                match(MAYOR_IGUAL);
                break;
            default:
                falso();
                throw new AssertionError();
        }
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
        Tokens actual=tokensDetectados.get(posicion);
        while (actual==AND||actual==OR) {
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
        Tokens actual = tokensDetectados.get(posicion);

    switch(actual){
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
        while(tokensDetectados.get(posicion)==OPERADOR_SUMA||tokensDetectados.get(posicion)==OPERADOR_RESTA){
            posicion++;
            termino();
        }
    }
    
    private void termino(){
        factor();
        while(tokensDetectados.get(posicion)==OPERADOR_MULTIPLICAR||tokensDetectados.get(posicion)==OPERADOR_DIVISION||tokensDetectados.get(posicion)==OPERADOR_MODULO){
            posicion++;
            factor();
        }
    }
    
    private void factor(){
        Tokens actual=tokensDetectados.get(posicion);
        switch (actual) {
            case IDENTIFICADOR: case NUMERO_ENTERO: case NUMERO_REAL:
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
        while(tokensDetectados.get(posicion)!=token){
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
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorSintactico;

import AnalizadorLexico.Identificadores;
import AnalizadorLexico.Tokens;
import static AnalizadorLexico.Tokens.*;
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

    public AnalizadorSintactico( List<Tokens> tokensDetectados, List<String> codigo,ArrayList<Identificadores> tabla) {
        this.pilaParentesis = new Stack<>();
        this.tokensDetectados = tokensDetectados;
        this.codigo = codigo;
        this.tabla=tabla;
        posicion=0;
        resultado=true;
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
            return false;
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
            case PARENTESIS_CIERRA:
                System.out.println("DETECTA EL PARETENTESIS QUE CIERRA");
                break;
            case null:
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
        if (match(ASIGANCION)) {
            switch (tipo) {
                case PALABRA_RESERVADA_ENT:  
                    match(NUMERO_ENTERO);
                    asignarValor(identificador, codigo.get(posicion-1));
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
                    match(PALABRA_RESERVADA_VER);
                    match(PALABRA_RESERVADA_FAL);
                    
                    break;
                default:
                    falso();
                    throw new AssertionError();
            }
            asignarValor(identificador, codigo.get(posicion-1));
        }
        if (!match(PUNTO_COMA)) {
            mostrarEsperado(PUNTO_COMA);
        }
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
    
    public void si(){
        match(PALABRA_RESERVADA_SI);
        match(PARENTESIS_ABRE);
        match(PARENTESIS_CIERRA);
        match(LLAVE_ABRE);
        instruccionesSi();
        match(LLAVE_CIERRA);
        
    }
    
    public void instruccionesSi(){
        while(tokensDetectados.get(posicion)!=LLAVE_CIERRA){
            instruccion();
        }
    }
    public void imprimirTabla(){
        for(Identificadores iden:tabla){
            iden.mostrarIdentificadorTerminal();
        }
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

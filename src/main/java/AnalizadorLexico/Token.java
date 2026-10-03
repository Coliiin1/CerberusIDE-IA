/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorLexico;

/**
 *
 * @author fabri
 */
public class Token {
    private Tokens tipo;
    private String lexema;
    private int linea;
    private int columna;

    public Token(Tokens tipo, String lexema, int linea, int columna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.linea = linea;
        this.columna = columna;
    }

    public Tokens getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }
    
    @Override
    public String toString() {
        return tipo.name() + " -> " + lexema +
               " [Fila: " + linea +
               ", Columna: " + columna + "]";
    }
}

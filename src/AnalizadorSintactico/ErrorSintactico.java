/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorSintactico;

/**
 *
 * @author fabri
 */
public class ErrorSintactico extends RuntimeException{

    private int fila;
    private int columna;
    private String lexema;

    public ErrorSintactico(
        String mensaje,
        int fila,
        int columna,
        String lexema
    ){
        super(mensaje);

        this.fila = fila;
        this.columna = columna;
        this.lexema = lexema;
    }

    public int getFila() {
        return fila;
    }

    public int getColumna() {
        return columna;
    }

    public String getLexema() {
        return lexema;
    }
}
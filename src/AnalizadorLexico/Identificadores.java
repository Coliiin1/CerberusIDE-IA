/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorLexico;

/**
 *
 * @author fabri
 */
public class Identificadores {
    private String tipo;
    private Tokens token;
    private String identificador;
    private String valor;

    public Identificadores(Tokens token, String identificador) {
        this.token = token;
        this.identificador = identificador;
        switch (token) {
            case PALABRA_RESERVADA_BOO:
                tipo="BOLEANO";
                break;
            case PALABRA_RESERVADA_CAD:
                tipo="CADENA";
                break;
            case PALABRA_RESERVADA_CAR:
                tipo="CARACTER";
                break;
            case PALABRA_RESERVADA_ENT:
                tipo="ENTERO";
                break;
            case PALABRA_RESERVADA_REA:
                tipo="REAL";
                break;
            default:
                tipo="NO SE ENCONTRO TIPO";
        }
    }
    
    
    public String mostrarIdentificadorTerminal(){
        String objeto;
        objeto=tipo+", "+token.name()+", "+identificador+", "+valor;
        System.out.println(tipo+", "+token.name()+", "+identificador+", "+valor);
        return objeto;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getTipo() {
        return tipo;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    
}


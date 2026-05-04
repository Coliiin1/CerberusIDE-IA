/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codigo;
import static codigo.Tokens.*;
import java.util.regex.*;
import javax.swing.JOptionPane;
/**
 *
 * @author fabri
 */
public class AnalizadorLexico {
    Matcher m;
    private Tokens [] tokensDetectados;
    private String[] codigo;
    int contador;
    public boolean seccionarCadena(String cadena){
        cadena=cadena.replace(";", " ; ");
        System.out.println(cadena);
        codigo= cadena.split("\\s+");
        contador=0;
        tokensDetectados=new Tokens[codigo.length];
        Tokens token;
        for(String cad:codigo){
            token=analizador(cad);
            switch (token) {
                case NUMERO_ENTERO:
                case IDENTIFICADOR:
                case PALABRA_RESERVADA_CLA:
                case PALABRA_RESERVADA_NUE:
                case PALABRA_RESERVADA_EST:
                case PALABRA_RESERVADA_PUB:
                case PALABRA_RESERVADA_PRI:
                case PALABRA_RESERVADA_SI:
                case PUNTO_COMA:
                    tokensDetectados[contador]=token;
                    contador++;
                    break;
                case ERROR:
                    tokensDetectados[contador]=token;
                    contador++;
                    JOptionPane.showConfirmDialog(null, "HAY UN ERROR EN: "+cad);
                    return false;
                default:
                    throw new AssertionError();
            }
        }
        return true;
    }
    public Tokens analizador(String cadena){
        Pattern numeroENtero=Pattern.compile("^(-)?\\d+$");
        Pattern identificador=Pattern.compile("^[a-zA-Z]([a-zA-Z_]|\\d)*");
        Pattern prClase=Pattern.compile("^clase");
        Pattern prNuevo=Pattern.compile("nuevo");
        Pattern prEste=Pattern.compile("este");
        Pattern prPublico=Pattern.compile("publico");
        Pattern prPrivado=Pattern.compile("privado");
        Pattern prSi=Pattern.compile("si");
        Pattern prSino=Pattern.compile("sino");
        Pattern prSegun=Pattern.compile("segun");
        Pattern prCaso=Pattern.compile("caso");
        Pattern prSalir=Pattern.compile("salir");
        Pattern prPara=Pattern.compile("para");
        Pattern prMientras=Pattern.compile("mientras");
        
        
        
        Pattern puntoComa=Pattern.compile(";");
        m=numeroENtero.matcher(cadena);
        if (m.find()) {
            return NUMERO_ENTERO;
        }
        
        m=prClase.matcher(cadena);
        if (m.find()) {
            return PALABRA_RESERVADA_CLA;
        }
        m=prNuevo.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_NUE;
        }
        m=prEste.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_EST;
        }
        m=prPublico.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_PUB;
        }
        m=prPrivado.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_PRI;
        }
        m=prSi.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_SI;
        }
        m=prSino.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_SIN;
        }
        m=prSegun.matcher(cadena);
        if(m.find()){
            return PALABRA_RESERVADA_SEG;
        }
        
        
        m=puntoComa.matcher(cadena);
        if(m.find()){
            return PUNTO_COMA;
        }
        m=identificador.matcher(cadena);
        if (m.find()) {
            return IDENTIFICADOR;
        }
        return ERROR;
    }
    
    public String mostrarTokens(){
        String resultado="";
        int contador=0;
        for(Tokens tok:tokensDetectados){
            resultado+=tok+": "+codigo[contador]+"\n";
            contador++;
        }
        return resultado;
    }
    
    public boolean nuevoAnalizador(String codigo){
        int conttador=0;
        Pattern numeroENtero=Pattern.compile("^(-)?\\d+$");
        Pattern identificador=Pattern.compile("^[a-zA-Z]([a-zA-Z_]|\\d)*");
        Pattern prClase=Pattern.compile("^clase");
        Pattern prNuevo=Pattern.compile("nuevo");
        Pattern prEste=Pattern.compile("este");
        Pattern prPublico=Pattern.compile("publico");
        Pattern prPrivado=Pattern.compile("privado");
        Pattern prSi=Pattern.compile("^si");
        Pattern prSino=Pattern.compile("sino");
        Pattern prSegun=Pattern.compile("segun");
        Pattern prCaso=Pattern.compile("caso");
        Pattern prSalir=Pattern.compile("salir");
        Pattern prPara=Pattern.compile("para");
        Pattern prMientras=Pattern.compile("mientras");
        
        
        
        Pattern error=Pattern.compile(".");
        Pattern puntoComa=Pattern.compile(".;");
        m=numeroENtero.matcher(codigo);
        if (m.find()) {
            tokensDetectados[contador]=NUMERO_ENTERO;
            contador++;
        }
        
        m=prClase.matcher(codigo);
        if (m.find()) {
            tokensDetectados[contador]=PALABRA_RESERVADA_CLA;
        }
        m=prNuevo.matcher(codigo);
        if(m.find()){
           tokensDetectados[contador]=PALABRA_RESERVADA_NUE;
        }
        m=prEste.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_EST;
        }
        m=prPublico.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_PUB;
        }
        m=prPrivado.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_PRI;
        }
        m=prSi.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_SI;
        }
        m=prSino.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_SIN;
        }
        m=prSegun.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]=PALABRA_RESERVADA_SEG;
        }
        
        
        m=puntoComa.matcher(codigo);
        if(m.find()){
            tokensDetectados[contador]= PUNTO_COMA;
        }
        m=identificador.matcher(codigo);
        if (m.find()) {
            tokensDetectados[contador]=IDENTIFICADOR;
        }
        m=error.matcher(codigo);
        if (m.find()) {
            tokensDetectados[contador]=ERROR;
        }
        return false;
    }
}

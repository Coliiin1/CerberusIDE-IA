/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codigo;
import static codigo.Tokens.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;
import javax.swing.JOptionPane;
/**
 *
 * @author fabri
 */
public class AnalizadorLexico {
    Matcher m;
    private List<Tokens> tokensDetectados;
    private List<String> codigo;
    int contador;
    public boolean seccionarCadena(String cadena){
        cadena=cadena.replace(";", " ; ");
        cadena=cadena.replace("{", " { ");
        cadena=cadena.replace("}", " } ");
        cadena=cadena.replace("(", " ( ");
        cadena=cadena.replace(")", " ) ");

        // en el codigo de arriba cambio los puntos y coma juntos para que el codigo los detecte por separado
        codigo = new ArrayList<>();

        Pattern patron = Pattern.compile("\"[^\"]*\"|\\S+");
        Matcher matcher = patron.matcher(cadena);

        while (matcher.find()) {
            codigo.add(matcher.group());
        }

        contador=0;
        tokensDetectados=new ArrayList<>();
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
                case PALABRA_RESERVADA_CAS:
                case PALABRA_RESERVADA_SAL:
                case PALABRA_RESERVADA_PAR:
                case PALABRA_RESERVADA_MIE:
                case PALABRA_RESERVADA_VER:
                case PALABRA_RESERVADA_FAL:
                case PALABRA_RESERVADA_FUN:
                case PALABRA_RESERVADA_RET:
                case PALABRA_RESERVADA_IMP:
                case PALABRA_RESERVADA_ENT:
                case PALABRA_RESERVADA_REA:
                case PALABRA_RESERVADA_CAR:
                case PALABRA_RESERVADA_CAD:
                case PALABRA_RESERVADA_BOO:
                case PALABRA_RESERVADA_NUL:
                case PALABRA_RESERVADA_VAC:
                    
                case TIPO_CADENA:
                    
                case PARENTESIS_ABRE:    
                case PARENTESIS_CIERRA:    
                case CORCHETE_ABRE:    
                case CORCHETE_CIERRA:    
                case LLAVE_ABRE:    
                case LLAVE_CIERRA:    
                    tokensDetectados.add(token);
                    contador++;
                    break;
                case ERROR:
                    tokensDetectados.add(token);
                    contador++;
                    JOptionPane.showConfirmDialog(null, "HAY UN ERROR EN: "+cad);
                    return false;
                default:
                    tokensDetectados.add(token);
                    contador++;
                    JOptionPane.showConfirmDialog(null, "TOKEN NO DETECTADO: "+cad);
            }
        }

        return true;
    }
    public Tokens analizador(String cadena){
        switch (cadena) {
            case "(": return PARENTESIS_ABRE;
            case ")": return PARENTESIS_CIERRA;
            case "[": return CORCHETE_ABRE;
            case "]": return CORCHETE_CIERRA;
            case "{": return LLAVE_ABRE;
            case "}": return LLAVE_CIERRA;
            case ";": return PUNTO_COMA;
            case "caso": return PALABRA_RESERVADA_CAS;
            case "salir": return PALABRA_RESERVADA_SAL;
            case "para": return PALABRA_RESERVADA_PAR;
            case "mientras": return PALABRA_RESERVADA_MIE;
            case "verdadero": return PALABRA_RESERVADA_VER;
            case "falso": return PALABRA_RESERVADA_FAL;
            case "funcion": return PALABRA_RESERVADA_FUN;
            case "retornar": return PALABRA_RESERVADA_RET;
            case "imprimir": return PALABRA_RESERVADA_IMP;
            case "entero": return PALABRA_RESERVADA_ENT;
            case "real": return PALABRA_RESERVADA_REA;
            case "caracter": return PALABRA_RESERVADA_CAR;
            case "cadena": return PALABRA_RESERVADA_CAD;
            case "booleano": return PALABRA_RESERVADA_BOO;
            case "nulo": return PALABRA_RESERVADA_NUL;
            case "vacio": return PALABRA_RESERVADA_VAC;
        }
        Pattern tipoNumeroEntero=Pattern.compile("^(-)?\\d+$");
        Pattern identificador=Pattern.compile("^[a-zA-Z]([a-zA-Z_]|\\d)*");
        Pattern prClase=Pattern.compile("clase");
        Pattern prNuevo=Pattern.compile("nuevo");
        Pattern prEste=Pattern.compile("este");
        Pattern prPublico=Pattern.compile("publico");
        Pattern prPrivado=Pattern.compile("privado");
        Pattern prSi=Pattern.compile("si");
        Pattern prSino=Pattern.compile("sino");
        Pattern prSegun=Pattern.compile("segun");
        Pattern tipoCadena=Pattern.compile("\"(\\w| |\\d|\\S)+\"");
        
        
        //crear patrones para CADA UNO de los TOKENS y acontinuacion buscar que matchen igual colocar el TOken en el switech de arriba 
        
        Pattern puntoComa=Pattern.compile(";");
        m=tipoNumeroEntero.matcher(cadena);
        if (m.matches()) {
            return NUMERO_ENTERO;
        }
        
        m=prClase.matcher(cadena);
        if (m.matches()) {
            return PALABRA_RESERVADA_CLA;
        }
        m=prNuevo.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_NUE;
        }
        m=prEste.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_EST;
        }
        m=prPublico.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_PUB;
        }
        m=prPrivado.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_PRI;
        }
        m=prSi.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_SI;
        }
        m=prSino.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_SIN;
        }
        m=prSegun.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_SEG;
        }
        m=tipoCadena.matcher(cadena);
        if(m.matches()){
            return TIPO_CADENA;
        }
        

        m=identificador.matcher(cadena);
        if (m.matches()) {
            return IDENTIFICADOR;
        }
        return ERROR;
    }
    
    public String mostrarTokens(){
        String resultado="";
        int conta=0;
        for(Tokens tok:tokensDetectados){
            if (codigo.get(conta)==null) {
                return resultado;
            }
            resultado+=tok+": "+codigo.get(conta)+"\n";
            if (conta>=codigo.size()) {
                return resultado;
            }
            conta++;
        }
        return resultado;
    }
    
    public void imprimirCodigo(){
        for(String cod:codigo){
            System.out.println(cod);
        }
    }
}
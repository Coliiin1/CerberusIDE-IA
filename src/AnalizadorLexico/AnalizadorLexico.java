/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AnalizadorLexico;
import static AnalizadorLexico.Tokens.*;
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
    private List<Token> tokenDetectados;
    private ArrayList<Identificadores> tabla;
    int contador;

    public AnalizadorLexico() {
        tabla=new ArrayList<>();
    }

    
    
    
    public boolean seccionarCadena(String cadena){
        cadena=cadena.replace(";", " ; ");
        cadena=cadena.replace(":", " : ");
        cadena=cadena.replace("{", " { ");
        cadena=cadena.replace("}", " } ");
        cadena=cadena.replace("(", " ( ");
        cadena=cadena.replace(")", " ) ");
        cadena=cadena.replace("[", " [ ");
        cadena=cadena.replace("]", " ] ");
        cadena=cadena.replace("+", " + ");
        cadena=cadena.replace("-", " - ");
        cadena=cadena.replace("*", " * ");
        cadena=cadena.replace("/", " / ");
        cadena=cadena.replace("%", " % ");
        cadena=cadena.replace("=", " = ");
        cadena=cadena.replace("!", " ! ");
        cadena=cadena.replace("<", " < ");
        cadena=cadena.replace(">", " > ");
        cadena=cadena.replace(" +  + ", " ++ ");
        cadena=cadena.replace("--", " -- ");
        cadena=cadena.replace("+ =", " += ");
        cadena=cadena.replace("* =", " *= ");
        cadena=cadena.replace("/ =", " /= ");
        cadena=cadena.replace("=  =", " == ");
        cadena=cadena.replace("!  =", " != ");
        cadena=cadena.replace("<  =", " <= ");
        cadena=cadena.replace(">  =", " >= ");
        cadena=cadena.replace("&", " & ");
        cadena=cadena.replace("|", " | ");
        



//        Pattern patron = Pattern.compile("\"[^\"]*\"|\\S+");
//        Matcher matcher = patron.matcher(cadena);
        tokenDetectados=new ArrayList<>();
        String[] lineas = cadena.split("\n");

        int fila = 1;

        for(String lineaTexto : lineas){

            int columna = 1;

            Pattern patron = Pattern.compile("\"[^\"]*\"|\\S+");
            Matcher matcher = patron.matcher(lineaTexto);

            while(matcher.find()){

                String lexema = matcher.group();

                Tokens token = analizador(lexema);

                tokenDetectados.add(
                    new Token(
                        token,
                        lexema,
                        fila,
                        matcher.start() + 1
                    )
                );
            }

            fila++;
        }
//        contador=0;
//        tokensDetectados=new ArrayList<>();
//        Tokens token;
//        for(String cad:codigo){
//            token=analizador(cad);
//            switch (token) {
//                case ERROR:
//                    tokensDetectados.add(token);
//                    contador++;
//                    JOptionPane.showMessageDialog(null, "HAY UN ERROR EN: "+cad);
//                    return false;
//                default:
//                    if (token==null) {
//                        JOptionPane.showMessageDialog(null, "TOKEN NO RECONOCIDO: "+cad);
//                        return false;
//                    }
//                    tokensDetectados.add(token);
//                    contador++;
//            }
//        }

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
            case ":": return DOS_PUNTOS;
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
            case "hacer": return PALABRA_RESERVADA_HAC;
            case "vacio": return PALABRA_RESERVADA_VAC;
            case "principal": return PALABRA_RESERVADA_PRIN;
            case "+": return OPERADOR_SUMA;
            case "-": return OPERADOR_RESTA;
            case "*": return OPERADOR_MULTIPLICAR;
            case "/": return OPERADOR_DIVISION;
            case "%": return OPERADOR_MODULO;
            case "=": return ASIGNACION;
            case "++": return INCREMENTO;
            case "--": return DECREMENTO;
            case "*=": return MUL_VARIABLE;
            case "/=": return DIV_VARIABLE;
            case "==": return IGUAL;
            case "!=": return DIFERENTE;
            case "<=": return MENOR_IGUAL;
            case ">=": return MAYOR_IGUAL;
            case "&": return AND;
            case "|": return OR;
            case "!": return NEGAR;
            case "<": return MENOR;
            case ">": return MAYOR;
  
        }
        Pattern tipoNumeroEntero=Pattern.compile("^(-)?\\d+$");
        Pattern identificador=Pattern.compile("^[a-zA-Z]([a-zA-Z_]|\\d)*");
        Pattern prClase=Pattern.compile("clase");
        Pattern prNuevo=Pattern.compile("nuevo");
        Pattern prEste=Pattern.compile("este");
        Pattern prPara=Pattern.compile("para");
        Pattern prPublico=Pattern.compile("publico");
        Pattern prPrivado=Pattern.compile("privado");
        Pattern prSi=Pattern.compile("si");
        Pattern prSino=Pattern.compile("sino");
        Pattern prSegun=Pattern.compile("segun");
        Pattern tipoCadena=Pattern.compile("\"(\\w| |\\d|\\S)+\"");
        Pattern tipoCaracter=Pattern.compile("'(\\w| |\\d|\\S)'");
        Pattern tipoNumeroReal=Pattern.compile("(\\d)+(\\.(\\d)+)?");
        
        
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
        m=prPara.matcher(cadena);
        if(m.matches()){
            return PALABRA_RESERVADA_PAR;
        }
        m=tipoCadena.matcher(cadena);
        if(m.matches()){
            return TIPO_CADENA;
        }
        m=tipoCaracter.matcher(cadena);
        if(m.matches()){
            return TIPO_CARACTER;
        }
        m=tipoNumeroReal.matcher(cadena);
        if(m.matches()){
            return NUMERO_REAL;
        }


        m=identificador.matcher(cadena);
        if (m.matches()) {
            return IDENTIFICADOR;

        }
        return ERROR;
    }
    
    public String mostrarTokens(){
        String resultado="";
        for(Token tok:tokenDetectados){
            if (tok.getLexema()==null) {
                return resultado;
            }
            resultado+=tok.getTipo().name()+"  ->  "+tok.getLexema()+"\n";
        }
        return resultado;
    }
    
    public void imprimirCodigo(){
        for(Token tok:tokenDetectados){
            System.out.println(tok.getLexema());
        }
    }
    

    
    
    public void generarTabla(){
        Tokens token;
        for (int j = 0; j < tokenDetectados.size(); j++) {
            token=tokenDetectados.get(j).getTipo();
            switch (token) {
                case PALABRA_RESERVADA_ENT:
                case PALABRA_RESERVADA_REA:
                case PALABRA_RESERVADA_CAR:
                case PALABRA_RESERVADA_CAD:
                case PALABRA_RESERVADA_BOO:
                    if (tokenDetectados.get(j+1)==null) {
                        JOptionPane.showMessageDialog(null, "OCURRIO UN ERROR", "ERROR", 0);
                        return;
                    }
                    if (tokenDetectados.get(j+1).getTipo()==IDENTIFICADOR) {
                        token=tokenDetectados.get(j).getTipo();
                        //tabla.add(new Identificadores(token,tokenDetectados.get(j+1).getLexema(),scope));
                    }else{
                        JOptionPane.showMessageDialog(null, "MAMO", "OCURRIO UN ERROR", 0);
                    }
                    break;
                default:
            }
        }
    }
    
    public void limpiarTabla(){
        tabla.clear();
    }
    
    public void imprimirTabla(){
        for(Identificadores iden:tabla){
            iden.mostrarIdentificadorTerminal();
        }
    }

    public List<Token> getTokensDetectados() {
        return tokenDetectados;
    }

    public ArrayList<Identificadores> getTabla() {
        return tabla;
    }
    
    
    
}
package AnalizadorSemantico;
import AnalizadorLexico.Identificadores;
import AnalizadorLexico.Tokens;
import static AnalizadorLexico.Tokens.*;
import java.util.ArrayList;

public class AnalizadorSemantico {
    
    
    public Tokens retornarTipo(Identificadores identificador){
        String tipo=identificador.getTipo();
        Tokens token;
        switch (tipo) {
            case "ENTERO":
                token=NUMERO_ENTERO;
                break;
            case "REAL":
                token=NUMERO_REAL;
                break;
            case "CADENA":
                token=TIPO_CADENA;
                break;
            case "CARACTER":
                token=TIPO_CARACTER;
                break;
            default:
                throw new AssertionError();
        }
        return token;
    }
    
    
    public boolean buscar(ArrayList<Identificadores> tabla,String nombre){
        if (tabla.isEmpty()) {
            return false;
        }
        for(Identificadores identificador: tabla){
            if(identificador.getIdentificador().equals(nombre)){
                return true;
            }
        }
        return false;
    }
    
    public Identificadores buscarIde(ArrayList<Identificadores> tabla,String nombre){
        if (tabla.isEmpty()) {
            return null;
        }
        for(Identificadores identificador: tabla){
            if(identificador.getIdentificador().equals(nombre)){
                return identificador;
            }
        }
        return null;
    }
    
    public boolean verificarIdentificador(ArrayList<Identificadores> tabla,String nombre){
        if (buscar(tabla,nombre)) {
            return false;
        }
        return true;
    }
    

}

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

    /**
     * Un identificador declarado en el scope {@code scopeIdentificador} es visible
     * desde {@code scopeActual} si el scope actual es el mismo o un descendiente
     * (rutas separadas por "/", p. ej. "global/principal/b1").
     */
    public boolean esVisible(String scopeIdentificador, String scopeActual){
        return scopeActual.equals(scopeIdentificador)
            || scopeActual.startsWith(scopeIdentificador + "/");
    }

    /**
     * Busca el identificador visible mas interno (el de scope mas profundo) con
     * ese nombre desde {@code scopeActual}. Devuelve {@code null} si no existe.
     */
    public Identificadores buscarIde(ArrayList<Identificadores> tabla,String nombre,String scopeActual){
        Identificadores encontrado=null;
        for(Identificadores identificador: tabla){
            if(identificador.getIdentificador().equals(nombre)
                    && esVisible(identificador.getScope(), scopeActual)){
                if(encontrado==null
                        || identificador.getScope().length()>encontrado.getScope().length()){
                    encontrado=identificador;
                }
            }
        }
        return encontrado;
    }

    public boolean buscar(ArrayList<Identificadores> tabla,String nombre,String scopeActual){
        return buscarIde(tabla,nombre,scopeActual)!=null;
    }

    /**
     * Comprueba si el nombre ya esta declarado en el scope EXACTO indicado
     * (redeclaracion en el mismo scope).
     */
    public boolean existeEnScope(ArrayList<Identificadores> tabla,String nombre,String scope){
        for(Identificadores identificador: tabla){
            if(identificador.getIdentificador().equals(nombre)
                    && scope.equals(identificador.getScope())){
                return true;
            }
        }
        return false;
    }

    public boolean verificarIdentificador(ArrayList<Identificadores> tabla,String nombre,String scopeActual){
        if (buscar(tabla,nombre,scopeActual)) {
            return false;
        }
        return true;
    }

    /** Mapea un token de tipo (palabra reservada) al tipo semantico. */
    public Tipo tipoDeToken(Tokens token){
        switch (token) {
            case PALABRA_RESERVADA_ENT: return Tipo.ENTERO;
            case PALABRA_RESERVADA_REA: return Tipo.REAL;
            case PALABRA_RESERVADA_CAD: return Tipo.CADENA;
            case PALABRA_RESERVADA_CAR: return Tipo.CARACTER;
            case PALABRA_RESERVADA_BOO: return Tipo.BOOLEANO;
            default: return null;
        }
    }

    /** Mapea el tipo textual guardado en un identificador al tipo semantico. */
    public Tipo tipoDeIdentificador(Identificadores identificador){
        return tipoDeNombre(identificador.getTipo());
    }

    public Tipo tipoDeNombre(String tipo){
        if (tipo == null) {
            return null;
        }
        switch (tipo) {
            case "ENTERO": return Tipo.ENTERO;
            case "REAL": return Tipo.REAL;
            case "CADENA": return Tipo.CADENA;
            case "CARACTER": return Tipo.CARACTER;
            case "BOLEANO":
            case "BOOLEANO": return Tipo.BOOLEANO;
            default: return null;
        }
    }

    /**
     * Reglas de compatibilidad de asignacion (tipo destino - tipo origen):
     *   entero   <- entero
     *   real     <- entero | real
     *   cadena   <- cadena
     *   caracter <- caracter
     *   booleano <- booleano
     */
    public boolean compatible(Tipo destino, Tipo origen){
        if (destino == null || origen == null) {
            return false;
        }
        switch (destino) {
            case ENTERO: return origen == Tipo.ENTERO;
            case REAL: return origen == Tipo.ENTERO || origen == Tipo.REAL;
            case CADENA: return origen == Tipo.CADENA;
            case CARACTER: return origen == Tipo.CARACTER;
            case BOOLEANO: return origen == Tipo.BOOLEANO;
            default: return false;
        }
    }


}

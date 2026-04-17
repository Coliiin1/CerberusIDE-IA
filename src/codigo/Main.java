package codigo;

import java.io.File;

/**
 *
 * @author fabri
 */
public class Main {
    public static void main(String[] args) {
        String ruta="C:/Users/fabri/Documents/NetBeansProjects/AnalizadorLexico/src/codigo/Lexer.flex";
        generarLexer(ruta);
    }
    public static void generarLexer(String ruta){
        File archivo=new File(ruta);
        JFlex.Main.generate(archivo);
    }
}

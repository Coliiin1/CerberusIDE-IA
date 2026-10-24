package ui;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorLexico.Token;
import AnalizadorLexico.Tokens;
import java.util.ArrayList;
import java.util.List;

public class SyntaxHighlighter {
    
    private final AnalizadorLexico lexer = new AnalizadorLexico();
    
    // Genera tokens con sus posiciones para coloreado del editor
    public List<SyntaxToken> highlight(String text) {
        List<SyntaxToken> tokens = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        
        // Procesamos línea por línea para obtener posiciones correctas
        String[] lines = text.split("\n", -1);
        int currentOffset = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.trim().isEmpty()) {
                // Usamos el lexer para tokenizar la línea
                List<Token> lexTokens = tokenizeLine(line);
                for (Token token : lexTokens) {
                    if (token.getLexema() == null) {
                        continue;
                    }
                    String lexema = token.getLexema();
                    // Buscar posición del lexema en la línea
                    int startInLine = line.indexOf(lexema);
                    if (startInLine >= 0) {
                        String category = mapTokenToCategory(token.getTipo());
                        tokens.add(new SyntaxToken(currentOffset + startInLine, lexema.length(), category));
                    }
                }
            }
            // Sumar longitud de línea + \n
            currentOffset += line.length() + 1;
        }
        
        return tokens;
    }
    
    private List<Token> tokenizeLine(String line) {
        List<Token> result = new ArrayList<>();
        // Aplicamos el mismo procesamiento que el lexer
        String processed = line;
        processed = processed.replace(";", " ; ");
        processed = processed.replace(",", " , ");
        processed = processed.replace(":", " : ");
        processed = processed.replace("{", " { ");
        processed = processed.replace("}", " } ");
        processed = processed.replace("(", " ( ");
        processed = processed.replace(")", " ) ");
        processed = processed.replace("[", " [ ");
        processed = processed.replace("]", " ] ");
        processed = processed.replace("+", " + ");
        processed = processed.replace("-", " - ");
        processed = processed.replace("*", " * ");
        processed = processed.replace("/", " / ");
        processed = processed.replace("%", " % ");
        processed = processed.replace("=", " = ");
        processed = processed.replace("!", " ! ");
        processed = processed.replace("<", " < ");
        processed = processed.replace(">", " > ");
        processed = processed.replace(" +  + ", " ++ ");
        processed = processed.replace(" -  - ", " -- ");
        processed = processed.replace("+  =", " += ");
        processed = processed.replace("-  =", " -= ");
        processed = processed.replace("*  =", " *= ");
        processed = processed.replace("/  =", " /= ");
        processed = processed.replace("=  =", " == ");
        processed = processed.replace("!  =", " != ");
        processed = processed.replace("<  =", " <= ");
        processed = processed.replace(">  =", " >= ");
        processed = processed.replace("&", " & ");
        processed = processed.replace("|", " | ");
        
        String[] tokens = processed.trim().split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            Tokens tipo = lexer.analizador(t);
            result.add(new Token(tipo, t, 0, 0));
        }
        return result;
    }
    
    private String mapTokenToCategory(Tokens tipo) {
        if (tipo == null) return "OTHER";
        
        // Palabras reservadas
        if (tipo.name().startsWith("PALABRA_RESERVADA_")) {
            // Tipos de dato
            if (tipo == Tokens.PALABRA_RESERVADA_ENT || tipo == Tokens.PALABRA_RESERVADA_REA ||
                tipo == Tokens.PALABRA_RESERVADA_CAR || tipo == Tokens.PALABRA_RESERVADA_CAD ||
                tipo == Tokens.PALABRA_RESERVADA_BOO || tipo == Tokens.PALABRA_RESERVADA_NUL ||
                tipo == Tokens.PALABRA_RESERVADA_VAC) {
                return "TYPE";
            }
            return "KEYWORD";
        }
        
        // Tipos literales
        if (tipo == Tokens.TIPO_CADENA) return "STRING";
        if (tipo == Tokens.TIPO_CARACTER) return "CHAR";
        if (tipo == Tokens.NUMERO_ENTERO || tipo == Tokens.NUMERO_REAL) return "NUMBER";
        if (tipo == Tokens.IDENTIFICADOR) return "IDENTIFIER";
        if (tipo == Tokens.ERROR) return "ERROR";
        
        // Operadores
        if (tipo.name().startsWith("OPERADOR_") || tipo == Tokens.ASIGNACION ||
            tipo == Tokens.INCREMENTO || tipo == Tokens.DECREMENTO ||
            tipo == Tokens.MAS_VARIABLE || tipo == Tokens.MENOS_VARIABLE ||
            tipo == Tokens.MUL_VARIABLE || tipo == Tokens.DIV_VARIABLE ||
            tipo == Tokens.IGUAL || tipo == Tokens.DIFERENTE ||
            tipo == Tokens.MENOR || tipo == Tokens.MENOR_IGUAL ||
            tipo == Tokens.MAYOR || tipo == Tokens.MAYOR_IGUAL ||
            tipo == Tokens.AND || tipo == Tokens.OR || tipo == Tokens.NEGAR) {
            return "OPERATOR";
        }
        
        // Puntuación
        if (tipo == Tokens.DOS_PUNTOS || tipo == Tokens.PUNTO_COMA || tipo == Tokens.COMA ||
            tipo == Tokens.PUNTO || tipo == Tokens.PARENTESIS_ABRE || tipo == Tokens.PARENTESIS_CIERRA ||
            tipo == Tokens.LLAVE_ABRE || tipo == Tokens.LLAVE_CIERRA ||
            tipo == Tokens.CORCHETE_ABRE || tipo == Tokens.CORCHETE_CIERRA) {
            return "PUNCTUATION";
        }
        
        return "OTHER";
    }
}

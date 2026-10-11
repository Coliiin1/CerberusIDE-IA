package audit;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Oraculo headless del compilador Cerberus (Fase 1 de CERBERUSIA_IMPLEMENTATION.md).
 *
 * Uso:
 *   java -cp target\classes audit.CompiladorCLI <archivo.txt>
 *   (sin argumentos, lee el codigo de stdin)
 *
 * Emite por stdout un objeto JSON:
 *   {"aceptado": true, "error": null, "linea": -1, "columna": -1, "lexema": null}
 *
 * Exit code: 0 si el codigo es aceptado, 1 si es rechazado.
 * Nota: el ruido de depuracion del parser se silencia durante la evaluacion, de
 * modo que stdout contiene unicamente el JSON.
 */
public class CompiladorCLI {

    public static void main(String[] args) throws IOException {
        String codigo = leerEntrada(args);
        PrintStream realOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        Evaluador.Resultado r;
        try {
            r = Evaluador.evaluar(codigo);
        } finally {
            System.setOut(realOut);
        }
        // Salida JSON en UTF-8 explicito (independiente de la codificacion de consola).
        PrintStream utf8 = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        utf8.println(aJson(r));
        utf8.flush();
        System.exit(r.aceptado ? 0 : 1);
    }

    private static String leerEntrada(String[] args) throws IOException {
        if (args.length > 0) {
            return Files.readString(Paths.get(args[0]), StandardCharsets.UTF_8);
        }
        return new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String aJson(Evaluador.Resultado r) {
        return "{"
                + "\"aceptado\": " + r.aceptado + ", "
                + "\"error\": " + jsonString(r.error) + ", "
                + "\"linea\": " + r.linea + ", "
                + "\"columna\": " + r.columna + ", "
                + "\"lexema\": " + jsonString(r.lexema)
                + "}";
    }

    private static String jsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}

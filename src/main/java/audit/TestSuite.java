package audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Suite de pruebas minima del compilador Cerberus, basada en archivos.
 *
 * Recorre:
 *   tests/valid/            -> cada archivo debe ser ACEPTADO por el compilador.
 *   tests/invalid/          -> cada archivo debe ser RECHAZADO.
 *   tests/expected/invalid/ -> fragmento del mensaje de error esperado (opcional,
 *                              mismo nombre de archivo que el caso invalid).
 *
 * Usa el evaluador compartido {@link Evaluador}.
 *
 * Ejecucion (desde la raiz del proyecto, tras `mvnw.cmd compile`):
 *   java -cp target\classes audit.TestSuite
 *
 * Devuelve codigo de salida 0 si todos los casos pasan, 1 en caso contrario.
 */
public class TestSuite {

    private static final Path VALID = Paths.get("tests", "valid");
    private static final Path INVALID = Paths.get("tests", "invalid");
    private static final Path EXPECTED = Paths.get("tests", "expected", "invalid");

    /**
     * Evalua el codigo con el evaluador compartido.
     *
     * @return null si el compilador acepta el codigo; en caso contrario, el
     *         mensaje de error capturado.
     */
    private static String evaluar(String codigo) {
        Evaluador.Resultado r = Evaluador.evaluar(codigo);
        if (r.aceptado) {
            return null;
        }
        return r.error == null ? "" : r.error;
    }

    private static List<Path> listar(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    public static void main(String[] args) throws IOException {
        int pass = 0;
        int fail = 0;
        List<String> fallos = new ArrayList<>();

        for (Path f : listar(VALID)) {
            String codigo = Files.readString(f, StandardCharsets.UTF_8);
            String error = evaluar(codigo);
            if (error == null) {
                pass++;
                System.out.println("PASS [valid]   " + f.getFileName());
            } else {
                fail++;
                fallos.add("FAIL [valid] " + f.getFileName() + " deberia ser aceptado pero fue rechazado: " + error);
                System.out.println("FAIL [valid]   " + f.getFileName() + " -> " + error);
            }
        }

        for (Path f : listar(INVALID)) {
            String codigo = Files.readString(f, StandardCharsets.UTF_8);
            String error = evaluar(codigo);
            if (error == null) {
                fail++;
                fallos.add("FAIL [invalid] " + f.getFileName() + " deberia ser rechazado pero fue aceptado");
                System.out.println("FAIL [invalid] " + f.getFileName() + " -> aceptado inesperadamente");
                continue;
            }

            Path esperado = EXPECTED.resolve(f.getFileName().toString());
            if (Files.exists(esperado)) {
                String fragmento = Files.readString(esperado, StandardCharsets.UTF_8).trim();
                if (fragmento.isEmpty() || error.contains(fragmento)) {
                    pass++;
                    System.out.println("PASS [invalid] " + f.getFileName());
                } else {
                    fail++;
                    fallos.add("FAIL [invalid] " + f.getFileName() + " error inesperado: " + error
                            + " (se esperaba que contuviera: " + fragmento + ")");
                    System.out.println("FAIL [invalid] " + f.getFileName() + " -> " + error);
                }
            } else {
                pass++;
                System.out.println("PASS [invalid] " + f.getFileName());
            }
        }

        System.out.println();
        System.out.println("Resumen: " + pass + " PASS, " + fail + " FAIL.");
        if (fail > 0) {
            System.out.println();
            fallos.forEach(System.out::println);
            System.exit(1);
        }
    }
}

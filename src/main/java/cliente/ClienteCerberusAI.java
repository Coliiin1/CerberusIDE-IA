package cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente HTTP de la API de CerberusAI (Java puro, sin JavaFX).
 *
 * Por defecto apunta a la API local (http://localhost:8000). Para usar la API
 * remota (quick tunnel de Cloudflare, URL https://...trycloudflare.com) se pasa
 * la URL al constructor o se define la variable de entorno CERBERUSAI_API_URL.
 */
public class ClienteCerberusAI {

    public static final String URL_POR_DEFECTO = "http://localhost:8000";
    public static final Duration TIEMPO_LIMITE_POR_DEFECTO = Duration.ofSeconds(180);
    private static final Duration TIEMPO_LIMITE_CONEXION = Duration.ofSeconds(10);

    public record Solicitud(String role, String prompt) {
    }

    public record Respuesta(
            String texto,
            @JsonProperty("es_codigo") boolean esCodigo,
            String codigo,
            Boolean compila,
            @JsonProperty("error_compilacion") String errorCompilacion) {
    }

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIEMPO_LIMITE_CONEXION)
            .build();

    private final String baseUrl;
    private final Duration tiempoLimite;

    public ClienteCerberusAI() {
        this(urlPorDefecto());
    }

    public ClienteCerberusAI(String baseUrl) {
        this(baseUrl, TIEMPO_LIMITE_POR_DEFECTO);
    }

    public ClienteCerberusAI(String baseUrl, Duration tiempoLimite) {
        this.baseUrl = normalizar(baseUrl);
        this.tiempoLimite = tiempoLimite;
    }

    /** URL de la API: variable de entorno CERBERUSAI_API_URL o el default local. */
    public static String urlPorDefecto() {
        String env = System.getenv("CERBERUSAI_API_URL");
        return (env == null || env.isBlank()) ? URL_POR_DEFECTO : env.trim();
    }

    /** Envia una peticion POST /chat y devuelve la respuesta de la API. */
    public Respuesta chat(String rol, String prompt) throws IOException, InterruptedException {
        String cuerpo = mapper.writeValueAsString(new Solicitud(rol, prompt));
        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat"))
                .timeout(tiempoLimite)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> respuesta = http.send(peticion,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (respuesta.statusCode() / 100 != 2) {
            throw new IOException(mensajeDeError(respuesta));
        }
        return mapper.readValue(respuesta.body(), Respuesta.class);
    }

    /** GET /health: true si la API responde 2xx. No lanza excepciones. */
    public boolean ping() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(TIEMPO_LIMITE_CONEXION)
                    .GET()
                    .build();
            HttpResponse<Void> respuesta = http.send(peticion, HttpResponse.BodyHandlers.discarding());
            return respuesta.statusCode() / 100 == 2;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private static String normalizar(String url) {
        String limpia = (url == null || url.isBlank()) ? urlPorDefecto() : url.trim();
        while (limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }

    /** Extrae el mensaje de error de FastAPI ({"detail": "..."}) o resume el HTTP. */
    private String mensajeDeError(HttpResponse<String> respuesta) {
        String detalle = null;
        try {
            detalle = mapper.readTree(respuesta.body()).path("detail").asText(null);
        } catch (IOException ignorado) {
            // Cuerpo no JSON: se usa el resumen HTTP.
        }
        if (detalle != null && !detalle.isBlank()) {
            return detalle;
        }
        String cuerpo = respuesta.body() == null ? "" : respuesta.body().strip();
        if (!cuerpo.isEmpty()) {
            return "HTTP " + respuesta.statusCode() + ": " + recortar(cuerpo, 200);
        }
        return "HTTP " + respuesta.statusCode();
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }
}

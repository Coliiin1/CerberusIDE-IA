package ui;

import cliente.ClienteCerberusAI;
import cliente.GrabadorMicrofono;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controlador del componente de chat CerberusAI (ui/chat_cerberus.fxml).
 *
 * Es un componente autonomo: no conoce el editor del IDE. El contenedor decide
 * que hacer con el codigo generado conectando un handler:
 *
 *     chatCerberusController.setInsertarHandler(codigo -> txtCodigo.setText(codigo));
 *
 * Sin handler, el boton "Insertar en editor" permanece deshabilitado. La URL de
 * la API se configura desde el contenedor con {@link #setApiUrl(String)}.
 */
public class ChatCerberusController implements Initializable {

    @FXML
    private ComboBox<String> cmbRol;

    @FXML
    private TextArea txtChatPrompt;

    @FXML
    private Button btnEnviarChat;

    @FXML
    private Button btnInsertarEditor;

    @FXML
    private Button btnDictar;

    @FXML
    private Label lblChatEstado;

    @FXML
    private TextArea txtChatRespuesta;

    private Consumer<String> insertarHandler;
    private String ultimoCodigo;
    private String apiUrl = ClienteCerberusAI.urlPorDefecto();
    private final GrabadorMicrofono grabador = new GrabadorMicrofono();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbRol.setItems(FXCollections.observableArrayList("Generar", "Explicar", "Corregir"));
        cmbRol.getSelectionModel().selectFirst();

        btnEnviarChat.setOnAction(e -> enviar());
        btnInsertarEditor.setOnAction(e -> insertar());
        btnDictar.setOnAction(e -> alternarDictado());
        actualizarEstado("Listo.", null);
    }

    /** El contenedor conecta aqui la insercion del codigo en el editor. */
    public void setInsertarHandler(Consumer<String> handler) {
        this.insertarHandler = handler;
        actualizarBotonInsertar();
    }

    /** Configura la URL de la API (por ejemplo, la URL del tunel de Cloudflare). */
    public void setApiUrl(String url) {
        String limpia = url == null ? "" : url.trim();
        this.apiUrl = limpia.isEmpty() ? ClienteCerberusAI.urlPorDefecto() : limpia;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    private void enviar() {
        String prompt = txtChatPrompt.getText() == null ? "" : txtChatPrompt.getText().trim();
        if (prompt.isEmpty()) {
            actualizarEstado("Escribe una peticion antes de enviar.", "error");
            return;
        }
        String rol = rolApi();

        btnEnviarChat.setDisable(true);
        btnInsertarEditor.setDisable(true);
        ultimoCodigo = null;
        txtChatRespuesta.setText("Consultando a CerberusAI...");
        actualizarEstado("Consultando (" + rol + ")...", null);

        ClienteCerberusAI cliente = new ClienteCerberusAI(apiUrl);
        Task<ClienteCerberusAI.Respuesta> tarea = new Task<>() {
            @Override
            protected ClienteCerberusAI.Respuesta call() throws Exception {
                return cliente.chat(rol, prompt);
            }
        };
        tarea.setOnSucceeded(e -> {
            ClienteCerberusAI.Respuesta r = tarea.getValue();
            txtChatRespuesta.setText(r.texto());
            if (r.codigo() != null && !r.codigo().isBlank()) {
                ultimoCodigo = r.codigo();
                if (Boolean.TRUE.equals(r.compila())) {
                    actualizarEstado("Codigo generado: compila correctamente.", "ok");
                } else if (Boolean.FALSE.equals(r.compila())) {
                    actualizarEstado("Codigo generado pero NO compila: " + resumir(r.errorCompilacion()), "error");
                } else {
                    actualizarEstado("Codigo generado (sin validar por el oraculo).", null);
                }
            } else {
                actualizarEstado("Respuesta sin codigo insertable.", null);
            }
            btnEnviarChat.setDisable(false);
            actualizarBotonInsertar();
        });
        tarea.setOnFailed(e -> {
            txtChatRespuesta.setText("No se pudo consultar la API:\n" + mensaje(tarea.getException()));
            actualizarEstado("Error de conexion con la API.", "error");
            btnEnviarChat.setDisable(false);
            actualizarBotonInsertar();
        });

        Thread hilo = new Thread(tarea, "cerberusai-chat");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void alternarDictado() {
        if (!grabador.isGrabando()) {
            try {
                grabador.iniciar();
            } catch (Exception ex) {
                actualizarEstado("No hay micrófono disponible: " + mensaje(ex), "error");
                return;
            }
            btnDictar.setText("■ Detener");
            btnDictar.getStyleClass().add("grabando");
            btnEnviarChat.setDisable(true);
            actualizarEstado("Escuchando... (máx. " + GrabadorMicrofono.SEGUNDOS_MAXIMOS + " s)", null);
            return;
        }

        btnDictar.setText("🎤 Dictar");
        btnDictar.getStyleClass().remove("grabando");
        btnDictar.setDisable(true);
        actualizarEstado("Transcribiendo...", null);

        ClienteCerberusAI cliente = new ClienteCerberusAI(apiUrl);
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() throws Exception {
                return cliente.transcribir(grabador.detener());
            }
        };
        tarea.setOnSucceeded(e -> {
            String texto = tarea.getValue();
            if (texto == null || texto.isBlank()) {
                actualizarEstado("No se entendió el audio.", "error");
            } else {
                String actual = txtChatPrompt.getText() == null ? "" : txtChatPrompt.getText().strip();
                txtChatPrompt.setText(actual.isEmpty() ? texto : actual + " " + texto);
                txtChatPrompt.positionCaret(txtChatPrompt.getLength());
                actualizarEstado("Dictado listo. Revisa y pulsa Enviar.", "ok");
            }
            terminarDictado();
        });
        tarea.setOnFailed(e -> {
            actualizarEstado("Error al transcribir: " + resumir(mensaje(tarea.getException())), "error");
            terminarDictado();
        });

        Thread hilo = new Thread(tarea, "cerberusai-dictado");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void terminarDictado() {
        btnDictar.setDisable(false);
        btnEnviarChat.setDisable(false);
    }

    private void insertar() {
        if (insertarHandler != null && ultimoCodigo != null && !ultimoCodigo.isBlank()) {
            insertarHandler.accept(ultimoCodigo);
        }
    }

    private String rolApi() {
        String seleccion = cmbRol.getValue();
        return seleccion == null ? "generar" : seleccion.toLowerCase();
    }

    private void actualizarBotonInsertar() {
        boolean hayCodigo = ultimoCodigo != null && !ultimoCodigo.isBlank();
        btnInsertarEditor.setDisable(!(hayCodigo && insertarHandler != null));
    }

    private void actualizarEstado(String texto, String tipo) {
        lblChatEstado.setText(texto);
        lblChatEstado.getStyleClass().removeAll("ok", "error");
        if (tipo != null) {
            lblChatEstado.getStyleClass().add(tipo);
        }
    }

    private static String resumir(String texto) {
        if (texto == null || texto.isBlank()) {
            return "(sin detalle)";
        }
        String primera = texto.strip().split("\n")[0];
        return primera.length() <= 120 ? primera : primera.substring(0, 120) + "...";
    }

    private static String mensaje(Throwable ex) {
        if (ex == null) {
            return "(error desconocido)";
        }
        String texto = ex.getMessage();
        return texto == null || texto.isBlank() ? ex.getClass().getSimpleName() : texto;
    }
}

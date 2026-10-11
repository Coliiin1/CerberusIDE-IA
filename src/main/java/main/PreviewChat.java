package main;

import cliente.ClienteCerberusAI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.ChatCerberusController;

/**
 * Herramienta de desarrollo (no forma parte del IDE): muestra solo el panel de
 * chat para probarlo aislado, sin tocar principal.fxml.
 *
 * Uso normal (ventana):
 *     mvnw.cmd javafx:run -Dmain.class=main.PreviewChat
 *
 * Smoke test sin ventana (carga el FXML y consulta la API real), util para CI/manual:
 *     set CERBERUS_PREVIEW_CHECK=1
 *     mvnw.cmd javafx:run -Dmain.class=main.PreviewChat
 */
public class PreviewChat extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(PreviewChat.class.getResource("/ui/chat_cerberus.fxml"));
        Parent panel = loader.load();
        ChatCerberusController chat = loader.getController();

        if (System.getenv("CERBERUS_PREVIEW_CHECK") != null) {
            smoke(chat);
            Platform.exit();
            return;
        }

        TextArea editor = new TextArea();
        editor.setPromptText("Aqui se insertaria el codigo generado (simula txtCodigo del IDE).");
        editor.setStyle("-fx-font-family: Consolas;");

        BorderPane root = new BorderPane(editor);
        root.setRight(panel);
        root.getStyleClass().add("modo-oscuro");

        chat.setInsertarHandler(codigo -> editor.setText(codigo));

        Scene scene = new Scene(root, 980, 640);
        scene.getStylesheets().add(PreviewChat.class.getResource("/ui/estilos.css").toExternalForm());
        stage.setTitle("CerberusAI - Preview del chat (dev)");
        stage.setScene(scene);
        stage.show();
    }

    private void smoke(ChatCerberusController chat) {
        System.out.println("FXML OK: controller=" + (chat != null));
        ClienteCerberusAI cliente = new ClienteCerberusAI();
        System.out.println("API " + cliente.getBaseUrl() + " health=" + cliente.ping());
        try {
            ClienteCerberusAI.Respuesta r = cliente.chat(
                    "generar", "imprime los numeros del 1 al 5 con un ciclo para");
            System.out.println("chat generar: esCodigo=" + r.esCodigo()
                    + " compila=" + r.compila()
                    + " error=" + r.errorCompilacion());
            System.out.println("codigo:");
            System.out.println(r.codigo());
        } catch (Exception e) {
            System.out.println("chat generar: ERROR " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class CerberusApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/ui/principal.fxml"));
        Scene scene = new Scene(root, 1750, 950);
        scene.getStylesheets().add(getClass().getResource("/ui/estilos.css").toExternalForm());
        stage.setTitle("CERBERUS IDE");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/Imagenes/logoC.png")));
        stage.setScene(scene);
        stage.show();
    }

}

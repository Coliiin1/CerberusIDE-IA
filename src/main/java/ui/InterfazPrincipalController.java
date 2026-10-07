package ui;

import Archivos.Archivo;
import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

public class InterfazPrincipalController implements Initializable {

    private final AnalizadorLexico lex = new AnalizadorLexico();
    private AnalizadorSintactico sin;
    private File archivo;

    @FXML
    private BorderPane root;

    @FXML
    private Button btnCompilar;

    @FXML
    private Label txtArchivo;

    @FXML
    private TextArea txtCodigo;

    @FXML
    private TextArea txtLineas;

    @FXML
    private TextArea txtLexico;

    @FXML
    private TextArea txtSintactico;

    @FXML
    private TextArea txtSemantico;

    @FXML
    private TextArea txtDocumentador;

    @FXML
    private MenuItem itemArchivoAbrir;

    @FXML
    private MenuItem itemArchivoNuevo;

    @FXML
    private MenuItem itemOpcionesSalir;

    @FXML
    private RadioMenuItem radioModoClaro;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lex.setReporter(this::mostrarError);

        btnCompilar.setOnAction(e -> compilar());
        itemArchivoAbrir.setOnAction(e -> abrirArchivo());
        itemArchivoNuevo.setOnAction(e -> nuevoArchivo());
        itemOpcionesSalir.setOnAction(e -> System.exit(0));
        radioModoClaro.setOnAction(e -> actualizarTema());

        txtCodigo.textProperty().addListener((obs, o, n) -> actualizarLineas());
        txtCodigo.scrollTopProperty().addListener((obs, o, n) -> txtLineas.setScrollTop(n.doubleValue()));

        aplicarModoOscuro();
        actualizarLineas();
    }

    private void abrirArchivo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Abrir archivo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Texto (.txt)", "*.txt"));
        File f = selector.showOpenDialog(root.getScene().getWindow());
        if (f == null) {
            return;
        }
        try {
            txtCodigo.setText(Archivo.leer(f));
            archivo = f;
            txtArchivo.setText("ARCHIVO: " + f.getAbsolutePath());
        } catch (IOException ex) {
            mostrarError("ERROR AL LEER ARCHIVO");
        }
    }

    private void nuevoArchivo() {
        txtCodigo.setText("");
        archivo = null;
        txtArchivo.setText("ARCHIVO: ");
    }

    private void compilar() {
        lex.limpiarTabla();

        if (archivo == null) {
            String nombre = "";
            while (true) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Nuevo archivo");
                dialog.setHeaderText("INGRESA EL NOMBRE DEL ARCHIVO");
                Optional<String> resultado = dialog.showAndWait();
                if (!resultado.isPresent()) {
                    return;
                }
                nombre = resultado.get().trim();
                if (!nombre.isEmpty()) {
                    break;
                }
            }
            if (Archivo.crear(nombre, txtCodigo.getText())) {
                mostrarInfo("ARCHIVO CREADO CON EXITO");
            } else {
                mostrarError("ERROR AL CREAR ARCHIVO");
            }
        } else {
            try {
                if (!Archivo.leer(archivo).equals(txtCodigo.getText())) {
                    if (Archivo.guardar(archivo.getName(), txtCodigo.getText())) {
                        mostrarInfo("ARCHIVO GUARDADO CON EXITO");
                    } else {
                        mostrarError("ERROR AL GUARDAR ARCHIVO");
                    }
                }
            } catch (IOException ex) {
                mostrarError("ERROR AL LEER ARCHIVO");
            }
        }

        // ===== LEXICO =====
        if (lex.seccionarCadena(txtCodigo.getText())) {
            txtLexico.setText("ANALIZADOR LÉXICO PASADO\n\n" + lex.mostrarTokens());
            lex.generarTabla();
            sin = new AnalizadorSintactico(lex.getTokensDetectados(), this::mostrarError);
        } else {
            txtLexico.setText("ANALIZADOR LÉXICO NO PASADO\n\n" + lex.mostrarTokens());
            return;
        }

        // ===== SINTACTICO =====
        if (sin.comprobarParentesis()) {
            sin.mostrarParentesis();
        } else {
            txtSintactico.setText("HAY UN PROBLEMA CON LLAVES, PARÉNTESIS Y CORCHETES");
        }
        sin.analizar();
        if (sin.getResultado()) {
            txtSintactico.setText("ANÁLISIS SINTÁCTICO PASADO\n\n" + sin.imprimirTabla());
        } else {
            txtSintactico.setText("NO SE PASÓ EL ANÁLISIS SINTÁCTICO");
        }

        // ===== SEMANTICO =====
        if (sin.getResultado()) {
            txtSemantico.setText("SE PASO EL ANALIZADOR SEMANTICO");
        } else {
            txtSemantico.setText("NO SE EJECUTO EL ANALIZADOR SEMANTICO (fallo el sintactico)");
        }

        // ===== DOCUMENTADOR =====
        if (!sin.getResultado()) {
            txtDocumentador.setText("NO SE PASO NI EL SEMANTICO NI EL SINTACTICO NO SE PUEDE CREAR DOCUMENTADOR");
        } else {
            txtDocumentador.setText(sin.getDocumentador().imprimir());
        }
    }

    private void actualizarLineas() {
        String texto = txtCodigo.getText();
        int lineas = texto.isEmpty() ? 1 : texto.split("\n", -1).length;
        StringBuilder numeros = new StringBuilder();
        for (int i = 1; i <= lineas; i++) {
            numeros.append(i).append("\n");
        }
        txtLineas.setText(numeros.toString());
    }

    private void actualizarTema() {
        if (radioModoClaro.isSelected()) {
            aplicarModoClaro();
        } else {
            aplicarModoOscuro();
        }
    }

    private void aplicarModoOscuro() {
        root.getStyleClass().remove("modo-claro");
        if (!root.getStyleClass().contains("modo-oscuro")) {
            root.getStyleClass().add("modo-oscuro");
        }
    }

    private void aplicarModoClaro() {
        root.getStyleClass().remove("modo-oscuro");
        if (!root.getStyleClass().contains("modo-claro")) {
            root.getStyleClass().add("modo-claro");
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR, mensaje);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

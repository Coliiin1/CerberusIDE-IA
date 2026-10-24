package ui;

import AnalizadorLexico.AnalizadorLexico;
import AnalizadorSintactico.AnalizadorSintactico;
import cliente.ClienteCerberusAI;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public class InterfazPrincipalController implements Initializable {

    private static final String PREF_API_URL = "cerberusai.api.url";
    private static final String PREF_ULTIMA_CARPETA = "cerberus.proyecto.ultimaCarpeta";
    private static final long TAMANO_MAXIMO_ARCHIVO = 2L * 1024 * 1024;
    private static final Set<String> CARPETAS_IGNORADAS = Set.of(
            ".git", ".idea", ".mvn", "target", "node_modules");

    private final AnalizadorLexico lex = new AnalizadorLexico();
    private final Preferences preferencias = Preferences.userNodeForPackage(InterfazPrincipalController.class);
    private AnalizadorSintactico sin;
    private Path proyectoActual;
    private boolean saliendo;

    @FXML
    private BorderPane root;

    @FXML
    private Button btnCompilar;

    @FXML
    private Button btnAbrirProyecto;

    @FXML
    private Button btnRefrescarProyecto;

    @FXML
    private ChatCerberusController chatCerberusController;

    @FXML
    private Label lblProyecto;

    @FXML
    private Label lblProyectoVacio;

    @FXML
    private Label lblPosicion;

    @FXML
    private TreeView<Path> treeProyecto;

    @FXML
    private TabPane tabsEditor;

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
    private MenuItem itemArchivoAbrirProyecto;

    @FXML
    private MenuItem itemArchivoGuardar;

    @FXML
    private MenuItem itemArchivoNuevo;

    @FXML
    private MenuItem itemOpcionesSalir;

    @FXML
    private MenuItem itemEditarCortar;

    @FXML
    private MenuItem itemEditarCopiar;

    @FXML
    private MenuItem itemEditarPegar;

    @FXML
    private MenuItem itemConfigurarApiIA;

    @FXML
    private RadioMenuItem radioModoClaro;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lex.setReporter(this::mostrarError);

        btnCompilar.setOnAction(e -> compilar());
        btnAbrirProyecto.setOnAction(e -> abrirProyecto());
        btnRefrescarProyecto.setOnAction(e -> refrescarProyecto());
        itemArchivoAbrir.setOnAction(e -> abrirArchivoDialogo());
        itemArchivoAbrirProyecto.setOnAction(e -> abrirProyecto());
        itemArchivoGuardar.setOnAction(e -> guardarPestanaActiva());
        itemArchivoNuevo.setOnAction(e -> nuevoArchivo());
        itemOpcionesSalir.setOnAction(e -> salir());
        itemEditarCortar.setOnAction(e -> conEditorActivo(TextArea::cut));
        itemEditarCopiar.setOnAction(e -> conEditorActivo(TextArea::copy));
        itemEditarPegar.setOnAction(e -> conEditorActivo(TextArea::paste));
        itemConfigurarApiIA.setOnAction(e -> configurarApi());
        radioModoClaro.setOnAction(e -> actualizarTema());

        chatCerberusController.setInsertarHandler(this::insertarEnEditor);
        chatCerberusController.setApiUrl(urlApiGuardada());

        configurarExplorador();
        tabsEditor.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> actualizarPosicion());

        Platform.runLater(() -> {
            Window ventana = ventana();
            if (ventana != null) {
                ventana.setOnCloseRequest(this::alCerrarVentana);
            }
        });

        aplicarModoOscuro();
        nuevoArchivo();
    }

    private void configurarExplorador() {
        treeProyecto.setCellFactory(tv -> {
            FileTreeCell celda = new FileTreeCell();
            celda.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Path ruta = celda.getItem();
                    if (ruta != null && Files.isRegularFile(ruta)) {
                        abrirArchivoEnEditor(ruta);
                    }
                }
            });
            return celda;
        });

        treeProyecto.setOnKeyPressed(event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }
            TreeItem<Path> seleccionado = treeProyecto.getSelectionModel().getSelectedItem();
            if (seleccionado != null && seleccionado.getValue() != null
                    && Files.isRegularFile(seleccionado.getValue())) {
                abrirArchivoEnEditor(seleccionado.getValue());
            }
        });
    }

    private void abrirProyecto() {
        DirectoryChooser selector = new DirectoryChooser();
        selector.setTitle("Abrir carpeta como proyecto");
        Path inicial = ultimaCarpeta();
        if (inicial != null) {
            selector.setInitialDirectory(inicial.toFile());
        }
        File carpeta = selector.showDialog(ventana());
        if (carpeta != null) {
            cargarProyecto(carpeta.toPath().toAbsolutePath().normalize());
        }
    }

    private void cargarProyecto(Path raiz) {
        cargarProyecto(raiz, new HashSet<>());
    }

    private void cargarProyecto(Path raiz, Set<Path> expandidos) {
        proyectoActual = raiz;

        TreeItem<Path> itemRaiz = new TreeItem<>(raiz);
        treeProyecto.setRoot(itemRaiz);
        cargarHijos(itemRaiz);
        itemRaiz.setExpanded(true);

        expandidos.stream()
                .sorted(Comparator.comparingInt(Path::getNameCount))
                .forEach(ruta -> {
                    TreeItem<Path> item = buscarItem(itemRaiz, ruta);
                    if (item == null) {
                        return;
                    }
                    if (!item.getChildren().isEmpty() && item.getChildren().get(0).getValue() == null) {
                        cargarHijos(item);
                    }
                    item.setExpanded(true);
                });

        treeProyecto.setVisible(true);
        treeProyecto.setManaged(true);
        lblProyectoVacio.setVisible(false);
        lblProyectoVacio.setManaged(false);
        lblProyecto.setText(nombreDe(raiz));
        preferencias.put(PREF_ULTIMA_CARPETA, raiz.toString());
        actualizarTituloVentana();
    }

    private void cargarHijos(TreeItem<Path> item) {
        Path carpeta = item.getValue();
        if (carpeta == null || !Files.isDirectory(carpeta)) {
            return;
        }

        boolean estabaExpandido = item.isExpanded();
        List<Path> hijos = new ArrayList<>();

        try (Stream<Path> stream = Files.list(carpeta)) {
            stream.filter(this::visible)
                    .sorted(Comparator
                            .comparing((Path ruta) -> !Files.isDirectory(ruta))
                            .thenComparing(ruta -> ruta.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(hijos::add);
        } catch (IOException ex) {
            mostrarError("No se pudo leer la carpeta:\n" + carpeta);
            return;
        }

        item.getChildren().clear();
        for (Path hijo : hijos) {
            TreeItem<Path> itemHijo = new TreeItem<>(hijo);
            if (Files.isDirectory(hijo)) {
                itemHijo.getChildren().add(new TreeItem<>(null));
                itemHijo.expandedProperty().addListener((obs, anterior, expandido) -> {
                    if (Boolean.TRUE.equals(expandido)
                            && !itemHijo.getChildren().isEmpty()
                            && itemHijo.getChildren().get(0).getValue() == null) {
                        cargarHijos(itemHijo);
                    }
                });
            }
            item.getChildren().add(itemHijo);
        }

        if (estabaExpandido) {
            item.setExpanded(true);
        }
    }

    private boolean visible(Path ruta) {
        Path nombre = ruta.getFileName();
        if (nombre == null) {
            return false;
        }
        String texto = nombre.toString();
        if (CARPETAS_IGNORADAS.contains(texto)) {
            return false;
        }
        return !(Files.isDirectory(ruta) && texto.startsWith("."));
    }

    private TreeItem<Path> buscarItem(TreeItem<Path> raiz, Path objetivo) {
        if (raiz == null || raiz.getValue() == null) {
            return null;
        }
        if (raiz.getValue().equals(objetivo)) {
            return raiz;
        }
        for (TreeItem<Path> hijo : raiz.getChildren()) {
            if (hijo.getValue() == null) {
                continue;
            }
            TreeItem<Path> encontrado = buscarItem(hijo, objetivo);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    private void recolectarExpandidos(TreeItem<Path> item, Set<Path> destinos) {
        if (item == null || item.getValue() == null) {
            return;
        }
        if (item.isExpanded()) {
            destinos.add(item.getValue());
        }
        for (TreeItem<Path> hijo : item.getChildren()) {
            if (hijo.getValue() != null) {
                recolectarExpandidos(hijo, destinos);
            }
        }
    }

    private void refrescarProyecto() {
        if (proyectoActual == null) {
            mostrarInfo("No hay ningún proyecto abierto.");
            return;
        }
        Set<Path> expandidos = new HashSet<>();
        recolectarExpandidos(treeProyecto.getRoot(), expandidos);
        cargarProyecto(proyectoActual, expandidos);
    }

    private void refrescarCarpetaDe(Path archivo) {
        if (proyectoActual == null || archivo == null) {
            return;
        }
        Path ruta = archivo.toAbsolutePath().normalize();
        if (!ruta.startsWith(proyectoActual)) {
            return;
        }
        Path padre = ruta.getParent();
        if (padre == null) {
            return;
        }
        TreeItem<Path> item = buscarItem(treeProyecto.getRoot(), padre);
        if (item != null) {
            cargarHijos(item);
        }
    }

    private Path ultimaCarpeta() {
        String guardada = preferencias.get(PREF_ULTIMA_CARPETA, "");
        if (guardada.isBlank()) {
            return null;
        }
        try {
            Path ruta = Paths.get(guardada);
            return Files.isDirectory(ruta) ? ruta : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void abrirArchivoDialogo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Abrir archivo");
        if (proyectoActual != null && Files.isDirectory(proyectoActual)) {
            selector.setInitialDirectory(proyectoActual.toFile());
        }
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos de texto",
                        "*.txt", "*.md", "*.json", "*.csv", "*.java", "*.xml", "*.yml", "*.yaml"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        File archivo = selector.showOpenDialog(ventana());
        if (archivo != null) {
            abrirArchivoEnEditor(archivo.toPath());
        }
    }

    private void abrirArchivoEnEditor(Path ruta) {
        Path normalizada = ruta.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizada)) {
            return;
        }

        for (Tab tab : tabsEditor.getTabs()) {
            EditorTab editor = editorDe(tab);
            if (editor != null && normalizada.equals(editor.getArchivo())) {
                tabsEditor.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            if (Files.size(normalizada) > TAMANO_MAXIMO_ARCHIVO) {
                mostrarError("El archivo es demasiado grande para abrirlo en el editor (máximo 2 MB).");
                return;
            }
            String contenido = Files.readString(normalizada, StandardCharsets.UTF_8);
            agregarPestana(new EditorTab(normalizada, contenido));
        } catch (MalformedInputException ex) {
            mostrarError("No se puede abrir el archivo: no es un archivo de texto UTF-8.\n" + normalizada);
        } catch (IOException ex) {
            mostrarError("ERROR AL LEER ARCHIVO:\n" + normalizada);
        }
    }

    private void agregarPestana(EditorTab editor) {
        Tab tab = editor.getTab();
        tab.setUserData(editor);
        tab.setOnCloseRequest(event -> {
            if (!confirmarCierrePestana(editor)) {
                event.consume();
            }
        });
        editor.getCodigo().caretPositionProperty()
                .addListener((obs, anterior, nuevo) -> actualizarPosicion());

        tabsEditor.getTabs().add(tab);
        tabsEditor.getSelectionModel().select(tab);
        actualizarPosicion();
        Platform.runLater(editor.getCodigo()::requestFocus);
    }

    private void nuevoArchivo() {
        agregarPestana(new EditorTab(null, ""));
    }

    private void guardarPestanaActiva() {
        EditorTab editor = editorActivo();
        if (editor != null) {
            guardarEditor(editor);
        }
    }

    private boolean guardarEditor(EditorTab editor) {
        if (editor.getArchivo() == null) {
            return guardarComo(editor);
        }
        try {
            Files.writeString(editor.getArchivo(), editor.getContenido(), StandardCharsets.UTF_8);
            editor.marcarGuardado();
            return true;
        } catch (IOException ex) {
            mostrarError("ERROR AL GUARDAR ARCHIVO:\n" + editor.getArchivo());
            return false;
        }
    }

    private boolean guardarComo(EditorTab editor) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar como");
        selector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        if (proyectoActual != null && Files.isDirectory(proyectoActual)) {
            selector.setInitialDirectory(proyectoActual.toFile());
        }

        File destino = selector.showSaveDialog(ventana());
        if (destino == null) {
            return false;
        }

        Path ruta = destino.toPath().toAbsolutePath().normalize();
        Path nombre = ruta.getFileName();
        if (nombre == null) {
            mostrarError("Nombre de archivo inválido.");
            return false;
        }
        if (!nombre.toString().contains(".")) {
            ruta = ruta.resolveSibling(nombre + ".txt");
        }

        try {
            Files.writeString(ruta, editor.getContenido(), StandardCharsets.UTF_8);
            editor.setArchivo(ruta);
            editor.marcarGuardado();
            refrescarCarpetaDe(ruta);
            return true;
        } catch (IOException ex) {
            mostrarError("ERROR AL GUARDAR ARCHIVO:\n" + ruta);
            return false;
        }
    }

    private boolean confirmarCierrePestana(EditorTab editor) {
        if (!editor.isDirty()) {
            return true;
        }

        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
        ButtonType noGuardar = new ButtonType("No guardar", ButtonBar.ButtonData.NO);

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Cambios sin guardar");
        alerta.setHeaderText("El archivo «" + editor.getNombre() + "» tiene cambios sin guardar.");
        alerta.setContentText("¿Deseas guardarlos antes de cerrar?");
        alerta.getButtonTypes().setAll(guardar, noGuardar, ButtonType.CANCEL);

        Optional<ButtonType> resultado = alerta.showAndWait();
        if (resultado.isEmpty() || resultado.get() == ButtonType.CANCEL) {
            return false;
        }
        if (resultado.get() == guardar) {
            return guardarEditor(editor);
        }
        return true;
    }

    private boolean confirmarCierrePendientes() {
        for (Tab tab : new ArrayList<>(tabsEditor.getTabs())) {
            EditorTab editor = editorDe(tab);
            if (editor != null && !confirmarCierrePestana(editor)) {
                return false;
            }
        }
        return true;
    }

    private void salir() {
        if (saliendo) {
            Platform.exit();
            return;
        }
        if (confirmarCierrePendientes()) {
            saliendo = true;
            Platform.exit();
        }
    }

    private void alCerrarVentana(WindowEvent event) {
        if (saliendo) {
            return;
        }
        if (!confirmarCierrePendientes()) {
            event.consume();
        }
    }

    private void insertarEnEditor(String codigo) {
        EditorTab editor = editorActivo();
        if (editor == null) {
            editor = new EditorTab(null, "");
            agregarPestana(editor);
        }
        editor.setContenido(codigo);
    }

    private void conEditorActivo(Consumer<TextArea> accion) {
        EditorTab editor = editorActivo();
        if (editor != null) {
            accion.accept(editor.getCodigo());
        }
    }

    private EditorTab editorActivo() {
        return editorDe(tabsEditor.getSelectionModel().getSelectedItem());
    }

    private EditorTab editorDe(Tab tab) {
        if (tab == null) {
            return null;
        }
        return tab.getUserData() instanceof EditorTab editor ? editor : null;
    }

    private void actualizarPosicion() {
        EditorTab editor = editorActivo();
        if (editor == null) {
            lblPosicion.setText("Línea 1, columna 1");
            return;
        }

        String texto = editor.getContenido();
        int posicion = Math.min(editor.getCodigo().getCaretPosition(), texto.length());
        int linea = 1;
        int columna = 1;
        for (int i = 0; i < posicion; i++) {
            if (texto.charAt(i) == '\n') {
                linea++;
                columna = 1;
            } else {
                columna++;
            }
        }
        lblPosicion.setText("Línea " + linea + ", columna " + columna);
    }

    private void compilar() {
        EditorTab editor = editorActivo();
        if (editor == null) {
            mostrarInfo("Abre o crea un archivo antes de compilar.");
            return;
        }
        if (editor.isDirty() || editor.getArchivo() == null) {
            if (!guardarEditor(editor)) {
                return;
            }
        }

        lex.limpiarTabla();
        String codigo = editor.getContenido();

        if (lex.seccionarCadena(codigo)) {
            txtLexico.setText("ANALIZADOR LÉXICO PASADO\n\n" + lex.mostrarTokens());
            lex.generarTabla();
            sin = new AnalizadorSintactico(lex.getTokensDetectados(), this::mostrarError);
        } else {
            txtLexico.setText("ANALIZADOR LÉXICO NO PASADO\n\n" + lex.mostrarTokens());
            return;
        }

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

        if (sin.getResultado()) {
            txtSemantico.setText("SE PASO EL ANALIZADOR SEMANTICO");
        } else {
            txtSemantico.setText("NO SE EJECUTO EL ANALIZADOR SEMANTICO (fallo el sintactico)");
        }

        if (!sin.getResultado()) {
            txtDocumentador.setText("NO SE PASO NI EL SEMANTICO NI EL SINTACTICO NO SE PUEDE CREAR DOCUMENTADOR");
        } else {
            txtDocumentador.setText(sin.getDocumentador().imprimir());
        }
    }

    private void configurarApi() {
        TextInputDialog dialog = new TextInputDialog(chatCerberusController.getApiUrl());
        dialog.setTitle("Configuracion de CerberusAI");
        dialog.setHeaderText("URL de la API de CerberusAI");
        dialog.setContentText("URL (vacio = localhost):");
        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isEmpty()) {
            return;
        }
        String entrada = resultado.get().trim();
        if (entrada.isEmpty()) {
            preferencias.remove(PREF_API_URL);
            chatCerberusController.setApiUrl(ClienteCerberusAI.urlPorDefecto());
            mostrarInfo("API restablecida a " + ClienteCerberusAI.urlPorDefecto());
            return;
        }
        String url = normalizarUrlApi(entrada);
        if (url == null) {
            mostrarError("URL invalida. Usa http://... o https://...");
            return;
        }
        preferencias.put(PREF_API_URL, url);
        chatCerberusController.setApiUrl(url);
        mostrarInfo("URL de la API actualizada.");
    }

    private String urlApiGuardada() {
        String guardada = preferencias.get(PREF_API_URL, "").trim();
        if (guardada.isEmpty()) {
            return ClienteCerberusAI.urlPorDefecto();
        }
        String normalizada = normalizarUrlApi(guardada);
        return normalizada == null ? ClienteCerberusAI.urlPorDefecto() : normalizada;
    }

    private static String normalizarUrlApi(String texto) {
        String limpia = texto == null ? "" : texto.trim();
        while (limpia.endsWith("/")) {
            limpia = limpia.substring(0, limpia.length() - 1);
        }
        if (limpia.isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(limpia);
            String esquema = uri.getScheme();
            if (esquema == null || uri.getHost() == null) {
                return null;
            }
            if (!esquema.equalsIgnoreCase("http") && !esquema.equalsIgnoreCase("https")) {
                return null;
            }
            return limpia;
        } catch (IllegalArgumentException e) {
            return null;
        }
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

    private void actualizarTituloVentana() {
        if (ventana() instanceof Stage stage) {
            stage.setTitle(proyectoActual == null
                    ? "CERBERUS IDE"
                    : "CERBERUS IDE - " + nombreDe(proyectoActual));
        }
    }

    private Window ventana() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }

    private static String nombreDe(Path ruta) {
        Path nombre = ruta.getFileName();
        return nombre == null ? ruta.toString() : nombre.toString();
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

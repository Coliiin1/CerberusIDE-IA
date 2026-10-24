package ui;

import java.nio.file.Path;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class EditorTab {

    private Path archivo;
    private final Tab tab;
    private final TextArea codigo;
    private final TextArea lineas;
    private boolean dirty;

    public EditorTab(Path archivo, String contenido) {
        this.archivo = archivo;

        this.codigo = new TextArea(contenido == null ? "" : contenido);
        this.codigo.setWrapText(false);
        this.codigo.setMaxWidth(Double.MAX_VALUE);
        this.codigo.setMaxHeight(Double.MAX_VALUE);
        this.codigo.getStyleClass().add("editor");

        this.lineas = new TextArea();
        this.lineas.setEditable(false);
        this.lineas.setFocusTraversable(false);
        this.lineas.setWrapText(false);
        this.lineas.setPrefWidth(48);
        this.lineas.setMinWidth(48);
        this.lineas.setMaxWidth(64);
        this.lineas.setMaxHeight(Double.MAX_VALUE);
        this.lineas.getStyleClass().add("lineas");

        HBox caja = new HBox();
        caja.getStyleClass().add("code-workspace");
        caja.setSpacing(0);
        caja.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        caja.getChildren().addAll(lineas, codigo);
        HBox.setHgrow(codigo, Priority.ALWAYS);

        this.tab = new Tab();
        this.tab.setContent(caja);
        actualizarTitulo();

        codigo.textProperty().addListener((obs, anterior, nuevo) -> {
            marcarModificado();
            actualizarLineas();
        });
        codigo.scrollTopProperty().addListener((obs, anterior, nuevo) ->
                lineas.setScrollTop(nuevo.doubleValue()));

        actualizarLineas();
        this.dirty = false;
        actualizarTitulo();
    }

    public Tab getTab() {
        return tab;
    }

    public TextArea getCodigo() {
        return codigo;
    }

    public String getContenido() {
        return codigo.getText();
    }

    public void setContenido(String contenido) {
        codigo.setText(contenido == null ? "" : contenido);
    }

    public Path getArchivo() {
        return archivo;
    }

    public void setArchivo(Path archivo) {
        this.archivo = archivo;
        actualizarTitulo();
    }

    public String getNombre() {
        if (archivo == null || archivo.getFileName() == null) {
            return "Sin título";
        }
        return archivo.getFileName().toString();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void marcarModificado() {
        if (!dirty) {
            dirty = true;
            actualizarTitulo();
        }
    }

    public void marcarGuardado() {
        if (dirty) {
            dirty = false;
            actualizarTitulo();
        }
    }

    private void actualizarTitulo() {
        tab.setText((dirty ? "● " : "") + getNombre());
        String ruta = archivo == null ? "Archivo sin guardar" : archivo.toString();
        tab.setTooltip(new Tooltip(ruta));
    }

    private void actualizarLineas() {
        String texto = codigo.getText();
        int total = texto.isEmpty() ? 1 : texto.split("\n", -1).length;
        StringBuilder numeros = new StringBuilder();
        for (int i = 1; i <= total; i++) {
            if (i > 1) {
                numeros.append('\n');
            }
            numeros.append(i);
        }
        lineas.setText(numeros.toString());
    }
}

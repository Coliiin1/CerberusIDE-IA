package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class ColorCustomizerDialog extends Dialog<Void> {
    
    private TableView<CategoriaColor> tableView;
    private ObservableList<CategoriaColor> categorias;
    
    public ColorCustomizerDialog() {
        setTitle("Personalizar colores del editor");
        setHeaderText("Selecciona los colores para cada tipo de token (solo afecta al editor de código)");
        initModality(Modality.APPLICATION_MODAL);
        
        // Crear categorías
        categorias = FXCollections.observableArrayList();
        categorias.add(new CategoriaColor("KEYWORD", "Palabras reservadas", ColorThemePrefs.getColor("KEYWORD")));
        categorias.add(new CategoriaColor("TYPE", "Tipos de dato", ColorThemePrefs.getColor("TYPE")));
        categorias.add(new CategoriaColor("STRING", "Cadenas de texto", ColorThemePrefs.getColor("STRING")));
        categorias.add(new CategoriaColor("CHAR", "Caracteres", ColorThemePrefs.getColor("CHAR")));
        categorias.add(new CategoriaColor("NUMBER", "Números", ColorThemePrefs.getColor("NUMBER")));
        categorias.add(new CategoriaColor("IDENTIFIER", "Identificadores", ColorThemePrefs.getColor("IDENTIFIER")));
        categorias.add(new CategoriaColor("OPERATOR", "Operadores", ColorThemePrefs.getColor("OPERATOR")));
        categorias.add(new CategoriaColor("PUNCTUATION", "Signos de puntuación", ColorThemePrefs.getColor("PUNCTUATION")));
        categorias.add(new CategoriaColor("ERROR", "Errores", ColorThemePrefs.getColor("ERROR")));
        categorias.add(new CategoriaColor("OTHER", "Otros", ColorThemePrefs.getColor("OTHER")));
        
        // Crear tabla
        tableView = new TableView<>(categorias);
        tableView.setPrefHeight(300);
        
        TableColumn<CategoriaColor, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colCategoria.setPrefWidth(200);
        
        TableColumn<CategoriaColor, String> colColor = new TableColumn<>("Color");
        colColor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getColor()));
        colColor.setPrefWidth(100);
        colColor.setCellFactory(column -> new TableCell<CategoriaColor, String>() {
            private final ColorPicker colorPicker = new ColorPicker();
            
            {
                colorPicker.setOnAction(event -> {
                    CategoriaColor categoria = getTableView().getItems().get(getIndex());
                    String hex = toHexString(colorPicker.getValue());
                    categoria.setColor(hex);
                    ColorThemePrefs.setColor(categoria.getClave(), hex);
                });
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CategoriaColor categoria = getTableRow().getItem();
                    colorPicker.setValue(javafx.scene.paint.Color.web(categoria.getColor()));
                    setGraphic(colorPicker);
                }
            }
        });
        
        tableView.getColumns().addAll(colCategoria, colColor);
        
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(new Label("Personaliza los colores del resaltado:"), tableView);
        
        getDialogPane().setContent(vbox);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Cerrar");
    }
    
    private String toHexString(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
    
    public static class CategoriaColor {
        private String clave;
        private String nombre;
        private String color;
        
        public CategoriaColor(String clave, String nombre, String color) {
            this.clave = clave;
            this.nombre = nombre;
            this.color = color;
        }
        
        public String getClave() { return clave; }
        public String getNombre() { return nombre; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}

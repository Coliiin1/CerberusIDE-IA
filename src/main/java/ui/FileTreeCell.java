package ui;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;

public class FileTreeCell extends TreeCell<Path> {

    @Override
    protected void updateItem(Path item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().removeAll("tree-cell-folder", "tree-cell-file");

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
            return;
        }

        boolean directorio = Files.isDirectory(item);
        String nombre = item.getFileName() == null ? item.toString() : item.getFileName().toString();
        setText((directorio ? "▸  " : "◇  ") + nombre);
        setTooltip(new Tooltip(item.toAbsolutePath().toString()));
        getStyleClass().add(directorio ? "tree-cell-folder" : "tree-cell-file");
    }
}

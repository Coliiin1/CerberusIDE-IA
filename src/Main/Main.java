package Main;
import UI.InterfazPrincipal;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception e) {
            }
            InterfazPrincipal ui = new InterfazPrincipal();
            ui.iniciarComponentes();
            ui.setVisible(true);
        });
    }
}

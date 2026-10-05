module CerberusIDE {
    requires javafx.controls;
    requires javafx.fxml;

    opens main to javafx.fxml;
    opens ui to javafx.fxml;

    exports main;
    exports ui;
    exports AnalizadorLexico;
    exports AnalizadorSintactico;
    exports AnalizadorSemantico;
    exports Archivos;
    exports Util;
}

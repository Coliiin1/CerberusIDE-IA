module CerberusIDE {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens main to javafx.fxml;
    opens ui to javafx.fxml;
    opens cliente to com.fasterxml.jackson.databind;

    exports main;
    exports ui;
    exports AnalizadorLexico;
    exports AnalizadorSintactico;
    exports AnalizadorSemantico;
    exports Archivos;
    exports Util;
}

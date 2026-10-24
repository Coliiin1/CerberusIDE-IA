package ui;

import java.util.prefs.Preferences;
import java.util.HashMap;
import java.util.Map;

public class ColorThemePrefs {
    private static final String PREF_PREFIX = "syntax.color.";
    private static final Preferences prefs = Preferences.userNodeForPackage(ColorThemePrefs.class);

    // Colores por defecto estilo NetBeans
    private static final Map<String, String> DEFAULT_COLORS = new HashMap<>();

    static {
        // Modo oscuro por defecto
        DEFAULT_COLORS.put("KEYWORD", "#569CD6");      // Palabras reservadas
        DEFAULT_COLORS.put("TYPE", "#4EC9B0");         // Tipos de dato
        DEFAULT_COLORS.put("STRING", "#CE9178");       // Cadenas
        DEFAULT_COLORS.put("CHAR", "#CE9178");         // Caracteres
        DEFAULT_COLORS.put("NUMBER", "#B5CEA8");       // Números
        DEFAULT_COLORS.put("IDENTIFIER", "#9CDCFE");   // Identificadores
        DEFAULT_COLORS.put("OPERATOR", "#D4D4D4");     // Operadores
        DEFAULT_COLORS.put("PUNCTUATION", "#D4D4D4");   // Signos de puntuación
        DEFAULT_COLORS.put("ERROR", "#F44747");        // Errores
        DEFAULT_COLORS.put("OTHER", "#D4D4D4");         // Otros
    }

    public static String getColor(String category) {
        String key = PREF_PREFIX + category;
        return prefs.get(key, DEFAULT_COLORS.getOrDefault(category, "#D4D4D4"));
    }

    public static void setColor(String category, String hexColor) {
        String key = PREF_PREFIX + category;
        if (hexColor == null || hexColor.isEmpty()) {
            prefs.remove(key);
        } else {
            prefs.put(key, hexColor);
        }
    }

    public static void restoreDefaults() {
        for (String category : DEFAULT_COLORS.keySet()) {
            prefs.remove(PREF_PREFIX + category);
        }
    }

    public static Map<String, String> getAllDefaults() {
        return new HashMap<>(DEFAULT_COLORS);
    }
}

package ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeEditorPainter {
    
    private final Canvas canvas;
    private final Map<String, Color> colorMap = new HashMap<>();
    private Font font;
    private double lineHeight;
    
    public CodeEditorPainter(Canvas canvas) {
        this.canvas = canvas;
        this.font = Font.font("Consolas", 13);
        updateColors();
    }
    
    public void updateColors() {
        colorMap.clear();
        colorMap.put("KEYWORD", Color.web(ColorThemePrefs.getColor("KEYWORD")));
        colorMap.put("TYPE", Color.web(ColorThemePrefs.getColor("TYPE")));
        colorMap.put("STRING", Color.web(ColorThemePrefs.getColor("STRING")));
        colorMap.put("CHAR", Color.web(ColorThemePrefs.getColor("CHAR")));
        colorMap.put("NUMBER", Color.web(ColorThemePrefs.getColor("NUMBER")));
        colorMap.put("IDENTIFIER", Color.web(ColorThemePrefs.getColor("IDENTIFIER")));
        colorMap.put("OPERATOR", Color.web(ColorThemePrefs.getColor("OPERATOR")));
        colorMap.put("PUNCTUATION", Color.web(ColorThemePrefs.getColor("PUNCTUATION")));
        colorMap.put("ERROR", Color.web(ColorThemePrefs.getColor("ERROR")));
        colorMap.put("OTHER", Color.web(ColorThemePrefs.getColor("OTHER")));
    }
    
    public void paint(String text, List<SyntaxToken> tokens, double scrollTop) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // Calcular altura de línea
        lineHeight = font.getSize() * 1.4;
        gc.setFont(font);
        gc.setTextBaseline(VPos.TOP);
        
        // Dibujar tokens coloreados
        String[] lines = text.split("\n", -1);
        int charOffset = 0;
        
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[i(lineNum)];
            double y = lineNum * lineHeight - scrollTop;
            
            if (y < -lineHeight || y > canvas.getHeight()) {
                charOffset += line.length() + 1;
                continue;
            }
            
            // Dibujar cada token coloreado en esta línea
            for (SyntaxToken token : tokens) {
                if (token.getStartIndex() >= charOffset && token.getStartIndex() < charOffset + line.length()) {
                    int tokenStartInLine = token.getStartIndex() - charOffset;
                    int tokenEndInLine = Math.min(tokenStartInLine + token.getLength(), line.length());
                    if (tokenStartInLine >= 0) {
                        String tokenText = line.substring(tokenStartInLine, tokenEndInLine);
                        if (!tokenText.isEmpty()) {
                            // Calcular ancho antes del token
                            String beforeToken = line.substring(0, tokenStartInLine);
                            double xOffset = computeTextWidth(gc, beforeToken);
                            Color color = colorMap.getOrDefault(token.getCategory(), Color.web("#D4D4D4"));
                            gc.setFill(color);
                            gc.fillText(tokenText, xOffset, y);
                        }
                    }
                }
            }
            charOffset += line.length() + 1;
        }
    }
    
    private int i(int n) { return n; }
    
    private double computeTextWidth(GraphicsContext gc, String text) {
        if (text.isEmpty()) return 0;
        // Aproximación simple usando medidas de fuente
        return text.length() * font.getSize() * 0.6;
    }
    
    public void setFont(Font font) {
        this.font = font;
    }
}

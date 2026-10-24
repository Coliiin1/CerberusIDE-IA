package ui;

import java.util.ArrayList;
import java.util.List;

public class SyntaxToken {
    private int startIndex;
    private int length;
    private String category; // KEYWORD, TYPE, STRING, CHAR, NUMBER, IDENTIFIER, OPERATOR, PUNCTUATION, ERROR, OTHER

    public SyntaxToken(int startIndex, int length, String category) {
        this.startIndex = startIndex;
        this.length = length;
        this.category = category;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getLength() {
        return length;
    }

    public String getCategory() {
        return category;
    }
}

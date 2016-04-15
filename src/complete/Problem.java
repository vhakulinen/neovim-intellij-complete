package complete;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Problem {
    @JsonProperty
    private int line;
    @JsonProperty
    private int column;
    @JsonProperty
    private String text;

    public Problem(int line, int column, String text) {
        this.line = line;
        this.column = column;
        this.text = text;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getText() {
        return text;
    }
}

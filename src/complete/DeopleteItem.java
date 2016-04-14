package complete;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeopleteItem {

    @JsonProperty
    private String word;
    @JsonProperty
    private String info;
    @JsonProperty
    private String kind;

    public DeopleteItem(
            String word,
            String info,
            String kind) {
        this.word = word;
        this.info = info;
        this.kind = kind;
    }

    public String getWord() {
        return word;
    }

    public String getInfo() {
        return info;
    }

    // Appends info with Strings and newline
    public void appendInfo(String s) {
        this.info += '\n' + s;
    }

    public String getKind() {
        return kind;
    }
}

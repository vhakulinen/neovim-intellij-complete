package codeinspect;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ville on 4/19/16.
 */
public class Fix {
    @JsonProperty
    private String description;
    @JsonProperty
    private int fixId;


    public Fix(String description, int fixId) {
        this.description = description;
        this.fixId = fixId;
    }

    public String getDescription() {
        return description;
    }

    public int getFixId() {
        return fixId;
    }
}

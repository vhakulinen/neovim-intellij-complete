package complete;

/**
 * @author traff
 */
public class ResolveOutcome {
    public static final ResolveOutcome NULL = new ResolveOutcome("", -1, -1, "");

    private final String myFilePath;
    private final int myColumn;
    private final int myRow;
    private final String myText; // a text to help identify outcome (element's presentation text, line preview, etc.)

    public ResolveOutcome(String path, int row, int column, String text) {
        myFilePath = path;
        myColumn = column;
        myRow = row;
        myText = text != null ? text : "";
    }

    public String getFilePath() {
        return myFilePath;
    }

    public int getColumn() {
        return myColumn;
    }

    public int getRow() {
        return myRow;
    }

    public String getText() {
        return myText;
    }
}

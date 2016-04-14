package complete;

public class Problem {
  private int line;
  private int column;
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

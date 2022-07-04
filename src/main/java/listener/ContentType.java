package listener;

public enum ContentType {

  SUMMARY(""), DOWNLOAD1("download"), DOWNLOAD2("download"), DOWNLOAD3("download"), TRAILER("trailer"), TITLE("summary"), SUBTITLE("subtitle"), IMAGE("image");
  private String name;

  ContentType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}

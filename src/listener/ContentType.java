package listener;

public enum ContentType {

    SUMMARY(""), DOWNLOAD1("download"), DOWNLOAD2("download"), DOWNLOAD3("download"), STREAM1("stream"), STREAM2("stream"), TRAILER("trailer"), TITLE("summary"),
    SUBTITLE("subtitle"), IMAGE("image");
    private String name;

    ContentType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

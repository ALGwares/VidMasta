package listener;

public enum ContentType {

    SUMMARY(""), DOWNLOAD1("Download"), DOWNLOAD2("Download"), DOWNLOAD3("Download"), STREAM1("Stream"), STREAM2("Stream"), TRAILER("Trailer"), TITLE("Summary"),
    SUBTITLE("Subtitle"), IMAGE("Image");
    private String name;

    ContentType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

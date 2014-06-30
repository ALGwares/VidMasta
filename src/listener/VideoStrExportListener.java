package listener;

public interface VideoStrExportListener {

    void export(ContentType contentType, String str, boolean cancel, GuiListener guiListener);

    boolean exportSecondaryContent();

    boolean showTVChoices();

    void setEpisode(String season, String episode);
}

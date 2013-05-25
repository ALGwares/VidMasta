package onlinesearch.download;

import listener.GuiListener;

public class TorrentSearchState {

    public final String format, minSize, maxSize;
    public final String[] whitelistedFileExts, blacklistedFileExts;
    public final boolean canIgnoreDownloadSize;

    public TorrentSearchState(GuiListener guiListener) {
        format = guiListener.getFormat();
        minSize = guiListener.getMinDownloadSize();
        maxSize = guiListener.getMaxDownloadSize();
        whitelistedFileExts = guiListener.getWhitelistedFileExts();
        blacklistedFileExts = guiListener.getBlacklistedFileExts();
        canIgnoreDownloadSize = guiListener.canIgnoreDownloadSize();
    }

    public TorrentSearchState(TorrentSearchState searchState) {
        format = searchState.format;
        minSize = searchState.minSize;
        maxSize = searchState.maxSize;
        whitelistedFileExts = searchState.whitelistedFileExts;
        blacklistedFileExts = searchState.blacklistedFileExts;
        canIgnoreDownloadSize = searchState.canIgnoreDownloadSize;
    }
}
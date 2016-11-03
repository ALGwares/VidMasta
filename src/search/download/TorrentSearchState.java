package search.download;

import listener.GuiListener;
import str.Str;
import util.Constant;

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

    @Override
    public String toString() {
        String state = "";
        if (!format.equals(Constant.ANY)) {
            state += ' ' + format;
        }
        boolean hasMinSize = !minSize.equals("0"), hasMaxSize = !maxSize.equals(Constant.INFINITY);
        if (hasMinSize && hasMaxSize) {
            if (!state.isEmpty()) {
                state += ',';
            }
            state += ' ' + minSize + '-' + maxSize + Str.str("GB");
        } else if (hasMinSize && !hasMaxSize) {
            if (!state.isEmpty()) {
                state += ',';
            }
            state += " \u2265" + minSize + Str.str("GB");
        } else if (!hasMinSize && hasMaxSize) {
            if (!state.isEmpty()) {
                state += ',';
            }
            state += " \u2264" + maxSize + Str.str("GB");
        }
        if (blacklistedFileExts.length != 0) {
            if (!state.isEmpty()) {
                state += ',';
            }
            for (String blacklistedFileExt : blacklistedFileExts) {
                state += " -" + blacklistedFileExt;
            }
        }
        return state;
    }
}

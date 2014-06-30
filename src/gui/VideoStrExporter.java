package gui;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import listener.ContentType;
import listener.GuiListener;
import listener.VideoStrExportListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.Regex;

public class VideoStrExporter implements VideoStrExportListener {

    private final String TITLE, YEAR;
    private final boolean IS_TV_SHOW, EXPORT_TO_EMAIL, EXPORT_SECONDARY_CONTENT;
    private final int NUM_STRS_TO_EXPORT;
    private final Map<ContentType, String> strs = new ConcurrentSkipListMap<ContentType, String>();
    private boolean showTVChoices = true;
    private volatile String season, episode;
    private static final String COPY_MSG = " copied and paste-able";

    public VideoStrExporter(String title, String year, boolean isTVShow, boolean exportToEmail, boolean exportSecondaryContent, int numStrsToExport) {
        TITLE = title;
        YEAR = year;
        IS_TV_SHOW = isTVShow;
        EXPORT_TO_EMAIL = exportToEmail;
        EXPORT_SECONDARY_CONTENT = exportSecondaryContent;
        NUM_STRS_TO_EXPORT = numStrsToExport;
    }

    @Override
    public void export(ContentType contentType, String str, boolean cancel, GuiListener guiListener) {
        try {
            export(contentType, cancel || str == null ? "" : str.trim(), guiListener);
        } catch (Exception e) {
            if (!cancel) {
                guiListener.error(e);
            }
        }
    }

    private void export(ContentType contentType, String str, GuiListener guiListener) throws Exception {
        strs.put(contentType, str);
        if (strs.size() != NUM_STRS_TO_EXPORT) {
            return;
        }

        String summary = strs.get(ContentType.SUMMARY);
        if (summary == null) {
            summary = "";
        } else {
            if (summary.isEmpty()) {
                summary = Regex.htmlToPlainText(TITLE) + ' ' + YEAR;
                if (!EXPORT_TO_EMAIL) {
                    strs.put(ContentType.SUMMARY, summary);
                }
            }
            if (EXPORT_TO_EMAIL) {
                strs.put(ContentType.SUMMARY, summary += ' ' + (IS_TV_SHOW ? "(TV Show" + episode() + ')' : "(Movie)"));
            }
        }

        String imagePath = null;
        StringBuilder strBuf = new StringBuilder(NUM_STRS_TO_EXPORT * 256);
        Set<String> lines = new HashSet<String>(NUM_STRS_TO_EXPORT);

        for (Entry<ContentType, String> entry : strs.entrySet()) {
            if (!lines.add(entry.toString())) {
                continue;
            }
            String currStr = entry.getValue();
            if (currStr.isEmpty()) {
                continue;
            }
            ContentType type = entry.getKey();
            if (type == ContentType.IMAGE) {
                imagePath = currStr;
                continue;
            }
            if (EXPORT_TO_EMAIL) {
                String typeName = type.toString();
                if (!typeName.isEmpty()) {
                    strBuf.append(type).append(": ");
                }
                strBuf.append(currStr).append("\n\n");
            } else {
                strBuf.append(currStr).append('\n');
            }
        }

        String content = strBuf.toString();
        if (EXPORT_TO_EMAIL) {
            if (content.trim().equals(summary)) {
                notFound(guiListener);
                return;
            }
            String emailBody = content + Str.get(625);
            if (guiListener.canEmailWithDefaultApp()) {
                Connection.email(summary, emailBody);
                if (UI.exportToClipboard(imagePath, "")) {
                    guiListener.timedMsg("image" + COPY_MSG + " also");
                }
                return;
            }
            UI.exportToClipboard(imagePath, emailBody);
            guiListener.timedMsg("content" + (imagePath == null ? "" : " and image") + COPY_MSG);
        } else if (UI.exportToClipboard(imagePath, content.trim())) {
            for (ContentType type : new ContentType[]{ContentType.DOWNLOAD1, ContentType.DOWNLOAD2, ContentType.DOWNLOAD3, ContentType.STREAM1,
                ContentType.STREAM2, ContentType.SUBTITLE}) {
                if (strs.get(type) != null) {
                    guiListener.timedMsg("content" + COPY_MSG);
                    return;
                }
            }
        } else {
            notFound(guiListener);
        }
    }

    @Override
    public boolean exportSecondaryContent() {
        return EXPORT_SECONDARY_CONTENT;
    }

    private static void notFound(GuiListener guiListener) {
        guiListener.timedMsg("content not copied, retry");
    }

    @Override
    public boolean showTVChoices() {
        boolean prevShowTVChoices = showTVChoices;
        showTVChoices = false;
        return prevShowTVChoices;
    }

    @Override
    public void setEpisode(String season, String episode) {
        this.season = season;
        this.episode = episode;
    }

    private String episode() {
        Integer seasonNum = toInteger(season), episodeNum = toInteger(episode);
        return seasonNum != null ? (" Season " + seasonNum + (episodeNum == null ? "" : " Episode " + episodeNum)) : (episodeNum != null ? " Episode "
                + episodeNum : "");
    }

    private static Integer toInteger(String number) {
        return number == null || number.equals(Constant.ANY) ? null : Integer.valueOf(number);
    }
}

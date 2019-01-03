package search;

import debug.Debug;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import listener.ContentType;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import listener.VideoStrExportListener;
import search.util.VideoSearch;
import str.Str;
import torrent.StreamingTorrentUtil;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;
import util.Worker;

public class SubtitleFinder extends Worker {

    private final GuiListener guiListener;
    private final String format, languageID;
    private final Video video;
    private boolean isTVShow, isTVShowAndMovie, stopped, complete;
    private final boolean foreground, firstMatch;
    private VideoStrExportListener strExportListener;
    private String subtitleLink, prevUrl;
    private static final Map<Long, String> cache = new HashMap<Long, String>(8);

    public SubtitleFinder(GuiListener guiListener, boolean foreground, String format, String languageID, Video video, boolean firstMatch,
            VideoStrExportListener strExportListener) {
        this.guiListener = guiListener;
        this.foreground = foreground;
        this.format = format;
        this.languageID = languageID;
        this.video = video;
        isTVShow = video.IS_TV_SHOW;
        isTVShowAndMovie = video.IS_TV_SHOW_AND_MOVIE;
        this.firstMatch = firstMatch;
        this.strExportListener = strExportListener;
    }

    @Override
    protected void doWork() {
        if (foreground) {
            guiListener.subtitleSearchStarted();
        }
        try {
            findSubtitle();
            searchStopped();
        } catch (Exception e) {
            searchStopped();
            if (!isCancelled()) {
                guiListener.error(e);
                if (!complete) {
                    guiListener.msg(Str.str("subtitleNotFound"), Constant.INFO_MSG);
                }
            }
        }
        done();
        if (strExportListener != null) {
            strExportListener.export(ContentType.SUBTITLE, subtitleLink, isCancelled(), guiListener);
        }
    }

    private void searchStopped() {
        if (!stopped) {
            if (foreground) {
                guiListener.subtitleSearchStopped();
            }
            stopped = true;
        }
    }

    private void findSubtitle() throws Exception {
        String searchTitle = URLEncoder.encode(video.title, Constant.UTF8), searchYear = String.valueOf(Integer.parseInt(video.year)
                - Integer.parseInt(Str.get(342)));
        String source = getSourceCode(isTVShow ? Str.get(439) + languageID + Str.get(440) + video.season + Str.get(441) + video.episode + Str.get(442) + searchYear
                + Str.get(443) + searchTitle : Str.get(444) + languageID + Str.get(445) + searchYear + Str.get(446) + searchTitle);
        String titleLink = Regex.match(source, 506);
        List<String> results;

        if (!titleLink.isEmpty()) {
            results = new ArrayList<String>(1);
            String subtitleID = Regex.match(source, 508);
            if (!subtitleID.isEmpty()) {
                String titleName = Regex.match(source, 514);
                results.add(titleLink + Constant.SEPARATOR1 + subtitleID + Constant.SEPARATOR1 + (titleName.isEmpty() ? ' ' : titleName));
            }
            saveSubtitle(results, true);
            return;
        }

        results = Regex.allMatches(source, 425);
        if (results.isEmpty()) {
            results = Regex.allMatches(source, 426);
            if (results.isEmpty()) {
                notFound();
            } else {
                for (String result : results) {
                    if (!resultMatches(result)) {
                        continue;
                    }
                    saveSubtitle(Regex.allMatches(getSourceCode(Str.get(454) + Regex.match(result, 430)), 425), false);
                    return;
                }
                notFound();
            }
        } else {
            saveSubtitle(results, false);
        }
    }

    private String getSourceCode(String url) throws Exception {
        String source;
        try {
            source = Connection.getSourceCode(prevUrl = url, DomainType.SUBTITLE);
        } catch (ConnectionException e) {
            // Handle server's unencoded redirect URL bug
            if (e.URL == null || e.URL.equals(prevUrl)) {
                throw e;
            }
            String titleName = Regex.match(e.URL, 510);
            if (!titleName.equals(URLDecoder.decode(titleName, Constant.UTF8))) {
                throw e;
            }
            source = Connection.getSourceCode(prevUrl = e.URL.replace(Str.get(512) + titleName, Str.get(512) + URLEncoder.encode(titleName, Constant.UTF8)),
                    DomainType.SUBTITLE);
        }

        if (!Regex.firstMatch(source, 453).isEmpty()) {
            Connection.removeFromCache(prevUrl);
            throw new ConnectionException(Connection.serverError(prevUrl));
        }

        return source;
    }

    private boolean resultMatches(String result) {
        if (isTVShow) {
            return true; // TV show's episode imdb title IDs != TV show's general imdb title ID
        }
        String titleLink = Regex.firstMatch(result, 427);
        String resultID = VideoSearch.normalize(Regex.replaceAll(Regex.replaceFirst(titleLink, 428), 423));
        if (Debug.DEBUG) {
            Debug.println("subtitle search result: resultID='" + resultID + "' titleID='" + video.ID + "'");
        }
        return video.ID.equals(resultID);
    }

    private void notFound() throws Exception {
        if (isCancelled()) {
            return;
        }
        if (isTVShowAndMovie) {
            isTVShowAndMovie = false;
            isTVShow = !isTVShow;
            findSubtitle();
            return;
        }
        searchStopped();
        guiListener.msg(Str.str("subtitleNotFound"), Constant.INFO_MSG);
        complete = true;
        String settings = "";
        if (!languageID.equals(Regex.subtitleLanguages.get(Constant.ANY))) {
            for (Entry<String, String> subtitleLanguagesEntry : Regex.subtitleLanguages.entrySet()) {
                if (subtitleLanguagesEntry.getValue().equals(languageID)) {
                    settings += ' ' + subtitleLanguagesEntry.getKey();
                    break;
                }
            }
        }
        if (!format.equals(Constant.ANY)) {
            settings += ' ' + format;
        }
        StreamingTorrentUtil.stream(video, Str.str("subtitle2") + " (" + (settings.isEmpty() ? VideoSearch.describe(video) : VideoSearch.describe(video) + ','
                + settings) + ')', true, format, languageID);
    }

    private void saveSubtitle(List<String> results, boolean isExactResult) throws Exception {
        if (results.isEmpty()) {
            notFound();
            return;
        }

        List<Subtitle> subtitles = new ArrayList<Subtitle>(results.size());
        if (isExactResult) {
            String[] resultParts = Regex.split(results.get(0), Constant.SEPARATOR1);
            if (resultMatches(resultParts[0]) && VideoSearch.isRightFormat(resultParts[2], format)) {
                subtitles.add(new Subtitle(resultParts[1], -1));
            }
        } else {
            for (String result : results) {
                if (!resultMatches(result)) {
                    continue;
                }

                String subtitleID = Regex.match(result, 432);
                String downloadCount = Regex.match(Regex.firstMatch(result, 434), 435);
                String titleName = Regex.match(Regex.firstMatch(result, 455), 456);
                if (Debug.DEBUG) {
                    Debug.println("subtitle match: name='" + Regex.replaceAll(titleName, "\\s++", " ") + "' subtitleID='" + subtitleID + "' downloadCount='"
                            + downloadCount + "'");
                }

                if (!VideoSearch.isRightFormat(titleName, format)) {
                    if (Debug.DEBUG) {
                        Debug.println("invalid format: not " + format);
                    }
                    continue;
                }

                subtitles.add(new Subtitle(subtitleID, Integer.parseInt(downloadCount)));
            }
        }

        if (subtitles.isEmpty()) {
            notFound();
            return;
        }

        Collections.sort(subtitles);

        long subtitleName = Str.hashCode(video.ID);
        String subtitleDir = Constant.APP_DIR + subtitleName + Constant.FILE_SEPARATOR;
        String subtitleZip = subtitleDir + subtitleName + Constant.ZIP;
        int numSubtitles = subtitles.size(), maxNumSubtitles = Integer.parseInt(Str.get(458));
        boolean tempFirstMatch = firstMatch;

        for (int i = 0; i < numSubtitles && i < maxNumSubtitles; i++) {
            if (isCancelled()) {
                return;
            }
            Subtitle subtitle = subtitles.get(i);
            if (Debug.DEBUG) {
                Debug.println("selected subtitle: ID='" + subtitle.subtitleID + "' Downloads='" + subtitle.downloadCount + "'");
            }

            Long tempSubtitleFileName = Str.hashCode(subtitle.subtitleID);
            String tempSubtitleFileNameAlias = cache.get(tempSubtitleFileName);
            File tempSubtitleFile = new File(Constant.TEMP_DIR + tempSubtitleFileName + Constant.TXT);
            String url = Str.get(437) + subtitle.subtitleID;
            if (tempSubtitleFileNameAlias != null) {
                if (!tempFirstMatch) {
                    tempFirstMatch = true;
                    continue;
                }
                if (strExportListener == null) {
                    searchStopped();
                    guiListener.saveSubtitle(tempSubtitleFileNameAlias, tempSubtitleFile);
                } else {
                    subtitleLink = url;
                }
                complete = true;
                return;
            }

            IO.fileOp(subtitleDir, IO.MK_DIR);
            try {
                try {
                    Connection.saveData(url, subtitleZip, DomainType.SUBTITLE, true, prevUrl);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    if (!isCancelled()) {
                        guiListener.error(new ConnectionException(Connection.error(url)));
                    }
                    continue;
                }

                try {
                    IO.unzip(subtitleZip, subtitleDir);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    continue;
                }

                if (isCancelled()) {
                    return;
                }

                File subtitleFile = IO.findFile(new File(subtitleDir), Regex.pattern(438));
                if (subtitleFile != null) {
                    String subtitleFileName = subtitleFile.getName();
                    if (tempFirstMatch) {
                        if (strExportListener == null) {
                            searchStopped();
                            guiListener.saveSubtitle(subtitleFileName, subtitleFile);
                        } else {
                            subtitleLink = url;
                        }
                        complete = true;
                    }
                    IO.fileOp(Constant.TEMP_DIR, IO.MK_DIR);
                    if (subtitleFile.renameTo(tempSubtitleFile)) {
                        cache.put(tempSubtitleFileName, subtitleFileName);
                    }
                    if (!tempFirstMatch) {
                        tempFirstMatch = true;
                        continue;
                    }
                    return;
                }
            } finally {
                IO.fileOp(subtitleDir, IO.RM_DIR);
            }
        }
        notFound();
    }

    private static class Subtitle implements Comparable<Subtitle> {

        String subtitleID;
        int downloadCount;

        Subtitle(String subtitleID, int downloadCount) {
            this.subtitleID = subtitleID;
            this.downloadCount = downloadCount;
        }

        @Override
        public int compareTo(Subtitle result) {
            return downloadCount > result.downloadCount ? -1 : (downloadCount < result.downloadCount ? 1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof Subtitle && downloadCount == ((Subtitle) obj).downloadCount);
        }

        @Override
        public int hashCode() {
            return 7 * 31 + downloadCount;
        }
    }
}

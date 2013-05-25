package onlinesearch;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import listener.GuiListener;
import main.Str;
import onlinesearch.download.TorrentFinder;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;
import util.io.Write;

public class SubtitleFinder extends AbstractSwingWorker {

    private GuiListener guiListener;
    private String format, languageID, titleID, title, year, season, episode;
    private boolean isTVShow, firstMatch;
    private static Map<Long, String> cache = new HashMap<Long, String>(8);
    private final static String NOT_FOUND = "The subtitle could not be found.";

    public SubtitleFinder(GuiListener guiListener, String format, String languageID, String titleID, String title, String year, String season, String episode,
            boolean firstMatch) {
        this.guiListener = guiListener;
        this.format = format;
        this.languageID = languageID;
        this.titleID = titleID;
        this.title = title;
        this.year = year;
        if (season != null && episode != null) {
            this.season = season;
            this.episode = episode;
            isTVShow = true;
        }
        this.firstMatch = firstMatch;
    }

    @Override
    protected Object doInBackground() {
        guiListener.subtitleSearchStarted();
        try {
            findSubtitle();
        } catch (Exception e) {
            if (!isCancelled()) {
                guiListener.subtitleSearchStopped();
                guiListener.connectionError(e);
            }
        }
        guiListener.subtitleSearchStopped();
        workDone();
        return null;
    }

    private void findSubtitle() throws Exception {
        title = URLEncoder.encode(title, Constant.UTF8);
        year = String.valueOf(Integer.parseInt(year) - Integer.parseInt(Str.get(342)));

        String source = getSourceCode(isTVShow ? Str.get(439) + languageID + Str.get(440) + season + Str.get(441) + episode + Str.get(442) + year + Str.get(443)
                + title : Str.get(444) + languageID + Str.get(445) + year + Str.get(446) + title);
        if (isCancelled()) {
            return;
        }

        List<String> results;
        String titleLink = Regex.match(source, Str.get(506), Str.get(507));
        if (!titleLink.isEmpty()) {
            results = new ArrayList<String>(1);
            String subtitleID = Regex.match(source, Str.get(508), Str.get(509));
            if (!subtitleID.isEmpty()) {
                String titleName = Regex.match(source, Str.get(514), Str.get(515));
                results.add(titleLink + Constant.SEPARATOR1 + subtitleID + Constant.SEPARATOR1 + (titleName.isEmpty() ? ' ' : titleName));
            }
            saveSubtitle(results, true);
            return;
        }

        results = Regex.matches(source, Str.get(425));
        if (results.isEmpty()) {
            results = Regex.matches(source, Str.get(426));
            if (results.isEmpty()) {
                guiListener.subtitleMsg(NOT_FOUND, Constant.INFO_MSG);
            } else {
                for (String result : results) {
                    if (!resultMatches(result)) {
                        continue;
                    }

                    source = getSourceCode(Str.get(454) + Regex.match(result, Str.get(430), Str.get(431)));
                    if (isCancelled()) {
                        return;
                    }

                    saveSubtitle(Regex.matches(source, Str.get(425)), false);
                    return;
                }
                guiListener.subtitleMsg(NOT_FOUND, Constant.INFO_MSG);
            }
        } else {
            saveSubtitle(results, false);
        }
    }

    private String getSourceCode(String urlStr) throws Exception {
        String url = urlStr, source;
        try {
            source = Connection.getSourceCode(url, Connection.SUBTITLE);
        } catch (ConnectionException e) {
            // Handle server's unencoded redirect URL bug
            if (e.url == null || e.url.equals(url)) {
                throw e;
            }
            String titleName = Regex.match(e.url, Str.get(510), Str.get(511));
            if (!titleName.equals(URLDecoder.decode(titleName, Constant.UTF8))) {
                throw e;
            }
            source = Connection.getSourceCode(url = e.url.replace(Str.get(512) + titleName, Str.get(512) + URLEncoder.encode(titleName, Constant.UTF8)),
                    Connection.SUBTITLE);
        }

        if (!Regex.match(source, Str.get(453)).isEmpty()) {
            Connection.removeFromCache(url);
            if (!isCancelled()) {
                guiListener.subtitleMsg(Connection.error(url), Constant.ERROR_MSG);
            }
            throw new ConnectionException();
        }

        return source;
    }

    private boolean resultMatches(String result) {
        if (isTVShow) {
            return true; //TV show's episode imdb title IDs != TV show's general imdb title ID
        }
        String titleLink = Regex.match(result, Str.get(427));
        String resultID = titleLink.replaceFirst(Str.get(428), Str.get(429)).replaceAll(Str.get(423),
                Str.get(424));
        if (Debug.DEBUG) {
            Debug.println("subtitle search result: resultID='" + resultID + "' titleID='" + titleID + "'");
        }
        return titleID.equals(resultID);
    }

    private void saveSubtitle(List<String> results, boolean isExactResult) throws Exception {
        if (results.isEmpty()) {
            guiListener.subtitleMsg(NOT_FOUND, Constant.INFO_MSG);
            return;
        }

        List<Subtitle> subtitles = new ArrayList<Subtitle>(results.size());
        if (isExactResult) {
            String[] resultParts = results.get(0).split(Constant.SEPARATOR1);
            if (resultMatches(resultParts[0]) && TorrentFinder.isRightFormat(resultParts[2], format)) {
                subtitles.add(new Subtitle(resultParts[1], -1));
            }
        } else {
            for (String result : results) {
                if (!resultMatches(result)) {
                    continue;
                }

                String subtitleID = Regex.match(result, Str.get(432), Str.get(433));
                String downloadCount = Regex.match(Regex.match(result, Str.get(434)), Str.get(435),
                        Str.get(436));
                String titleName = Regex.match(Regex.match(result, Str.get(455)), Str.get(456),
                        Str.get(457));
                if (Debug.DEBUG) {
                    Debug.println("subtitle match: name='" + titleName.replaceAll("\\s++", " ") + "' subtitleID='" + subtitleID + "' downloadCount='"
                            + downloadCount + "'");
                }

                if (!TorrentFinder.isRightFormat(titleName, format)) {
                    if (Debug.DEBUG) {
                        Debug.println("invalid format: not " + format);
                    }
                    continue;
                }

                subtitles.add(new Subtitle(subtitleID, Integer.parseInt(downloadCount)));
            }
        }

        if (subtitles.isEmpty()) {
            guiListener.subtitleMsg(NOT_FOUND, Constant.INFO_MSG);
            return;
        }

        Collections.sort(subtitles);

        String subtitleName = Str.hashCodeStr(titleID);
        String subtitleDir = Constant.APP_DIR + subtitleName + Constant.FILE_SEPARATOR;
        String subtitleZip = subtitleDir + subtitleName + Constant.ZIP;
        int numSubtitles = subtitles.size(), maxNumSubtitles = Integer.parseInt(Str.get(458));

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
            File tempSubtitleFile = new File(Constant.TEMP_DIR + tempSubtitleFileName.toString() + Constant.TXT);
            if (tempSubtitleFileNameAlias != null) {
                if (!firstMatch) {
                    firstMatch = true;
                    continue;
                }
                guiListener.saveSubtitle(tempSubtitleFileNameAlias, tempSubtitleFile);
                return;
            }

            Write.fileOp(subtitleDir, Write.MK_DIR);
            String url = Str.get(437) + subtitle.subtitleID;
            try {
                try {
                    Connection.saveData(url, subtitleZip, Connection.SUBTITLE);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    if (!isCancelled()) {
                        guiListener.connectionError(new ConnectionException(Connection.error("", "", url)));
                    }
                    continue;
                }

                if (isCancelled()) {
                    return;
                }

                try {
                    Write.unzip(subtitleZip, subtitleDir);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    continue;
                }

                if (isCancelled()) {
                    return;
                }

                File subtitleFile = getSubtitle(new File(subtitleDir));
                if (subtitleFile == null) {
                    continue;
                } else {
                    String subtitleFileName = subtitleFile.getName();
                    if (firstMatch) {
                        guiListener.saveSubtitle(subtitleFileName, subtitleFile);
                    }
                    Write.fileOp(Constant.TEMP_DIR, Write.MK_DIR);
                    if (subtitleFile.renameTo(tempSubtitleFile)) {
                        cache.put(tempSubtitleFileName, subtitleFileName);
                    }
                    if (!firstMatch) {
                        firstMatch = true;
                        continue;
                    }
                    return;
                }
            } finally {
                Write.rmDir(new File(subtitleDir));
            }
        }
        guiListener.subtitleMsg(NOT_FOUND, Constant.INFO_MSG);
    }

    private File getSubtitle(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    return getSubtitle(file);
                } else if (Regex.isMatch(file.getName(), Str.get(438))) {
                    return file;
                }
            }
        }
        return null;
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
            if (downloadCount > result.downloadCount) {
                return -1;
            } else if (downloadCount < result.downloadCount) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Subtitle)) {
                return false;
            }

            Subtitle subtitle = (Subtitle) obj;
            return downloadCount == subtitle.downloadCount;
        }

        @Override
        public int hashCode() {
            return 7 * 31 + downloadCount;
        }
    }
}

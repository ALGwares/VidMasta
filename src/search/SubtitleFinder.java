package search;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import listener.GuiListener;
import main.Str;
import search.download.TorrentFinder;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;

public class SubtitleFinder extends AbstractSwingWorker {

    private final GuiListener guiListener;
    private final String format, languageID, titleID, title, year, season, episode;
    private boolean isTVShow, isTVShowAndMovie;
    private final boolean firstMatch;
    private static final Map<Long, String> cache = new HashMap<Long, String>(8);

    public SubtitleFinder(GuiListener guiListener, String format, String languageID, String titleID, String title, String year, boolean isTVShow,
            boolean isTVShowAndMovie, String season, String episode, boolean firstMatch) {
        this.guiListener = guiListener;
        this.format = format;
        this.languageID = languageID;
        this.titleID = titleID;
        this.title = title;
        this.year = year;
        this.isTVShow = isTVShow;
        this.isTVShowAndMovie = isTVShowAndMovie;
        this.season = season;
        this.episode = episode;
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
                guiListener.error(e);
            }
        }
        guiListener.subtitleSearchStopped();
        workDone();
        return null;
    }

    private void findSubtitle() throws Exception {
        String searchTitle = URLEncoder.encode(title, Constant.UTF8), searchYear = String.valueOf(Integer.parseInt(year) - Integer.parseInt(Str.get(342)));
        String source = getSourceCode(isTVShow ? Str.get(439) + languageID + Str.get(440) + season + Str.get(441) + episode + Str.get(442) + searchYear
                + Str.get(443) + searchTitle : Str.get(444) + languageID + Str.get(445) + searchYear + Str.get(446) + searchTitle);
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
                notFound();
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
                notFound();
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
        String resultID = Regex.replaceAll(Regex.replaceFirst(titleLink, Str.get(428), Str.get(429)), Str.get(423), Str.get(424));
        if (Debug.DEBUG) {
            Debug.println("subtitle search result: resultID='" + resultID + "' titleID='" + titleID + "'");
        }
        return titleID.equals(resultID);
    }

    private void notFound() throws Exception {
        if (isTVShowAndMovie) {
            isTVShowAndMovie = false;
            isTVShow = !isTVShow;
            findSubtitle();
            return;
        }
        guiListener.subtitleMsg("The subtitle could not be found.", Constant.INFO_MSG);
    }

    private void saveSubtitle(List<String> results, boolean isExactResult) throws Exception {
        if (results.isEmpty()) {
            notFound();
            return;
        }

        List<Subtitle> subtitles = new ArrayList<Subtitle>(results.size());
        if (isExactResult) {
            String[] resultParts = Regex.split(results.get(0), Constant.SEPARATOR1);
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
                    Debug.println("subtitle match: name='" + Regex.replaceAll(titleName, "\\s++", " ") + "' subtitleID='" + subtitleID + "' downloadCount='"
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
            notFound();
            return;
        }

        Collections.sort(subtitles);

        String subtitleName = Str.hashCodeStr(titleID);
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
            File tempSubtitleFile = new File(Constant.TEMP_DIR + tempSubtitleFileName.toString() + Constant.TXT);
            if (tempSubtitleFileNameAlias != null) {
                if (!tempFirstMatch) {
                    tempFirstMatch = true;
                    continue;
                }
                guiListener.saveSubtitle(tempSubtitleFileNameAlias, tempSubtitleFile);
                return;
            }

            IO.fileOp(subtitleDir, IO.MK_DIR);
            String url = Str.get(437) + subtitle.subtitleID;
            try {
                try {
                    Connection.saveData(url, subtitleZip, Connection.SUBTITLE);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    if (!isCancelled()) {
                        guiListener.error(new ConnectionException(Connection.error("", "", url)));
                    }
                    continue;
                }

                if (isCancelled()) {
                    return;
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

                File subtitleFile = getSubtitle(new File(subtitleDir));
                if (subtitleFile == null) {
                    continue;
                } else {
                    String subtitleFileName = subtitleFile.getName();
                    if (tempFirstMatch) {
                        guiListener.saveSubtitle(subtitleFileName, subtitleFile);
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
                IO.rmDir(new File(subtitleDir));
            }
        }
        notFound();
    }

    public static File getSubtitle(File dir) {
        if (!dir.exists()) {
            return null;
        }

        File[] files = dir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                if (file1.isFile()) {
                    return -1;
                } else if (file2.isFile()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                File subtitleFile = getSubtitle(file);
                if (subtitleFile != null) {
                    return subtitleFile;
                }
            } else if (Regex.isMatch(file.getName(), Str.get(438))) {
                return file;
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
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj instanceof Subtitle ? downloadCount == ((Subtitle) obj).downloadCount : false;
        }

        @Override
        public int hashCode() {
            return 7 * 31 + downloadCount;
        }
    }
}

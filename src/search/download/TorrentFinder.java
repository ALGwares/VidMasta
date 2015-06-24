package search.download;

import debug.Debug;
import java.io.File;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import javax.swing.SwingWorker;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.BoxSetVideo;
import search.util.TitleParts;
import search.util.VideoSearch;
import str.Str;
import torrent.FileTypeChecker;
import torrent.Magnet;
import torrent.Torrent;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;

public class TorrentFinder extends SwingWorker<Object, Object> {

    private GuiListener guiListener;
    private Collection<Torrent> torrents;
    private Video video;
    private String seasonAndEpisode;
    private boolean orderByLeechers, magnetLinkOnly, ignoreYear, isOldTitle, isTitlePrefix, possiblyInconsistent, generalSearch, altSearch;
    private final int MAX_NUM_ATTEMPTS = Integer.parseInt(Str.get(176)), COUNTER1_MAX = Integer.parseInt(Str.get(168));
    private final int COUNTER2_MAX1 = Integer.parseInt(Str.get(170)), COUNTER2_MAX2 = Integer.parseInt(Str.get(336));
    private int attemptNum, counter1, counter2, counter2Max;
    private String categorySearch, prevUrl;
    private TorrentSearchState searchState;
    private List<BoxSetVideo> boxSet;
    private static final Map<String, Boolean> savedTorrents = new ConcurrentHashMap<String, Boolean>(16);

    TorrentFinder(GuiListener guiListener, Collection<Torrent> torrents, Video video, String seasonAndEpisode, boolean orderByLeechers, boolean magnetLinkOnly,
            boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix, TorrentSearchState searchState, List<BoxSetVideo> boxSet, boolean altSearch) {
        this.guiListener = guiListener;
        this.torrents = torrents;
        this.video = video;
        this.seasonAndEpisode = seasonAndEpisode;
        this.orderByLeechers = orderByLeechers;
        this.magnetLinkOnly = magnetLinkOnly;
        this.ignoreYear = ignoreYear;
        this.isOldTitle = isOldTitle;
        this.isTitlePrefix = isTitlePrefix;
        this.searchState = searchState;
        this.boxSet = boxSet;
        categorySearch = Str.get(video.IS_TV_SHOW ? 658 : 659);
        counter2Max = (boxSet == null ? COUNTER2_MAX1 : COUNTER2_MAX2);
        this.altSearch = altSearch;
    }

    private boolean isCancelled2() {
        return isCancelled() || Connection.downloadLinkInfoFail();
    }

    @Override
    protected Object doInBackground() {
        if (altSearch) {
            try {
                getTorrent();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
            return null;
        }

        while (true) {
            if (Debug.DEBUG) {
                Debug.println("Thread: " + Thread.currentThread().getName() + " Attempt: " + (attemptNum + 1));
            }
            if (isCancelled()) {
                return null;
            }

            possiblyInconsistent = true;

            Torrent torrent = null;
            try {
                torrent = getTorrent(false, generalSearch = !generalSearch);
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }

            if (isCancelled()) {
                return null;
            }

            if (possiblyInconsistent && ++attemptNum < MAX_NUM_ATTEMPTS) {
                if (prevUrl != null) {
                    Connection.removeDownloadLinkInfoProxyUrlFromCache(prevUrl);
                }
                continue;
            }

            if (torrent != null && !isCancelled()) {
                torrents.add(torrent);
            }
            return null;
        }
    }

    public Torrent getTorrent(boolean prefetch, boolean generalSearch) throws Exception {
        String urlForm = Str.get(33), urlFormOptions = URLEncoder.encode(Regex.clean(video.title) + (ignoreYear ? "" : (' ' + video.year)) + seasonAndEpisode,
                Constant.UTF8) + (generalSearch ? Str.get(657) : categorySearch);
        if (isCancelled2()) {
            return null;
        }

        String sourceCode;
        try {
            sourceCode = Connection.getSourceCode(prevUrl = urlForm + urlFormOptions, DomainType.DOWNLOAD_LINK_INFO, !prefetch);
        } catch (ConnectionException e) {
            if (Connection.isIgnorable(e.getCause())) {
                return null;
            }
            throw e;
        }

        if (!Regex.firstMatch(sourceCode, 146).isEmpty() || Regex.firstMatch(sourceCode, 504).isEmpty()) {
            Connection.removeDownloadLinkInfoProxyUrlFromCache(prevUrl);
            if (!prefetch && !isCancelled2()) {
                Connection.failDownloadLinkInfo();
            }
            throw new ConnectionException(Connection.serverError(urlForm));
        }

        if (isCancelled()) {
            return null;
        }

        String firstPageLink = Regex.firstMatch(sourceCode, Str.get(orderByLeechers ? 660 : 661));
        firstPageLink = Regex.replaceAllRepeatedly(firstPageLink, 666);
        if (firstPageLink.isEmpty()) {
            return null;
        }

        sourceCode = Connection.getSourceCode(Str.get(39) + firstPageLink, DomainType.DOWNLOAD_LINK_INFO, !prefetch);
        if (prefetch) {
            return null;
        }

        int currPageNum = Integer.parseInt(Str.get(662));
        while (counter2 != counter2Max) {
            Torrent torrent = getTorrent(39, sourceCode);

            if (counter1 == COUNTER1_MAX || isCancelled()) {
                return null;
            }
            if (torrent != null) {
                return torrent;
            }
            if (++counter2 == counter2Max) {
                return null;
            }

            currPageNum++;
            String nextPageLink = Regex.firstMatch(sourceCode, Str.get(663) + currPageNum + Str.get(orderByLeechers ? 664 : 665));
            nextPageLink = Regex.replaceAllRepeatedly(nextPageLink, 666);
            if (nextPageLink.isEmpty()) {
                break;
            }

            String query = Regex.match(nextPageLink, 488);
            if (Boolean.parseBoolean(Str.get(490))) {
                query = URLEncoder.encode(query, Constant.UTF8);
            }

            sourceCode = Connection.getSourceCode(Str.get(491) + query + Str.get(492) + currPageNum + Regex.match(nextPageLink, 493),
                    DomainType.DOWNLOAD_LINK_INFO);
        }

        return null;
    }

    private void getTorrent() throws Exception {
        String sourceCode = Connection.getSourceCode(Str.get(700) + URLEncoder.encode(video.ID, Constant.UTF8), DomainType.DOWNLOAD_LINK_INFO, true, true);
        if (!isCancelled() && !sourceCode.isEmpty()) {
            Torrent torrent = getTorrent(700, sourceCode);
            if (torrent != null && !isCancelled()) {
                torrents.add(torrent);
            }
        }
    }

    private Torrent getTorrent(int urlIndex, String sourceCode) throws Exception {
        Matcher titleMatcher = Regex.matcher(48, sourceCode);
        while (!titleMatcher.hitEnd() && counter1 != COUNTER1_MAX) {
            if (isCancelled()) {
                return null;
            }
            if (!titleMatcher.find()) {
                continue;
            }

            String videoStr = Regex.match(sourceCode.substring(titleMatcher.end()), 49), titleName = Regex.match(videoStr, 51);
            if (titleName.isEmpty() || !Regex.isMatch(Regex.match(videoStr, 53), video.IS_TV_SHOW_AND_MOVIE ? 588 : (video.IS_TV_SHOW ? 562 : 563))) {
                continue;
            }

            possiblyInconsistent = false;

            String torrentID = Regex.match(Regex.firstMatch(videoStr, 460), 461);
            Boolean prevOrderByLeechers = savedTorrents.get(torrentID);
            boolean isBoxSet;
            if ((prevOrderByLeechers != null && orderByLeechers != prevOrderByLeechers) || VideoSearch.isUploadYearTooOld(videoStr, 1, Integer.parseInt(
                    video.year)) || !VideoSearch.isRightFormat(titleName, searchState.format) || (!(isBoxSet = !Regex.firstMatch(Regex.replaceAll(titleName, 220),
                            video.IS_TV_SHOW ? 207 : 208).isEmpty()) && boxSet != null)) {
                continue;
            }

            int sizeInGiB = 1;
            String size = Regex.match(videoStr, 62);
            if (size.isEmpty()) {
                if (!(size = Regex.match(videoStr, 64)).isEmpty()) {
                    sizeInGiB = (int) Math.ceil(Double.parseDouble(size));
                    if ((!isBoxSet || !searchState.canIgnoreDownloadSize) && (sizeInGiB < Integer.parseInt(searchState.minSize)
                            || (!searchState.maxSize.equals(Constant.INFINITY) && sizeInGiB > Integer.parseInt(searchState.maxSize)))) {
                        continue;
                    }
                }
            } else if ((!isBoxSet || !searchState.canIgnoreDownloadSize) && Integer.parseInt(searchState.minSize) >= 1) {
                continue;
            }

            if (video.IS_TV_SHOW) {
                boolean anyEpisode = video.episode.equals(Constant.ANY);
                if (video.season.equals(Constant.ANY)) {
                    if (!anyEpisode && episode(titleName, 626).isEmpty()) {
                        continue;
                    }
                } else if (anyEpisode) {
                    if (isBoxSet && !isRightSeason(titleName)) {
                        continue;
                    }
                } else {
                    TitleParts titleParts = VideoSearch.getTitleParts(titleName, video.IS_TV_SHOW);
                    if (!titleParts.season.isEmpty() && !titleParts.episode.isEmpty()) {
                        if (!video.season.equals(titleParts.season) || !video.episode.equals(titleParts.episode)) {
                            continue;
                        }
                    } else if (!episode(titleName, 686).isEmpty() || !isRightSeason(titleName)) {
                        continue;
                    }
                }
            }

            if (!isRightTitle(titleName, isBoxSet)) {
                continue;
            }

            if (isCancelled()) {
                return null;
            }

            String numSources = (orderByLeechers ? Regex.match(videoStr, 70) : Regex.match(videoStr, 72));
            int numSourcesNum = 0;
            if (!numSources.isEmpty()) {
                numSourcesNum = Integer.parseInt(numSources);
            }

            String magnetLink = Regex.match(videoStr, 389);
            if (magnetLink.isEmpty()) {
                throw new ConnectionException(Connection.serverError(Str.get(urlIndex)));
            }
            Magnet magnet = new Magnet(Str.get(388) + magnetLink);
            boolean isTorrentDownloaded = magnet.download(guiListener, this, magnetLinkOnly);
            if (isCancelled()) {
                return null;
            }

            File torrent;
            String extensions;
            if (isTorrentDownloaded) {
                FileTypeChecker fileTypeChecker = new FileTypeChecker(searchState.whitelistedFileExts, searchState.blacklistedFileExts);
                if (!fileTypeChecker.isValidFileType(magnet.TORRENT)) {
                    continue;
                }

                if (isCancelled()) {
                    return null;
                }

                torrent = magnet.TORRENT;
                extensions = fileTypeChecker.getFileExts();
            } else {
                torrent = null;
                extensions = "";
            }

            return new Torrent(torrentID, magnet.MAGNET_LINK, Regex.htmlToPlainText(titleName), torrent, extensions, Str.get(674) + Regex.match(Regex.firstMatch(
                    videoStr, 675), 676) + Str.get(678), !Regex.match(videoStr, 74).isEmpty(), numSourcesNum, sizeInGiB);
        }
        return null;
    }

    private String episode(String title, int startRegex) {
        int episodeNum = Integer.parseInt(video.episode);
        return Regex.firstMatch(Regex.replaceAll(title, 222), Str.get(startRegex) + '(' + video.episode + ")|(" + episodeNum + ")|(" + getRomanNumeral(episodeNum)
                + ')' + Str.get(startRegex + 1));
    }

    private boolean isRightSeason(String title) {
        String titleName = Regex.replaceAll(title, 222);
        int season = Integer.parseInt(video.season);
        if (Debug.DEBUG) {
            Debug.print("Desired Season (" + titleName + "): " + season);
        }

        Matcher seasonMatcher = Regex.matcher(215, titleName);
        while (!seasonMatcher.hitEnd()) {
            if (!seasonMatcher.find()) {
                continue;
            }

            Matcher numListMatcher = Regex.matcher(216, titleName);
            while (!numListMatcher.hitEnd()) {
                if (numListMatcher.find()) {
                    if (Debug.DEBUG) {
                        Debug.println("\tPossibly in list of seasons: '" + titleName + '\'');
                    }
                    return true;
                }
            }

            String seasonNum = Regex.replaceAll(seasonMatcher.group(), 217);
            if (Debug.DEBUG) {
                Debug.print("\tseasonNum: '" + seasonNum + '\'');
            }
            if (Regex.isMatch(seasonNum, 219)) {
                String correctSeasonNum = getRomanNumeral(season);
                if (Debug.DEBUG) {
                    Debug.print("\tcorrectSeasonNum: '" + correctSeasonNum + '\'');
                }
                if (!seasonNum.equalsIgnoreCase(correctSeasonNum)) {
                    if (Debug.DEBUG) {
                        Debug.println("\tWrong Season!");
                    }
                    return false;
                }
            } else if (Integer.parseInt(seasonNum) != season) {
                if (Debug.DEBUG) {
                    Debug.println("\tWrong Season!");
                }
                return false;
            }
            if (Debug.DEBUG) {
                Debug.println("\tCorrect Season!");
            }
            return true;
        }

        if (Debug.DEBUG) {
            Debug.println("\tPossibly Correct Season: '" + titleName + '\'');
        }
        return true;
    }

    private static String getRomanNumeral(int num) {
        String[] rCode = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] bVal = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        StringBuilder roman = new StringBuilder(8);
        int number = num;

        for (int i = 0; i < rCode.length; i++) {
            while (number >= bVal[i]) {
                number -= bVal[i];
                roman.append(rCode[i]);
            }
        }

        return roman.toString();
    }

    private boolean isRightTitle(String titleName, boolean isBoxSet) throws Exception {
        counter1++;
        if (altSearch) {
            return true;
        }

        TitleParts titleParts = VideoSearch.getTitleParts(titleName, video.IS_TV_SHOW);
        String titleLink = VideoSearch.getTitleLink(titleParts.title, titleParts.year);
        if (titleLink == null) {
            return false;
        }

        if (isCancelled()) {
            return true;
        }

        String source = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        titleParts = VideoSearch.getImdbTitleParts(source);
        if (isOldTitle && (titleParts.title = Regex.match(source, 174)).isEmpty()) {
            return false;
        }

        if (!VideoSearch.isImdbVideoType(source, video.IS_TV_SHOW)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (video.IS_TV_SHOW ? "TV show" : "movie") + "): '" + titleParts.title + "' '" + titleParts.year + '\'');
            }
            return false;
        }

        if (isBoxSet) {
            if (boxSet == null) {
                String cleanTitle1, cleanTitle2;
                return startsWith(video.title, titleParts.title) || startsWith(titleParts.title, video.title) || startsWith(cleanTitle1 = Regex.htmlToPlainText(
                        video.title), cleanTitle2 = Regex.htmlToPlainText(titleParts.title)) || startsWith(cleanTitle2, cleanTitle1);
            } else {
                int numTitles = boxSet.size();
                for (int i = 1; i < numTitles; i++) {
                    BoxSetVideo currVideo = boxSet.get(i);
                    if (currVideo.isSameTitle(titleParts.title, titleParts.year)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            if (isTitlePrefix) {
                String titlePrefix = VideoSearch.getMovieTitlePrefix(titleParts.title);
                if (titlePrefix != null) {
                    titleParts.title = titlePrefix;
                }
            }
            return video.year.equals(titleParts.year) && (video.title.equals(titleParts.title) || Regex.htmlToPlainText(video.title).equals(Regex.htmlToPlainText(
                    titleParts.title)));
        }
    }

    private static boolean startsWith(String title1, String title2) {
        if (title1.startsWith(title2) || title1.startsWith(Regex.replaceFirst(title2, 209))) {
            return true;
        }

        Matcher numMatcher = Regex.matcher(211, title2);
        while (!numMatcher.hitEnd()) {
            if (numMatcher.find()) {
                String tempTitle = title2.substring(0, numMatcher.start());
                return title1.startsWith(tempTitle) || title1.startsWith(Regex.replaceFirst(tempTitle, 212));
            }
        }
        return false;
    }

    static void saveTorrent(String torrentID, boolean orderByLeechers) {
        if (!torrentID.isEmpty() && !savedTorrents.containsKey(torrentID)) {
            savedTorrents.put(torrentID, orderByLeechers);
        }
    }
}

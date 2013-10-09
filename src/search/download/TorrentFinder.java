package search.download;

import debug.Debug;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import search.BoxSetVideo;
import search.Video;
import search.util.VideoSearch;
import torrent.FileTypeChecker;
import torrent.Magnet;
import torrent.Torrent;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;

public class TorrentFinder extends SwingWorker<Object, Object[]> {

    private GuiListener guiListener;
    private static final Object saveTorrentLock = new Object();
    private Collection<Torrent> torrents;
    private Video video;
    private String seasonAndEpisode, orderBy;
    private boolean ignoreYear, isOldTitle, isTitlePrefix, possiblyInconsistent, generalSearch;
    private static final int MAX_NUM_ATTEMPTS = Integer.parseInt(Str.get(176)), COUNTER1_MAX = Integer.parseInt(Str.get(168));
    private static final int COUNTER2_MAX1 = Integer.parseInt(Str.get(170)), COUNTER2_MAX2 = Integer.parseInt(Str.get(336));
    private int attemptNum, counter1, counter2, counter2Max;
    private String categorySearch, prevUrl;
    private TorrentSearchState searchState;
    private List<BoxSetVideo> boxSet;

    TorrentFinder(GuiListener guiListener, Collection<Torrent> torrents, Video video, String seasonAndEpisode, String orderBy, boolean ignoreYear,
            boolean isOldTitle, boolean isTitlePrefix, TorrentSearchState searchState, List<BoxSetVideo> boxSet) {
        this.guiListener = guiListener;
        this.torrents = torrents;
        this.video = video;
        this.seasonAndEpisode = seasonAndEpisode;
        this.orderBy = orderBy;
        this.ignoreYear = ignoreYear;
        this.isOldTitle = isOldTitle;
        this.isTitlePrefix = isTitlePrefix;
        this.searchState = searchState;
        this.boxSet = boxSet;
        categorySearch = Str.get(video.isTVShow ? 177 : 178);
        counter2Max = (boxSet == null ? COUNTER2_MAX1 : COUNTER2_MAX2);
    }

    private boolean isCancelled2() {
        return isCancelled() || Connection.downloadLinkInfoFail();
    }

    @Override
    protected Object doInBackground() {
        Torrent torrent = search();

        if (isCancelled()) {
            return null;
        }
        synchronized (saveTorrentLock) {
            if (isCancelled()) {
                return null;
            }
            if (torrent != null) {
                torrents.add(torrent);
            }
        }

        return null;
    }

    private Torrent search() {
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

            return torrent;
        }
    }

    public Torrent getTorrent(boolean prefetch, boolean generalSearch) throws Exception {
        String urlForm = Str.get(33);
        String urlFormOptions = URLEncoder.encode(Str.clean(video.title) + (ignoreYear ? "" : (' ' + video.year)) + seasonAndEpisode, Constant.UTF8)
                + (generalSearch ? Str.get(34) : categorySearch);
        if (isCancelled2()) {
            return null;
        }
        String sourceStr = Connection.getSourceCode(prevUrl = urlForm + urlFormOptions, Connection.DOWNLOAD_LINK_INFO, !prefetch);

        if (!Regex.match(sourceStr, Str.get(146)).isEmpty() || Regex.match(sourceStr, Str.get(504)).isEmpty()) {
            Connection.removeDownloadLinkInfoProxyUrlFromCache(prevUrl);
            if (!prefetch && !isCancelled2()) {
                Connection.failDownloadLinkInfo();
            }
            throw new ConnectionException(Connection.error(urlForm));
        }

        if (isCancelled()) {
            return null;
        }

        Matcher linkMatcher = Regex.matcher(Str.get(35) + orderBy + Str.get(36), sourceStr);
        while (!linkMatcher.hitEnd()) {
            if (!linkMatcher.find()) {
                continue;
            }

            String currPageLink = Regex.match(sourceStr.substring(linkMatcher.end()), Str.get(37), Str.get(38));
            if (isCancelled2()) {
                return null;
            }
            String currPageSourceCode = Connection.getSourceCode(Str.get(39) + currPageLink, Connection.DOWNLOAD_LINK_INFO, !prefetch);
            if (prefetch) {
                return null;
            }

            int currPageNum = Integer.parseInt(Regex.match(currPageLink, Str.get(486), Str.get(487)));

            while (counter2 != counter2Max) {
                Torrent torrent = getTorrentHelper(currPageSourceCode);

                if (counter1 == COUNTER1_MAX || isCancelled()) {
                    return null;
                }

                if (torrent != null) {
                    return torrent;
                }

                currPageNum++;
                String nextPageLink;
                if ((nextPageLink = Regex.match(currPageSourceCode, Str.get(486) + currPageNum + Str.get(487))).isEmpty()) {
                    break;
                }

                String query = Regex.match(nextPageLink, Str.get(488), Str.get(489));
                if (Boolean.parseBoolean(Str.get(490))) {
                    query = URLEncoder.encode(query, Constant.UTF8);
                }
                nextPageLink = Str.get(491) + query + Str.get(492) + currPageNum + Regex.match(nextPageLink, Str.get(493),
                        Str.get(494));

                if (isCancelled2()) {
                    return null;
                }
                currPageSourceCode = Connection.getSourceCode(nextPageLink, Connection.DOWNLOAD_LINK_INFO);
                counter2++;
            }

            return null;
        }

        return null;
    }

    private Torrent getTorrentHelper(String sourceStr) throws Exception {
        Matcher titleMatcher = Regex.matcher(Str.get(48), sourceStr);
        while (!titleMatcher.hitEnd() && counter1 != COUNTER1_MAX) {
            if (isCancelled()) {
                return null;
            }
            if (!titleMatcher.find()) {
                continue;
            }

            String videoStr = Regex.match(sourceStr.substring(titleMatcher.end()), Str.get(49), Str.get(50));
            String titleName = Regex.match(videoStr, Str.get(51), Str.get(52));
            if (titleName.isEmpty() || !Regex.isMatch(Regex.match(videoStr, Str.get(53), Str.get(54)), Str.get(video.isTVShowAndMovie ? 588 : (video.isTVShow ? 562
                    : 563)))) {
                continue;
            }

            possiblyInconsistent = false;

            String torrentID = Regex.match(Regex.match(videoStr, Str.get(460)), Str.get(461), Str.get(462));
            Boolean prevOrderBy = VideoFinder.savedTorrents.get(torrentID);
            if (prevOrderBy != null && orderBy.equals(Str.get(161)) != prevOrderBy) {
                continue;
            }

            String uploadYear = Regex.match(videoStr, Str.get(60));
            if (!uploadYear.isEmpty()) {
                int uploadYearNum = Integer.parseInt(uploadYear.substring(uploadYear.length() - Integer.parseInt(Str.get(61))));
                if (uploadYearNum + 1 < Integer.parseInt(video.year)) {
                    continue;
                }
            }

            if (!isRightFormat(titleName, searchState.format)) {
                continue;
            }

            boolean isBoxSet = !Regex.match(Regex.replaceAll(titleName, Str.get(220), Str.get(221)), Str.get(video.isTVShow ? 207 : 208)).isEmpty();
            if (!isBoxSet && boxSet != null) {
                continue;
            }

            int sizeInGiB = 1;
            String size = Regex.match(videoStr, Str.get(62), Str.get(63));
            if (size.isEmpty()) {
                if (!(size = Regex.match(videoStr, Str.get(64), Str.get(65))).isEmpty()) {
                    sizeInGiB = (int) Math.ceil(Double.parseDouble(size));
                    if ((!isBoxSet || !searchState.canIgnoreDownloadSize) && (sizeInGiB < Integer.parseInt(searchState.minSize)
                            || (!searchState.maxSize.equals(Constant.INFINITY) && sizeInGiB > Integer.parseInt(searchState.maxSize)))) {
                        continue;
                    }
                }
            } else if ((!isBoxSet || !searchState.canIgnoreDownloadSize) && Integer.parseInt(searchState.minSize) >= 1) {
                continue;
            }

            if (isBoxSet && video.isTVShow && !video.season.equals(Constant.ANY) && !isRightSeason(Integer.parseInt(video.season), titleName)) {
                continue;
            }

            if (!isRightTitle(titleName, isBoxSet)) {
                continue;
            }

            if (isCancelled()) {
                return null;
            }

            String numSources = (orderBy.equals(Str.get(163)) ? Regex.match(videoStr, Str.get(70), Str.get(71)) : Regex.match(videoStr, Str.get(72), Str.get(73)));
            int numSourcesNum = 0;
            if (!numSources.isEmpty()) {
                numSourcesNum = Integer.parseInt(numSources);
            }

            Magnet magnet = new Magnet(Str.get(388) + Regex.match(videoStr, Str.get(389), Str.get(390)), Str.toFileName(titleName));
            magnet.download(this);
            if (isCancelled()) {
                return null;
            }

            FileTypeChecker fileTypeChecker = new FileTypeChecker(searchState.whitelistedFileExts, searchState.blacklistedFileExts);
            if (!fileTypeChecker.isValidFileType(magnet.torrentFile)) {
                continue;
            }

            if (isCancelled()) {
                return null;
            }

            boolean isSafe = !Regex.match(videoStr, Str.get(74), Str.get(75)).isEmpty();
            return new Torrent(torrentID, magnet.torrentFile, fileTypeChecker.getFileExts(), isSafe, numSourcesNum, sizeInGiB);
        }
        return null;
    }

    private static boolean isRightSeason(int season, String title) {
        if (Debug.DEBUG) {
            Debug.print("Desired Season: '" + season + '\'');
        }
        String titleName = Regex.replaceAll(title, Str.get(222), Str.get(223));

        Matcher seasonMatcher = Regex.matcher(Str.get(215), titleName);
        while (!seasonMatcher.hitEnd()) {
            if (!seasonMatcher.find()) {
                continue;
            }

            Matcher numListMatcher = Regex.matcher(Str.get(216), titleName);
            while (!numListMatcher.hitEnd()) {
                if (numListMatcher.find()) {
                    if (Debug.DEBUG) {
                        Debug.println("\tPossibly in list of seasons: '" + titleName + '\'');
                    }
                    return true;
                }
            }

            String seasonNum = Regex.replaceAll(seasonMatcher.group(), Str.get(217), Str.get(218));
            if (Debug.DEBUG) {
                Debug.print("\tseasonNum: '" + seasonNum + '\'');
            }
            if (Regex.isMatch(seasonNum, Str.get(219))) {
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
        String[] titleParts = VideoSearch.getTitleParts(titleName, video.isTVShow);
        String titleLink = VideoSearch.getTitleLink(titleParts[0], titleParts[1]);
        if (Debug.DEBUG) {
            Debug.println('\'' + titleParts[0] + "' '" + titleParts[1] + "' '" + titleParts[2] + "' '" + titleParts[3] + "' '" + titleName + '\'');
        }
        if (titleLink == null) {
            return false;
        }

        if (isCancelled()) {
            return true;
        }

        String sourceStr = Connection.getSourceCode(titleLink, Connection.VIDEO_INFO);
        titleParts = VideoSearch.getImdbTitleParts(sourceStr);
        String title = titleParts[0];
        if (isOldTitle && (title = Regex.match(sourceStr, Str.get(174), Str.get(175))).isEmpty()) {
            return false;
        }

        String year = titleParts[1];

        if (!VideoSearch.isImdbVideoType(sourceStr, video.isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (video.isTVShow ? "TV show" : "movie") + "): '" + title + "' '" + year + '\'');
            }
            return false;
        }

        if (isBoxSet) {
            if (boxSet == null) {
                String cleanTitle1, cleanTitle2;
                return startsWith(video.title, title) || startsWith(title, video.title) || startsWith(cleanTitle1 = Str.htmlToPlainText(video.title), cleanTitle2 =
                        Str.htmlToPlainText(title)) || startsWith(cleanTitle2, cleanTitle1);
            } else {
                int numTitles = boxSet.size();
                for (int i = 1; i < numTitles; i++) {
                    BoxSetVideo currVideo = boxSet.get(i);
                    if (currVideo.isSameTitle(title, year)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            if (isTitlePrefix) {
                String titlePrefix = Video.getMovieTitlePrefix(title);
                if (titlePrefix != null) {
                    title = titlePrefix;
                }
            }
            return video.year.equals(year) && (video.title.equals(title) || Str.htmlToPlainText(video.title).equals(Str.htmlToPlainText(title)));
        }
    }

    private static boolean startsWith(String title1, String title2) {
        if (title1.startsWith(title2) || title1.startsWith(Regex.replaceFirst(title2, Str.get(209), Str.get(210)))) {
            return true;
        }

        Matcher numMatcher = Regex.matcher(Str.get(211), title2);
        while (!numMatcher.hitEnd()) {
            if (numMatcher.find()) {
                String tempTitle = title2.substring(0, numMatcher.start());
                return title1.startsWith(tempTitle) || title1.startsWith(Regex.replaceFirst(tempTitle, Str.get(212), Str.get(213)));
            }
        }
        return false;
    }

    public static boolean isRightFormat(String titleName, String format) {
        if (format.equals(Constant.ANY)) {
            return true;
        }

        String title = Regex.replaceAll(titleName, Str.get(77), Str.get(78));
        if (format.equals(Constant.DVD)) {
            return hasType(title, Str.get(79));
        } else if (format.equals(Constant.HD720)) {
            return (hasType(title, Str.get(80)) || hasType(title, Str.get(81))) && !hasType(title, Str.get(82));
        }
        return hasType(title, Str.get(83));
    }

    private static boolean hasType(String title, String typeRegex) {
        Matcher typeMatcher = Regex.matcher(Str.get(84) + typeRegex + Str.get(85), title);
        while (!typeMatcher.hitEnd()) {
            if (typeMatcher.find()) {
                return true;
            }
        }
        return false;
    }
}

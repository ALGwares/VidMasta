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
    private boolean orderByLeechers, magnetLinkOnly, ignoreYear, isOldTitle, isTitlePrefix, possiblyInconsistent, generalSearch;
    private static final int MAX_NUM_ATTEMPTS = Integer.parseInt(Str.get(176)), COUNTER1_MAX = Integer.parseInt(Str.get(168));
    private static final int COUNTER2_MAX1 = Integer.parseInt(Str.get(170)), COUNTER2_MAX2 = Integer.parseInt(Str.get(336));
    private int attemptNum, counter1, counter2, counter2Max;
    private String categorySearch, prevUrl;
    private TorrentSearchState searchState;
    private List<BoxSetVideo> boxSet;
    private static final Map<String, Boolean> savedTorrents = new ConcurrentHashMap<String, Boolean>(16);

    TorrentFinder(GuiListener guiListener, Collection<Torrent> torrents, Video video, String seasonAndEpisode, boolean orderByLeechers, boolean magnetLinkOnly,
            boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix, TorrentSearchState searchState, List<BoxSetVideo> boxSet) {
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
        categorySearch = Str.get(video.IS_TV_SHOW ? 177 : 178);
        counter2Max = (boxSet == null ? COUNTER2_MAX1 : COUNTER2_MAX2);
    }

    private boolean isCancelled2() {
        return isCancelled() || Connection.downloadLinkInfoFail();
    }

    @Override
    protected Object doInBackground() {
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
        String urlForm = Str.get(33);
        String urlFormOptions = URLEncoder.encode(Regex.clean(video.title) + (ignoreYear ? "" : (' ' + video.year)) + seasonAndEpisode, Constant.UTF8)
                + (generalSearch ? Str.get(34) : categorySearch);
        if (isCancelled2()) {
            return null;
        }
        String sourceStr = Connection.getSourceCode(prevUrl = urlForm + urlFormOptions, DomainType.DOWNLOAD_LINK_INFO, !prefetch);

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

        Matcher linkMatcher = Regex.matcher(Str.get(35) + Str.get(orderByLeechers ? 161 : 162) + Str.get(36), sourceStr);
        while (!linkMatcher.hitEnd()) {
            if (!linkMatcher.find()) {
                continue;
            }

            String currPageLink = Regex.match(sourceStr.substring(linkMatcher.end()), Str.get(37), Str.get(38));
            if (isCancelled2()) {
                return null;
            }
            String currPageSourceCode = Connection.getSourceCode(Str.get(39) + currPageLink, DomainType.DOWNLOAD_LINK_INFO, !prefetch);
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
                currPageSourceCode = Connection.getSourceCode(nextPageLink, DomainType.DOWNLOAD_LINK_INFO);
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
            if (titleName.isEmpty() || !Regex.isMatch(Regex.match(videoStr, Str.get(53), Str.get(54)), Str.get(video.IS_TV_SHOW_AND_MOVIE ? 588
                    : (video.IS_TV_SHOW ? 562 : 563)))) {
                continue;
            }

            possiblyInconsistent = false;

            String torrentID = Regex.match(Regex.match(videoStr, Str.get(460)), Str.get(461), Str.get(462));
            Boolean prevOrderByLeechers = savedTorrents.get(torrentID);
            if (prevOrderByLeechers != null && orderByLeechers != prevOrderByLeechers) {
                continue;
            }

            String uploadYear = Regex.match(videoStr, Str.get(60));
            if (!uploadYear.isEmpty()) {
                int uploadYearNum = Integer.parseInt(uploadYear.substring(uploadYear.length() - Integer.parseInt(Str.get(61))));
                if (uploadYearNum + 1 < Integer.parseInt(video.year)) {
                    continue;
                }
            }

            if (!VideoSearch.isRightFormat(titleName, searchState.format)) {
                continue;
            }

            boolean isBoxSet = !Regex.match(Regex.replaceAll(titleName, Str.get(220), Str.get(221)), Str.get(video.IS_TV_SHOW ? 207 : 208)).isEmpty();
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

            if (video.IS_TV_SHOW) {
                if (video.season.equals(Constant.ANY)) {
                    int episodeNum;
                    if (!video.episode.equals(Constant.ANY) && Regex.match(Regex.replaceAll(titleName, Str.get(222), Str.get(223)), Str.get(626) + "("
                            + (episodeNum = Integer.parseInt(video.episode)) + ")|(" + getRomanNumeral(episodeNum) + ')' + Str.get(627)).isEmpty()) {
                        continue;
                    }
                } else if (isBoxSet && video.episode.equals(Constant.ANY) && !isRightSeason(Integer.parseInt(video.season), titleName)) {
                    continue;
                }
            }

            if (!isRightTitle(titleName, isBoxSet)) {
                continue;
            }

            if (isCancelled()) {
                return null;
            }

            String numSources = (orderByLeechers ? Regex.match(videoStr, Str.get(70), Str.get(71)) : Regex.match(videoStr, Str.get(72), Str.get(73)));
            int numSourcesNum = 0;
            if (!numSources.isEmpty()) {
                numSourcesNum = Integer.parseInt(numSources);
            }

            Magnet magnet = new Magnet(Str.get(388) + Regex.match(videoStr, Str.get(389), Str.get(390)));
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

            return new Torrent(torrentID, magnet.MAGNET_LINK, titleName, torrent, extensions, !Regex.match(videoStr, Str.get(74), Str.get(75)).isEmpty(),
                    numSourcesNum, sizeInGiB);
        }
        return null;
    }

    private static boolean isRightSeason(int season, String title) {
        String titleName = Regex.replaceAll(title, Str.get(222), Str.get(223));
        if (Debug.DEBUG) {
            Debug.print("Desired Season (" + titleName + "): " + season);
        }

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
        String[] titleParts = VideoSearch.getTitleParts(titleName, video.IS_TV_SHOW);
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

        String sourceStr = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        titleParts = VideoSearch.getImdbTitleParts(sourceStr);
        String title = titleParts[0];
        if (isOldTitle && (title = Regex.match(sourceStr, Str.get(174), Str.get(175))).isEmpty()) {
            return false;
        }

        String year = titleParts[1];

        if (!VideoSearch.isImdbVideoType(sourceStr, video.IS_TV_SHOW)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (video.IS_TV_SHOW ? "TV show" : "movie") + "): '" + title + "' '" + year + '\'');
            }
            return false;
        }

        if (isBoxSet) {
            if (boxSet == null) {
                String cleanTitle1, cleanTitle2;
                return startsWith(video.title, title) || startsWith(title, video.title) || startsWith(cleanTitle1 = Regex.htmlToPlainText(video.title), cleanTitle2
                        = Regex.htmlToPlainText(title)) || startsWith(cleanTitle2, cleanTitle1);
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
                String titlePrefix = VideoSearch.getMovieTitlePrefix(title);
                if (titlePrefix != null) {
                    title = titlePrefix;
                }
            }
            return video.year.equals(year) && (video.title.equals(title) || Regex.htmlToPlainText(video.title).equals(Regex.htmlToPlainText(title)));
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

    static void saveTorrent(String torrentID, boolean orderByLeechers) {
        if (!torrentID.isEmpty() && !savedTorrents.containsKey(torrentID)) {
            savedTorrents.put(torrentID, orderByLeechers);
        }
    }
}

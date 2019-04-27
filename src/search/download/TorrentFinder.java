package search.download;

import debug.Debug;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
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
import util.IOException2;
import util.Regex;
import util.Worker;

public class TorrentFinder extends Worker {

    private GuiListener guiListener;
    private Iterable<? extends Future<?>> cohort;
    private Collection<Torrent> torrents;
    private Video video;
    private String seasonAndEpisode;
    private boolean orderByLeechers, magnetLinkOnly, ignoreYear, isOldTitle, isTitlePrefix, possiblyInconsistent, generalSearch, altSearch, singleOrderByMode;
    private final int maxNumAttempts, counter1Max, counter2Max;
    private int attemptNum, counter1, counter2;
    private String categorySearch, url;
    private TorrentSearchState searchState;
    private List<BoxSetVideo> boxSet;
    private static final ConcurrentMap<TorrentSearchID, Boolean> orderings = new ConcurrentHashMap<TorrentSearchID, Boolean>(16);

    TorrentFinder(GuiListener guiListener, Iterable<? extends Future<?>> cohort, Collection<Torrent> torrents, Video video, String seasonAndEpisode,
            boolean orderByLeechers, boolean magnetLinkOnly, boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix, TorrentSearchState searchState,
            List<BoxSetVideo> boxSet, boolean altSearch, boolean singleOrderByMode) {
        this.guiListener = guiListener;
        this.cohort = cohort;
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
        maxNumAttempts = Integer.parseInt(Str.get(176));
        counter1Max = Integer.parseInt(Str.get(168));
        counter2Max = Integer.parseInt(Str.get(boxSet == null ? 170 : 336));
        this.altSearch = altSearch;
        this.singleOrderByMode = singleOrderByMode;
    }

    TorrentFinder(TorrentFinder torrentFinder, Iterable<? extends Future<?>> cohort) {
        guiListener = torrentFinder.guiListener;
        this.cohort = cohort;
        torrents = torrentFinder.torrents;
        video = torrentFinder.video;
        seasonAndEpisode = torrentFinder.seasonAndEpisode;
        orderByLeechers = torrentFinder.orderByLeechers;
        magnetLinkOnly = torrentFinder.magnetLinkOnly;
        ignoreYear = torrentFinder.ignoreYear;
        isOldTitle = torrentFinder.isOldTitle;
        isTitlePrefix = torrentFinder.isTitlePrefix;
        searchState = torrentFinder.searchState;
        boxSet = torrentFinder.boxSet;
        categorySearch = torrentFinder.categorySearch;
        maxNumAttempts = torrentFinder.maxNumAttempts;
        counter1Max = torrentFinder.counter1Max;
        counter2Max = torrentFinder.counter2Max;
        altSearch = torrentFinder.altSearch;
        singleOrderByMode = torrentFinder.singleOrderByMode;
    }

    @Override
    protected void doWork() {
        if (altSearch) {
            try {
                getTorrents(false);
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
            return;
        }

        while (true) {
            if (Debug.DEBUG) {
                Debug.println("Thread: " + Thread.currentThread().getName() + " Attempt: " + (attemptNum + 1));
            }
            if (isCancelled()) {
                return;
            }

            possiblyInconsistent = true;

            Collection<Torrent> newTorrents = null;
            try {
                newTorrents = getTorrents(false, generalSearch = !generalSearch);
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                    possiblyInconsistent = false;
                }
            }

            if (isCancelled()) {
                return;
            }

            if (possiblyInconsistent && ++attemptNum < maxNumAttempts) {
                if (url != null) {
                    Connection.removeDownloadLinkInfoFromCache(url);
                }
                continue;
            }

            if (newTorrents != null && !newTorrents.isEmpty() && !isCancelled()) {
                torrents.addAll(newTorrents);
            }
            return;
        }
    }

    public Collection<Torrent> getTorrents(boolean prefetch, boolean generalSearch) throws Exception {
        String urlForm = Str.get(721), urlFormOptions = URLEncoder.encode(Regex.clean(video.title) + (ignoreYear ? "" : (' ' + video.year)) + seasonAndEpisode,
                Constant.UTF8) + (generalSearch ? Str.get(657) : categorySearch);
        if (isCancelled()) {
            return null;
        }

        String sourceCode;
        try {
            sourceCode = Connection.getSourceCode(url = urlForm + urlFormOptions, DomainType.DOWNLOAD_LINK_INFO, !prefetch, false, FileNotFoundException.class);
        } catch (IOException2 e) {
            if (Debug.DEBUG) {
                Debug.println(e);
            }
            if (Regex.firstMatch(e.extraMsg, 146).isEmpty()) {
                return null;
            }
            sourceCode = e.extraMsg;
        }

        if (!isSourceCodeValid(sourceCode) || isCancelled()) {
            return null;
        }

        String firstPageLink = Regex.firstMatch(sourceCode, Str.get(orderByLeechers ? 660 : 661));
        firstPageLink = Regex.replaceAllRepeatedly(firstPageLink, 666);
        if (firstPageLink.isEmpty()) {
            return null;
        }

        sourceCode = Connection.getSourceCode(url = Str.get(722) + firstPageLink, DomainType.DOWNLOAD_LINK_INFO, !prefetch);
        if (!isSourceCodeValid(sourceCode) || isCancelled() || prefetch) {
            return null;
        }

        int currPageNum = Integer.parseInt(Str.get(662));
        while (counter2 != counter2Max) {
            Collection<Torrent> newTorrents = getTorrents(722, sourceCode);

            if (counter1 == counter1Max || isCancelled()) {
                return null;
            }
            if (!newTorrents.isEmpty()) {
                return newTorrents;
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

            sourceCode = Connection.getSourceCode(url = Str.get(724) + query + Str.get(492) + currPageNum + Regex.match(nextPageLink, 493),
                    DomainType.DOWNLOAD_LINK_INFO);
            if (!isSourceCodeValid(sourceCode) || isCancelled()) {
                return null;
            }
        }

        return null;
    }

    private boolean isSourceCodeValid(String sourceCode) throws ConnectionException {
        if (!Regex.firstMatch(sourceCode, 146).isEmpty() || Regex.firstMatch(sourceCode, 504).isEmpty()) {
            Connection.removeDownloadLinkInfoFromCache(url);
            if (Connection.deproxyDownloadLinkInfo()) {
                for (Future<?> future : cohort) {
                    future.cancel(true);
                }
                return false;
            }
            throw new ConnectionException(Connection.serverError(url));
        }
        return true;
    }

    public void getTorrents(boolean prefetch) throws Exception {
        String sourceCode = Connection.getSourceCode(Str.get(700) + URLEncoder.encode(video.ID, Constant.UTF8), DomainType.DOWNLOAD_LINK_INFO, !prefetch, true);
        if (!isCancelled() && !sourceCode.isEmpty() && !prefetch) {
            Collection<Torrent> newTorrents = getTorrents(700, sourceCode);
            if (!newTorrents.isEmpty() && !isCancelled()) {
                torrents.addAll(newTorrents);
            }
        }
    }

    private Collection<Torrent> getTorrents(int urlIndex, String sourceCode) throws Exception {
        int i = 0, j = Integer.parseInt(Str.get(764));
        Collection<Torrent> newTorrents = new ArrayList<Torrent>(j);
        Matcher titleMatcher = Regex.matcher(48, sourceCode);

        while (!titleMatcher.hitEnd() && counter1 != counter1Max) {
            if (isCancelled()) {
                break;
            }
            if (!titleMatcher.find()) {
                continue;
            }

            String videoStr = Regex.match(sourceCode.substring(titleMatcher.end()), 49), titleName = Regex.match(videoStr, 51);
            if (titleName.isEmpty() || !Regex.isMatch(Regex.match(videoStr, 53), video.IS_TV_SHOW_AND_MOVIE ? 588 : (video.IS_TV_SHOW ? 562 : 563))) {
                continue;
            }

            i++;
            possiblyInconsistent = false;

            String torrentID = Regex.match(Regex.firstMatch(videoStr, 460), 461);
            Boolean prevOrderByLeechers;
            boolean isBoxSet;
            if ((!singleOrderByMode && (prevOrderByLeechers = orderings.get(new TorrentSearchID(torrentID, video.season, video.episode))) != null
                    && orderByLeechers != prevOrderByLeechers) || VideoSearch.isUploadYearTooOld(videoStr, 1, Integer.parseInt(video.year))
                    || !VideoSearch.isRightFormat(titleName, searchState.format) || (!(isBoxSet = !Regex.firstMatch(Regex.replaceAll(titleName, 220),
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
                        String name = Regex.replaceFirst(titleName, Str.get(733) + Integer.valueOf(video.episode) + Str.get(734), " E" + video.episode + ' ');
                        if (name.equals(titleName)) {
                            continue;
                        }
                        titleName = name;
                    }
                } else if (anyEpisode) {
                    if (isBoxSet) {
                        if (!isRightSeason(titleName)) {
                            continue;
                        }
                    } else {
                        TitleParts titleParts = VideoSearch.getTitleParts(titleName, video.IS_TV_SHOW);
                        if (!titleParts.season.isEmpty() && !video.season.equals(titleParts.season)) {
                            continue;
                        }
                    }
                } else {
                    TitleParts titleParts = VideoSearch.getTitleParts(titleName, video.IS_TV_SHOW);
                    if (!titleParts.season.isEmpty() && !titleParts.episodes.isEmpty()) {
                        if (!video.season.equals(titleParts.season) || !titleParts.episodes.contains(video.episode)) {
                            continue;
                        }
                    } else if ((!episode(titleName, 686).isEmpty() && episode(titleName, 626).isEmpty()) || !isRightSeason(titleName)) {
                        continue;
                    }
                }
            }

            if (!isRightTitle(titleName, isBoxSet)) {
                continue;
            }

            if (isCancelled()) {
                break;
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
                break;
            }

            File torrent;
            String extensions;
            if (isTorrentDownloaded) {
                FileTypeChecker fileTypeChecker = new FileTypeChecker(searchState.whitelistedFileExts, searchState.blacklistedFileExts);
                if (!fileTypeChecker.isValidFileType(magnet.TORRENT)) {
                    continue;
                }

                if (isCancelled()) {
                    break;
                }

                torrent = magnet.TORRENT;
                extensions = fileTypeChecker.getFileExts();
            } else {
                torrent = null;
                extensions = "";
            }

            boolean isSafe = !Regex.match(videoStr, 74).isEmpty();
            newTorrents.add(new Torrent(torrentID, magnet.MAGNET_LINK, Regex.htmlToPlainText(titleName), torrent, extensions, Str.get(730) + Regex.match(
                    Regex.firstMatch(videoStr, 675), 676) + Str.get(678), isSafe, numSourcesNum, sizeInGiB));
            if (isSafe || i >= j) {
                break;
            }
        }
        return newTorrents;
    }

    private String episode(String title, int startRegex) {
        int episodeNum = Integer.parseInt(video.episode);
        return Regex.firstMatch(Regex.replaceAll(title, 222), Str.get(startRegex) + '(' + video.episode + ")|(" + episodeNum + ")|(" + getRomanNumeral(episodeNum)
                + ')' + Str.get(startRegex + 1));
    }

    private boolean isRightSeason(String title) {
        int season = Integer.parseInt(video.season);
        String seasonRomanNumeral = getRomanNumeral(season), titleName = Regex.replaceAll(title, 222);
        if (Debug.DEBUG) {
            Debug.print("Desired Season (" + titleName + "): " + season + " (" + seasonRomanNumeral + ')');
        }

        Matcher numListMatcher = Regex.matcher(216, titleName);
        while (!numListMatcher.hitEnd()) {
            if (numListMatcher.find()) {
                for (String num : Regex.split(numListMatcher.group().trim(), 705)) {
                    if ((Regex.isMatch(num, "\\d++") && season == Integer.parseInt(num)) || num.equalsIgnoreCase(seasonRomanNumeral)) {
                        if (Debug.DEBUG) {
                            Debug.println("\tCorrect Season!");
                        }
                        return true;
                    }
                }
            }
        }

        Matcher seasonMatcher = Regex.matcher(215, titleName);
        while (!seasonMatcher.hitEnd()) {
            if (!seasonMatcher.find()) {
                continue;
            }

            String seasonNum = Regex.replaceAll(seasonMatcher.group(), 217);
            if (Debug.DEBUG) {
                Debug.print("\tseasonNum: '" + seasonNum + '\'');
            }
            if (Regex.isMatch(seasonNum, 219)) {
                if (Debug.DEBUG) {
                    Debug.print("\tcorrectSeasonNum: '" + seasonRomanNumeral + '\'');
                }
                if (!seasonNum.equalsIgnoreCase(seasonRomanNumeral)) {
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
        if ((titleParts.year.isEmpty() || titleParts.year.equals(video.year)) && Regex.replaceAll(Regex.htmlToPlainText(titleParts.title.replace(':', ' ')),
                339).trim().equalsIgnoreCase(Regex.replaceAll(Regex.htmlToPlainText(Regex.replaceAll(video.title, 103).replace(':', ' ')), 339).trim())) {
            return true;
        }

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
                String titlePrefix = VideoSearch.getTitlePrefix(titleParts.title);
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

    static void saveOrdering(String torrentID, String season, String episode, boolean orderByLeechers) {
        if (!torrentID.isEmpty()) {
            orderings.putIfAbsent(new TorrentSearchID(torrentID, season, episode), orderByLeechers);
        }
    }

    private static class TorrentSearchID {

        private final String torrentID, season, episode;

        TorrentSearchID(String torrentID, String season, String episode) {
            this.torrentID = torrentID;
            this.season = season;
            this.episode = episode;
        }

        @Override
        public boolean equals(Object obj) {
            TorrentSearchID torrentSearchID;
            return this == obj || (obj instanceof TorrentSearchID && torrentID.equals((torrentSearchID = (TorrentSearchID) obj).torrentID)
                    && season.equals(torrentSearchID.season) && episode.equals(torrentSearchID.episode));
        }

        @Override
        public int hashCode() {
            int hash = 7 * 31 + (torrentID == null ? 0 : torrentID.hashCode());
            hash = hash * 31 + (season == null ? 0 : season.hashCode());
            return hash * 31 + (episode == null ? 0 : episode.hashCode());
        }
    }
}

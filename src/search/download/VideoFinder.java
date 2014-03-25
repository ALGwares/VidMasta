package search.download;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import javax.swing.text.Element;
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
import util.ExceptionUtil;
import util.IO;
import util.Regex;
import util.RunnableUtil;

public class VideoFinder extends AbstractSwingWorker {

    GuiListener guiListener;
    private static final String BROWSE_ACTION = "started";
    private static final String NOT_FOUND = " could not be found.";
    String titleID;
    private String dirtyOldTitle, oldTitle, dirtyTitle, title, year, season, episode, summaryLink, imageLink, orderBy;
    volatile String streamLink;
    public final int row, action;
    private static SwingWorker<?, ?> nextEpisodeFinder;
    static final Map<String, Boolean> savedTorrents = new HashMap<String, Boolean>(16);
    private List<Torrent> torrents;
    private Collection<TorrentFinder> torrentFinders;
    boolean isStream2;
    private boolean isTVShow, isTVShowAndMovie, isLink, findOldTitleStream, prefetch;
    private TorrentSearchState searchState;
    private CommentsFinder commentsFinder;
    private static int failCount;
    private final AtomicBoolean isLinkProgressDone = new AtomicBoolean();

    public VideoFinder(GuiListener guiListener, int action, String titleID, String title, String summaryLink, String imageLink, boolean isLink, String year,
            boolean isTVShow, boolean isTVShowAndMovie, String season, String episode, int row) {
        this(guiListener, action, titleID, title, summaryLink, imageLink, isLink, year, isTVShow, isTVShowAndMovie, season, episode, row, false);
    }

    public VideoFinder(int action, VideoFinder finder) {
        this(finder.guiListener, action, finder.titleID, finder.title, finder.summaryLink, finder.imageLink, finder.isLink, finder.year, finder.isTVShow,
                finder.isTVShowAndMovie, finder.season, finder.episode, finder.row, true);
    }

    private VideoFinder(GuiListener guiListener, int action, String titleID, String title, String summaryLink, String imageLink, boolean isLink, String year,
            boolean isTVShow, boolean isTVShowAndMovie, String season, String episode, int row, boolean prefetch) {
        this.guiListener = guiListener;
        this.action = action;
        this.titleID = titleID;
        dirtyTitle = title;
        this.title = Str.clean(title);
        this.summaryLink = summaryLink;
        this.imageLink = imageLink;
        this.isLink = isLink;
        this.year = year;
        this.isTVShow = isTVShow;
        this.isTVShowAndMovie = isTVShowAndMovie;
        this.season = season;
        this.episode = episode;
        this.row = row;
        this.prefetch = prefetch;
        if (action == Constant.TORRENT1_ACTION || action == Constant.TORRENT2_ACTION) {
            torrents = new ArrayList<Torrent>(12);
            torrentFinders = new ArrayList<TorrentFinder>(12);
        } else if (action == Constant.TORRENT3_ACTION) {
            torrents = new ArrayList<Torrent>(1);
        }
    }

    @Override
    protected Object doInBackground() {
        guiListener.loading(true);
        search();
        workDone();
        guiListener.loading(false);
        return null;
    }

    public boolean isLinkProgressDone() {
        return isLinkProgressDone.get();
    }

    public void prefetch() throws Exception {
        if (action == Constant.SUMMARY_ACTION) {
        } else if (action == Constant.TRAILER_ACTION) {
            findTrailer();
        } else if (action == Constant.STREAM1_ACTION || (isStream2 = (action == Constant.STREAM2_ACTION))) {
            directStreamSearch();
        } else {
            initDownloadState();

            if (isTVShow) {
                if (season == null) {
                    if (guiListener.getSeason().equals(Constant.ANY)) {
                        season = Constant.ANY;
                    } else {
                        return;
                    }
                }
                findTVDownloadLink();
            } else {
                findMovieDownloadLink();
            }

            for (TorrentFinder finder : torrentFinders) {
                finder.getTorrent(true, true);
            }
        }
    }

    private void initDownloadState() {
        searchState = new TorrentSearchState(guiListener);
        orderBy = Str.get(action == Constant.TORRENT1_ACTION ? 161 : 162);
    }

    private void linkProgressDone() {
        isLinkProgressDone.set(true);
        guiListener.enableLinkProgress(false);
    }

    private void watchStopped() {
        guiListener.videoWatchStopped();
        linkProgressDone();
    }

    private void downloadStopped() {
        guiListener.videoDownloadStopped();
        linkProgressDone();
    }

    private void msg(String msg, int msgType) {
        linkProgressDone();
        guiListener.msg(msg, msgType);
    }

    private void browseStream(String url) throws Exception {
        linkProgressDone();
        guiListener.browserNotification("video", BROWSE_ACTION, Connection.VIDEO_STREAMER);
        Connection.browse(url);
    }

    private void search() {
        if (action == Constant.SUMMARY_ACTION) {
            guiListener.readSummaryStarted();
            try {
                findSummary();
            } catch (Exception e) {
                guiListener.error(e);
            }
            guiListener.readSummaryStopped();
        } else if (action == Constant.TRAILER_ACTION) {
            guiListener.watchTrailerStarted();
            try {
                findTrailer();
            } catch (Exception e) {
                guiListener.error(e);
            }
            guiListener.watchTrailerStopped();
        } else if (action == Constant.STREAM1_ACTION || (isStream2 = (action == Constant.STREAM2_ACTION))) {
            guiListener.enableWatch(false);

            try {
                findStream();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }

            watchStopped();
        } else {
            initDownloadState();
            guiListener.enableDownload(false);

            try {
                if (action == Constant.TORRENT3_ACTION) {
                    findAltDownloadLink();
                } else if (isTVShow) {
                    if (!findTVDownloadLink()) {
                        downloadStopped();
                        return;
                    }
                } else {
                    findMovieDownloadLink();
                }

                if (isCancelled()) {
                    downloadStopped();
                    return;
                }

                if (action != Constant.TORRENT3_ACTION && Connection.downloadLinkInfoFail()) {
                    if (torrents.isEmpty()) {
                        findAltDownloadLink();
                    } else {
                        Connection.downloadLinkInfoUnFail();
                    }
                }

                if (isCancelled()) {
                    downloadStopped();
                    return;
                }

                Torrent torrent = null;
                if (!torrents.isEmpty()) {
                    Collections.sort(torrents);
                    torrent = torrents.get(0);
                    if (Debug.DEBUG) {
                        Debug.println(torrents);
                    }
                }

                if (torrent == null) {
                    msg("The download link" + NOT_FOUND, Constant.INFO_MSG);
                    if (Magnet.isPortPossiblyBlocked() && ++failCount == 2) {
                        Magnet.enableIpFilter(false);
                        guiListener.msg(Constant.APP_TITLE + "'s internet connectivity may be limited. Check that the TCP/UDP port, under the download menu, is unfirewalled and forwarded.",
                                Constant.ERROR_MSG);
                    }
                } else {
                    if (Debug.DEBUG) {
                        Debug.println("Selected torrent: " + torrent);
                    }

                    if (!torrent.ID.isEmpty() && !savedTorrents.containsKey(torrent.ID)) {
                        savedTorrents.put(torrent.ID, orderBy.equals(Str.get(161)));
                    }

                    if (torrent.IS_SAFE || !guiListener.canShowSafetyWarning()) {
                        saveTorrent(torrent);
                    } else if (torrent.ID.isEmpty()) {
                        if (guiListener.canProceedWithUnsafeDownload(torrent.saveName(false))) {
                            saveTorrent(torrent);
                        }
                    } else {
                        String torrentSaveName = torrent.saveName(false);
                        guiListener.initSafetyDialog(torrentSaveName);

                        commentsFinder = new CommentsFinder(guiListener, Str.get(150) + torrent.ID, torrentSaveName);
                        commentsFinder.execute();

                        guiListener.showSafetyDialog();

                        commentsFinder.cancel(true);

                        if (guiListener.canProceedWithUnsafeDownload()) {
                            saveTorrent(torrent);
                        }
                    }
                }
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }

            downloadStopped();
        }
    }

    private void findSummary() throws Exception {
        if (nextEpisodeFinder != null) {
            nextEpisodeFinder.cancel(true);
        }

        if (isLink) {
            updateSummary();
            if (isCancelled()) {
                return;
            }
        } else {
            summaryLink = Regex.split(summaryLink, Constant.SEPARATOR2)[0];
        }

        String imagePath = Constant.CACHE_DIR + Video.imagePath(titleID);
        if (!(new File(imagePath)).exists()) {
            if (imageLink.equals(Constant.NULL)) {
                imagePath = null;
            } else {
                try {
                    Connection.saveData(imageLink, imagePath, Connection.VIDEO_INFO);
                } catch (Exception e) {
                    imagePath = null;
                    guiListener.msg("Error: cannot display video's poster image." + Constant.NEWLINE + ExceptionUtil.toString(e), Constant.ERROR_MSG);
                }
            }
        }

        guiListener.summary(summaryLink, imagePath);
        if (isTVShow) {
            findNextEpisode();
        }
    }

    private void findNextEpisode() {
        final Element nextEpisodeElement = guiListener.getSummaryElement("nextEpisode");
        final Calendar currDate = Calendar.getInstance();
        currDate.set(Calendar.HOUR_OF_DAY, 0);
        currDate.set(Calendar.MINUTE, 0);
        currDate.set(Calendar.SECOND, 0);
        currDate.set(Calendar.MILLISECOND, 0);

        (nextEpisodeFinder = new SwingWorker<Object, Object>() {
            private String nextEpisodeText = "unknown";

            @Override
            protected Object doInBackground() {
                try {
                    nextEpisode();
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                } finally {
                    if (!isCancelled()) {
                        guiListener.insertAfterSummaryElement(nextEpisodeElement, nextEpisodeText);
                    }
                }
                return null;
            }

            private void nextEpisode() throws Exception {
                String url = Str.get(519) + titleID;
                String source = Connection.getSourceCode(url, Connection.VIDEO_INFO, false);
                String latestSeason;
                if (!Regex.isMatch(latestSeason = Regex.match(source, Str.get(550), Str.get(551)), Str.get(522)) && !Regex.isMatch(latestSeason
                        = Regex.match(source, Str.get(520), Str.get(521)), Str.get(522))) {
                    return;
                }

                source = Connection.getSourceCode(url + Str.get(523) + latestSeason, Connection.VIDEO_INFO, false);
                List<String> nextEpisodes = Regex.matches(source, Str.get(524) + latestSeason + Str.get(525), Str.get(526));
                SimpleDateFormat dateFormat = new SimpleDateFormat(Str.get(538), Locale.ENGLISH);
                String nextEpisode = null, airdate = "";
                for (int i = nextEpisodes.size() - 1; i > -1; i--) {
                    String currEpisode = nextEpisodes.get(i);
                    if (Regex.isMatch(currEpisode, Str.get(527))) {
                        String date = Regex.replaceAll(Regex.match(source, Str.get(528) + latestSeason + Str.get(529) + currEpisode + Str.get(530), Str.get(531)),
                                Str.get(532), Str.get(533));
                        if (Regex.isMatch(date, Str.get(534)) && dateFormat.parse(Regex.replaceAll(date, Str.get(535), Str.get(536))).compareTo(currDate.getTime())
                                <= 0) {
                            if (nextEpisode == null) {
                                airdate = date;
                                nextEpisode = currEpisode;
                            }
                            break;
                        }
                        airdate = date;
                        nextEpisode = currEpisode;
                    }
                }

                if (nextEpisode != null) {
                    if (Regex.isMatch(airdate, Str.get(534))) {
                        airdate = Video.dateToString(dateFormat, Regex.replaceAll(airdate, Str.get(535), Str.get(536)), Boolean.parseBoolean(Str.get(558)));
                    } else if (Regex.isMatch(airdate, Str.get(546))) {
                        airdate = Video.dateToString(new SimpleDateFormat(Str.get(547), Locale.ENGLISH), Regex.replaceAll(airdate, Str.get(535), Str.get(536)),
                                Boolean.parseBoolean(Str.get(559)));
                    } else if (airdate.isEmpty() || Regex.isMatch(airdate, Str.get(537))) {
                        airdate = "unknown";
                    }
                    nextEpisodeText = "S" + String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(latestSeason)) + "E"
                            + String.format(Constant.TV_EPISODE_FORMAT, Integer.valueOf(nextEpisode)) + " (airdate " + airdate + ")";
                }
            }
        }).execute();
    }

    private void browse(String torrentFileName) throws Exception {
        String autoDownloader = guiListener.getAutoDownloader();
        String url = Regex.replaceFirst(autoDownloader, Str.get(463), Str.get(464));

        try {
            Connection.getSourceCode(url, Connection.DOWNLOAD_LINK_INFO);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (isCancelled()) {
                return;
            }
            guiListener.error(new ConnectionException(Connection.error("", "selecting a different auto-downloader under the download menu or ", url)));
            autoDownloader = Str.get(autoDownloader.equals(Str.get(393)) ? 394 : 393);
        } finally {
            Connection.removeFromCache(url);
        }
        if (isCancelled()) {
            return;
        }

        linkProgressDone();
        guiListener.startPeerBlock();
        guiListener.browserNotification("download", BROWSE_ACTION, Connection.DOWNLOAD_LINK_INFO);
        Connection.browse(autoDownloader + URLEncoder.encode(torrentFileName, Constant.UTF8));
    }

    private void saveTorrent(Torrent torrent) throws Exception {
        if (torrent.FILE == null || !torrent.FILE.exists()) {
            guiListener.error(new ConnectionException(Connection.error("downloading<br>" + torrent.saveName(false), "", "")));
            linkProgressDone();
            Connection.browse(torrent.MAGNET_LINK, "BitTorrent client", "magnet");
            return;
        }

        String torrentFilePath = IO.parentDir(torrent.FILE) + torrent.saveName(true);
        File torrentFile = new File(torrentFilePath);
        if (!torrentFile.exists()) {
            IO.write(torrent.FILE, torrentFile);
        }

        if (guiListener.canAutoDownload()) {
            browse(torrentFilePath);
        } else {
            linkProgressDone();
            guiListener.saveTorrent(torrentFile);
        }
    }

    private void findStream() throws Exception {
        guiListener.enableLinkProgress(true);

        directStreamSearch();

        if (isCancelled()) {
            return;
        }

        if (streamLink == null) {
            indirectStreamSearch();
        } else {
            browseStream(Str.get(isStream2 ? 265 : 410) + streamLink);
        }
    }

    private void indirectStreamSearch() throws Exception {
        String link = indirectStreamSearchHelper();
        if (isCancelled()) {
            return;
        }

        if (link.isEmpty()) {
            findOldTitleStream();
        } else {
            link = Str.get(isStream2 ? 284 : 376) + link;
            boolean isValidStream = isValidateStream(link);
            if (isCancelled()) {
                return;
            }

            if (isValidStream) {
                browseStream(link);
            } else {
                findOldTitleStream();
            }
        }
    }

    private void findOldTitleStream() throws Exception {
        if (findOldTitleStream) {
            msg("The video" + NOT_FOUND, Constant.INFO_MSG);
            return;
        }

        findOldTitleStream = true;
        getOldTitle();
        if (isCancelled()) {
            return;
        }

        if (oldTitle == null) {
            msg("The video" + NOT_FOUND, Constant.INFO_MSG);
        } else {
            findStream();
        }
    }

    boolean isValidateStream(String link) throws Exception {
        String source = Connection.getSourceCode(link, Connection.VIDEO_STREAMER);
        if (isStream2) {
            return isValidateStreamHelper(Regex.match(source, Str.get(285)));
        } else {
            String newYear = Regex.match(Regex.match(source, Str.get(377), Str.get(378)),
                    Str.get(368));
            if (!isTVShow && newYear.isEmpty()) {
                return false;
            }
            String newTitle = Regex.match(source, Str.get(373), Str.get(374));
            return isValidateStream(newTitle, newYear);
        }
    }

    boolean isValidateStream(String title, String year) throws Exception {
        return isValidateStreamHelper(VideoSearch.getTitleLink(title, year));
    }

    private boolean isValidateStreamHelper(String titleLink) throws Exception {
        if (isCancelled() || titleLink == null || titleLink.isEmpty()) {
            return false;
        }
        String source = Connection.getSourceCode(titleLink, Connection.VIDEO_INFO);
        String[] titleParts = VideoSearch.getImdbTitleParts(source);
        String newTitle = titleParts[0];
        if (findOldTitleStream && (newTitle = Regex.match(source, Str.get(174), Str.get(175))).isEmpty()) {
            return false;
        }
        newTitle = Str.clean(newTitle);
        String newYear = titleParts[1];

        if (!VideoSearch.isImdbVideoType(source, isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + newTitle + "' '" + newYear + '\'');
            }
            return false;
        }

        if (Debug.DEBUG) {
            Debug.println("Stream result: '" + newTitle + "' '" + newYear + "'");
        }
        return newTitle.equals(findOldTitleStream ? oldTitle : title) && newYear.equals(year);
    }

    private String indirectStreamSearchHelper() throws Exception {
        String link = Regex.match(VideoSearch.searchEngineQuery((findOldTitleStream ? oldTitle : title) + (year.isEmpty() ? "" : ' ' + year) + ' '
                + Str.get(isStream2 ? 279 : 371)), Str.get(isStream2 ? 281 : 372));
        return link.isEmpty() ? link : link.substring(0, link.length() - 1);
    }

    private void directStreamSearch() throws Exception {
        List<String> results;
        if (isStream2) {
            String source = Connection.getSourceCode(Str.get(isTVShow ? 260 : 261) + URLEncoder.encode(findOldTitleStream ? oldTitle : title, Constant.UTF8),
                    Connection.VIDEO_STREAMER, !prefetch);
            if (prefetch) {
                return;
            }
            results = Regex.matches(source, Str.get(isTVShow ? 262 : 263));
        } else {
            String source, key;
            if (Boolean.parseBoolean(Str.get(572))) {
                source = Connection.getSourceCode(Str.get(396), Connection.VIDEO_STREAMER, !prefetch);
                key = Regex.match(source, Str.get(573), Str.get(574));
                if (!key.isEmpty()) {
                    key = Str.get(575) + key;
                }
            } else {
                key = "";
            }

            source = Connection.getSourceCode(Str.get(399) + Str.get(isTVShow ? 400 : 401) + Str.get(402) + URLEncoder.encode(findOldTitleStream ? oldTitle
                    : title, Constant.UTF8) + Str.get(576) + key, Connection.VIDEO_STREAMER, !prefetch);
            if (prefetch) {
                return;
            }
            results = Regex.matches(source, Str.get(405));
        }

        Collection<StreamFinder> streamFinders = new ArrayList<StreamFinder>(35);
        int numResults = results.size(), maxNumSerialSearches = Integer.parseInt(Str.get(290)), maxNumResults = Integer.parseInt(Str.get(310));
        for (int i = 0; i < numResults && i < maxNumResults; i++) {
            StreamFinder streamFinder = new StreamFinder(results.get(i));
            if (i < maxNumSerialSearches) {
                if (isCancelled()) {
                    return;
                }

                streamFinder.execute();
                RunnableUtil.waitFor(streamFinder);
                if (streamLink != null) {
                    return;
                }
            } else {
                streamFinders.add(streamFinder);
            }
        }

        RunnableUtil.execute(streamFinders);
    }

    private void findTrailer() throws Exception {
        if (!prefetch && isTVShow && guiListener.tvChoices(season, episode)) {
            return;
        }

        String seasonStr;
        if (isTVShow) {
            if (!prefetch) {
                season = guiListener.getSeason();
            } else if (season == null) {
                season = Constant.ANY;
            }
            seasonStr = (season.equals(Constant.ANY) ? "" : " \"season " + Integer.valueOf(season).toString() + '\"');
        } else {
            seasonStr = "";
        }
        String link = getTrailerLink(seasonStr);

        if (prefetch) {
            return;
        }

        if (link == null) {
            guiListener.msg("A trailer" + NOT_FOUND, Constant.INFO_MSG);
        } else {
            if (Debug.DEBUG) {
                Debug.println("Trailer: '" + link + '\'');
            }
            guiListener.browserNotification("trailer", BROWSE_ACTION, Connection.TRAILER);
            Connection.browse(link);
        }
    }

    private String getTrailerLink(String seasonStr) throws Exception {
        String urlForm = Str.get(86);
        String urlFormOptions = URLEncoder.encode(title + seasonStr + (isTVShow ? "" : (' ' + year)) + Str.get(87), Constant.UTF8);
        String source = Connection.getSourceCode(urlForm + urlFormOptions, Connection.TRAILER, !prefetch);
        if (prefetch) {
            return null;
        }

        String noResultsStr = Regex.match(source, Str.get(88), Str.get(89));
        if (!noResultsStr.isEmpty() && noResultsStr.contains(URLDecoder.decode(urlFormOptions, Constant.UTF8))) {
            return null;
        }

        String trailerID = Regex.match(Regex.match(source, Str.get(90)), Str.get(92), Str.get(93));
        if (trailerID.isEmpty()) {
            return null;
        }

        return Str.get(91) + URLEncoder.encode(trailerID, Constant.UTF8);
    }

    private void findAltDownloadLink() throws Exception {
        guiListener.altVideoDownloadStarted();

        String[] results = Regex.split(Connection.getSourceCode(Str.get(isTVShow ? 483 : 484), Connection.DOWNLOAD_LINK_INFO), Constant.STD_NEWLINE);
        for (int i = 0; i < results.length; i += 5) {
            if (!results[i].trim().equals(dirtyTitle) || !results[i + 1].trim().equals(year)) {
                continue;
            }

            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return;
            }

            Magnet magnet = new Magnet(results[i + 4].trim());
            try {
                boolean isTorrentDownloaded = magnet.download(this);
                if (isCancelled()) {
                    return;
                }

                File torrent;
                String extensions;
                if (isTorrentDownloaded) {
                    torrent = magnet.TORRENT;
                    extensions = FileTypeChecker.getFileExts(magnet.TORRENT, searchState.whitelistedFileExts);
                } else {
                    torrent = null;
                    extensions = "";
                }

                torrents.add(new Torrent("", magnet.MAGNET_LINK, results[i + 2].trim(), torrent, extensions, Integer.parseInt(results[i + 3].trim()) == 1, 0, 0));
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
            return;
        }
    }

    private boolean findTVDownloadLink() throws Exception {
        if (!prefetch) {
            if (guiListener.tvChoices(season, episode)) {
                return false;
            }

            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return false;
            }

            season = guiListener.getSeason();
            episode = guiListener.getEpisode();
        }

        getOldTitle();
        if (isCancelled()) {
            return false;
        }

        Collection<String> seasonAndEpisodes = new ArrayList<String>(3);
        if (season.equals(Constant.ANY)) {
            seasonAndEpisodes.add("");
        } else {
            String seasonNum = Integer.valueOf(season).toString();
            if (episode.equals(Constant.ANY)) {
                seasonAndEpisodes.add(" season " + seasonNum);
            } else {
                seasonAndEpisodes.add(" s" + season + 'e' + episode);
                seasonAndEpisodes.add(" " + seasonNum + 'x' + episode);
                boolean isSeasonZeroPadded = !seasonNum.equals(season);
                if (isSeasonZeroPadded) {
                    seasonAndEpisodes.add(" " + season + 'x' + episode);
                }
                String episodeNum = Integer.valueOf(episode).toString();
                if (!episodeNum.equals(episode)) {
                    seasonAndEpisodes.add(" " + seasonNum + 'x' + episodeNum);
                    if (isSeasonZeroPadded) {
                        seasonAndEpisodes.add(" " + season + 'x' + episodeNum);
                    }
                }
            }
        }
        addTVLinks(seasonAndEpisodes);

        if (!prefetch) {
            RunnableUtil.execute(torrentFinders);
        }
        return true;
    }

    private void findMovieDownloadLink() throws Exception {
        if (!prefetch) {
            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return;
            }
        }

        getOldTitle();
        if (isCancelled()) {
            return;
        }

        addMovieLinks(dirtyTitle, false);
        if (dirtyOldTitle != null) {
            addMovieLinks(dirtyOldTitle, true);
        }

        if ((Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(year)) > Integer.parseInt(Str.get(337))) {
            outer:
            for (List<BoxSetVideo> boxSet : BoxSetVideo.movieBoxSets) {
                int numTitles = boxSet.size();
                for (int i = 1; i < numTitles; i++) {
                    BoxSetVideo video = boxSet.get(i);
                    if (video.isSameTitle(dirtyTitle, year)) {
                        if (Debug.DEBUG) {
                            Debug.println("title belongs to movie box set");
                        }
                        addBoxSetLinks(boxSet, false);
                        break outer;
                    } else if (dirtyOldTitle != null && video.isSameTitle(dirtyOldTitle, year)) {
                        if (Debug.DEBUG) {
                            Debug.println("ORIGINAL title belongs to movie box set");
                        }
                        addBoxSetLinks(boxSet, true);
                        break outer;
                    }
                }
            }
        }

        if (!prefetch) {
            RunnableUtil.execute(torrentFinders);
        }
    }

    private void addTVLinks(Iterable<String> seasonAndEpisodes) {
        for (String seasonAndEpisode : seasonAndEpisodes) {
            addFinder(dirtyTitle, seasonAndEpisode, true, false, false);
            addFinder(dirtyTitle, seasonAndEpisode, false, false, false);
        }
        if (dirtyOldTitle != null) {
            for (String seasonAndEpisode : seasonAndEpisodes) {
                addFinder(dirtyOldTitle, seasonAndEpisode, true, true, false);
                addFinder(dirtyOldTitle, seasonAndEpisode, false, true, false);
            }
        }
    }

    private void addMovieLinks(String dirtyMovieTitle, boolean isOldTitle) {
        String dirtyMovieTitlePrefix = Video.getMovieTitlePrefix(dirtyMovieTitle);
        if (dirtyMovieTitlePrefix != null) {
            addFinder(dirtyMovieTitlePrefix, "", false, isOldTitle, true);
            addFinder(dirtyMovieTitlePrefix, "", true, isOldTitle, true);
        }
        addFinder(dirtyMovieTitle, "", false, isOldTitle, false);
        addFinder(dirtyMovieTitle, "", true, isOldTitle, false);
    }

    private void addBoxSetLinks(List<BoxSetVideo> boxSet, boolean isOldTitle) {
        for (String currTitle : BoxSetVideo.getSearchTitles(boxSet)) {
            if (currTitle == null) {
                continue;
            }
            if (Debug.DEBUG) {
                Debug.println('\'' + currTitle + "' added to download links to query for");
            }
            addFinder(currTitle, "", true, isOldTitle, false, boxSet);
        }
    }

    private void addFinder(String dirtyTitle, String seasonAndEpisode, boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix) {
        addFinder(dirtyTitle, seasonAndEpisode, ignoreYear, isOldTitle, isTitlePrefix, null);
    }

    private void addFinder(String dirtyTitle, String seasonAndEpisode, boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix, List<BoxSetVideo> boxSet) {
        torrentFinders.add(new TorrentFinder(guiListener, torrents, new Video("", dirtyTitle, year, null, null, Constant.NULL, season, episode, isTVShow,
                isTVShowAndMovie), seasonAndEpisode, orderBy, ignoreYear, isOldTitle, isTitlePrefix, new TorrentSearchState(searchState), boxSet));
    }

    private void updateSummary() throws Exception {
        String sourceCode = Connection.getSourceCode(summaryLink, Connection.VIDEO_INFO, !prefetch);
        dirtyOldTitle = Video.getDirtyOldTitle(sourceCode);
        if (dirtyOldTitle != null) {
            oldTitle = Str.clean(dirtyOldTitle);

            if (prefetch) {
                return;
            }

            String displayTitle = guiListener.getDisplayTitle(row, titleID);
            if (displayTitle != null) {
                int beginOffset = 6, endOffSet = 7;
                String startHtml = "<html>", endHtml = "</html>";
                if (displayTitle.startsWith("<html><b>")) {
                    beginOffset = 9;
                    endOffSet = 11;
                    startHtml += "<b>";
                    endHtml = "</b>" + endHtml;
                }
                displayTitle = displayTitle.substring(beginOffset, displayTitle.length() - endOffSet);
                String extraTitleInfo = Regex.match(displayTitle, " \\(Latest Episode: S\\d{2}+E\\d{2}+\\)");
                displayTitle = Regex.replaceFirst(displayTitle, " \\(Latest Episode: S\\d{2}+E\\d{2}+\\)", "") + " (AKA: " + dirtyOldTitle + ')'
                        + (extraTitleInfo.isEmpty() ? "" : ' ' + extraTitleInfo);
                guiListener.setDisplayTitle(startHtml + displayTitle + endHtml, row, titleID);
            }
        }

        if (prefetch) {
            return;
        }

        summaryLink = Video.getSummary(sourceCode, isTVShow) + (dirtyOldTitle == null ? "" : Constant.SEPARATOR2 + dirtyOldTitle);

        imageLink = Regex.match(sourceCode, Str.get(188), Str.get(189));
        if (imageLink.isEmpty()) {
            imageLink = Constant.NULL;
        }
        guiListener.setDisplaySummary(dirtyTitle + Constant.SEPARATOR1 + summaryLink + Constant.SEPARATOR1 + imageLink + Constant.SEPARATOR1 + Constant.FALSE
                + Constant.SEPARATOR1 + isTVShow + Constant.SEPARATOR1 + isTVShowAndMovie, row, titleID);
    }

    private void getOldTitle() throws Exception {
        if (isLink) {
            updateSummary();
        } else {
            String[] summaryParts = Regex.split(summaryLink, Constant.SEPARATOR2);
            if (summaryParts.length == 2) {
                dirtyOldTitle = summaryParts[1];
                oldTitle = Str.clean(dirtyOldTitle);
                if (Debug.DEBUG) {
                    Debug.println("Original Title: " + dirtyOldTitle);
                }
            }
        }
    }

    private class StreamFinder extends SwingWorker<Object, Object> {

        private String result;

        StreamFinder(String result) {
            this.result = result;
        }

        @Override
        protected Object doInBackground() {
            if (isCancelled() || streamLink != null) {
                return null;
            }

            String link = "";
            try {
                link = search();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }

            if (isCancelled() || streamLink != null || link.isEmpty()) {
                return null;
            }

            synchronized (StreamFinder.class) {
                if (isCancelled()) {
                    return null;
                }
                if (streamLink == null) {
                    streamLink = link;
                }
            }

            return null;
        }

        private String search() throws Exception {
            if (isStream2) {
                String newTitle = Regex.match(result, Str.get(276), Str.get(277));
                String newYear = Regex.match(result, Str.get(264));
                if (!newYear.isEmpty()) {
                    newYear = newYear.substring(0, 4);
                }
                if (!Regex.isMatch(newYear, Str.get(289)) && isValidateStream(newTitle, newYear)) {
                    return Regex.match(result, Str.get(287), Str.get(288));
                }
            } else {
                String[] titleParts = VideoSearch.getImdbTitleParts(result, 406);
                if (isValidateStream(titleParts[0], titleParts[1])) {
                    return Regex.match(result, Str.get(408), Str.get(409));
                }
            }
            return "";
        }
    }

    public String getComments() {
        return commentsFinder == null || commentsFinder.comments == null ? CommentsFinder.NO_COMMENTS : commentsFinder.comments;
    }
}

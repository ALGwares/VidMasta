package search.download;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import listener.ContentType;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import listener.VideoStrExportListener;
import search.BoxSetVideo;
import search.util.VideoSearch;
import str.Str;
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

    final GuiListener guiListener;
    public final ContentType CONTENT_TYPE;
    public final int ROW;
    private final VideoStrExportListener strExportListener;
    final Video video;
    public final boolean PREFETCH, PLAY;
    public final String TITLE;
    volatile String streamLink;
    final AtomicBoolean isStream2 = new AtomicBoolean(), findOldTitleStream = new AtomicBoolean(), isLinkProgressDone = new AtomicBoolean();
    private boolean isDownload1, cancelTVSelection, playAutoStart = true, startPeerblock = true;
    private String oldTitle, export;
    private static volatile SwingWorker<?, ?> episodeFinder;
    private Collection<Torrent> torrents;
    private Collection<TorrentFinder> torrentFinders;
    private TorrentSearchState searchState;
    private static volatile CommentsFinder commentsFinder;
    private static final Object tvChoicesLock = new Object();
    private static final String BROWSE_ACTION = "started";
    private static final String NOT_FOUND = " could not be found.";

    public VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener, boolean play) {
        this(guiListener, contentType, row, video, strExportListener, play, false);
    }

    public VideoFinder(ContentType contentType, VideoFinder finder) {
        this(finder.guiListener, contentType, finder.ROW, finder.video, null, false, true);
    }

    private VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener, boolean play,
            boolean prefetch) {
        this.guiListener = guiListener;
        CONTENT_TYPE = contentType;
        ROW = row;
        this.video = video;
        TITLE = Regex.clean(video.title);
        this.strExportListener = strExportListener;
        PLAY = play;
        PREFETCH = prefetch;
        if (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD2) {
            torrents = new CopyOnWriteArrayList<Torrent>();
            torrentFinders = new ArrayList<TorrentFinder>(12);
        } else if (contentType == ContentType.DOWNLOAD3) {
            torrents = new ArrayList<Torrent>(1);
        }
    }

    @Override
    protected Object doInBackground() {
        guiListener.loading(true);
        search(CONTENT_TYPE);

        boolean isCancelled = false;
        ContentType export2ContentType = null;
        String export1 = export;

        if (strExportListener == null) {
            if (PLAY && !cancelTVSelection && !isCancelled() && (CONTENT_TYPE == ContentType.DOWNLOAD1 || CONTENT_TYPE == ContentType.DOWNLOAD3)
                    && !Connection.downloadLinkInfoFail()) {
                resetDownloadVideoFinder();
                search(ContentType.DOWNLOAD2);
            }
        } else {
            isCancelled = isCancelled();
            if (strExportListener.exportSecondaryContent()) {
                if (CONTENT_TYPE == ContentType.DOWNLOAD1 || CONTENT_TYPE == ContentType.DOWNLOAD3) {
                    export = null;
                    export2ContentType = ContentType.DOWNLOAD2;
                    if (!Connection.downloadLinkInfoFail()) {
                        resetDownloadVideoFinder();
                        search(export2ContentType);
                    }
                } else if (CONTENT_TYPE == ContentType.STREAM1) {
                    export = null;
                    export2ContentType = ContentType.STREAM2;
                    isLinkProgressDone.set(false);
                    findOldTitleStream.set(false);
                    streamLink = null;
                    search(export2ContentType);
                }
            }
        }

        workDone();
        guiListener.loading(false);

        if (strExportListener != null) {
            strExportListener.export(CONTENT_TYPE, export1, isCancelled, guiListener);
            if (export2ContentType != null) {
                strExportListener.export(export2ContentType, export, isCancelled(), guiListener);
            }
        }

        return null;
    }

    private void resetDownloadVideoFinder() {
        isLinkProgressDone.set(false);
        torrents.clear();
        if (torrentFinders == null) {
            torrentFinders = new ArrayList<TorrentFinder>(12);
        } else {
            torrentFinders.clear();
        }
        startPeerblock = false;
    }

    public boolean isLinkProgressDone() {
        return isLinkProgressDone.get();
    }

    public void prefetch() throws Exception {
        if (CONTENT_TYPE == ContentType.SUMMARY) {
        } else if (CONTENT_TYPE == ContentType.TRAILER) {
            findTrailer();
        } else if (CONTENT_TYPE == ContentType.STREAM1 || CONTENT_TYPE == ContentType.STREAM2) {
            isStream2.set(CONTENT_TYPE == ContentType.STREAM2);
            directStreamSearch();
        } else {
            searchState = new TorrentSearchState(guiListener);
            isDownload1 = (CONTENT_TYPE == ContentType.DOWNLOAD1);

            if (video.IS_TV_SHOW) {
                if (video.season.isEmpty() || video.episode.isEmpty()) {
                    return;
                }
                findTVDownloadLink(false);
            } else {
                findMovieDownloadLink();
            }

            for (TorrentFinder finder : torrentFinders) {
                finder.getTorrent(true, true);
            }
        }
    }

    private void linkProgressDone() {
        isLinkProgressDone.set(true);
        guiListener.enableLinkProgress(false);
    }

    private void startPeerBlock() {
        guiListener.enableTorrentSearchStop(false);
        linkProgressDone();
        if (startPeerblock) {
            try {
                guiListener.startPeerBlock();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    private void watchStopped() {
        guiListener.enableStreamSearchStop(false);
        guiListener.videoWatchStopped();
        linkProgressDone();
    }

    private void downloadStopped() {
        guiListener.enableTorrentSearchStop(false);
        guiListener.videoDownloadStopped();
        linkProgressDone();
    }

    private String streamContent() {
        String name = "video";
        return strExportListener == null || !strExportListener.exportSecondaryContent() ? name : content(!isStream2.get(), name);
    }

    private String downloadContent(ContentType contentType) {
        String name = "download link";
        if (!PLAY && (strExportListener == null || !strExportListener.exportSecondaryContent())) {
            return name;
        }
        String content = content(isDownload1 || contentType == ContentType.DOWNLOAD3, name);
        return PLAY ? content + " to play" : content;
    }

    private static String content(boolean first, String name) {
        return (first ? "first" : "second") + ' ' + name;
    }

    private void msg(String msg, boolean isTorrentSearch) {
        if (isTorrentSearch) {
            guiListener.enableTorrentSearchStop(false);
        } else {
            guiListener.enableStreamSearchStop(false);
        }
        linkProgressDone();
        guiListener.msg("The " + msg + NOT_FOUND, Constant.INFO_MSG);
    }

    private void error(Exception e) {
        if (!isCancelled()) {
            guiListener.error(e);
        }
    }

    private void browseStream(String url) throws IOException {
        if (strExportListener != null) {
            export = url;
            return;
        }
        guiListener.enableStreamSearchStop(false);
        linkProgressDone();
        guiListener.browserNotification("video", BROWSE_ACTION, DomainType.VIDEO_STREAMER);
        Connection.browse(url);
    }

    private void search(ContentType contentType) {
        if (contentType == ContentType.SUMMARY) {
            guiListener.readSummaryStarted();
            guiListener.enableSummarySearchStop(true);
            try {
                findSummary();
            } catch (Exception e) {
                error(e);
            }
            guiListener.enableSummarySearchStop(false);
            guiListener.readSummaryStopped();
        } else if (contentType == ContentType.TRAILER) {
            guiListener.watchTrailerStarted();
            guiListener.enableTrailerSearchStop(true);
            try {
                findTrailer();
            } catch (Exception e) {
                error(e);
            }
            guiListener.enableTrailerSearchStop(false);
            guiListener.watchTrailerStopped();
        } else if (contentType == ContentType.STREAM1 || contentType == ContentType.STREAM2) {
            isStream2.set(contentType == ContentType.STREAM2);
            guiListener.enableWatch(false);
            guiListener.enableStreamSearchStop(true);

            try {
                findStream();
            } catch (Exception e) {
                error(e);
            }
            watchStopped();
        } else {
            isDownload1 = (contentType == ContentType.DOWNLOAD1);
            searchState = new TorrentSearchState(guiListener);
            guiListener.enableDownload(false);
            guiListener.enableTorrentSearchStop(true);

            try {
                if (contentType == ContentType.DOWNLOAD3) {
                    findAltDownloadLink();
                } else if (video.IS_TV_SHOW) {
                    if (!findTVDownloadLink(true)) {
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

                if (contentType != ContentType.DOWNLOAD3) {
                    if (video.IS_TV_SHOW && torrents.isEmpty() && !video.episode.isEmpty() && !Constant.ANY.equals(video.episode)
                            && String.format(Constant.TV_EPISODE_FORMAT, 1).equals(video.season) && !Connection.downloadLinkInfoFail()) { // Assumes TV show with
                        // season 1 just released and download listed with 'episode #' in name; worst case is false positive of newer season and with desired
                        // episode
                        torrentFinders.clear();
                        String prevSeason = video.season;
                        try {
                            video.season = Constant.ANY;
                            if (!findTVDownloadLink(false) || isCancelled()) {
                                downloadStopped();
                                return;
                            }
                        } finally {
                            video.season = prevSeason;
                        }
                    }

                    if (Connection.downloadLinkInfoFail()) {
                        if (torrents.isEmpty()) {
                            findAltDownloadLink();
                        } else {
                            Connection.unfailDownloadLinkInfo();
                        }
                    }
                }

                if (isCancelled()) {
                    downloadStopped();
                    return;
                }

                Torrent torrent = null;
                if (!torrents.isEmpty()) {
                    torrent = Collections.min(torrents);
                    if (Debug.DEBUG) {
                        Debug.println(torrents);
                    }
                }

                if (torrent == null) {
                    msg(downloadContent(contentType), true);
                } else {
                    if (Debug.DEBUG) {
                        Debug.println("Selected torrent: " + torrent);
                    }
                    TorrentFinder.saveTorrent(torrent.ID, contentType == ContentType.DOWNLOAD1);

                    if (torrent.IS_SAFE || !guiListener.canShowSafetyWarning()) {
                        saveTorrent(torrent);
                    } else if (torrent.ID.isEmpty()) {
                        if (guiListener.canProceedWithUnsafeDownload(torrent.saveName(false))) {
                            saveTorrent(torrent);
                        }
                    } else {
                        String torrentSaveName = torrent.saveName(false);
                        guiListener.initSafetyDialog(torrentSaveName);

                        if (commentsFinder != null) {
                            commentsFinder.cancel(true);
                        }

                        commentsFinder = new CommentsFinder(guiListener, torrent.COMMENTS_LINK, torrentSaveName);
                        commentsFinder.execute();

                        guiListener.showSafetyDialog();

                        commentsFinder.cancel(true);

                        if (guiListener.canProceedWithUnsafeDownload()) {
                            saveTorrent(torrent);
                        }
                    }
                }
            } catch (Exception e) {
                error(e);
            }

            downloadStopped();
        }
    }

    private void findSummary() throws Exception {
        if (strExportListener != null) {
            updateOldTitleAndSummary();
            export = Regex.htmlToPlainText(video.title) + (video.oldTitle.isEmpty() ? "" : " (" + Regex.htmlToPlainText(video.oldTitle) + ')') + ' ' + video.year;
            return;
        }

        if (episodeFinder != null) {
            episodeFinder.cancel(true);
        }

        if (video.summary.isEmpty()) {
            updateOldTitleAndSummaryHelper();
            if (isCancelled()) {
                return;
            }
        }

        String imagePath = Constant.CACHE_DIR + VideoSearch.imagePath(video);
        if (!(new File(imagePath)).exists()) {
            if (video.imageLink.isEmpty()) {
                imagePath = null;
            } else {
                try {
                    Connection.saveData(video.imageLink, imagePath, DomainType.VIDEO_INFO);
                    guiListener.setImagePath(video.imagePath = imagePath, ROW, video.ID);
                    if (isCancelled()) {
                        return;
                    }
                } catch (Exception e) {
                    if (isCancelled()) {
                        return;
                    }
                    imagePath = null;
                    guiListener.msg("Error: cannot display video's poster image." + Constant.NEWLINE + ExceptionUtil.toString(e), Constant.ERROR_MSG);
                }
            }
        }

        guiListener.summary(video.summary, imagePath);
        if (video.IS_TV_SHOW && video.summary.contains(Constant.TV_NEXT_EPISODE_HTML_AND_PLACEHOLDER)) {
            (episodeFinder = new EpisodeFinder(guiListener, ROW, video)).execute();
        }
    }

    private void browseDownloadURL(String url) throws IOException {
        startPeerBlock();
        guiListener.browserNotification("download", BROWSE_ACTION, DomainType.DOWNLOAD_LINK_INFO);
        Connection.browse(url);
    }

    private static void browseMagnetLink(Torrent torrent) throws IOException {
        Connection.browse(torrent.MAGNET_LINK, "a BitTorrent client", "magnet");
    }

    private void torrentDownloadError(Torrent torrent) {
        guiListener.error(new ConnectionException(Connection.error("downloading " + torrent.saveName(false), "", null)));
    }

    private void saveTorrent(Torrent torrent) throws Exception {
        if (strExportListener != null) {
            export = torrent.magnetLinkURL();
            return;
        }

        if (torrent.FILE == null || !torrent.FILE.exists()) {
            if (guiListener.canAutoDownload()) {
                if (searchState.blacklistedFileExts.length != 0) {
                    torrentDownloadError(torrent);
                }
                browseDownloadURL(torrent.magnetLinkURL());
            } else if (guiListener.canDownloadWithDefaultApp()) {
                if (searchState.blacklistedFileExts.length != 0) {
                    torrentDownloadError(torrent);
                }
                startPeerBlock();
                try {
                    browseMagnetLink(torrent);
                } catch (Exception e) {
                    if (searchState.blacklistedFileExts.length == 0) {
                        torrentDownloadError(torrent);
                    }
                    throw e;
                }
            } else {
                torrentDownloadError(torrent);
                startPeerBlock();
                browseMagnetLink(torrent);
            }
            return;
        }

        String torrentFilePath = IO.parentDir(torrent.FILE) + torrent.saveName(true);
        File torrentFile = new File(torrentFilePath);
        if (!torrentFile.exists()) {
            IO.write(torrent.FILE, torrentFile);
        }

        if (guiListener.canAutoDownload()) {
            browseDownloadURL(guiListener.getAutoDownloader() + URLEncoder.encode(torrentFilePath, Constant.UTF8));
        } else if (guiListener.canDownloadWithDefaultApp()) {
            startPeerBlock();
            try {
                IO.open(torrentFile);
            } catch (Exception e) {
                try {
                    browseMagnetLink(torrent);
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                } catch (Exception e2) {
                    if (Debug.DEBUG) {
                        Debug.print(e2);
                    }
                    throw e;
                }
            }
        } else {
            startPeerBlock();
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
            browseStream(Str.get(isStream2.get() ? 265 : 410) + streamLink);
        }
    }

    private void indirectStreamSearch() throws Exception {
        String link = VideoSearch.searchEngineQuery((findOldTitleStream.get() ? oldTitle : TITLE) + (video.year.isEmpty() ? "" : ' ' + video.year) + ' '
                + Str.get(isStream2.get() ? 279 : 371), isStream2.get() ? 620 : 621);
        if (isCancelled()) {
            return;
        }

        if (link == null) {
            findOldTitleStream();
            return;
        }

        link = Str.get(isStream2.get() ? 284 : 376) + link;
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

    private void findOldTitleStream() throws Exception {
        if (findOldTitleStream.get()) {
            msg(streamContent(), false);
            return;
        }

        findOldTitleStream.set(true);
        updateOldTitleAndSummary();
        if (isCancelled()) {
            return;
        }

        if (oldTitle == null) {
            msg(streamContent(), false);
        } else {
            findStream();
        }
    }

    boolean isValidateStream(String link) throws Exception {
        String source = Connection.getSourceCode(link, DomainType.VIDEO_STREAMER);
        if (isStream2.get()) {
            return isValidateStreamHelper(Regex.firstMatch(source, 285));
        } else {
            String newYear = Regex.firstMatch(Regex.match(source, 377), 368);
            if (!video.IS_TV_SHOW && newYear.isEmpty()) {
                return false;
            }
            String newTitle = Regex.match(source, 373);
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
        String source = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        updateOldTitleAndSummary();
        String[] titleParts = VideoSearch.getImdbTitleParts(source);
        String newTitle = titleParts[0];
        if (findOldTitleStream.get() && (newTitle = Regex.match(source, 174)).isEmpty()) {
            return false;
        }
        newTitle = Regex.clean(newTitle);
        String newYear = titleParts[1];

        if (!VideoSearch.isImdbVideoType(source, video.IS_TV_SHOW)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (video.IS_TV_SHOW ? "TV show" : "movie") + "): '" + newTitle + "' '" + newYear + '\'');
            }
            return false;
        }

        if (Debug.DEBUG) {
            Debug.println("Stream result: '" + newTitle + "' '" + newYear + "'");
        }
        return newTitle.equals(findOldTitleStream.get() ? oldTitle : TITLE) && newYear.equals(video.year);
    }

    private void directStreamSearch() throws Exception {
        List<String> results;
        if (isStream2.get()) {
            String source = Connection.getSourceCode(Str.get(video.IS_TV_SHOW ? 260 : 261) + URLEncoder.encode(findOldTitleStream.get() ? oldTitle : TITLE,
                    Constant.UTF8), DomainType.VIDEO_STREAMER, !PREFETCH);
            if (PREFETCH) {
                return;
            }
            results = Regex.allMatches(source, video.IS_TV_SHOW ? 262 : 263);
        } else {
            String source, key;
            if (Boolean.parseBoolean(Str.get(572))) {
                source = Connection.getSourceCode(Str.get(396), DomainType.VIDEO_STREAMER, !PREFETCH);
                key = Regex.match(source, 573);
                if (!key.isEmpty()) {
                    key = Str.get(575) + key;
                }
            } else {
                key = "";
            }

            source = Connection.getSourceCode(Str.get(399) + Str.get(video.IS_TV_SHOW ? 400 : 401) + Str.get(402) + URLEncoder.encode(findOldTitleStream.get()
                    ? oldTitle : TITLE, Constant.UTF8) + Str.get(576) + key, DomainType.VIDEO_STREAMER, !PREFETCH);
            if (PREFETCH) {
                return;
            }
            results = Regex.allMatches(source, 405);
        }

        Collection<StreamFinder> streamFinders = new ArrayList<StreamFinder>(35);
        int numResults = results.size(), maxNumSerialSearches = Integer.parseInt(Str.get(290)), maxNumResults = Integer.parseInt(Str.get(310));
        for (int i = 0; i < numResults && i < maxNumResults; i++) {
            StreamFinder streamFinder = new StreamFinder(results.get(i));
            if (i < maxNumSerialSearches) {
                if (isCancelled()) {
                    return;
                }

                RunnableUtil.runAndWaitFor(Arrays.asList(streamFinder));
                if (streamLink != null) {
                    return;
                }
            } else {
                streamFinders.add(streamFinder);
            }
        }

        RunnableUtil.runAndWaitFor(streamFinders);
    }

    private boolean tvChoices() {
        synchronized (tvChoicesLock) {
            if ((strExportListener == null || strExportListener.showTVChoices()) && (!PLAY || isDownload1)) {
                cancelTVSelection = guiListener.tvChoices(video.season, video.episode);
                if (strExportListener != null) {
                    strExportListener.setEpisode(guiListener.getSeason(), guiListener.getEpisode());
                }
                return cancelTVSelection;
            }
            return false;
        }
    }

    private void findTrailer() throws Exception {
        if (!PREFETCH && video.IS_TV_SHOW && tvChoices()) {
            return;
        }

        String seasonStr;
        if (video.IS_TV_SHOW) {
            if (!PREFETCH) {
                video.season = guiListener.getSeason();
                video.episode = guiListener.getEpisode();
            } else if (video.season.isEmpty()) {
                return;
            }
            seasonStr = (video.season.equals(Constant.ANY) ? "" : " \"season " + Integer.valueOf(video.season) + '\"');
        } else {
            seasonStr = "";
        }
        String link = getTrailerLink(seasonStr);

        if (PREFETCH || isCancelled()) {
            return;
        }

        if (link == null) {
            guiListener.msg("A trailer" + NOT_FOUND, Constant.INFO_MSG);
        } else {
            if (Debug.DEBUG) {
                Debug.println("Trailer: '" + link + '\'');
            }
            if (strExportListener != null) {
                export = link;
                return;
            }
            guiListener.browserNotification("trailer", BROWSE_ACTION, DomainType.TRAILER);
            Connection.browse(link);
        }
    }

    private String getTrailerLink(String seasonStr) throws Exception {
        String urlForm = Str.get(86);
        String urlFormOptions = URLEncoder.encode(TITLE + seasonStr + (video.IS_TV_SHOW ? "" : (' ' + video.year)) + Str.get(87), Constant.UTF8);
        String source = Connection.getSourceCode(urlForm + urlFormOptions, DomainType.TRAILER, !PREFETCH);
        if (PREFETCH || isCancelled()) {
            return null;
        }

        String noResultsStr = Regex.match(source, 88);
        if (!noResultsStr.isEmpty() && noResultsStr.contains(URLDecoder.decode(urlFormOptions, Constant.UTF8))) {
            return null;
        }

        String trailerID = Regex.match(Regex.firstMatch(source, 90), 92);
        if (trailerID.isEmpty()) {
            return null;
        }

        return Str.get(91) + URLEncoder.encode(trailerID, Constant.UTF8);
    }

    private boolean magnetLinkOnly() {
        return (PLAY && guiListener.getDownloadLinkTimeout() == 0) || strExportListener != null;
    }

    private void findAltDownloadLink() throws Exception {
        guiListener.altVideoDownloadStarted();

        String[] results = Regex.split(Connection.getSourceCode(Str.get(video.IS_TV_SHOW ? 483 : 484), DomainType.DOWNLOAD_LINK_INFO), Constant.STD_NEWLINE);
        for (int i = 0; i < results.length; i += 5) {
            if (!results[i].trim().equals(video.title) || !results[i + 1].trim().equals(video.year)) {
                continue;
            }

            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return;
            }

            Magnet magnet = new Magnet(results[i + 4].trim());
            try {
                boolean isTorrentDownloaded = magnet.download(guiListener, this, magnetLinkOnly());
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

                torrents.add(new Torrent("", magnet.MAGNET_LINK, results[i + 2].trim(), torrent, extensions, null, Integer.parseInt(results[i + 3].trim()) == 1, 0,
                        0));
            } catch (Exception e) {
                error(e);
            }
            return;
        }
    }

    private boolean findTVDownloadLink(boolean canShowTVChoices) throws Exception {
        if (canShowTVChoices) {
            if (tvChoices()) {
                return false;
            }

            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return false;
            }

            video.season = guiListener.getSeason();
            video.episode = guiListener.getEpisode();
        }

        updateOldTitleAndSummary();
        if (isCancelled()) {
            return false;
        }

        Collection<String> seasonAndEpisodes = new ArrayList<String>(3);
        if (video.season.equals(Constant.ANY) && video.episode.equals(Constant.ANY)) {
            seasonAndEpisodes.add("");
        } else if (!video.season.equals(Constant.ANY) && video.episode.equals(Constant.ANY)) {
            seasonAndEpisodes.add(" season " + Integer.valueOf(video.season));
        } else if (video.season.equals(Constant.ANY) && !video.episode.equals(Constant.ANY)) {
            Collections.addAll(seasonAndEpisodes, " e" + video.episode, " episode " + Integer.valueOf(video.episode));
        } else {
            String seasonNum = Integer.valueOf(video.season).toString();
            Collections.addAll(seasonAndEpisodes, " s" + video.season + 'e' + video.episode, ' ' + seasonNum + 'x' + video.episode);
            boolean isSeasonZeroPadded = !seasonNum.equals(video.season);
            if (isSeasonZeroPadded) {
                seasonAndEpisodes.add(' ' + video.season + 'x' + video.episode);
            }
            String episodeNum = Integer.valueOf(video.episode).toString();
            if (!episodeNum.equals(video.episode)) {
                seasonAndEpisodes.add(' ' + seasonNum + 'x' + episodeNum);
                if (isSeasonZeroPadded) {
                    seasonAndEpisodes.add(' ' + video.season + 'x' + episodeNum);
                }
            }
        }
        addTVLinks(seasonAndEpisodes);

        if (!PREFETCH) {
            RunnableUtil.runAndWaitFor(torrentFinders);
        }
        return true;
    }

    private void findMovieDownloadLink() throws Exception {
        if (!PREFETCH) {
            guiListener.enableLinkProgress(true);
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return;
            }
        }

        updateOldTitleAndSummary();
        if (isCancelled()) {
            return;
        }

        addMovieLinks(video.title, false);
        if (!video.oldTitle.isEmpty()) {
            addMovieLinks(video.oldTitle, true);
        }

        if ((Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(video.year)) > Integer.parseInt(Str.get(337))) {
            outer:
            for (List<BoxSetVideo> boxSet : BoxSetVideo.movieBoxSets) {
                int numTitles = boxSet.size();
                for (int i = 1; i < numTitles; i++) {
                    BoxSetVideo boxSetVideo = boxSet.get(i);
                    if (boxSetVideo.isSameTitle(video.title, video.year)) {
                        if (Debug.DEBUG) {
                            Debug.println("title belongs to movie box set");
                        }
                        addBoxSetLinks(boxSet, false);
                        break outer;
                    } else if (!video.oldTitle.isEmpty() && boxSetVideo.isSameTitle(video.oldTitle, video.year)) {
                        if (Debug.DEBUG) {
                            Debug.println("ORIGINAL title belongs to movie box set");
                        }
                        addBoxSetLinks(boxSet, true);
                        break outer;
                    }
                }
            }
        }

        if (!PREFETCH) {
            RunnableUtil.runAndWaitFor(torrentFinders);
        }
    }

    private void addTVLinks(Iterable<String> seasonAndEpisodes) {
        for (String seasonAndEpisode : seasonAndEpisodes) {
            addFinder(video.title, seasonAndEpisode, true, false, false);
            addFinder(video.title, seasonAndEpisode, false, false, false);
        }
        if (!video.oldTitle.isEmpty()) {
            for (String seasonAndEpisode : seasonAndEpisodes) {
                addFinder(video.oldTitle, seasonAndEpisode, true, true, false);
                addFinder(video.oldTitle, seasonAndEpisode, false, true, false);
            }
        }
    }

    private void addMovieLinks(String dirtyMovieTitle, boolean isOldTitle) {
        String dirtyMovieTitlePrefix = VideoSearch.getMovieTitlePrefix(dirtyMovieTitle);
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
        Video vid = new Video("", dirtyTitle, video.year, video.IS_TV_SHOW, video.IS_TV_SHOW_AND_MOVIE);
        vid.season = video.season;
        vid.episode = video.episode;
        torrentFinders.add(new TorrentFinder(guiListener, torrents, vid, seasonAndEpisode, isDownload1, magnetLinkOnly(), ignoreYear, isOldTitle, isTitlePrefix,
                new TorrentSearchState(searchState), boxSet));
    }

    private void updateOldTitleAndSummaryHelper() throws Exception {
        String sourceCode = Connection.getSourceCode(VideoSearch.url(video), DomainType.VIDEO_INFO, !PREFETCH);
        video.oldTitle = VideoSearch.getOldTitle(sourceCode);
        if (!video.oldTitle.isEmpty()) {
            oldTitle = Regex.clean(video.oldTitle);

            if (PREFETCH) {
                return;
            }

            String displayTitle = guiListener.getTitle(ROW, video.ID);
            if (displayTitle != null) {
                int beginIndex = 6, endOffSet = 7;
                String startHtml = "<html>", endHtml = "</html>";
                if (displayTitle.startsWith("<b>", beginIndex)) {
                    beginIndex += 3;
                    endOffSet += 4;
                    startHtml += "<b>";
                    endHtml = "</b>" + endHtml;
                }
                displayTitle = displayTitle.substring(beginIndex, displayTitle.length() - endOffSet);
                String extraTitleInfo = Regex.firstMatch(displayTitle, Constant.TV_EPISODE_REGEX);
                displayTitle = Regex.replaceFirst(displayTitle, Constant.TV_EPISODE_REGEX, "") + Constant.aka(video.oldTitle) + (extraTitleInfo.isEmpty() ? ""
                        : ' ' + extraTitleInfo);
                guiListener.setTitle(startHtml + displayTitle + endHtml, ROW, video.ID);
            }
        }

        if (PREFETCH) {
            return;
        }

        guiListener.setSummary(video.summary = VideoSearch.getSummary(sourceCode, video.IS_TV_SHOW), ROW, video.ID);
        guiListener.setImageLink(video.imageLink = Regex.match(sourceCode, 188), ROW, video.ID);
    }

    private void updateOldTitleAndSummary() throws Exception {
        if (video.summary.isEmpty()) {
            updateOldTitleAndSummaryHelper();
        } else if (oldTitle == null && !video.oldTitle.isEmpty()) {
            oldTitle = Regex.clean(video.oldTitle);
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
                error(e);
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
            if (isStream2.get()) {
                String newTitle = Regex.match(result, 276);
                String newYear = Regex.firstMatch(result, 264);
                if (!newYear.isEmpty()) {
                    newYear = newYear.substring(0, 4);
                }
                if (!Regex.isMatch(newYear, 289) && isValidateStream(newTitle, newYear)) {
                    return Regex.match(result, 287);
                }
            } else {
                String[] titleParts = VideoSearch.getImdbTitleParts(result, 406);
                if (isValidateStream(titleParts[0], titleParts[1])) {
                    return Regex.match(result, 408);
                }
            }
            return "";
        }
    }

    public static String getComments() {
        return commentsFinder == null || commentsFinder.comments == null ? CommentsFinder.NO_COMMENTS : commentsFinder.comments;
    }
}

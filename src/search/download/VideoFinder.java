package search.download;

import debug.Debug;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
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
import util.AbstractWorker;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.Regex;
import util.Task;
import util.ThrowableUtil;
import util.Worker;

public class VideoFinder extends Worker {

    final GuiListener guiListener;
    public final ContentType CONTENT_TYPE;
    private ContentType currContentType;
    public final int ROW;
    private final VideoStrExportListener strExportListener;
    final Video video;
    public final boolean PREFETCH;
    private boolean isDownload1, play, startPeerblock = true, foreground;
    private String export;
    private static volatile Worker episodeFinder;
    private Collection<Torrent> torrents;
    private Collection<TorrentFinder> torrentFinders;
    private TorrentSearchState searchState;
    private static volatile CommentsFinder commentsFinder;
    private static final Object TV_CHOICES_LOCK = new Object(), DOWNLOAD_LOCK = new Object(), VIDEO_LOCK = new Object();
    private final Runnable rerunner;

    public VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener) {
        this(guiListener, contentType, row, video, strExportListener, false, null);
    }

    public VideoFinder(ContentType contentType, VideoFinder finder) {
        this(finder.guiListener, contentType, finder.ROW, finder.video, null, true, null);
    }

    public VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener, boolean prefetch,
            Runnable rerunner) {
        this.guiListener = guiListener;
        CONTENT_TYPE = contentType;
        ROW = row;
        this.video = video;
        this.strExportListener = strExportListener;
        PREFETCH = prefetch;
        this.rerunner = rerunner;
        foreground = (row != -1);
        if (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD2) {
            torrents = new CopyOnWriteArrayList<Torrent>();
            torrentFinders = new ArrayList<TorrentFinder>(12);
        } else if (contentType == ContentType.DOWNLOAD3) {
            torrents = new ArrayList<Torrent>(1);
        }
    }

    @Override
    protected void doWork() {
        if (foreground) {
            guiListener.loading(true);
        }
        search(CONTENT_TYPE);

        boolean isCancelled = false;
        ContentType export2ContentType = null;
        String export1 = export;

        if (strExportListener != null) {
            isCancelled = isCancelled();
            if (strExportListener.exportSecondaryContent()) {
                if (CONTENT_TYPE == ContentType.DOWNLOAD1 || CONTENT_TYPE == ContentType.DOWNLOAD3) {
                    export = null;
                    export2ContentType = ContentType.DOWNLOAD2;
                    if (!Connection.downloadLinkInfoFail()) {
                        resetDownloadVideoFinder();
                        search(export2ContentType);
                    }
                }
            }
        }

        done();
        if (foreground) {
            guiListener.loading(false);
        }

        if (strExportListener != null) {
            strExportListener.export(CONTENT_TYPE, export1, isCancelled, guiListener);
            if (export2ContentType != null) {
                strExportListener.export(export2ContentType, export, isCancelled(), guiListener);
            }
        }
    }

    private void resetDownloadVideoFinder() {
        torrents.clear();
        if (torrentFinders == null) {
            torrentFinders = new ArrayList<TorrentFinder>(12);
        } else {
            torrentFinders.clear();
        }
        startPeerblock = false;
    }

    public void prefetch() throws Exception {
        if (CONTENT_TYPE == ContentType.SUMMARY) {
        } else if (CONTENT_TYPE == ContentType.TRAILER) {
            findTrailer();
        } else {
            searchState = new TorrentSearchState(guiListener);
            isDownload1 = (CONTENT_TYPE == ContentType.DOWNLOAD1 || CONTENT_TYPE == ContentType.DOWNLOAD3);

            if (video.IS_TV_SHOW) {
                if (video.season.isEmpty() || video.episode.isEmpty()) {
                    return;
                }
                findTVDownloadLink(false);
            } else {
                findMovieDownloadLink();
            }
        }
    }

    private void startPeerBlock() {
        if (foreground) {
            guiListener.enable(false, null, true, currContentType);
        }
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

    private void error(Exception e) {
        if (!isCancelled()) {
            guiListener.error(e);
        }
    }

    private void search(ContentType contentType) {
        currContentType = contentType;
        if (currContentType == ContentType.SUMMARY) {
            guiListener.enable(true, false, false, currContentType);
            try {
                findSummary();
            } catch (Exception e) {
                error(e);
            }
            guiListener.enable(currContentType);
        } else if (currContentType == ContentType.TRAILER) {
            guiListener.enable(true, false, false, currContentType);
            try {
                findTrailer();
            } catch (Exception e) {
                error(e);
            }
            guiListener.enable(currContentType);
        } else {
            isDownload1 = (currContentType == ContentType.DOWNLOAD1 || currContentType == ContentType.DOWNLOAD3);
            play = (strExportListener == null && guiListener.canDownloadWithPlaylist());
            searchState = new TorrentSearchState(guiListener);
            if (foreground) {
                guiListener.enable(true, false, false, currContentType);
            }

            try {
                if (currContentType == ContentType.DOWNLOAD3) {
                    findAltDownloadLink();
                } else if (video.IS_TV_SHOW) {
                    if (!findTVDownloadLink(true)) {
                        if (foreground) {
                            guiListener.enable(currContentType);
                        }
                        return;
                    }
                } else {
                    findMovieDownloadLink();
                }

                if (isCancelled()) {
                    if (foreground) {
                        guiListener.enable(currContentType);
                    }
                    return;
                }

                if (currContentType != ContentType.DOWNLOAD3) {
                    if (video.IS_TV_SHOW && torrents.isEmpty() && !video.episode.isEmpty() && !Constant.ANY.equals(video.episode)
                            && String.format(Constant.TV_EPISODE_FORMAT, 1).equals(video.season) && !Connection.downloadLinkInfoFail()) { // Assumes TV show with
                        // season 1 just released and download listed with 'episode #' in name; worst case is false positive of newer season with desired episode
                        torrentFinders.clear();
                        String prevSeason = video.season;
                        try {
                            video.season = Constant.ANY;
                            if (!findTVDownloadLink(false) || isCancelled()) {
                                if (foreground) {
                                    guiListener.enable(currContentType);
                                }
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
                    if (foreground) {
                        guiListener.enable(currContentType);
                    }
                    return;
                }

                Torrent torrent = null;
                if (!torrents.isEmpty()) {
                    torrent = Collections.min(torrents);
                    if (Debug.DEBUG) {
                        Debug.println(torrents);
                    }
                }

                synchronized (DOWNLOAD_LOCK) {
                    saveTorrent(torrent);
                }
            } catch (Exception e) {
                error(e);
            }

            if (foreground) {
                guiListener.enable(currContentType);
            }
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

        updateOldTitleAndSummary();
        if (isCancelled()) {
            return;
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
                    guiListener.error(new ConnectionException(Str.str("posterDisplayError") + Constant.STD_NEWLINE2 + ThrowableUtil.toString(e)));
                }
            }
        }

        guiListener.summary(video.summary, imagePath);
        if (video.IS_TV_SHOW && video.summary.contains("</b>" + Constant.TV_EPISODE_PLACEHOLDER)) {
            (episodeFinder = new EpisodeFinder(guiListener, ROW, video)).execute();
        }
    }

    private void browseDownloadURL(String url) throws IOException {
        startPeerBlock();
        guiListener.browserNotification(DomainType.DOWNLOAD_LINK_INFO);
        Connection.browse(url);
    }

    private static void browseMagnetLink(Torrent torrent) throws IOException {
        Connection.browse(torrent.MAGNET_LINK, "bitTorrentClient");
    }

    private void torrentDownloadError(Torrent torrent) {
        error(new ConnectionException(Str.str("downloadingProblem", torrent.name())));
    }

    private void saveTorrent(Torrent torrent) throws Exception {
        if (torrent == null) {
            if (foreground) {
                guiListener.enable(false, null, true, currContentType);
            }
            String settings = searchState.toString();
            guiListener.msg(Str.str(("download" + (strExportListener == null || !strExportListener.exportSecondaryContent() ? "" : (isDownload1 ? "1" : "2")))
                    + "LinkNotFound") + Constant.STD_NEWLINE2 + Str.str("settings", " " + (settings.isEmpty() ? VideoSearch.describe(video) : VideoSearch.describe(
                                            video) + ',' + settings)), Constant.INFO_MSG);
            addVideoToPlaylist();
        } else {
            if (Debug.DEBUG) {
                Debug.println("Selected torrent: " + torrent);
            }
            boolean orderByLeechers = (currContentType == ContentType.DOWNLOAD1);
            TorrentFinder.saveOrdering(torrent.ID, video.season, video.episode, orderByLeechers);
            if (String.format(Constant.TV_EPISODE_FORMAT, 1).equals(video.season)) {
                TorrentFinder.saveOrdering(torrent.ID, Constant.ANY, video.episode, orderByLeechers);
            }

            if (torrent.IS_SAFE || !guiListener.canShowSafetyWarning()) {
                saveTorrentHelper(torrent);
            } else if (torrent.ID.isEmpty()) {
                if (guiListener.canProceedWithUnsafeDownload(torrent.name())) {
                    saveTorrentHelper(torrent);
                } else {
                    addVideoToPlaylist();
                }
            } else {
                String torrentSaveName = torrent.name();
                guiListener.initSafetyDialog(torrentSaveName);

                if (commentsFinder != null) {
                    commentsFinder.cancel(true);
                }

                commentsFinder = new CommentsFinder(guiListener, torrent.COMMENTS_LINK, torrentSaveName);
                commentsFinder.execute();

                guiListener.showSafetyDialog();

                commentsFinder.cancel(true);

                if (guiListener.canProceedWithUnsafeDownload()) {
                    saveTorrentHelper(torrent);
                } else {
                    addVideoToPlaylist();
                }
            }
        }
    }

    private void saveTorrentHelper(Torrent torrent) throws Exception {
        if (strExportListener != null) {
            export = torrent.magnetLinkURL();
            return;
        }

        if (!guiListener.unbanDownload(Str.hashCode(torrent.MAGNET_LINK), torrent.name())) {
            addVideoToPlaylist();
            return;
        }

        if (torrent.FILE == null || !torrent.FILE.exists()) {
            if (guiListener.getWebBrowserAppDownloader() != null) {
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

        String torrentFilePath = IO.parentDir(torrent.FILE) + torrent.fileName();
        File torrentFile = new File(torrentFilePath);
        if (!torrentFile.exists()) {
            IO.write(torrent.FILE, torrentFile);
        }

        String webBrowserAppDownloader = guiListener.getWebBrowserAppDownloader();
        if (webBrowserAppDownloader != null) {
            browseDownloadURL(webBrowserAppDownloader + URLEncoder.encode(torrentFilePath, Constant.UTF8));
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

    private void addVideoToPlaylist() {
    }

    private boolean tvChoices() {
        synchronized (TV_CHOICES_LOCK) {
            if (strExportListener == null || strExportListener.showTVChoices()) {
                boolean cancelTVSelection = guiListener.tvChoices(video.season, video.episode);
                if (strExportListener != null) {
                    strExportListener.setEpisode(guiListener.getSeason(), guiListener.getEpisode());
                }
                return cancelTVSelection;
            }
            return false;
        }
    }

    private void findTrailer() throws Exception {
        if (!PREFETCH && video.IS_TV_SHOW && rerunner != null && tvChoices()) {
            return;
        }

        Integer season = null;
        if (video.IS_TV_SHOW) {
            if (!PREFETCH) {
                video.season = guiListener.getSeason();
                video.episode = guiListener.getEpisode();
            } else if (video.season.isEmpty()) {
                return;
            }
            if (!video.season.equals(Constant.ANY)) {
                season = Integer.valueOf(video.season);
            }
        }

        String[] link1 = null;
        try {
            link1 = getTrailerLink1(season, true);
            if (PREFETCH || isCancelled()) {
                return;
            }
        } catch (Exception e) {
            if (PREFETCH || isCancelled()) {
                throw e;
            }
            error(e);
        }

        final String[] link;
        if (link1 == null) {
            String[] link2 = null;
            try {
                link2 = getTrailerLink2(season);
                if (isCancelled()) {
                    return;
                }
            } catch (Exception e) {
                error(e);
            }
            link = link2;
        } else {
            link = link1;
        }

        if (link == null) {
            guiListener.msg(Str.str("trailerNotFound"), Constant.INFO_MSG);
        } else {
            if (Debug.DEBUG) {
                Debug.println("Trailer: '" + Arrays.toString(link) + '\'');
            }
            if (strExportListener != null) {
                export = link[0];
                return;
            }

            final Runnable browseLink = new Runnable() {
                @Override
                public void run() {
                    try {
                        guiListener.browserNotification(DomainType.TRAILER);
                        Connection.browse(link[0]);
                    } catch (Exception e) {
                        error(e);
                    }
                }
            };

            if (rerunner == null && link1 != null) {
                String[] link2 = null;
                try {
                    link2 = getTrailerLink2(season);
                } catch (Exception e) {
                    error(e);
                }
                if (isCancelled()) {
                    return;
                }
                if (link2 == null) {
                    browseLink.run();
                } else {
                    openTrailerLink(link2, browseLink, browseLink);
                }
            } else {
                openTrailerLink(link, link1 == null ? browseLink : rerunner, browseLink);
            }
        }
    }

    private String[] getTrailerLink1(Integer season, boolean canRetry) throws Exception {
        String urlFormOptions = URLEncoder.encode(Regex.clean(video.title) + (season == null ? "" : " \"season " + season + "\"") + (video.IS_TV_SHOW ? "" : ' '
                + video.year) + Str.get(87), Constant.UTF8), url = Str.get(86) + urlFormOptions;
        String source = Connection.getSourceCode(url, DomainType.TRAILER, !PREFETCH);
        if (PREFETCH || isCancelled()) {
            return null;
        }

        String noResults = Regex.match(source, 88), link, trailerID;
        if ((!noResults.isEmpty() && noResults.contains(URLDecoder.decode(urlFormOptions, Constant.UTF8))) || (trailerID = Regex.match(link = Regex.firstMatch(
                source, 90), 92)).isEmpty()) {
            Connection.removeFromCache(url);
            if (canRetry) {
                Thread.sleep(1000);
                return getTrailerLink1(season, false);
            }
            return null;
        }

        return new String[]{Str.get(91) + URLEncoder.encode(trailerID, Constant.UTF8), Regex.firstMatch(link, 740)};
    }

    private String[] getTrailerLink2(Integer season) throws Exception {
        String sourceCode = Connection.getSourceCode(season == null ? String.format(Str.get(748), video.ID) : String.format(Str.get(749), video.ID, season),
                DomainType.VIDEO_INFO, !PREFETCH);
        if (isCancelled()) {
            return null;
        }

        for (String videoId : Regex.matches(sourceCode, 750)) {
            sourceCode = Connection.getSourceCode(String.format(Str.get(752), videoId), DomainType.VIDEO_INFO, !PREFETCH);
            if (isCancelled()) {
                return null;
            }

            List<String> videoInfos = Regex.matches(sourceCode, 753);
            if (videoInfos.isEmpty()) {
                sourceCode = Connection.getSourceCode(String.format(Str.get(755), videoId), DomainType.VIDEO_INFO, !PREFETCH);
                if (isCancelled()) {
                    return null;
                }

                videoInfos = Regex.matches(sourceCode, 753);
            }

            for (String videoInfo : videoInfos) {
                for (String videoInfoContent : Regex.matches(videoInfo, 756)) {
                    String link = Regex.match(videoInfoContent, 758);
                    if (!link.isEmpty() && !Regex.firstMatch(videoInfoContent, 760).isEmpty()) {
                        return new String[]{link, video.title + " (" + video.year + ')' + (season == null ? "" : " Season " + season)};
                    }
                }
            }
        }

        return null;
    }

    private void openTrailerLink(String[] link, Runnable browseLink1, Runnable browseLink2) {
        int player = guiListener.getTrailerPlayer();
        if (player == 6 || !MediaPlayer.open(738, link[0], player == 5 ? 240 : (player == 4 ? 360 : (player == 3 ? 480 : (player == 2 ? 720 : (player == 1 ? 1080
                : -1)))), Regex.htmlToPlainText(link[1]), browseLink1)) {
            browseLink2.run();
        }
    }

    private boolean magnetLinkOnly() {
        return (play && guiListener.getDownloadLinkTimeout() == 0) || strExportListener != null;
    }

    private void findAltDownloadLink() throws Exception {
        if (!foreground) {
            return;
        }

        guiListener.altVideoDownloadStarted();

        String sourceCode;
        try {
            sourceCode = Connection.getSourceCode(Str.get(video.IS_TV_SHOW ? 483 : 484), DomainType.DOWNLOAD_LINK_INFO);
        } catch (Exception e) {
            error(e);
            findAlt2DownloadLink(true);
            return;
        }

        String[] results = Regex.split(sourceCode, Constant.STD_NEWLINE);
        for (int i = 0; i < results.length; i += 5) {
            if (!results[i].trim().equals(video.title) || !results[i + 1].trim().equals(video.year)) {
                continue;
            }

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
                return;
            } catch (Exception e) {
                error(e);
            }
            break;
        }

        findAlt2DownloadLink(true);
    }

    private void findAlt2DownloadLink(boolean singleOrderByMode) throws Exception {
        if (!isCancelled() && torrents.isEmpty()) {
            torrents = new CopyOnWriteArrayList<Torrent>();
            torrentFinders = new ArrayList<TorrentFinder>(1);
            Magnet.waitForAzureusToStart();
            addFinder(video.title, "", false, false, false, null, true, singleOrderByMode);
            AbstractWorker.executeAndWaitFor(torrentFinders);
        }
    }

    private boolean findTVDownloadLink(boolean canShowTVChoices) throws Exception {
        if (canShowTVChoices) {
            if (foreground && tvChoices()) {
                return false;
            }

            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return false;
            }

            if (foreground) {
                video.season = guiListener.getSeason();
                video.episode = guiListener.getEpisode();
            }
        }

        updateOldTitleAndSummary();
        if (isCancelled()) {
            return false;
        }

        Collection<String> seasonAndEpisodes = new ArrayList<String>(5);
        if (video.season.equals(Constant.ANY) && video.episode.equals(Constant.ANY)) {
            seasonAndEpisodes.add("");
        } else if (!video.season.equals(Constant.ANY) && video.episode.equals(Constant.ANY)) {
            seasonAndEpisodes.add(" season " + Integer.valueOf(video.season));
        } else if (video.season.equals(Constant.ANY) && !video.episode.equals(Constant.ANY)) {
            Integer episode = Integer.valueOf(video.episode);
            String episodeNum = episode.toString();
            Collections.addAll(seasonAndEpisodes, " e" + video.episode, " episode " + episodeNum, ' ' + String.format("%03d", episode));
            if (!episodeNum.equals(video.episode)) {
                seasonAndEpisodes.add(' ' + video.episode);
            }
            seasonAndEpisodes.add(' ' + episodeNum);
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
        addTVFinders(seasonAndEpisodes);

        findDownloadLink();
        return true;
    }

    private void findMovieDownloadLink() throws Exception {
        if (!PREFETCH) {
            Magnet.waitForAzureusToStart();
            if (isCancelled()) {
                return;
            }
        }

        updateOldTitleAndSummary();
        if (isCancelled()) {
            return;
        }

        addMovieFinders(video.title, false);
        if (!video.oldTitle.isEmpty()) {
            addMovieFinders(video.oldTitle, true);
        }

        if ((Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(video.year)) > Integer.parseInt(Str.get(337))) {
            BoxSetVideo.initMovieBoxSets();
            outer:
            for (List<BoxSetVideo> boxSet : BoxSetVideo.movieBoxSets) {
                int numTitles = boxSet.size();
                for (int i = 1; i < numTitles; i++) {
                    BoxSetVideo boxSetVideo = boxSet.get(i);
                    if (boxSetVideo.isSameTitle(video.title, video.year)) {
                        if (Debug.DEBUG) {
                            Debug.println("title belongs to movie box set");
                        }
                        addBoxSetFinders(boxSet, false);
                        break outer;
                    } else if (!video.oldTitle.isEmpty() && boxSetVideo.isSameTitle(video.oldTitle, video.year)) {
                        if (Debug.DEBUG) {
                            Debug.println("ORIGINAL title belongs to movie box set");
                        }
                        addBoxSetFinders(boxSet, true);
                        break outer;
                    }
                }
            }
        }

        findDownloadLink();
    }

    private void addTVFinders(Iterable<String> seasonAndEpisodes) {
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

    private void addMovieFinders(String title, boolean isOldTitle) {
        String titlePrefix = VideoSearch.getTitlePrefix(title);
        if (titlePrefix != null) {
            addFinder(titlePrefix, "", false, isOldTitle, true);
            addFinder(titlePrefix, "", true, isOldTitle, true);
        }
        addFinder(title, "", false, isOldTitle, false);
        addFinder(title, "", true, isOldTitle, false);
    }

    private void addBoxSetFinders(List<BoxSetVideo> boxSet, boolean isOldTitle) {
        for (String title : BoxSetVideo.getSearchTitles(boxSet)) {
            if (title == null) {
                continue;
            }
            if (Debug.DEBUG) {
                Debug.println('\'' + title + "' added to download links to query for");
            }
            addFinder(title, "", true, isOldTitle, false, boxSet, false, false);
        }
    }

    private void addFinder(String title, String seasonAndEpisode, boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix) {
        addFinder(title, seasonAndEpisode, ignoreYear, isOldTitle, isTitlePrefix, null, false, false);
    }

    private void addFinder(String title, String seasonAndEpisode, boolean ignoreYear, boolean isOldTitle, boolean isTitlePrefix, List<BoxSetVideo> boxSet,
            boolean altSearch, boolean singleOrderByMode) {
        Video vid = new Video(video.ID, title, video.year, video.IS_TV_SHOW, video.IS_TV_SHOW_AND_MOVIE);
        vid.season = video.season;
        vid.episode = video.episode;
        torrentFinders.add(new TorrentFinder(guiListener, torrentFinders, torrents, vid, seasonAndEpisode, isDownload1, magnetLinkOnly(), ignoreYear, isOldTitle,
                isTitlePrefix, new TorrentSearchState(searchState), boxSet, altSearch, singleOrderByMode));
    }

    private void findDownloadLink() throws Exception {
        Connection.runDownloadLinkInfoDeproxier(new Task() {
            @Override
            public void run() throws Exception {
                if (PREFETCH) {
                    for (TorrentFinder finder : torrentFinders) {
                        finder.getTorrents(true, true);
                    }
                    return;
                }

                boolean isDownloadLinkInfoDeproxied = Connection.isDownloadLinkInfoDeproxied();
                try {
                    if (foreground) {
                        AbstractWorker.executeAndWaitFor(torrentFinders);
                    } else {
                        for (Worker torrentFinder : torrentFinders) {
                            AbstractWorker.executeAndWaitFor(Arrays.asList(torrentFinder));
                        }
                    }
                } catch (CancellationException e) {
                    if (Debug.DEBUG) {
                        Debug.println(e);
                    }
                }
                if (isCancelled()) {
                    return;
                }

                if (!isDownloadLinkInfoDeproxied && Connection.isDownloadLinkInfoDeproxied()) {
                    Collection<TorrentFinder> newTorrentFinders = new ArrayList<TorrentFinder>(torrentFinders.size());
                    for (TorrentFinder torrentFinder : torrentFinders) {
                        newTorrentFinders.add(new TorrentFinder(torrentFinder, newTorrentFinders));
                    }
                    if (foreground) {
                        AbstractWorker.executeAndWaitFor(newTorrentFinders);
                    } else {
                        for (Worker torrentFinder : newTorrentFinders) {
                            AbstractWorker.executeAndWaitFor(Arrays.asList(torrentFinder));
                        }
                    }
                }
            }
        });
        if (!PREFETCH) {
            findAlt2DownloadLink(false);
        }
    }

    private void updateOldTitleAndSummary() throws Exception {
        synchronized (VIDEO_LOCK) {
            if (!video.summary.isEmpty() || !foreground) {
                return;
            }

            String sourceCode = Connection.getSourceCode(VideoSearch.url(video), DomainType.VIDEO_INFO, !PREFETCH);
            video.oldTitle = VideoSearch.getOldTitle(sourceCode);
            if (!video.oldTitle.isEmpty()) {
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
                    String popularEpisode = VideoSearch.popularEpisode("", "");
                    String extraTitleInfo = Regex.firstMatch(displayTitle, popularEpisode);
                    displayTitle = Regex.replaceFirst(displayTitle, popularEpisode, "") + VideoSearch.aka(video.oldTitle) + (extraTitleInfo.isEmpty() ? "" : ' '
                            + extraTitleInfo);
                    guiListener.setTitle(startHtml + displayTitle + endHtml, ROW, video.ID);
                }
            }

            String imageLink = Regex.match(sourceCode, 188);
            guiListener.setImageLink(imageLink, ROW, video.ID);
            video.imageLink = imageLink;
            String summary = VideoSearch.getSummary(sourceCode, video.IS_TV_SHOW);
            guiListener.setSummary(summary, ROW, video.ID);
            video.summary = summary;
        }
    }

    public static String getComments() {
        return commentsFinder == null || commentsFinder.comments == null ? Str.str("noComments") + Constant.STD_NEWLINE2 : commentsFinder.comments;
    }
}

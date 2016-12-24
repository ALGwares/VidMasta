package listener;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Future;

public interface WorkerListener {

    void regularSearchStarted(int numResultsPerSearch, Boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres, String[] languages,
            String[] countries, String minRating);

    void searchStopped(boolean isRegularSearcher);

    void summarySearchStopped();

    void trailerSearchStopped();

    void torrentSearchStopped();

    void loadMoreSearchResults(boolean isRegularSearcher);

    void popularSearchStarted(int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed, boolean startAsap);

    boolean isSummarySearchDone();

    boolean isTrailerSearchDone();

    boolean isTorrentSearchDone();

    boolean areWorkersDone();

    String getSafetyComments();

    void summarySearchStarted(int row, Video video, VideoStrExportListener strExportListener);

    void trailerSearchStarted(int row, Video video, VideoStrExportListener strExportListener);

    void torrentSearchStarted(ContentType contentType, int row, Video video, VideoStrExportListener strExportListener);

    Future<?> torrentSearchStarted(Video video);

    void proxyListDownloadStarted();

    void subtitleSearchStarted(String format, String languageID, Video video, boolean firstMatch, VideoStrExportListener strExportListener);

    void subtitleSearchStopped();

    void summaryReadStarted(String summary);

    void summaryReadStopped();

    void updateStarted(boolean isStartUp);

    void portChanged(int port);

    boolean canFilterIpsWithoutBlocking();

    void initPlaylist() throws Exception;

    void stream(String magnetLink, String name);

    void reloadGroup(PlaylistItem playlistItem);

    FormattedNum playlistItemSize(long size);

    FormattedNum playlistItemProgress(double progress);

    PlaylistItem playlistItem(String groupID, String uri, File groupFile, int groupIndex, String name, boolean isFirstVersion);

    void changeLocale(Locale locale);

    void license(String activationCode);

    void licenseActivated();

    boolean isLicensePresent();
}

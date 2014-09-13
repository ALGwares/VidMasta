package listener;

import java.util.Calendar;

public interface WorkerListener {

    void regularSearchStarted(int numResultsPerSearch, boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres, String[] languages,
            String[] countries, String minRating);

    void searchStopped(boolean isRegularSearcher);

    void summarySearchStopped();

    void trailerSearchStopped();

    void torrentSearchStopped();

    void streamSearchStopped();

    void loadMoreSearchResults(boolean isRegularSearcher);

    void popularSearchStarted(int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed, boolean startAsap);

    boolean isSummarySearchDone();

    boolean isTrailerSearchDone();

    boolean isTorrentSearchDone();

    boolean isStreamSearchDone();

    boolean areWorkersDone();

    boolean isLinkProgressDone();

    String getSafetyComments();

    void summarySearchStarted(int row, Video video, VideoStrExportListener strExportListener);

    void trailerSearchStarted(int row, Video video, VideoStrExportListener strExportListener);

    void torrentSearchStarted(ContentType contentType, int row, Video video, VideoStrExportListener strExportListener);

    void streamSearchStarted(ContentType contentType, int row, Video video, VideoStrExportListener strExportListener);

    void proxyListDownloadStarted();

    void subtitleSearchStarted(String format, String languageID, Video video, boolean firstMatch, VideoStrExportListener strExportListener);

    void subtitleSearchStopped();

    void summaryReadStarted(String summary);

    void summaryReadStopped();

    void updateStarted(boolean isStartUp);

    void portChanged(int port);
}

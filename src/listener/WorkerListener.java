package listener;

import java.util.Calendar;
import java.util.EventListener;

public interface WorkerListener extends EventListener {

    void regularSearchStarted(int numResultsPerSearch, boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres, String[] languages,
            String[] countries, String minRating);

    void searchStopped(boolean isRegularSearcher);

    void searchStarted();

    void torrentAndStreamSearchStopped();

    void loadMoreSearchResults(boolean isRegularSearcher);

    void popularSearchStarted(int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed, boolean startAsap);

    boolean isSummarySearchDone();

    boolean isTrailerSearchDone();

    boolean isTorrentSearchDone();

    boolean isStreamSearchDone();

    boolean areWorkersDone();

    boolean isLinkProgressDone();

    String getSafetyComments();

    void summarySearchStarted(int action, String titleID, String title, String summaryLink, String imageLink, boolean isLink, String year, boolean isTVShow,
            String season, String episode, int row);

    void trailerSearchStarted(int action, String title, String summaryLink, boolean isLink, String year, boolean isTVShow, String season, String episode, int row);

    void torrentSearchStarted(int action, String title, String summaryLink, boolean isLink, String year, boolean isTVShow, String season, String episode, int row);

    void streamSearchStarted(int action, String title, String summaryLink, boolean isLink, String year, boolean isTVShow, String season, String episode, int row);

    void proxyListDownloadStarted();

    void subtitleSearchStarted(String format, String languageID, String titleID, String title, String year, String season, String episode, boolean firstMatch);

    void subtitleSearchStopped();

    void summaryReadStarted(String summary);

    void summaryReadStopped();

    void updateStarted(boolean isStartUp);
}

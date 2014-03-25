package listener;

import java.io.File;
import javax.swing.text.Element;

public interface GuiListener {

    void loading(boolean isLoading);

    void error(Exception e);

    void readSummaryStarted();

    void readSummaryStopped();

    void watchTrailerStarted();

    void watchTrailerStopped();

    void enableDownload(boolean enable);

    void enableWatch(boolean enable);

    void enableLinkProgress(boolean enable);

    void videoDownloadStopped();

    void videoWatchStopped();

    void altVideoDownloadStarted();

    void msg(String msg, int msgType);

    void initSafetyDialog(String name);

    void safetyDialogMsg(String statistic, String link, String name);

    void showSafetyDialog();

    boolean canProceedWithUnsafeDownload();

    boolean canProceedWithUnsafeDownload(String name);

    void summary(String summary, String imagePath);

    Element getSummaryElement(String id);

    void insertAfterSummaryElement(Element element, String text);

    void browserNotification(String item, String action, int type);

    void startPeerBlock();

    void saveTorrent(File torrentFile);

    void saveSubtitle(String saveFileName, File subtitleFile);

    boolean tvChoices(String season, String episode);

    String getDisplayTitle(int row, String titleID);

    void setDisplayTitle(Object val, int row, String titleID);

    void setDisplaySummary(Object val, int row, String titleID);

    String getSeason();

    String getEpisode();

    void searchStarted();

    void newSearch(int maxProgress);

    boolean oldSearch(int maxProgress);

    void searchStopped();

    void searchProgressMaxOut();

    void moreResults(boolean areMoreResults);

    void newResult(Object[] result);

    void newResults(Iterable<Object[]> results);

    void searchNumResultsUpdate(int numResults);

    void searchProgressIncrement();

    boolean isConfirmed(String msg);

    boolean isAuthorizationConfirmed(String msg);

    String getAuthorizationUsername();

    char[] getAuthorizationPassword();

    void proxyListDownloadStarted();

    void proxyListDownloadStopped();

    void proxyListDownloadError(Exception e);

    void proxyListDownloadMsg(String msg, int msgType);

    boolean proxyListDownloadConfirm(String msg);

    void newProxies(Iterable<String> proxies);

    int getTimeout();

    void setStatusBar(String msg);

    void clearStatusBar();

    String getSelectedProxy();

    boolean canProxyDownloadLinkInfo();

    boolean canProxyVideoInfo();

    boolean canProxySearchEngines();

    boolean canProxyTrailers();

    boolean canProxyVideoStreamers();

    boolean canProxyUpdates();

    boolean canProxySubtitles();

    String getFormat();

    String getMinDownloadSize();

    String getMaxDownloadSize();

    String getAutoDownloader();

    String[] getWhitelistedFileExts();

    String[] getBlacklistedFileExts();

    boolean canShowSafetyWarning();

    boolean canAutoDownload();

    boolean canIgnoreDownloadSize();

    void commentsFinderStarted();

    void commentsFinderStopped();

    void commentsFinderError(Exception e);

    Object[] makeRow(String titleID, String imagePath, String title, String year, String rating, String summary);

    void updateStarted();

    void updateStopped();

    void updateMsg(String msg);

    boolean canUpdate();

    void subtitleSearchStarted();

    void subtitleSearchStopped();

    void summaryReadStarted();

    void summaryReadStopped();

    void subtitleMsg(String msg, int msgType);

    void restrictedWebsite();

    void setCanProxy(int type);

    int getPort();

    boolean[] confirmFeedOffer();
}

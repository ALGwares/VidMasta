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

    void enableSummarySearchStop(boolean enable);

    void enableTrailerSearchStop(boolean enable);

    void enableTorrentSearchStop(boolean enable);

    void enableStreamSearchStop(boolean enable);

    void enableLinkProgress(boolean enable);

    void videoDownloadStopped();

    void videoWatchStopped();

    void altVideoDownloadStarted();

    void msg(String msg, int msgType);

    void timedMsg(String msg);

    void initSafetyDialog(String name);

    void safetyDialogMsg(String statistic, String link, String name);

    void showSafetyDialog();

    boolean canProceedWithUnsafeDownload();

    boolean canProceedWithUnsafeDownload(String name);

    void summary(String summary, String imagePath);

    Element getSummaryElement(String id);

    void insertAfterSummaryElement(Element element, String text);

    void browserNotification(String item, String action, DomainType domainType);

    void startPeerBlock();

    void saveTorrent(File torrentFile) throws Exception;

    void saveSubtitle(String saveFileName, File subtitleFile) throws Exception;

    boolean tvChoices(String season, String episode);

    String getTitle(int row, String titleID);

    void setTitle(String title, int row, String titleID);

    void setSummary(String summary, int row, String titleID);

    String getSeason(int row, String titleID);

    void setSeason(String season, int row, String titleID);

    void setEpisode(String episode, int row, String titleID);

    void setImageLink(String imageLink, int row, String titleID);

    void setImagePath(String imagePath, int row, String titleID);

    String getSeason();

    String getEpisode();

    void searchStarted();

    void newSearch(int maxProgress, boolean isTVShow);

    boolean oldSearch(int maxProgress);

    void searchStopped();

    void searchProgressMaxOut();

    void moreResults(boolean areMoreResults);

    void newResult(Object[] result);

    void newResults(Iterable<Object[]> results);

    void searchNumResultsUpdate(int numResults);

    void searchProgressIncrement();

    boolean newPlaylistItem(Object[] item);

    void removePlaylistItem(PlaylistItem playlistItem);

    void setPlaylistItemProgress(FormattedNum progress, PlaylistItem playlistItem, boolean updateValOnly);

    boolean showPlaylist(PlaylistItem selectedPlaylistItem);

    String getPlaylistSaveDir();

    void playlistError(String msg, boolean html);

    void refreshPlaylistControls();

    void setPlaylistPlayHint(String msg);

    boolean useMediaServer();

    boolean isConfirmed(String msg);

    boolean isAuthorizationConfirmed(String msg);

    String getAuthorizationUsername();

    char[] getAuthorizationPassword();

    void proxyListDownloadStarted();

    void proxyListDownloadStopped();

    void newProxies(Iterable<String> proxies);

    int getTimeout();

    int getDownloadLinkTimeout();

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

    boolean canAutoOpenPlaylistItem();

    String getFormat();

    String getMinDownloadSize();

    String getMaxDownloadSize();

    String getAutoDownloader();

    String[] getWhitelistedFileExts();

    String[] getBlacklistedFileExts();

    boolean canShowSafetyWarning();

    boolean canAutoDownload();

    boolean canDownloadWithDefaultApp();

    boolean canEmailWithDefaultApp();

    boolean canIgnoreDownloadSize();

    void commentsFinderStarted();

    void commentsFinderStopped();

    Object[] makeRow(String titleID, String imagePath, String title, String currTitle, String oldTitle, String year, String rating, String summary,
            String imageLink, boolean isTVShow, boolean isTVShowAndMovie, String season, String episode);

    Object[] makePlaylistRow(String name, FormattedNum size, FormattedNum progress, PlaylistItem playlistItem);

    void updateStarted();

    void updateStopped();

    void updateMsg(String msg);

    boolean canUpdate();

    void subtitleSearchStarted();

    void subtitleSearchStopped();

    void summaryReadStarted();

    void summaryReadStopped();

    int getPort();

    String wideSpace();

    String invisibleSeparator();

    void licenseActivation();

    void showLicenseActivation();

    void licenseActivated(boolean alert, String activationCode);

    void licenseDeactivated(boolean alert);

    void licenseActivationStarted();

    void licenseActivationStopped();
}

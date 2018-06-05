package listener;

import java.io.File;
import java.util.List;
import javax.swing.text.Element;

public interface GuiListener {

    void loading(boolean isLoading);

    void error(Exception e);

    void enable(Boolean enablePrimary, Boolean enableSecondary, Boolean startPrimary, ContentType contentType);

    void enable(ContentType contentType);

    void altVideoDownloadStarted();

    void msg(String msg, int msgType);

    void timedMsg(String msg);

    boolean canProceedWithUnsafeDownload(String name, int numFakeComments, int numComments, String link, String comments);

    void summary(String summary, String imagePath);

    Element getSummaryElement(String id);

    void insertAfterSummaryElement(Element element, String text);

    void browserNotification(DomainType domainType);

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

    void newSearch(boolean isTVShow);

    void searchStopped();

    void moreResults(boolean areMoreResults);

    void newResult(Object[] result);

    void searchProgressUpdate(int numResults, double progress);

    boolean isBanned(String id, String title, String year);

    boolean unbanDownload(Long downloadID, String downloadName);

    boolean newPlaylistItems(List<Object[]> items, int insertRow, int primaryItemIndex);

    int newPlaylistItem(Object[] item, int insertRow);

    void removePlaylistItem(PlaylistItem playlistItem);

    void setPlaylistItemProgress(FormattedNum progress, PlaylistItem playlistItem, boolean updateValOnly);

    void showPlaylist();

    boolean selectPlaylistItem(PlaylistItem playlistItem);

    String getPlaylistSaveDir();

    void playlistError(String msg);

    void refreshPlaylistControls();

    void setPlaylistPlayHint(String msg);

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

    boolean canProxyUpdates();

    boolean canProxySubtitles();

    boolean canAutoOpenPlaylistItem();

    int getTrailerPlayer();

    String getFormat();

    String getMinDownloadSize();

    String getMaxDownloadSize();

    boolean canDownloadWithPlaylist();

    String getWebBrowserAppDownloader();

    String[] getWhitelistedFileExts();

    String[] getBlacklistedFileExts();

    boolean canShowSafetyWarning();

    boolean canDownloadWithDefaultApp();

    boolean canEmailWithDefaultApp();

    boolean canPlayWithDefaultApp();

    boolean canIgnoreDownloadSize();

    Object[] makeRow(String titleID, String imagePath, String title, String currTitle, String oldTitle, String year, String rating, String summary,
            String imageLink, boolean isTVShow, boolean isTVShowAndMovie, String season, String episode);

    Object[] makePlaylistRow(String name, FormattedNum size, FormattedNum progress, PlaylistItem playlistItem);

    void updateStarted();

    void updateStopped();

    void updateMsg(String msg);

    boolean canUpdate();

    void subtitleSearchStarted();

    void subtitleSearchStopped();

    void summarySearchStarted(Video video);

    int getPort();

    String wideSpace();

    String invisibleSeparator();

    void showLicenseActivation();

    void licenseActivated(String activationCode);

    void licenseDeactivated();

    void licenseActivationStarted();

    void licenseActivationStopped();
}

package search;

import debug.Debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.TitleParts;
import search.util.TorrentSite2;
import search.util.VideoSearch;
import str.Str;
import util.AbstractWorker;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.ProxyException;
import util.Regex;

public class PopularSearcher extends AbstractSearcher {

  private String[] languages, countries;
  private boolean isFeed, startAsap;
  private final List<String[]> titleNames;
  // The popular torrents of the torrent site searched along with The Pirate Bay, in apibay.org's JSON. Stopped along with the search (see cancel()).
  private volatile AbstractWorker<String> popular2;
  private final int sleep = Integer.parseInt(Str.get(571));

  public PopularSearcher(GuiListener guiListener, int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed,
          boolean startAsap) {
    super(guiListener, numResultsPerSearch, isTVShow, Constant.MS_1HR);
    this.languages = locales(languages, Regex.languages);
    this.countries = locales(countries, Regex.countries);
    this.isFeed = isFeed;
    this.startAsap = startAsap;
    titleNames = new ArrayList<String[]>(50);
  }

  public PopularSearcher(PopularSearcher searcher) {
    super(searcher);
    languages = searcher.languages;
    countries = searcher.countries;
    isFeed = searcher.isFeed;
    startAsap = searcher.startAsap;
    titleNames = searcher.titleNames;
    popular2 = searcher.popular2;
  }

  private static String[] locales(String[] locales, Map<String, String> localeCodes) {
    String[] newLocales = new String[locales.length];
    for (int i = 0; i < locales.length; i++) {
      if (locales[i].equals(Constant.ANY)) {
        return Constant.EMPTY_STRS;
      }
      newLocales[i] = localeCodes.get(locales[i]);
    }
    return newLocales;
  }

  static boolean isValidLocale(String sourceCode, String[] locales, int regexStartIndex) {
    for (String locale : locales) {
      if (Regex.firstMatch(sourceCode, Str.get(regexStartIndex) + locale + Str.get(regexStartIndex + 1)).isEmpty()) {
        if (Debug.DEBUG) {
          TitleParts titleParts = VideoSearch.getImdbTitleParts(sourceCode);
          Debug.println("Wrong locale (NOT " + locale + "): '" + titleParts.title + "' '" + titleParts.year + '\'');
        }
        return false;
      }
    }
    return true;
  }

  @Override
  protected void initialSearch() throws Exception {
    if (sleep > 0 && isFeed && !startAsap) {
      Thread.sleep(sleep);
    }

    try {
      for (String page : Regex.split(Regex.replaceAll(Connection.getSourceCode(Str.get(isTVShow ? 688 : 689), DomainType.DOWNLOAD_LINK_INFO, cacheExpirationMs),
              Pattern.quote(Constant.NEWLINE), Constant.STD_NEWLINE), 690)) {
        titleNames.add(Regex.split(page.trim(), 691));
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  @Override
  protected int anotherPageRegexIndex() {
    return 649;
  }

  @Override
  protected void initCurrVideos() throws Exception {
    // The other torrent site's popular videos are searched for along with The Pirate Bay's, which are on the page after the pages of titleNames
    boolean search2 = (currSearchPage == titleNames.size());
    AbstractWorker<String> currPopular2 = popular2;
    if (search2 && (currPopular2 == null || currPopular2.isCancelled())) { // Such as when a search that was stopped is continued
      (popular2 = currPopular2 = new AbstractWorker<String>() {
        @Override
        protected String doInBackground() throws Exception {
          return TorrentSite2.popular(isTVShow, cacheExpirationMs, this::isCancelled);
        }
      }).execute();
      if (isCancelled()) { // The search could have been stopped before cancel() saw this worker
        currPopular2.cancel(true);
      }
    }

    try {
      Connection.runDownloadLinkInfoDeproxier(() -> {
        boolean isDownloadLinkInfoDeproxied = Connection.isDownloadLinkInfoDeproxied();
        String currSource = currSourceCode, prevSource = prevSourceCode;
        PopularSearcher.super.initCurrVideos();
        if (!isCancelled() && !isDownloadLinkInfoDeproxied && Connection.isDownloadLinkInfoDeproxied()) {
          currSourceCode = currSource;
          prevSourceCode = prevSource;
          PopularSearcher.super.initCurrVideos();
        }
      });
    } catch (Exception e) {
      if (!search2 || isCancelled() || !addVideos2()) {
        throw e;
      }
      if (currSourceCode == null) {
        currSourceCode = ""; // So that the search ends instead of searching The Pirate Bay again
      }
      return;
    }

    if (search2 && !isCancelled()) {
      addVideos2();
    }
  }

  // Adds the other torrent site's popular videos to the videos found, alternating with The Pirate Bay's. Returns whether any were added.
  private boolean addVideos2() throws Exception {
    String popularVideos;
    try {
      popularVideos = AbstractWorker.get(popular2);
    } catch (InterruptedException | CancellationException e) {
      throw e;
    } catch (Exception e) {
      popular2 = null; // Searched for again if the page is loaded again
      if (Debug.DEBUG) { // Not shown, because the other site's videos only add to The Pirate Bay's, whose errors are shown
        Debug.print(e);
      }
      return false;
    }

    List<Video> videos = new ArrayList<Video>(videoBuffer);
    videoBuffer.clear();
    Matcher titleMatcher = Regex.matcher(122, popularVideos);
    for (Iterator<Video> videosIt = videos.iterator(); videosIt.hasNext() || !titleMatcher.hitEnd();) {
      if (videosIt.hasNext()) {
        videoBuffer.add(videosIt.next());
      }
      for (int numVideos = videoBuffer.size(); videoBuffer.size() == numVideos && !titleMatcher.hitEnd();) { // The next video that wasn't found already
        if (titleMatcher.find()) {
          addVideo(popularVideos.substring(titleMatcher.end()));
        }
      }
    }
    return videoBuffer.size() > videos.size();
  }

  // Also stops the search of the other torrent site, whose requests (and their status, or the web browser window of a bot check) would otherwise outlive
  // the search. A stopped search that is continued searches the other site again unless that search was done.
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean isCancelled = super.cancel(mayInterruptIfRunning);
    AbstractWorker<String> currPopular2 = popular2;
    if (currPopular2 != null) {
      currPopular2.cancel(true);
    }
    return isCancelled;
  }

  @Override
  protected boolean addCurrVideos() {
    if (currSearchPage >= titleNames.size()) {
      return false;
    }

    videoBuffer.clear();
    for (String titleName : titleNames.get(currSearchPage)) {
      addCurrVideo(titleName);
    }

    return true;
  }

  @Override
  protected String getUrl(int page, boolean isTVShow, int numResultsPerSearch) {
    return Str.get(729) + Str.get(isTVShow ? 651 : 652) + (page == 0 ? Str.get(653) : Str.get(654) + (page + Integer.parseInt(Str.get(655)))) + Str.get(656);
  }

  @Override
  protected DomainType domainType() {
    return DomainType.DOWNLOAD_LINK_INFO;
  }

  @Override
  protected boolean connectionException(String url, ConnectionException e) {
    if (e instanceof ProxyException) {
      guiListener.msg(e.getMessage(), Constant.ERROR_MSG);
    } else {
      if (Connection.downloadLinkInfoFail() && backupMode()) {
        return true;
      }
      guiListener.msg(Connection.error(url), Constant.ERROR_MSG);
    }
    return false;
  }

  @Override
  protected int getTitleRegexIndex(Iterable<String> urls) throws Exception {
    if (!Regex.firstMatch(currSourceCode, 147).isEmpty()) {
      fail(urls);
      return -1;
    }
    return 122;
  }

  protected void fail(Iterable<String> urls) throws Exception {
    for (String url : urls) {
      Connection.removeDownloadLinkInfoFromCache(url);
    }
    if (Connection.deproxyDownloadLinkInfo()) {
      return;
    }
    if (!isCancelled()) {
      if (backupMode()) {
        return;
      }
      guiListener.msg(Connection.serverError(urls.iterator().next()), Constant.ERROR_MSG);
    }
    throw new ConnectionException();
  }

  @Override
  protected void addVideo(String titleMatch) {
    String video = Regex.match(titleMatch, 123);
    String titleName = Regex.match(video, 125);
    if (!titleName.isEmpty() && (!isFeed || isTitleValid(video))) {
      addCurrVideo(titleName);
    }
  }

  private void addCurrVideo(String titleName) {
    if (isFeed && !isTVShow && Regex.firstMatch(Regex.replaceAll(titleName, 77), 569).isEmpty()) {
      return; // Wrong format
    }

    TitleParts titleParts = VideoSearch.getTitleParts(titleName, isTVShow);
    Video video = new Video(titleParts.title.toLowerCase(Locale.ENGLISH) + titleParts.year, titleParts.title, titleParts.year, isTVShow, false);
    if (allBufferVideos.add(video.id)) {
      video.season = titleParts.season;
      video.episode = (titleParts.episodes.isEmpty() ? "" : titleParts.episodes.get(titleParts.episodes.size() - 1));
      videoBuffer.add(video);
    }
  }

  @Override
  protected void checkVideoes(Iterable<String> urls) throws Exception {
    if ((currSearchPage - titleNames.size()) == 0 && videoBuffer.isEmpty()) {
      fail(urls);
    }
  }

  private boolean backupMode() {
    try {
      currSourceCode = Connection.getSourceCode(Str.get(isTVShow ? 483 : 484), DomainType.DOWNLOAD_LINK_INFO, cacheExpirationMs);
      String[] results = Regex.split(Regex.replaceAll(currSourceCode, Pattern.quote(Constant.NEWLINE), Constant.STD_NEWLINE), Constant.STD_NEWLINE);
      for (int i = 0; i < results.length; i += 5) {
        if (!isFeed || isTitleValid(results[i + 3], results[i + 1])) {
          addCurrVideo(results[i + 2].trim());
        }
      }
      return true;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    return false;
  }

  @Override
  protected boolean findImage(Video video) {
    return true;
  }

  @Override
  protected Video update(Video video) throws Exception {
    String titleLink = VideoSearch.getTitleLink(video.title, video.year);
    if (isCancelled() || titleLink == null) {
      return null;
    }

    String titleID = VideoSearch.normalize(Regex.firstMatch(titleLink, 628));
    if (titleID.isEmpty()) {
      return null;
    }

    File sourceCodeFile = new File(Constant.CACHE_DIR + Str.hashPath(titleLink) + Constant.HTML);
    if (sourceCodeFile.exists()) {
      TitleParts titleParts = VideoSearch.getImdbTitleParts(IO.read(sourceCodeFile));
      if (!titleParts.title.isEmpty() && !titleParts.year.isEmpty() && guiListener.isBanned(titleID, titleParts.title, titleParts.year)) {
        return null;
      }
    }

    String sourceCode = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO, isTVShow ? Constant.MS_2DAYS : Constant.MS_3DAYS);
    TitleParts titleParts = VideoSearch.getImdbTitleParts(sourceCode);
    if (titleParts.title.isEmpty() || titleParts.year.isEmpty() || (isFeed && !isTitleYearValid(titleParts.year)) || guiListener.isBanned(titleID,
            titleParts.title, titleParts.year)) {
      return null;
    }

    if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow)) {
      if (Debug.DEBUG) {
        Debug.println("Probably wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + titleParts.title + "' '" + titleParts.year + '\'');
      }
      if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590)) {
        if (Debug.DEBUG) {
          Debug.println("Definitely wrong video type (NOT a TV show and movie): '" + titleParts.title + "' '" + titleParts.year + '\'');
        }
        return null;
      }
    }

    if (!isValidLocale(sourceCode, languages, 183) || !isValidLocale(sourceCode, countries, 185)) {
      return null;
    }

    Video vid = new Video(titleID, titleParts.title, titleParts.year, video.isTVShow, VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590));
    vid.rating = VideoSearch.rating(Regex.match(sourceCode, 127));
    vid.season = video.season;
    vid.episode = video.episode;
    vid.summary = sourceCode;
    return vid;
  }

  @Override
  protected boolean noImage(Video video) {
    return isFeed;
  }

  private static boolean isTitleValid(String video) {
    if (Boolean.parseBoolean(Str.get(565)) && Regex.match(video, 74).isEmpty()) {
      return false; // Unsafe source
    }
    String size = Regex.match(video, 64);
    if (!size.isEmpty() && (int) Math.ceil(Double.parseDouble(size)) > Integer.parseInt(Str.get(567))) {
      return false; // Size too large
    }
    if (VideoSearch.isUploadYearTooOld(video, Integer.parseInt(Str.get(568)), -1)) {
      return false; // Upload year too old
    }
    String numSeeders = Regex.match(video, 72);
    return numSeeders.isEmpty() || Integer.parseInt(numSeeders) >= Integer.parseInt(Str.get(566)); // Seeders too few if false
  }

  private static boolean isTitleValid(String isSafe, String year) {
    return (!Boolean.parseBoolean(Str.get(565)) || Integer.parseInt(isSafe.trim()) == 1) && isTitleYearValid(year.trim()); // Unsafe source or year too old if false
  }

  private static boolean isTitleYearValid(String year) {
    return Integer.parseInt(year) + Integer.parseInt(Str.get(570)) >= Calendar.getInstance().get(Calendar.YEAR); // Year too old if false
  }
}

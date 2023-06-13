package search.download;

import debug.Debug;
import gui.UI;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import listener.ContentType;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import listener.VideoStrExportListener;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import search.BoxSetVideo;
import search.util.VideoSearch;
import str.Str;
import torrent.FileTypeChecker;
import torrent.Magnet;
import torrent.StreamingTorrentUtil;
import torrent.Torrent;
import util.AbstractWorker;
import util.Connection;
import util.Connection.WebBrowserRequest;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.Regex;
import util.ThrowableUtil;
import util.ThrowingRunnable;
import util.Worker;

public class VideoFinder extends Worker {

  final GuiListener guiListener;
  public final ContentType contentType;
  private ContentType currContentType;
  public final int row;
  private final VideoStrExportListener strExportListener;
  final Video video;
  public final boolean prefetch;
  private boolean isDownload1, play, startPeerblock = true, foreground;
  private String export;
  private static volatile Worker summaryFinder, episodeFinder;
  private Collection<Torrent> torrents;
  private Collection<TorrentFinder> torrentFinders;
  private TorrentSearchState searchState;
  private static final Object TV_CHOICES_LOCK = new Object(), DOWNLOAD_LOCK = new Object(), VIDEO_LOCK = new Object();
  private final Runnable rerunner;

  public VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener) {
    this(guiListener, contentType, row, video, strExportListener, false, null);
  }

  public VideoFinder(ContentType contentType, VideoFinder finder) {
    this(finder.guiListener, contentType, finder.row, finder.video, null, true, null);
  }

  public VideoFinder(GuiListener guiListener, ContentType contentType, int row, Video video, VideoStrExportListener strExportListener, boolean prefetch,
          Runnable rerunner) {
    this.guiListener = guiListener;
    this.contentType = contentType;
    this.row = row;
    this.video = video;
    this.strExportListener = strExportListener;
    this.prefetch = prefetch;
    this.rerunner = rerunner;
    foreground = (row != -1);
    if (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD2 || contentType == ContentType.DOWNLOAD3) {
      torrents = new CopyOnWriteArrayList<Torrent>();
      torrentFinders = new ArrayList<TorrentFinder>(12);
    }
  }

  @Override
  protected void doWork() {
    if (foreground) {
      guiListener.loading(true);
    }
    search(contentType);

    boolean isCancelled = false;
    ContentType export2ContentType = null;
    String export1 = export;

    if (strExportListener != null) {
      isCancelled = isCancelled();
      if (strExportListener.exportSecondaryContent()) {
        if (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD3) {
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
      strExportListener.export(contentType, export1, isCancelled, guiListener);
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
    if (contentType == ContentType.SUMMARY) {
    } else if (contentType == ContentType.TRAILER) {
      findTrailer();
    } else {
      searchState = new TorrentSearchState(guiListener);
      isDownload1 = (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD3);

      if (video.isTVShow) {
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
        } else if (video.isTVShow) {
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
          if (video.isTVShow && torrents.isEmpty() && !video.episode.isEmpty() && !Constant.ANY.equals(video.episode)
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

    if (summaryFinder != null) {
      summaryFinder.cancel(true);
    }
    if (episodeFinder != null) {
      episodeFinder.cancel(true);
    }

    String imagePath = Constant.CACHE_DIR + VideoSearch.imagePath(video);
    boolean imageExists = (new File(imagePath)).exists();
    if (video.summary.isEmpty()) {
      guiListener.summary("<html><head><title></title></head><body><table><tr><!--poster--></tr></table></body></html>", imageExists ? imagePath
              : Constant.PROGRAM_DIR + "noPosterBig.jpg");
    }

    updateOldTitleAndSummary();
    if (isCancelled()) {
      return;
    }

    if (!imageExists) {
      if (video.imageLink.isEmpty()) {
        imagePath = null;
      } else {
        try {
          Connection.saveData(video.imageLink, imagePath, DomainType.VIDEO_INFO);
          guiListener.setImagePath(video.imagePath = imagePath, row, video.id);
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
    if (!Regex.firstMatch(video.summary, "\\sid\\s*+\\=\\s*+\"((" + Constant.STORYLINE_LINK1_HTML_ID + ")|(" + Constant.STORYLINE_LINK2_HTML_ID
            + "))\"").isEmpty()) {
      (summaryFinder = new SummaryFinder(guiListener, row, video)).execute();
    }
    if (video.isTVShow && !Regex.firstMatch(video.summary, VideoSearch.summaryTagRegex(Constant.TV_NEXT_EPISODE_HTML_ID) + "\\s*+\\<").isEmpty()) {
      (episodeFinder = new EpisodeFinder(guiListener, row, video)).execute();
    }

    if (rerunner != null) {
      readSummary();
    }
  }

  private void readSummary() throws Exception {
    File speech = new File(Constant.TEMP_DIR + Regex.toFileName(video.title + '-' + video.year) + "-" + (video.id.hashCode() & 0xfffffff) + Str.get(768));
    if (speech.exists()) {
      read(speech);
      return;
    }

    if (summaryFinder != null) {
      summaryFinder.get();
    }

    String br1 = "\\<br\\>";
    String tempNewSummary = Regex.match(video.summary, VideoSearch.summaryTagRegex(Constant.STORYLINE_HTML_ID), "\\</td\\>"), newSummary;
    if (tempNewSummary.isEmpty()) {
      String br2 = br1 + "\\s*+" + br1;
      newSummary = Regex.firstMatch(video.summary, VideoSearch.summaryTagRegex(Constant.GENRE_HTML_ID)).isEmpty() ? Regex.match(video.summary,
              "\\<font[^\\>]++\\>", br2) : Regex.match(video.summary, br2, br2);
    } else {
      newSummary = Regex.match(tempNewSummary, br1, "\\z");
    }

    WebBrowserRequest request = new WebBrowserRequest(857) {
      @Override
      protected void triggerSubRequest(FirefoxDriver driver, ThrowingRunnable<InterruptedException> sleep) throws Exception {
        Duration timeout = driver.manage().timeouts().getPageLoadTimeout();
        (new WebDriverWait(driver, timeout)).until(ExpectedConditions.and(ExpectedConditions.presenceOfElementLocated(By.id(
                Str.get(855))), ExpectedConditions.presenceOfElementLocated(By.id(Str.get(856)))));
        WebElement textArea = driver.findElement(By.id(Str.get(855)));
        textArea.clear();
        sleep.run();
        textArea.sendKeys(Regex.clean(Regex.replaceAll(Regex.replaceAll(newSummary, 468), 470), false));
        sleep.run();
        driver.findElement(By.id(Str.get(856))).click();
        sleep.run();
        driver.get(driver.getCurrentUrl());
        (new WebDriverWait(driver, timeout)).until(ExpectedConditions.presenceOfElementLocated(By.id(Str.get(856))));
      }
    };
    Connection.getSourceCode(Str.get(854), DomainType.VIDEO_INFO, true, false, -1, request);
    Connection.saveData(request.subrequestUrl.get(), speech.getPath(), DomainType.VIDEO_INFO, true, null, 2, request.cookies.get());
    if (!isCancelled()) {
      read(speech);
    }
  }

  private void read(File speech) throws Exception {
    if (MediaPlayer.open(704, speech, true, true)) {
      return;
    }

    String speechName = speech.getName();
    File speechPage = new File(Constant.TEMP_DIR + speechName.substring(0, speechName.lastIndexOf('.')) + Constant.HTML);
    if (!speechPage.exists()) {
      String imagePath = ((new File(imagePath = Constant.CACHE_DIR + VideoSearch.imagePath(video))).exists() ? imagePath : Constant.PROGRAM_DIR
              + "noPosterBig.jpg");
      String posterFilePath = Constant.TEMP_DIR + "2_" + (new File(imagePath)).getName();
      File posterFile = new File(posterFilePath);
      try {
        ImageIO.write(UI.image(new ImageIcon((new ImageIcon(imagePath)).getImage().getScaledInstance(214, -1, Image.SCALE_SMOOTH))), "png", posterFile);
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        IO.fileOp(posterFile, IO.RM_FILE);
        posterFilePath = imagePath;
      }
      IO.write(speechPage, Str.get(769).replace(Str.get(480), speech.getPath().replace('\\', '/')).replace(Str.get(481), Regex.cleanWeirdChars(video.title)
              + " (" + video.year + ')').replace(Str.get(482), posterFilePath.replace('\\', '/')));
    }

    guiListener.browserNotification(DomainType.VIDEO_INFO);
    IO.browse(speechPage);
  }

  private static void browseMagnetLink(Torrent torrent) throws IOException {
    Connection.browse(torrent.magnetLink, "bitTorrentClient");
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
              + "LinkNotFound") + Constant.STD_NEWLINE2 + Str.str("settings", " " + (settings.isEmpty() ? VideoUtil.describe(video) : VideoUtil.describe(
                      video) + ',' + settings)), Constant.INFO_MSG);
      addVideoToPlaylist();
    } else {
      if (Debug.DEBUG) {
        Debug.println("Selected torrent: " + torrent);
      }
      boolean orderByLeechers = (currContentType == ContentType.DOWNLOAD1);
      TorrentFinder.saveOrdering(torrent.id, video.season, video.episode, orderByLeechers);
      if (String.format(Constant.TV_EPISODE_FORMAT, 1).equals(video.season)) {
        TorrentFinder.saveOrdering(torrent.id, Constant.ANY, video.episode, orderByLeechers);
      }

      if (!guiListener.unbanDownload(Str.hashCode(torrent.magnetLink), torrent.name())) {
        addVideoToPlaylist();
      } else if (torrent.isSafe || !guiListener.canShowSafetyWarning()) {
        saveTorrentHelper(torrent);
      } else {
        int numFakeComments = 0, numComments = 0;
        String comments = null;
        try {
          if (torrent.commentsLink != null && !torrent.commentsLink.isEmpty() && !torrent.commentsLink.equals(Str.get(730) + Str.get(678))) {
            Collection<String> commentsArr = Regex.matches(Connection.getSourceCode(torrent.commentsLink, DomainType.DOWNLOAD_LINK_INFO, true, true,
                    Constant.MS_1HR), 151);
            if ((numComments = commentsArr.size()) != 0) {
              StringBuilder commentsBuf = new StringBuilder(4096);
              int number = 0;
              for (String comment : commentsArr) {
                commentsBuf.append(++number).append(". ").append(Regex.htmlToPlainText(Regex.replaceAllRepeatedly(comment, 672))).append(
                        Constant.STD_NEWLINE2);
                if (!Regex.firstMatch(comment, 153).isEmpty()) {
                  numFakeComments++;
                }
              }
              comments = commentsBuf.toString();
            }
          }
        } catch (Exception e) {
          error(e);
        }
        if (isCancelled()) {
          return;
        }
        if (guiListener.canProceedWithUnsafeDownload(torrent.name(), numFakeComments, numComments, torrent.commentsLink, comments)) {
          saveTorrentHelper(torrent);
        } else {
          addVideoToPlaylist();
        }
      }
    }
  }

  private void saveTorrentHelper(Torrent torrent) throws Exception {
    if (strExportListener != null) {
      export = torrent.magnetLink;
      return;
    }

    if (!video.isTVShow && !VideoSearch.isRightFormat(torrent.name, Constant.HQ)) {
      addVideoToPlaylist();
    }

    if (play) {
      if ((torrent.file == null || !torrent.file.exists()) && searchState.blacklistedFileExts.length != 0) {
        torrentDownloadError(torrent);
      }
      startPeerBlock();
      StreamingTorrentUtil.stream(torrent.magnetLink, VideoSearch.getTitleParts(torrent.name, video.isTVShow).title + guiListener.invisibleSeparator()
              + Constant.FILE_SEPARATOR + torrent.name, true);

      if (video.isTVShow && !video.season.equals(Constant.ANY) && !video.episode.equals(Constant.ANY)) {
        (new Worker() {
          @Override
          public void doWork() {
            try {
              EpisodeFinder episodeSearcher = new EpisodeFinder();
              episodeSearcher.findEpisodes(VideoSearch.url(video));
              String nextEpisode = Regex.firstMatch(episodeSearcher.nextEpisodeText, "\\AS\\d++E\\d++");
              if (nextEpisode.isEmpty()) {
                return;
              }
              Video nextVideo = new Video(video.id, video.title, video.year, video.isTVShow, video.isTVShowAndMovie);
              int index = nextEpisode.indexOf('E');
              nextVideo.season = nextEpisode.substring(1, index);
              nextVideo.episode = nextEpisode.substring(index + 1);
              String prevEpisode;
              if ((nextVideo.season.equals(video.season) && Integer.parseInt(nextVideo.episode) == Integer.parseInt(video.episode) + 1)
                      || (Integer.parseInt(nextVideo.season) == Integer.parseInt(video.season) + 1 && !(prevEpisode = Regex.firstMatch(
                      episodeSearcher.prevEpisodeText, "\\AS\\d++E\\d++")).isEmpty() && prevEpisode.substring(1, index = prevEpisode.indexOf(
                              'E')).equals(video.season) && prevEpisode.substring(index + 1).equals(video.episode))) {
                nextVideo.oldTitle = video.oldTitle;
                StreamingTorrentUtil.stream(nextVideo, VideoUtil.describe(nextVideo), false);
              }
            } catch (Exception e) {
              if (Debug.DEBUG) {
                Debug.print(e);
              }
            }
          }
        }).execute();
      }
      return;
    }

    if (torrent.file == null || !torrent.file.exists()) {
      if (guiListener.canDownloadWithDefaultApp()) {
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

    String torrentFilePath = IO.parentDir(torrent.file) + torrent.fileName();
    File torrentFile = new File(torrentFilePath);
    if (!torrentFile.exists()) {
      IO.write(torrent.file, torrentFile);
    }

    if (guiListener.canDownloadWithDefaultApp()) {
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
    if (foreground) {
      StreamingTorrentUtil.stream(video, VideoUtil.describe(video), true);
    }
  }

  private boolean tvChoices(boolean enableEpisode) {
    synchronized (TV_CHOICES_LOCK) {
      if (strExportListener == null || strExportListener.showTVChoices()) {
        boolean cancelTVSelection = guiListener.tvChoices(video.season, video.episode, enableEpisode);
        if (strExportListener != null) {
          strExportListener.setEpisode(guiListener.getSeason(), guiListener.getEpisode());
        }
        return cancelTVSelection;
      }
      return false;
    }
  }

  private void findTrailer() throws Exception {
    if (!prefetch && video.isTVShow && rerunner != null && tvChoices(false)) {
      return;
    }

    Integer season = null;
    if (video.isTVShow) {
      if (!prefetch) {
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
      link1 = getTrailerLink(season, true);
      if (prefetch || isCancelled()) {
        return;
      }
    } catch (Exception e) {
      if (prefetch || isCancelled()) {
        throw e;
      }
      error(e);
    }

    final String[] link;
    if (link1 == null) {
      String[] link2 = null;
      try {
        link2 = getTrailerLink(season, false);
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
          link2 = getTrailerLink(season, false);
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

  private String[] getTrailerLink(Integer season, boolean link1) throws Exception {
    boolean useLink2AsLink1 = Boolean.parseBoolean(Str.get(835)) && (season == null || video.season.equals(guiListener.getSeason(row, video.id)));
    return link1 ? (useLink2AsLink1 ? getTrailerLink2(season) : getTrailerLink1(season, true)) : (useLink2AsLink1 ? getTrailerLink1(season, true)
            : getTrailerLink2(season));
  }

  private String[] getTrailerLink1(Integer season, boolean canRetry) throws Exception {
    String urlFormOptions = URLEncoder.encode(Regex.clean(video.title) + (season == null ? "" : " \"season " + season + "\"") + (video.isTVShow ? "" : ' '
            + video.year) + Str.get(87), Constant.UTF8), url = Str.get(86) + urlFormOptions;
    String source = Connection.getSourceCode(url, DomainType.TRAILER, !prefetch, Constant.MS_1HR);
    if (prefetch || isCancelled()) {
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
    String sourceCode1 = Connection.getSourceCode(season == null ? String.format(Str.get(748), video.id) : String.format(Str.get(749), video.id, season),
            DomainType.VIDEO_INFO, !prefetch, video.isTVShow ? Constant.MS_2DAYS : Constant.MS_3DAYS);
    if (isCancelled()) {
      return null;
    }

    Pattern officialRegex = Regex.pattern(858), titleRegex = Regex.pattern("(?i)((" + Pattern.quote(video.title) + ")|(" + Pattern.quote(Regex.htmlToPlainText(
            video.title)) + "))");
    List<Entry<String, String>> results = Regex.matches(sourceCode1, 750).stream().map(videoId -> new SimpleImmutableEntry<>(videoId, Regex.match(sourceCode1,
            String.format(Str.get(804), Pattern.quote(videoId)), Str.get(805)))).sorted(Collections.reverseOrder((result1, result2) -> {
      CompareToBuilder compare = new CompareToBuilder();
      Consumer<Function<String, Boolean>> appendToCompare = function -> compare.append(function.apply(result1.getValue()), function.apply(result2.getValue()));
      if (season != null) {
        appendToCompare.accept(title -> Boolean.TRUE.equals(TorrentFinder.isRightSeason(' ' + title, season)));
      }
      appendToCompare.accept(title -> officialRegex.matcher(title).find());
      appendToCompare.accept(title -> titleRegex.matcher(title).find());
      return compare.toComparison();
    })).collect(Collectors.toList());
    for (Entry<String, String> result : results) {
      String videoId = result.getKey();

      // Expire cached source code in 1hr because video link in source code has built in expiration of a few hours
      String sourceCode = Connection.getSourceCode(String.format(Str.get(752), videoId), DomainType.VIDEO_INFO, !prefetch, Constant.MS_1HR);
      if (isCancelled()) {
        return null;
      }

      List<String> videoInfos = Regex.matches(sourceCode, 753);
      if (videoInfos.isEmpty()) {
        sourceCode = Connection.getSourceCode(String.format(Str.get(812), videoId), DomainType.VIDEO_INFO, !prefetch, Constant.MS_1HR);
        if (isCancelled()) {
          return null;
        }

        videoInfos = Regex.matches(sourceCode, 753);
      }

      for (String videoInfo : videoInfos) {
        for (String videoInfoContent : Regex.matches(videoInfo, 756)) {
          String link = Regex.replaceAllRepeatedly(Regex.match(videoInfoContent, 758), 813);
          if (!link.isEmpty() && !Regex.firstMatch(videoInfoContent, 760).isEmpty()) {
            String title = result.getValue();
            return new String[]{link, title.isEmpty() ? video.title + " (" + video.year + ')' : title + (Regex.replaceAll(Regex.htmlToPlainText(
              Regex.replaceAll(title, 103).replace(':', ' ')), 339).trim().equalsIgnoreCase(Regex.replaceAll(Regex.htmlToPlainText(Regex.replaceAll(video.title,
              103).replace(':', ' ')), 339).trim()) ? "" : " (" + video.title + ')')};
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

    if (video.isTVShow) {
      if (video.season.isEmpty()) {
        video.season = Constant.ANY;
      }
      if (video.episode.isEmpty()) {
        video.episode = Constant.ANY;
      }
    }
    findAlt2DownloadLink(true);
    if (isCancelled() || !torrents.isEmpty()) {
      return;
    }

    String sourceCode;
    try {
      sourceCode = Connection.getSourceCode(Str.get(video.isTVShow ? 483 : 484), DomainType.DOWNLOAD_LINK_INFO, Constant.MS_1HR);
    } catch (Exception e) {
      error(e);
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
          torrent = magnet.torrent;
          extensions = FileTypeChecker.getFileExts(magnet.torrent, searchState.whitelistedFileExts);
        } else {
          torrent = null;
          extensions = "";
        }

        torrents.add(new Torrent("", magnet.magnetLink, results[i + 2].trim(), torrent, extensions, null, Integer.parseInt(results[i + 3].trim()) == 1, 0,
                0));
      } catch (Exception e) {
        error(e);
      }
      return;
    }
  }

  private void findAlt2DownloadLink(boolean singleOrderByMode) throws Exception {
    torrentFinders.clear();
    Magnet.waitForAzureusToStart();
    addFinder(video.title, "", false, false, false, null, true, singleOrderByMode);
    if (prefetch) {
      for (TorrentFinder finder : torrentFinders) {
        finder.getTorrents(true);
      }
    } else {
      AbstractWorker.executeAndWaitFor(torrentFinders);
    }
  }

  private boolean findTVDownloadLink(boolean canShowTVChoices) throws Exception {
    if (canShowTVChoices) {
      if (foreground && tvChoices(true)) {
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
    if (video.season.isEmpty()) {
      video.season = Constant.ANY;
    }
    if (video.episode.isEmpty()) {
      video.episode = Constant.ANY;
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
    if (!prefetch) {
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
    String newTitle = Regex.cleanAbbreviations(title);
    for (String currTitle : newTitle.equals(title) ? new String[]{title} : new String[]{title, newTitle}) {
      Video vid = new Video(video.id, currTitle, video.year, video.isTVShow, video.isTVShowAndMovie);
      vid.season = video.season;
      vid.episode = video.episode;
      torrentFinders.add(new TorrentFinder(guiListener, torrentFinders, torrents, vid, seasonAndEpisode, isDownload1, magnetLinkOnly(), ignoreYear,
              isOldTitle, isTitlePrefix, new TorrentSearchState(searchState), boxSet, altSearch, singleOrderByMode));
    }
  }

  private void findDownloadLink() throws Exception {
    Connection.runDownloadLinkInfoDeproxier(() -> {
      if (prefetch) {
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
    });
    if (!isCancelled()) {
      findAlt2DownloadLink(false);
    }
  }

  private void updateOldTitleAndSummary() throws Exception {
    synchronized (VIDEO_LOCK) {
      if (!video.summary.isEmpty() || !foreground) {
        return;
      }

      String sourceCode = Connection.getSourceCode(VideoSearch.url(video), DomainType.VIDEO_INFO, !prefetch, video.isTVShow ? Constant.MS_2DAYS
              : Constant.MS_3DAYS);
      video.oldTitle = VideoSearch.getOldTitle(sourceCode);
      if (!video.oldTitle.isEmpty()) {
        String displayTitle = guiListener.getTitle(row, video.id);
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
          guiListener.setTitle(startHtml + displayTitle + endHtml, row, video.id);
        }
      }

      String imageLink = Regex.match(sourceCode, 188);
      guiListener.setImageLink(imageLink, row, video.id);
      video.imageLink = imageLink;
      String summary = VideoSearch.getSummary(sourceCode, video.isTVShow);
      guiListener.setSummary(summary, row, video.id);
      video.summary = summary;
    }
  }
}

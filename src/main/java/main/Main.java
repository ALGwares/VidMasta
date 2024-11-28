package main;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import debug.Debug;
import gui.GUI;
import gui.SplashScreen;
import gui.UI;
import i18n.I18n;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import listener.ContentType;
import listener.FormattedNum;
import listener.GuiListener;
import listener.PlaylistItem;
import listener.Video;
import listener.VideoStrExportListener;
import listener.WorkerListener;
import proxy.ProxyListDownloader;
import search.PopularSearcher;
import search.RegularSearcher;
import search.download.Prefetcher;
import search.download.SubtitleFinder;
import search.download.VideoFinder;
import str.Str;
import torrent.Magnet;
import torrent.StreamingTorrentUtil;
import util.AbstractWorker;
import util.AbstractWorker.StateValue;
import util.Connection;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.ModClass;
import util.ThrowableUtil;
import util.Worker;

public class Main implements WorkerListener {

  private static File appLockFile;
  private static FileChannel appLockFileChannel;
  private static FileLock appLockFileLock;
  private static Thread releaseSingleInstanceShutdownHook;
  private static volatile Frame mainFrame;
  private final GuiListener gui;
  private Updater updater;
  private RegularSearcher regularSearcher;
  private PopularSearcher popularSearcher;
  private VideoFinder summaryFinder, trailerFinder, torrentFinder;
  private ProxyListDownloader proxyDownloader;
  private SubtitleFinder subtitleFinder;
  private Worker streamingTorrentReloader;
  private Prefetcher prefetcher;

  static {
    suppressStdOutput();
    System.setProperty("https.protocols", "TLSv1.2");
    System.setProperty("jdk.tls.allowUnsafeServerCertChange", String.valueOf(true));
    System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", String.valueOf(true));
    System.setProperty("htmlFont1", "<font face=\"Verdana, Geneva, sans-serif\">");
    System.setProperty("htmlFont2", "<font face=\"Tahoma\">");
    System.setProperty("htmlFont3", "<font size=\"5\">");
    System.setProperty("msgComponentPreferredHeight", "70");
    I18n.setLocale(new Locale("en", "US"));
    Str.init(new StrUpdater());
    setLookAndFeel();
    singleInstance();
    Runtime.getRuntime().addShutdownHook(releaseSingleInstanceShutdownHook = new Thread() {
      @Override
      public void run() {
        releaseSingleInstance();
      }
    });
  }

  private Main() throws Exception {
    gui = UI.run(new Callable<GuiListener>() {
      @Override
      public GuiListener call() throws Exception {
        return new GUI(Main.this);
      }
    });
  }

  public static void init() {
    try {
      initialize();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      JOptionPane.showMessageDialog(null, ThrowableUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
      IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
      System.exit(-1);
    }
  }

  private static void initialize() throws Exception {
    final SplashScreen splashScreen = new SplashScreen();
    splashScreen.setVisible(true);
    final Rectangle initialSplashScreenBounds = splashScreen.getBounds();

    final Main main = new Main();
    final GUI gui = (GUI) main.gui;
    Connection.init(gui);
    StreamingTorrentUtil.init(gui, main);

    for (int i = 0; i < Str.MAX_SUBDIRECTORIES; i++) {
      IO.fileOp(Constant.CACHE_DIR + i, IO.MK_DIR);
    }

    Runtime runtime = Runtime.getRuntime();
    runtime.removeShutdownHook(releaseSingleInstanceShutdownHook);
    runtime.addShutdownHook(new Thread() {
      @Override
      public void run() {
        gui.saveUserSettings();
        gui.savePlaylist();
        AbstractWorker.shutdown();
        removeTempFiles();
        gui.stopPosterCacher();
        Connection.stopStatusBar();
        StreamingTorrentUtil.stopPlayer();
        releaseSingleInstance();
        Magnet.stopAzureus();
      }
    });

    removeTempFiles();
    IO.fileOp(Constant.TEMP_DIR, IO.MK_DIR);
    Connection.startStatusBar();
    if (Boolean.parseBoolean(Str.get(866))) {
      Worker.submit(() -> Connection.initWebBrowser());
    }

    (new Timer()).schedule(new TimerTask() {
      @Override
      public void run() {
        main.updateStarted(true);
      }
    }, Calendar.getInstance().getTime(), 14400000);

    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          if (!splashScreen.isVisible()) {
            System.exit(0);
          }
          if ((splashScreen.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
            gui.setExtendedState(gui.getExtendedState() | Frame.ICONIFIED);
          }
          Rectangle splashScreenBounds = splashScreen.getBounds();
          if (!splashScreenBounds.equals(initialSplashScreenBounds)) {
            gui.setBounds(splashScreenBounds);
          }
          gui.setVisible(true);
          gui.resizeContent();
          gui.setInitialFocus();
          splashScreen.setVisible(false);
          gui.startPosterCacher();
          gui.doPopularVideosSearch(false, true, true, null);
          splashScreen.dispose();
          mainFrame = gui;
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          JOptionPane.showMessageDialog(null, ThrowableUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
          IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
          System.exit(-1);
        }
      }
    });

    Worker.submit(() -> IO.listAllFiles(Constant.CACHE_DIR).stream().filter(file -> file.getName().endsWith(Constant.HTML) && IO.isFileTooOld(file,
            2592000000L)).forEach(File::delete)); // Warm and clean cache 
    Magnet.initIpFilter();
    MediaPlayer.install();
    cleanUpAppDir();
  }

  private static void singleInstance() {
    try {
      if (showMainFrame()) {
        if (Debug.DEBUG) {
          Debug.println("Only 1 instance of " + Constant.APP_TITLE + " can run.");
        }
        System.exit(-1);
      }
      appLockFile = new File(Constant.APP_DIR + "lock");
      if (appLockFile.exists()) {
        IO.fileOp(appLockFile, IO.RM_FILE);
      }
      appLockFileChannel = new RandomAccessFile(appLockFile, "rw").getChannel();
      appLockFileLock = appLockFileChannel.tryLock();
      if (appLockFileLock == null) {
        IO.close(appLockFileChannel);
        showMainFrame();
        if (Debug.DEBUG) {
          Debug.println("Only 1 instance of " + Constant.APP_TITLE + " can run.");
        }
        System.exit(-1);
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    try {
      LocateRegistry.createRegistry(1099);
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.println(e);
      }
    }
    try {
      Naming.rebind(RemoteSingleInstance.NAME, new RemoteSingleInstance());
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.println(e);
      }
    }
  }

  private static boolean showMainFrame() {
    try {
      ((RemoteInstance) Naming.lookup(RemoteSingleInstance.NAME)).showMainFrame();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  static void removeTempFiles() {
    long maxAge = Long.parseLong(Str.get(608));
    for (File torrent : IO.listFiles(Constant.TORRENTS_DIR)) {
      if (IO.isFileTooOld(torrent, maxAge)) {
        IO.fileOp(torrent, IO.RM_FILE);
      }
    }
    IO.fileOp(Constant.TEMP_DIR, IO.RM_DIR);
  }

  static void releaseSingleInstance() {
    if (appLockFileLock != null) {
      IO.release(appLockFileLock);
      IO.close(appLockFileChannel);
      IO.fileOp(appLockFile, IO.RM_FILE);
    }
  }

  private static boolean isWorkDone(Worker worker) {
    return worker == null || worker.getState() == StateValue.DONE;
  }

  private static void stop(Future<?> worker) {
    if (worker != null) {
      worker.cancel(true);
    }
  }

  @Override
  public boolean isSummarySearchDone() {
    return isWorkDone(summaryFinder);
  }

  @Override
  public boolean isTrailerSearchDone() {
    return isWorkDone(trailerFinder);
  }

  @Override
  public boolean isTorrentSearchDone() {
    return isWorkDone(torrentFinder);
  }

  private boolean areSearchersDone() {
    return isWorkDone(regularSearcher) && isWorkDone(popularSearcher);
  }

  @Override
  public boolean areWorkersDone() {
    return isSummarySearchDone() && isTrailerSearchDone() && isTorrentSearchDone() && areSearchersDone();
  }

  @Override
  public void regularSearchStarted(int numResultsPerSearch, Boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres,
          String[] languages, String[] countries, String minRating) {
    if (areSearchersDone()) {
      regularSearcher = new RegularSearcher(gui, numResultsPerSearch, isTVShow, startDate, endDate, title, genres, languages, countries, minRating);
      stopPrefetcher();
      regularSearcher.execute();
    }
  }

  @Override
  public void searchStopped(boolean isRegularSearcher) {
    stop(isRegularSearcher ? regularSearcher : popularSearcher);
  }

  @Override
  public void summarySearchStopped() {
    stop(summaryFinder);
  }

  @Override
  public void trailerSearchStopped() {
    stop(trailerFinder);
  }

  @Override
  public void torrentSearchStopped() {
    stop(torrentFinder);
  }

  @Override
  public void loadMoreSearchResults(boolean isRegularSearcher) {
    if (areSearchersDone()) {
      if (isRegularSearcher) {
        (regularSearcher = new RegularSearcher(regularSearcher)).execute();
      } else {
        (popularSearcher = new PopularSearcher(popularSearcher)).execute();
      }
    }
  }

  @Override
  public void popularSearchStarted(int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed, boolean startAsap) {
    if (areSearchersDone()) {
      popularSearcher = new PopularSearcher(gui, numResultsPerSearch, isTVShow, languages, countries, isFeed, startAsap);
      stopPrefetcher();
      popularSearcher.execute();
    }
  }

  @Override
  public void summarySearchStarted(int row, Video video, boolean read, VideoStrExportListener strExportListener) {
    if (isSummarySearchDone()) {
      startPrefetcher(summaryFinder = new VideoFinder(gui, ContentType.SUMMARY, row, video, strExportListener, false, read ? new Runnable() {
        @Override
        public void run() {
        }
      } : null));
      summaryFinder.execute();
    }
  }

  @Override
  public void trailerSearchStarted(final int row, final Video video, final VideoStrExportListener strExportListener) {
    if (isTrailerSearchDone()) {
      startPrefetcher(trailerFinder = new VideoFinder(gui, ContentType.TRAILER, row, video, strExportListener, false, new Runnable() {
        @Override
        public void run() {
          UI.run(false, new Runnable() {
            @Override
            public void run() {
              trailerFinder.cancel(true);
              (trailerFinder = new VideoFinder(gui, ContentType.TRAILER, row, video, strExportListener)).execute();
            }
          });
        }
      }));
      trailerFinder.execute();
    }
  }

  @Override
  public void torrentSearchStarted(ContentType contentType, int row, Video video, VideoStrExportListener strExportListener) {
    if (isTorrentSearchDone()) {
      Magnet.startAzureus(gui);
      startPrefetcher(torrentFinder = new VideoFinder(gui, contentType, row, video, strExportListener));
      torrentFinder.execute();
    }
  }

  @Override
  public Future<?> torrentSearchStarted(Video video) {
    Magnet.startAzureus(gui);
    AbstractWorker<?> videoFinder = new VideoFinder(gui, ContentType.DOWNLOAD1, -1, video, null);
    videoFinder.execute();
    return videoFinder;
  }

  @Override
  public void proxyListDownloadStarted() {
    if (isWorkDone(proxyDownloader)) {
      (proxyDownloader = new ProxyListDownloader(gui)).execute();
    }
  }

  @Override
  public void subtitleSearchStarted(String format, String languageID, Video video, boolean firstMatch, VideoStrExportListener strExportListener) {
    if (isWorkDone(subtitleFinder)) {
      (subtitleFinder = new SubtitleFinder(gui, true, format, languageID, video, firstMatch, strExportListener)).execute();
    }
  }

  @Override
  public Future<?> subtitleSearchStarted(String format, String languageID, Video video) {
    SubtitleFinder altSubtitleFinder = new SubtitleFinder(gui, false, format, languageID, video, true, null);
    altSubtitleFinder.execute();
    return altSubtitleFinder;
  }

  @Override
  public void subtitleSearchStopped() {
    stop(subtitleFinder);
  }

  @Override
  public void updateStarted(boolean silent) {
    if (isWorkDone(updater)) {
      (updater = new Updater(gui, silent)).execute();
    }
  }

  @Override
  public void portChanged(int port) {
    Magnet.changePorts(port);
  }

  @Override
  public void initPlaylist() throws Exception {
    Magnet.startAzureus(gui);
    Magnet.waitForAzureusToStart();
    StreamingTorrentUtil.startPlayer();
  }

  @Override
  public void stream(String magnetLink, String name) {
    StreamingTorrentUtil.stream(magnetLink, name, false);
  }

  @Override
  public void reloadGroup(PlaylistItem playlistItem) {
    if (isWorkDone(streamingTorrentReloader)) {
      (streamingTorrentReloader = StreamingTorrentUtil.torrentReloader(playlistItem)).execute();
    }
  }

  @Override
  public FormattedNum playlistItemSize(long size) {
    return StreamingTorrentUtil.size(size);
  }

  @Override
  public FormattedNum playlistItemProgress(double progress) {
    return StreamingTorrentUtil.progress(progress, "");
  }

  @Override
  public PlaylistItem playlistItem(String groupID, String uri, File groupFile, int groupIndex, String name, boolean isFirstVersion) {
    return StreamingTorrentUtil.playlistItem(groupID, uri, groupFile, groupIndex, name, isFirstVersion);
  }

  @Override
  public synchronized void changeLocale(Locale locale) {
    I18n.setLocale(locale);
    Magnet.localeChanged();
  }

  private void startPrefetcher(VideoFinder videoFinder) {
    if (prefetcher == null) {
      (prefetcher = new Prefetcher(videoFinder)).execute();
    } else if (!prefetcher.isForRow(videoFinder.row) || (!prefetcher.isForContentType(videoFinder.contentType) && prefetcher.getState() != StateValue.DONE)) {
      prefetcher.cancel(true);
      (prefetcher = new Prefetcher(videoFinder)).execute();
    }
  }

  private void stopPrefetcher() {
    if (prefetcher != null) {
      prefetcher.cancel(true);
      prefetcher = null;
    }
  }

  private static void cleanUpAppDir() {
    try {
      File cleanVersion = new File(Constant.APP_DIR, "clean" + Constant.APP_VERSION);
      if (cleanVersion.exists()) {
        return;
      }
      IO.fileOp(cleanVersion, IO.MK_FILE);

      Pattern filenameRegex = Pattern.compile("(?!((" + Pattern.quote(cleanVersion.getName()) + ")|(" + Pattern.quote(Constant.UPDATE_FILE) + ")|("
              + Pattern.quote(Constant.USER_SETTINGS) + ")|(" + Pattern.quote(Magnet.VUZE_VERSION) + ")|(" + Pattern.quote(Constant.PEER_BLOCK_CONF_VERSION)
              + ")|(" + Pattern.quote(Str.get(697)) + ")|(" + Pattern.quote(Str.get(Constant.WINDOWS ? 836 : (Constant.MAC ? 840 : 844))) + ")|("
              + Pattern.quote(AppUpdater.APP_UPDATER) + ")))((clean[\\d\\.]++)|(update\\d*+\\.txt)|(userSettings\\d*+\\.txt)|(vuze\\d*+)|(biglybt\\d*+)"
              + "|(ipfilter[\\d\\.]*+)|(ipfilter\\.((dat)|(txt)))|(peerblockConf[\\d\\.]++)|(vlc\\-[\\d\\.]++)|(firefox\\-[\\d\\.]++)"
              + "|(update(r|(Fail))[\\d\\.]++)|(peerblock)|(jre\\-8u9[12]\\-windows\\-i586\\.exe)|(vidmasta\\-setup\\-21\\.[67]\\.exe)"
              + "|(java\\d*+Version_?+\\d*+\\.txt)|(BitTorrentClient\\d*+\\.cer)|(torrents))");
      Arrays.stream(IO.listFiles(Constant.APP_DIR)).filter(file -> filenameRegex.matcher(file.getName()).matches()).forEach(file -> IO.fileOp(file,
              file.isDirectory() ? IO.RM_DIR : IO.RM_FILE));
      if (Arrays.stream(IO.listFiles(Constant.CACHE_DIR)).anyMatch(File::isFile)) {
        IO.listAllFiles(Constant.CACHE_DIR).stream().filter(File::isFile).forEach(File::delete);
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  private static void suppressStdOutput() {
    if (Debug.DEBUG) {
      return;
    }
    try {
      System.setErr(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
        }
      }));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    try {
      System.setOut(new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
        }
      }));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  private static void setLookAndFeel() {
    try {
      String classNamePrefix = "de.javasoft.plaf.synthetica.Synthetica", rootPaneUIClass = classNamePrefix + "RootPaneUI", anonymousInnerClass1 = "$1";
      ModClass.mod(Arrays.stream((new File((new File("target")).exists() ? Constant.PROGRAM_DIR + "target" : Constant.PROGRAM_DIR, "lib")).listFiles()).filter(
              lib -> lib.getName().startsWith("synthetica-")).findFirst().get().getPath(), new ModClass(rootPaneUIClass, new byte[]{0, 2, 4, -84, 0},
                      new byte[]{0, 2, 3, -84, 0}), new ModClass(rootPaneUIClass + anonymousInnerClass1, new byte[]{71, 16, 16, 96, -75}, new byte[]{71, 16, 0,
        96, -75}), new ModClass(classNamePrefix + "LookAndFeel" + anonymousInnerClass1, new byte[]{0, 29, -103, 0, 6}, new byte[]{0, 29, -102, 0, 6}));
      UIManager.put("Synthetica.menuItem.toolTipEnabled", true);
      SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
    } catch (Throwable t) {
      if (Debug.DEBUG) {
        Debug.print(t);
      }
      try {
        System.setProperty("htmlFont1", "<font face=\"Verdana, Geneva, sans-serif\" size=\"3\">");
        System.setProperty("htmlFont2", "<font face=\"Tahoma\" size=\"3\">");
        System.setProperty("htmlFont3", "<font size=\"4\">");
        System.setProperty("msgComponentPreferredHeight", "77");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Throwable t2) {
        if (Debug.DEBUG) {
          Debug.print(t2);
        }
        System.setProperty("msgComponentPreferredHeight", "85");
      }
    }
  }

  private static class RemoteSingleInstance extends UnicastRemoteObject implements RemoteInstance {

    private static final long serialVersionUID = 1L;
    static final String NAME = "//localhost/" + Constant.APP_TITLE;

    RemoteSingleInstance() throws RemoteException {
      super(0);
    }

    @Override
    public void showMainFrame() {
      if (mainFrame != null) {
        UI.show(mainFrame);
      }
    }
  }

  private interface RemoteInstance extends Remote {

    void showMainFrame() throws RemoteException;
  }
}

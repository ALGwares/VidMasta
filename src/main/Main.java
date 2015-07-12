package main;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import debug.Debug;
import gui.AbstractSwingWorker;
import gui.GUI;
import gui.SplashScreen;
import gui.UI;
import i18n.I18n;
import i18n.I18nStr;
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
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
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
import search.SubtitleFinder;
import search.SummaryReader;
import search.download.Prefetcher;
import search.download.VideoFinder;
import str.Str;
import torrent.Magnet;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.ModClass;
import util.RunnableUtil.AbstractWorker;
import util.VideoPlayer;

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
    private Prefetcher prefetcher;
    private SummaryReader summaryReader;
    private Video summaryReaderVideo;

    static {
        suppressStdOutput();
        I18n.setLocale(new Locale("en", "US"));
        I18nStr.localeChanged();
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

    private Main(SplashScreen splashScreen) throws Exception {
        gui = new GUI(this, splashScreen);
    }

    public static void init() {
        try {
            initialize();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            JOptionPane.showMessageDialog(null, ExceptionUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
            IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
            System.exit(-1);
        }
    }

    private static void initialize() throws Exception {
        AppUpdater.install();

        final SplashScreen splashScreen = new SplashScreen();
        splashScreen.setVisible(true);
        final Rectangle initialSplashScreenBounds = splashScreen.getBounds();

        final Main main = new Main(splashScreen);
        final GUI gui = (GUI) main.gui;
        Connection.init(gui);

        for (int i = 0; i < Constant.MAX_SUBDIRECTORIES; i++) {
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
                releaseSingleInstance();
                Connection.clearCache();
                Magnet.stopAzureus();
            }
        });

        removeTempFiles();
        Connection.startStatusBar();

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                main.updateStarted(true);
            }
        }, Calendar.getInstance().getTime(), 43200000);

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
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
                splashScreen.setVisible(false);
                gui.maximize();
                gui.setInitialFocus();
                gui.startPosterCacher();
                gui.showFeed(true);
                splashScreen.dispose();
                mainFrame = gui;
            }
        });

        (new Thread() {
            @Override
            public void run() {
                // Warm and clean cache
                for (File file : IO.listFiles(Constant.CACHE_DIR)) {
                    if (file.isDirectory()) {
                        file.listFiles();
                    } else {
                        IO.fileOp(file, IO.RM_FILE);
                    }
                }
            }
        }).start();

        Magnet.initIpFilter();
        VideoPlayer.install();
    }

    private static void singleInstance() {
        try {
            if (showMainFrame()) {
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
                Debug.print(e);
            }
        }
        try {
            Naming.rebind(RemoteSingleInstance.NAME, new RemoteSingleInstance());
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    private static boolean showMainFrame() {
        try {
            ((RemoteInstance) Naming.lookup(RemoteSingleInstance.NAME)).showMainFrame();
            if (Debug.DEBUG) {
                Debug.println("Only 1 instance of " + Constant.APP_TITLE + " can run.");
            }
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

    private static boolean isWorkDone(AbstractSwingWorker worker) {
        return worker == null || worker.isWorkDone();
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
    public boolean isLinkProgressDone() {
        return torrentFinder == null || torrentFinder.isLinkProgressDone();
    }

    @Override
    public void regularSearchStarted(int numResultsPerSearch, boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres,
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
    public String getSafetyComments() {
        return VideoFinder.getComments();
    }

    @Override
    public void summarySearchStarted(int row, Video video, VideoStrExportListener strExportListener) {
        if (isSummarySearchDone()) {
            summaryReaderVideo = new Video(video.ID, video.title, video.year, video.IS_TV_SHOW, video.IS_TV_SHOW_AND_MOVIE);
            startPrefetcher(summaryFinder = new VideoFinder(gui, ContentType.SUMMARY, row, video, strExportListener, false));
            summaryFinder.execute();
        }
    }

    @Override
    public void trailerSearchStarted(int row, Video video, VideoStrExportListener strExportListener) {
        if (isTrailerSearchDone()) {
            startPrefetcher(trailerFinder = new VideoFinder(gui, ContentType.TRAILER, row, video, strExportListener, false));
            trailerFinder.execute();
        }
    }

    @Override
    public void torrentSearchStarted(ContentType contentType, int row, Video video, VideoStrExportListener strExportListener, boolean play) {
        if (isTorrentSearchDone()) {
            Magnet.startAzureus(gui);
            startPrefetcher(torrentFinder = new VideoFinder(gui, contentType, row, video, strExportListener, play));
            torrentFinder.execute();
        }
    }

    @Override
    public void proxyListDownloadStarted() {
        if (isWorkDone(proxyDownloader)) {
            (proxyDownloader = new ProxyListDownloader(gui)).execute();
        }
    }

    @Override
    public void summaryReadStarted(String summary) {
        if (isWorkDone(summaryReader) && summaryReaderVideo != null) {
            summaryReaderVideo.summary = summary;
            (summaryReader = new SummaryReader(gui, summaryReaderVideo)).execute();
        }
    }

    @Override
    public void summaryReadStopped() {
        stop(summaryReader);
    }

    @Override
    public void subtitleSearchStarted(String format, String languageID, Video video, boolean firstMatch, VideoStrExportListener strExportListener) {
        if (isWorkDone(subtitleFinder)) {
            (subtitleFinder = new SubtitleFinder(gui, format, languageID, video, firstMatch, strExportListener)).execute();
        }
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
    }

    @Override
    public void stream(String magnetLink, String name) {
    }

    @Override
    public FormattedNum playlistItemSize(long size) {
        return null;
    }

    @Override
    public FormattedNum playlistItemProgress(double progress) {
        return null;
    }

    @Override
    public PlaylistItem playlistItem(String groupID, String uri, File groupFile, int groupIndex, String name, boolean isFirstVersion) {
        return null;
    }

    @Override
    public synchronized void changeLocale(Locale locale) {
        I18n.setLocale(locale);
        I18nStr.localeChanged();
        Magnet.localeChanged();
    }

    @Override
    public void license(String activationCode, boolean check) {
    }

    private void startPrefetcher(VideoFinder videoFinder) {
        if (prefetcher == null) {
            (prefetcher = new Prefetcher(videoFinder)).execute();
        } else if (!prefetcher.isForRow(videoFinder.ROW) || (!prefetcher.isForContentType(videoFinder.CONTENT_TYPE) && !prefetcher.isWorkDone())) {
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
            ModClass.mod(Constant.PROGRAM_DIR + "lib" + Constant.FILE_SEPARATOR + "libs" + Constant.JAR, new ModClass(rootPaneUIClass, new byte[]{0, 2, 4, -84, 0},
                    new byte[]{0, 2, 3, -84, 0}), new ModClass(rootPaneUIClass + anonymousInnerClass1, new byte[]{77, 16, 16, 96, -75},
                            new byte[]{77, 16, 0, 96, -75}), new ModClass(classNamePrefix + "LookAndFeel" + anonymousInnerClass1, new byte[]{0, 29, -103, 0, 6},
                            new byte[]{0, 29, -102, 0, 6}));
            UIManager.put("Synthetica.text.antialias", true);
            UIManager.put("Synthetica.menuItem.toolTipEnabled", true);
            UIManager.put("Synthetica.translucency4DisabledIcons.enabled", true);
            UIManager.put("Synthetica.translucency4DisabledIcons.alpha", 25);
            SyntheticaLookAndFeel.setLookAndFeel("de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e2) {
                if (Debug.DEBUG) {
                    Debug.print(e2);
                }
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

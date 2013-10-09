package main;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import debug.Debug;
import gui.AbstractSwingWorker;
import gui.GUI;
import gui.SplashScreen;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import listener.GuiListener;
import listener.WorkerListener;
import proxy.ProxyListDownloader;
import search.PopularSearcher;
import search.Prefetcher;
import search.RegularSearcher;
import search.SubtitleFinder;
import search.SummaryReader;
import search.download.CommentsFinder;
import search.download.VideoFinder;
import torrent.Magnet;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.ModClass;

public class Main implements WorkerListener {

    private static File appLockFile;
    private static FileChannel appLockFileChannel;
    private static FileLock appLockFileLock;
    private static Thread releaseSingleInstanceShutdownHook;
    private final GuiListener gui;
    private Updater updater;
    private RegularSearcher regularSearcher;
    private PopularSearcher popularSearcher;
    private VideoFinder summaryFinder, trailerFinder, torrentFinder, streamFinder;
    private ProxyListDownloader proxyDownloader;
    private SubtitleFinder subtitleFinder;
    private Prefetcher prefetcher;
    private SummaryReader summaryReader;
    private String summaryReaderTitleID, summaryReaderTitle, summaryReaderYear;

    static {
        suppressStdOutput();
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
            JOptionPane.showMessageDialog(null, ExceptionUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
            IO.writeToErrorLog(e);
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
        Connection.setGuiListener(gui);
        Str.setGuiListener(gui);
        AppUpdater.setGuiListener(gui);
        Magnet.setGuiListener(gui);

        for (int i = 0; i < Constant.MAX_SUBDIRECTORIES; i++) {
            IO.fileOp(Constant.CACHE_DIR + i, IO.MK_DIR);
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.removeShutdownHook(releaseSingleInstanceShutdownHook);
        runtime.addShutdownHook(new Thread() {
            @Override
            public void run() {
                gui.saveUserSettings();
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
                } else if ((splashScreen.getExtendedState() & JFrame.ICONIFIED) == JFrame.ICONIFIED) {
                    gui.setExtendedState(gui.getExtendedState() | JFrame.ICONIFIED);
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
            }
        });

        (new Thread() {
            @Override
            public void run() {
                // Warm cache
                File[] files = (new File(Constant.CACHE_DIR)).listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        file.listFiles();
                    } else {
                        IO.fileOp(file, IO.RM_FILE);
                    }
                }
            }
        }).start();

        if (!(new File(Constant.APP_DIR + Constant.CONNECTIVITY)).exists()) {
            (new ConnectionTester(gui)).execute();
        }

        Magnet.initIPs();
    }

    private static void singleInstance() {
        try {
            appLockFile = new File(Constant.APP_DIR + "lock");
            if (appLockFile.exists()) {
                IO.fileOp(appLockFile, IO.RM_FILE);
            }
            appLockFileChannel = new RandomAccessFile(appLockFile, "rw").getChannel();
            appLockFileLock = appLockFileChannel.tryLock();
            if (appLockFileLock == null) {
                IO.close(appLockFileChannel);
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
    }

    static void removeTempFiles() {
        IO.rmDir(new File(Constant.TORRENTS_DIR));
        IO.rmDir(new File(Constant.TEMP_DIR));
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

    private static void stop(AbstractSwingWorker worker) {
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

    @Override
    public boolean isStreamSearchDone() {
        return isWorkDone(streamFinder);
    }

    private boolean areSearchersDone() {
        return isWorkDone(regularSearcher) && isWorkDone(popularSearcher);
    }

    @Override
    public boolean areWorkersDone() {
        return isSummarySearchDone() && isTrailerSearchDone() && isTorrentSearchDone() && isStreamSearchDone() && areSearchersDone();
    }

    @Override
    public boolean isLinkProgressDone() {
        return (torrentFinder == null || torrentFinder.isLinkProgressDone()) && (streamFinder == null || streamFinder.isLinkProgressDone());
    }

    @Override
    public void regularSearchStarted(int numResultsPerSearch, boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres,
            String[] languages, String[] countries, String minRating) {
        if (!areSearchersDone()) {
            return;
        }
        regularSearcher = new RegularSearcher(gui, numResultsPerSearch, isTVShow, startDate, endDate, title, genres, languages, countries, minRating);
        stopPrefetcher();
        regularSearcher.execute();
    }

    @Override
    public void searchStopped(boolean isRegularSearcher) {
        stop(isRegularSearcher ? regularSearcher : popularSearcher);
    }

    @Override
    public void torrentAndStreamSearchStopped() {
        stop(torrentFinder);
        stop(streamFinder);
    }

    @Override
    public void loadMoreSearchResults(boolean isRegularSearcher) {
        if (!areSearchersDone()) {
            return;
        }
        if (isRegularSearcher) {
            (regularSearcher = new RegularSearcher(regularSearcher)).execute();
        } else {
            (popularSearcher = new PopularSearcher(popularSearcher)).execute();
        }
    }

    @Override
    public void popularSearchStarted(int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed, boolean startAsap) {
        if (!areSearchersDone()) {
            return;
        }
        popularSearcher = new PopularSearcher(gui, numResultsPerSearch, isTVShow, languages, countries, isFeed, startAsap);
        stopPrefetcher();
        popularSearcher.execute();
    }

    @Override
    public String getSafetyComments() {
        return torrentFinder == null ? CommentsFinder.NO_COMMENTS : torrentFinder.getComments();
    }

    @Override
    public void summarySearchStarted(int action, String titleID, String title, String summaryLink, String imageLink, boolean isLink, String year, boolean isTVShow,
            boolean isTVShowAndMovie, String season, String episode, int row) {
        if (!isSummarySearchDone()) {
            return;
        }
        summaryReaderTitleID = titleID;
        summaryReaderTitle = title;
        summaryReaderYear = year;
        startPrefetcher(summaryFinder = new VideoFinder(gui, action, titleID, title, summaryLink, imageLink, isLink, year, isTVShow, isTVShowAndMovie, season,
                episode, row));
        summaryFinder.execute();
    }

    @Override
    public void trailerSearchStarted(int action, String titleID, String title, String summaryLink, boolean isLink, String year, boolean isTVShow,
            boolean isTVShowAndMovie, String season, String episode, int row) {
        if (!isTrailerSearchDone()) {
            return;
        }
        startPrefetcher(trailerFinder = new VideoFinder(gui, action, titleID, title, summaryLink, null, isLink, year, isTVShow, isTVShowAndMovie, season, episode,
                row));
        trailerFinder.execute();
    }

    @Override
    public void torrentSearchStarted(int action, String titleID, String title, String summaryLink, boolean isLink, String year, boolean isTVShow,
            boolean isTVShowAndMovie, String season, String episode, int row) {
        if (!isTorrentSearchDone()) {
            return;
        }
        Magnet.startAzureus();
        startPrefetcher(torrentFinder = new VideoFinder(gui, action, titleID, title, summaryLink, null, isLink, year, isTVShow, isTVShowAndMovie, season, episode,
                row));
        torrentFinder.execute();
    }

    @Override
    public void streamSearchStarted(int action, String titleID, String title, String summaryLink, boolean isLink, String year, boolean isTVShow,
            boolean isTVShowAndMovie, String season, String episode, int row) {
        if (!isStreamSearchDone()) {
            return;
        }
        startPrefetcher(streamFinder = new VideoFinder(gui, action, titleID, title, summaryLink, null, isLink, year, isTVShow, isTVShowAndMovie, season, episode,
                row));
        streamFinder.execute();
    }

    @Override
    public void proxyListDownloadStarted() {
        if (!isWorkDone(proxyDownloader)) {
            return;
        }
        (proxyDownloader = new ProxyListDownloader(gui)).execute();
    }

    @Override
    public void summaryReadStarted(String summary) {
        if (!isWorkDone(summaryReader)) {
            return;
        }
        (summaryReader = new SummaryReader(gui, summaryReaderTitleID, summaryReaderTitle, summaryReaderYear, summary)).execute();
    }

    @Override
    public void summaryReadStopped() {
        stop(summaryReader);
    }

    @Override
    public void subtitleSearchStarted(String format, String languageID, String titleID, String title, String year, boolean isTVShow, boolean isTVShowAndMovie,
            String season, String episode, boolean firstMatch) {
        if (!isWorkDone(subtitleFinder)) {
            return;
        }
        (subtitleFinder = new SubtitleFinder(gui, format, languageID, titleID, title, year, isTVShow, isTVShowAndMovie, season, episode, firstMatch)).execute();
    }

    @Override
    public void subtitleSearchStopped() {
        stop(subtitleFinder);
    }

    @Override
    public void updateStarted(boolean silent) {
        if (!isWorkDone(updater)) {
            return;
        }
        (updater = new Updater(gui, silent)).execute();
    }

    private void startPrefetcher(VideoFinder videoFinder) {
        if (prefetcher == null) {
            (prefetcher = new Prefetcher(videoFinder)).execute();
        } else if (!prefetcher.isForRow(videoFinder.row) || (!prefetcher.isForAction(videoFinder.action) && !prefetcher.isWorkDone())) {
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
                    new byte[]{0, 2, 3, -84, 0}), new ModClass(rootPaneUIClass + anonymousInnerClass1, new byte[]{17, 16, 16, 96, -75},
                    new byte[]{17, 16, 0, 96, -75}), new ModClass(classNamePrefix + "LookAndFeel" + anonymousInnerClass1, new byte[]{0, 29, -103, 0, 6},
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
}

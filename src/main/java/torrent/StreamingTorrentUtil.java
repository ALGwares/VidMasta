package torrent;

import com.biglybt.core.CoreFactory;
import com.biglybt.core.disk.DiskManagerFileInfo;
import com.biglybt.core.disk.DiskManagerFileInfoSet;
import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.download.DownloadManagerEnhancer;
import com.biglybt.core.download.DownloadManagerStats;
import com.biglybt.core.download.EnhancedDownloadManager;
import com.biglybt.core.global.GlobalManager;
import com.biglybt.core.global.GlobalManagerEvent;
import com.biglybt.core.global.GlobalManagerEventListener;
import com.biglybt.core.torrent.TOTorrent;
import com.biglybt.core.torrent.TOTorrentFile;
import com.biglybt.core.util.ByteFormatter;
import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.core.util.TimeFormatter;
import com.biglybt.core.util.TorrentUtils;
import debug.Debug;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import listener.FormattedNum;
import listener.GuiListener;
import listener.PlaylistItem;
import listener.Video;
import listener.WorkerListener;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.mutable.MutableLong;
import str.Str;
import util.AbstractWorker.StateValue;
import util.Constant;
import util.IO;
import util.MediaPlayer;
import util.Regex;
import util.ThrowableUtil;
import util.Worker;

public class StreamingTorrentUtil {

  private static GuiListener guiListener;
  private static WorkerListener workerListener;
  private static String wideSpace;
  private static final Object lock = new Object(), lock2 = new Object(), lock3 = new Object();
  private static final int _40_5KB = 41472, _270KB = 276480, _540KB = 552960, _200MB = 209715200, _1000MB = 1048576000;
  private static final int _30DAYS_IN_SECS = 2592000;
  private static volatile PlaylistTorrentItem currPlaylistItem;
  private static File currSaveDir;
  private static Collection<File> currExcludedFiles;
  private static DiskManagerFileInfoSet currFileInfoSet;
  private static volatile Field canStream;
  private static volatile Thread player;
  private static final BlockingDeque<PlaylistTorrentItem> playlist = new LinkedBlockingDeque<PlaylistTorrentItem>();
  private static final AtomicBoolean canAutoOpenPlaylistItem = new AtomicBoolean(true);

  public static void init(GuiListener gui, WorkerListener worker) {
    synchronized (lock) {
      guiListener = gui;
      workerListener = worker;
      wideSpace = gui.wideSpace();
    }
  }

  public static void startPlayer() {
    synchronized (lock) {
      if (player != null) {
        return;
      }
      DownloadManagerEnhancer.initialise(CoreFactory.getSingleton());
      (player = new Thread() {
        @Override
        public void run() {
          try {
            while (true) {
              currPlaylistItem = playlist.take();
              refreshPlaylist();
              currPlaylistItem.start();
            }
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.println("player stopped: " + e);
            }
          }
        }
      }).start();
    }
  }

  public static void stopPlayer() {
    if (player != null) {
      try {
        playlist.clear();
        player.interrupt();
        CoreFactory.getSingleton().getGlobalManager().stopAllDownloads();
        rmSkippedFiles();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
  }

  public static void stream(final Video video, String name, boolean autoStart, final String... searchArgs) {
    String uri = IO.writeListToBase64(new ArrayList<Object>(Arrays.asList(video.id, video.title, video.year, video.isTVShow, video.isTVShowAndMovie,
            video.season, video.episode, video.oldTitle, searchArgs)));
    try {
      final PlaylistTorrentItem playlistItem = new PlaylistTorrentItem(null, uri, uri, null, 0) {
        @Override
        public boolean canPlay() {
          return !isActive();
        }

        @Override
        public void play(boolean force) {
          if (isActive()) {
            return;
          }

          (streamer = new Worker() {
            @Override
            protected void doWork() {
              Future<?> searcher = (searchArgs.length == 2 ? workerListener.subtitleSearchStarted(searchArgs[0], searchArgs[1], video)
                      : workerListener.torrentSearchStarted(video));
              try {
                setProgress(-1.0, Str.str("searching") + wideSpace + wideSpace + wideSpace);
                while (!searcher.isDone()) {
                  Thread.sleep(333);
                  if (searcher.isDone()) {
                    return;
                  }
                  setProgress(-1.0, Str.str("searching") + '.' + wideSpace + wideSpace);
                  Thread.sleep(333);
                  if (searcher.isDone()) {
                    return;
                  }
                  setProgress(-1.0, Str.str("searching") + ".." + wideSpace);
                  Thread.sleep(334);
                  if (searcher.isDone()) {
                    return;
                  }
                  setProgress(-1.0, Str.str("searching") + "...");
                }
              } catch (Exception e) {
                if (Debug.DEBUG) {
                  Debug.println(e);
                }
              } finally {
                searcher.cancel(true);
                setProgress(-1.0, "");
                guiListener.refreshPlaylistControls();
              }
            }
          }).execute();
          guiListener.refreshPlaylistControls();
        }

        @Override
        public void stop() {
          if (isActive()) {
            streamer.cancel(true);
            guiListener.refreshPlaylistControls();
          }
        }

        @Override
        public boolean canBan() {
          return false;
        }
      };
      Object[] item = guiListener.makePlaylistRow(name, size(-1L), progress(-1.0, ""), playlistItem);

      synchronized (lock2) {
        if (autoStart) {
          guiListener.showPlaylist();
        }
        guiListener.newPlaylistItem(item, -1);
        if (autoStart) {
          guiListener.selectPlaylistItem(playlistItem);
        }
      }
    } catch (Exception e) {
      guiListener.error(e);
    } finally {
      guiListener.refreshPlaylistControls();
    }
  }

  public static void stream(final String magnetLink, final String name, final boolean autoStart) {
    if (!Regex.isMatch(magnetLink, 766)) {
      Iterator<?> videoIt = IO.readListFromBase64(magnetLink).iterator();
      Video video = new Video((String) videoIt.next(), (String) videoIt.next(), (String) videoIt.next(), (Boolean) videoIt.next(), (Boolean) videoIt.next());
      video.season = (String) videoIt.next();
      video.episode = (String) videoIt.next();
      video.oldTitle = (String) videoIt.next();
      stream(video, name, autoStart, videoIt.hasNext() ? (String[]) videoIt.next() : Constant.EMPTY_STRS /* Backward compatibility */);
      return;
    }

    startPlayer();
    (new Worker() {
      @Override
      protected void doWork() {
        try {
          int playlistItemRow;
          PlaylistTorrentItem playlistItem = null;
          PlaylistTorrentItem tempPlaylistItem = new PlaylistTorrentItem(null, magnetLink, Base64.encodeBase64String(magnetLink.getBytes(Constant.UTF8)),
                  null, 0) {
            @Override
            public boolean isStoppable() {
              return false;
            }
          };
          Object[] item = guiListener.makePlaylistRow(name, size(-1L), progress(-1.0, Str.str("initializing") + wideSpace + wideSpace + wideSpace),
                  tempPlaylistItem);

          synchronized (lock2) {
            if (autoStart) {
              guiListener.showPlaylist();
            }
            playlistItemRow = guiListener.newPlaylistItem(item, -1);
            if (autoStart) {
              guiListener.selectPlaylistItem(tempPlaylistItem);
            }
            if (playlistItemRow < 0) {
              return;
            }
          }

          (playlistItem = tempPlaylistItem).setStreamer(this);

          Magnet magnet;
          while (true) {
            (magnet = new Magnet(magnetLink)).start();
            while (magnet.isAlive() && !playlistItem.isStopped()) {
              initializing(playlistItem);
            }
            if (playlistItem.isStopped()) {
              return;
            }
            if (magnet.torrent.exists()) {
              break;
            }
            Thread.sleep(333);
          }

          guiListener.removePlaylistItem(playlistItem);

          stream(magnet.torrent, name, -1, playlistItemRow, autoStart);
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          playlistError(e, "playingProblem", name);
        } finally {
          done();
          guiListener.refreshPlaylistControls();
        }
      }
    }).execute();
  }

  public static Worker torrentReloader(final PlaylistItem playlistItem) {
    return new Worker() {
      @Override
      protected void doWork() {
        try {
          stream(playlistItem.groupFile(), playlistItem.name(), playlistItem.groupIndex(), -1, false);
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          playlistError(e, "playingProblem", playlistItem.name());
        } finally {
          done();
          guiListener.refreshPlaylistControls();
        }
      }
    };
  }

  private static void stream(File torrent, String name, int groupIndex, int playlistItemRow, boolean autoStart) throws Exception {
    TOTorrent toTorrent = TorrentUtils.readFromFile(torrent, false);
    TOTorrentFile[] torrentFiles;
    if (toTorrent == null || (torrentFiles = toTorrent.getFiles()) == null || torrentFiles.length == 0) {
      guiListener.playlistError(Str.str("nothingToPlay", name));
      return;
    }

    List<TOTorrentFile> files = new ArrayList<TOTorrentFile>(Arrays.asList(torrentFiles));
    Collections.sort(files, new Comparator<TOTorrentFile>() {
      @Override
      public int compare(TOTorrentFile file1, TOTorrentFile file2) {
        return file1.getRelativePath().compareToIgnoreCase(file2.getRelativePath());
      }
    });

    String localisedTorrentName = TorrentUtils.getLocalisedName(toTorrent) + Constant.FILE_SEPARATOR, groupID = Base64.encodeBase64String(toTorrent.getHash());
    long largestSize = Long.MIN_VALUE;
    PlaylistItem playlistItem = null;
    int playlistItemIndex = -1;
    List<Object[]> items = new ArrayList<Object[]>(torrentFiles.length);

    String namePrefix;
    if (groupIndex == -1) {
      namePrefix = Regex.replaceFirst(name, guiListener.invisibleSeparator() + ".*+", "") + Constant.FILE_SEPARATOR;
    } else {
      namePrefix = "";
      for (TOTorrentFile file : files) {
        if (file.getIndex() == groupIndex) {
          String filePath = file.getRelativePath();
          if (name.endsWith(filePath)) {
            namePrefix = name.substring(0, name.length() - filePath.length());
          }
          break;
        }
      }
    }

    for (TOTorrentFile file : files) {
      long size = file.getLength();
      String filePath = file.getRelativePath();
      PlaylistTorrentItem tempPlaylistItem = new PlaylistTorrentItem(torrent, localisedTorrentName + filePath, groupID, namePrefix + filePath,
              file.getIndex());
      items.add(guiListener.makePlaylistRow(tempPlaylistItem.name, size(size), progress(0.0, ""), tempPlaylistItem));
      if (size > largestSize) {
        largestSize = size;
        playlistItem = tempPlaylistItem;
        playlistItemIndex = items.size() - 1;
      }
    }

    if (playlistItem == null) {
      guiListener.playlistError(Str.str("nothingToPlay", name));
      return;
    }

    if (autoStart) {
      guiListener.showPlaylist();
    }
    boolean isPlaylistItemNew = guiListener.newPlaylistItems(items, playlistItemRow, playlistItemIndex);
    if (autoStart && guiListener.selectPlaylistItem(playlistItem) && isPlaylistItemNew) {
      playlistItem.play(false);
    }
  }

  static void stream(PlaylistTorrentItem playlistItem) throws Exception {
    GlobalManager globalManager = CoreFactory.getSingleton().getGlobalManager();
    DownloadManager downloadManager;
    EnhancedDownloadManager enhancedDownloadManager;
    String savePath = guiListener.getPlaylistSaveDir();
    File saveDir = new File(savePath.isEmpty() ? Constant.DESKTOP_DIR : savePath);
    synchronized (lock) {
      if ((downloadManager = globalManager.addDownloadManager(playlistItem.torrent.getPath(), saveDir.getPath())) == null
              || (enhancedDownloadManager = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(downloadManager)) == null) {
        throw new Exception("null download manager");
      }
    }
    if (!playlistItem.groupID.equals(Base64.encodeBase64String(downloadManager.getTorrent().getHash()))) {
      throw new Exception("download manager (" + downloadManager.getTorrentFileName() + ") is not for torrent: " + playlistItem.torrent);
    }

    try {
      File saveLocation = downloadManager.getAbsoluteSaveLocation().getParentFile();
      if (!saveDir.getCanonicalPath().equals(saveLocation.getCanonicalPath()) && downloadManager.canMoveDataFiles()) {
        downloadManager.moveDataFiles(saveDir);
        downloadManager.copyDataFiles(saveLocation, null);
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    DiskManagerFileInfoSet fileInfoSet = downloadManager.getDiskManagerFileInfoSet();
    DiskManagerFileInfo[] fileInfos = fileInfoSet.getFiles();
    if (playlistItem.groupIndex >= fileInfos.length) {
      if (Debug.DEBUG) {
        Debug.println(playlistItem.name + " is not in the list of " + fileInfos.length + " files for " + downloadManager.getTorrentFileName());
      }
      throw new Exception("internal download error");
    }

    playlistItem.item = fileInfos[playlistItem.groupIndex];
    try {
      stream(playlistItem, fileInfoSet, downloadManager, enhancedDownloadManager);
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      playlistError(e, "playingProblem", playlistItem.name);
    } finally {
      playlistItem.setProgress(progress(playlistItem.item), Str.str("stopping"));
      downloadManager.stopIt(DownloadManager.STATE_STOPPED, false, false);
      playlistItem.setProgress(progress(playlistItem.item), "");
      if (Debug.DEBUG) {
        Debug.println("Playback of " + playlistItem.name + " stopped at " + progress(progress(playlistItem.item), ""));
      }
      rmSkippedFiles();
    }
  }

  private static void rmSkippedFiles() {
    synchronized (lock3) {
      if (currFileInfoSet == null || currExcludedFiles == null || currSaveDir == null) {
        return;
      }

      for (DiskManagerFileInfo fileInfo : currFileInfoSet.getFiles()) {
        File file;
        if (fileInfo.isSkipped() && fileInfo.getDownloaded() == 0 && !currExcludedFiles.contains(file = fileInfo.getFile(false))) {
          IO.fileOp(file, IO.RM_FILE);
        }
      }
      IO.rmEmptyDirs(currSaveDir, currExcludedFiles);

      currSaveDir = null;
      currExcludedFiles = null;
      currFileInfoSet = null;
    }
  }

  private static void stream(PlaylistTorrentItem playlistItem, DiskManagerFileInfoSet fileInfoSet, DownloadManager downloadManager,
          EnhancedDownloadManager enhancedDownloadManager) throws Exception {
    playlistItem.setProgress(-1.0, Str.str("initializing") + wideSpace + wideSpace + wideSpace);

    synchronized (lock3) {
      currExcludedFiles = IO.listAllFiles(currSaveDir = downloadManager.getAbsoluteSaveLocation());
      currExcludedFiles.add(currSaveDir);
      currFileInfoSet = fileInfoSet;
    }

    downloadManager.stopIt(DownloadManager.STATE_STOPPED, false, false);

    boolean[] indexesToChange = new boolean[fileInfoSet.getFiles().length];
    Arrays.fill(indexesToChange, true);
    fileInfoSet.setSkipped(indexesToChange, true);
    Arrays.fill(indexesToChange, false);
    indexesToChange[playlistItem.groupIndex] = true;
    fileInfoSet.setSkipped(indexesToChange, false);

    downloadManager.getDownloadState().clearResumeData();
    downloadManager.initialize();
    downloadManager.setForceStart(true);

    boolean isRechecked = false;
    DownloadManagerStats stats = downloadManager.getStats();
    int state;

    while ((state = downloadManager.getState()) != DownloadManager.STATE_READY) {
      if (Debug.DEBUG) {
        Debug.println(debugStatus(downloadManager, playlistItem.item));
      }
      if (playlistItem.isStopped()) {
        return;
      }

      if (state == DownloadManager.STATE_ERROR) {
        if (isRechecked) {
          throw new Exception(downloadManager.getErrorDetails());
        }
        isRechecked = true;
        downloadManager.stopIt(DownloadManager.STATE_STOPPED, false, false);
        downloadManager.getGlobalManager().addEventListener(new GlobalManagerEventListener() {
          @Override
          public void eventOccurred(GlobalManagerEvent evt) {
            if (evt.getEventType() == GlobalManagerEvent.ET_RECHECK_COMPLETE) {
              evt.getDownload().initialize();
              evt.getDownload().setForceStart(true);
              downloadManager.getGlobalManager().removeEventListener(this);
            }
          }
        });
        downloadManager.forceRecheck();
      }

      if (state == DownloadManager.STATE_CHECKING) {
        playlistItem.setProgress(stats.getCompleted() / (double) 1000, Str.str("initializing"));
        Thread.sleep(1000);
      } else {
        initializing(playlistItem);
      }
    }

    DiskManagerFileInfo[] fileInfos = fileInfoSet.getFiles();
    Collection<PlaylistTorrentItem> playlistItems = new ArrayList<PlaylistTorrentItem>(fileInfos.length);
    for (DiskManagerFileInfo fileInfo : fileInfos) {
      PlaylistTorrentItem currPlaylistTorrentItem = new PlaylistTorrentItem(null, null, playlistItem.groupID, null, fileInfo.getIndex());
      currPlaylistTorrentItem.item = fileInfo;
      updateItemAndProgress(currPlaylistTorrentItem);
      playlistItems.add(currPlaylistTorrentItem);
    }

    boolean isDownloadComplete, isStarted = false, isPlaying = false, isOpen = false;
    long sizeInBytes = playlistItem.item.getLength(), minBufferBytes = 30L * (sizeInBytes < _200MB ? _40_5KB : (sizeInBytes < _1000MB ? _270KB : _540KB));
    MutableLong prevDownloadRate = new MutableLong(Long.MAX_VALUE);

    while (!(isDownloadComplete = downloadManager.isDownloadComplete(false)) && !playlistItem.isStopped()) {
      if (Debug.DEBUG) {
        Debug.println(debugStatus(downloadManager, playlistItem.item));
      }
      if ((state = downloadManager.getState()) == DownloadManager.STATE_READY) {
        downloadManager.startDownload();
        if (!isStarted) {
          isStarted = true;
          synchronized (lock) {
            if (!enhancedDownloadManager.getProgressiveMode()) {
              enhancedDownloadManager.setExplicitProgressive(_30DAYS_IN_SECS, _200MB, playlistItem.groupIndex);
              if (canStream == null) {
                canStream = DownloadManagerEnhancer.class.getDeclaredField("progressive_enabled");
                canStream.setAccessible(true);
              }
              canStream.setBoolean(DownloadManagerEnhancer.getSingleton(), true);
              if (!enhancedDownloadManager.setProgressiveMode(true)) {
                guiListener.playlistError(Str.str("playFailure", playlistItem.name));
                return;
              }
            }
          }
        }
      } else if (state == DownloadManager.STATE_ERROR) {
        Exception e = new Exception(downloadManager.getErrorDetails());
        if (isOpen) {
          playlistError(e, "bufferingProblem", playlistItem.name);
          return;
        }
        throw e;
      }

      double progress = progress(playlistItem.item);
      if (isStarted) {
        long playETA;
        if (isPlaying) {
          playlistItem.setProgress(progress, Str.str("buffering") + eta(stats.getSmoothedETA()));
        } else if ((playETA = playETA(playlistItem.item, stats, minBufferBytes, prevDownloadRate)) <= 0) {
          isPlaying = true;
          if (canAutoOpenPlaylistItem()) {
            play(playlistItem, progress);
            isOpen = true;
          } else {
            playlistItem.setProgress(progress, Str.str("buffering") + eta(stats.getSmoothedETA()));
          }
        } else {
          playlistItem.setProgress(progress, Str.str("play" + (canAutoOpenPlaylistItem() ? "ing" : "able"), eta(playETA).trim()).trim());
        }
      } else {
        playlistItem.setProgress(progress, Str.str("buffering"));
      }

      for (PlaylistTorrentItem currPlaylistTorrentItem : playlistItems) {
        if (!playlistItem.equals(currPlaylistTorrentItem)) {
          updateItemAndProgress(currPlaylistTorrentItem);
        }
      }

      Thread.sleep(1000);
    }

    if (isDownloadComplete && !playlistItem.isStopped() && !isPlaying && canAutoOpenPlaylistItem()) {
      play(playlistItem, progress(playlistItem.item));
      Thread.sleep(1000);
    }
  }

  private static boolean canAutoOpenPlaylistItem() {
    return canAutoOpenPlaylistItem.get() && guiListener.canAutoOpenPlaylistItem();
  }

  private static void updateItemAndProgress(PlaylistTorrentItem playlistTorrentItem) {
    for (PlaylistTorrentItem currPlaylistTorrentItem : playlist) {
      if (currPlaylistTorrentItem.equals(playlistTorrentItem)) {
        if (currPlaylistTorrentItem.item == null) {
          currPlaylistTorrentItem.item = playlistTorrentItem.item;
        }
        break;
      }
    }
    playlistTorrentItem.setProgress();
  }

  private static long playETA(DiskManagerFileInfo fileInfo, DownloadManagerStats stats, long minBufferBytes, MutableLong prevDownloadRate) {
    long downloadRate = stats.getSmoothedDataReceiveRate();
    if (downloadRate <= 0) {
      if ((downloadRate = prevDownloadRate.longValue()) == Long.MAX_VALUE) {
        return Long.MAX_VALUE;
      }
    } else {
      prevDownloadRate.setValue(downloadRate);
    }
    long remainingBuffer = minBufferBytes - fileInfo.getDownloaded();
    return remainingBuffer <= 0 ? 0 : remainingBuffer / downloadRate;
  }

  private static void initializing(PlaylistTorrentItem playlistItem) throws InterruptedException {
    Thread.sleep(333);
    playlistItem.setProgress(-1.0, Str.str("initializing") + '.' + wideSpace + wideSpace);
    Thread.sleep(333);
    playlistItem.setProgress(-1.0, Str.str("initializing") + ".." + wideSpace);
    Thread.sleep(334);
    playlistItem.setProgress(-1.0, Str.str("initializing") + "...");
  }

  private static String eta(long etaSecs) {
    String eta = TimeFormatter.format(etaSecs).trim();
    return eta.isEmpty() ? "" : ' ' + eta;
  }

  private static void play(PlaylistTorrentItem playlistItem, double progress) {
    playlistItem.setProgress(progress, Str.str("playing2"));
    if (Debug.DEBUG) {
      Debug.println("Starting playback of " + playlistItem.name);
    }
    canAutoOpenPlaylistItem.set(false);
    playlistItem.open();
  }

  private static double progress(DiskManagerFileInfo fileInfo) {
    long sizeInBytes = fileInfo.getLength();
    return sizeInBytes == 0 ? 1.0 : fileInfo.getDownloaded() / (double) sizeInBytes;
  }

  static void refreshPlaylist() {
    int position = 0;
    for (PlaylistTorrentItem playlistItem : playlist) {
      playlistItem.setProgress(Str.str("queued") + " #" + (++position));
    }
  }

  static void playlistError(Throwable t, String msgKey, String msgReplacement) {
    guiListener.playlistError(Str.str(msgKey, msgReplacement) + Constant.STD_NEWLINE2 + ThrowableUtil.toString(ThrowableUtil.rootCause(t)));
  }

  private static class PlaylistTorrentItem implements PlaylistItem {

    final File torrent;
    private final String uri;
    final String groupID, name;
    final int groupIndex;
    private final AtomicBoolean isStopped = new AtomicBoolean();
    volatile Worker streamer;
    volatile DiskManagerFileInfo item;

    PlaylistTorrentItem(File torrent, String uri, String groupID, String name, int groupIndex) {
      this.torrent = torrent;
      this.uri = uri;
      this.groupID = groupID;
      this.name = name;
      this.groupIndex = groupIndex;
    }

    @Override
    public boolean canPlay() {
      return torrent != null && (isStopped() || !isActive());
    }

    @Override
    public boolean isActive() {
      return streamer != null && streamer.getState() != StateValue.DONE;
    }

    @Override
    public boolean isStoppable() {
      return true;
    }

    @Override
    public void play(boolean force) {
      if (torrent == null || isActive()) {
        return;
      }

      try {
        TOTorrent toTorrent = TorrentUtils.readFromFile(torrent, false);
        for (TOTorrentFile file : toTorrent.getFiles()) {
          if (Regex.isMatch(file.getRelativePath(), 775)) {
            guiListener.playlistError(Str.str("cannotPlayFake", TorrentUtils.getLocalisedName(toTorrent)));
            return;
          }
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }

      setProgress(Str.str("queued"));
      isStopped.set(false);
      streamer = new Worker() {
        @Override
        protected void doWork() {
          try {
            stream(PlaylistTorrentItem.this);
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
            playlistError(e, "playingProblem", name);
          } finally {
            done();
            if (playlist.isEmpty()) {
              canAutoOpenPlaylistItem.set(true);
            }
            guiListener.refreshPlaylistControls();
          }
        }
      };
      guiListener.refreshPlaylistControls();
      if (force) {
        playlist.addFirst(this);
        PlaylistItem playlistItem = currPlaylistItem;
        if (playlistItem != null && !equals(playlistItem)) {
          playlistItem.stop();
        }
      } else {
        playlist.add(this);
      }
      refreshPlaylist();
    }

    @Override
    public void stop() {
      if (isStopped() || !isActive()) {
        return;
      }

      boolean pending = (streamer != null && streamer.getState() == StateValue.PENDING);
      if (pending && playlist.contains(this)) {
        setProgress("");
      }
      isStopped.set(true);
      if (playlist.remove(this)) {
        refreshPlaylist();
        if (pending) {
          streamer = null;
        }
        if (playlist.isEmpty()) {
          canAutoOpenPlaylistItem.set(true);
        }
      }
      guiListener.refreshPlaylistControls();
    }

    void setProgress() {
      setProgress(progress(item), "", true);
    }

    private void setProgress(String status) {
      setProgress(item == null ? 0.0 : progress(item), status);
    }

    void setProgress(double progress, String status) {
      setProgress(progress, status, false);
    }

    void setProgress(double progress, String status, boolean updateVal) {
      guiListener.setPlaylistItemProgress(progress(progress, status), this, updateVal);
    }

    void setStreamer(Worker worker) {
      streamer = worker;
      guiListener.refreshPlaylistControls();
    }

    void start() throws InterruptedException {
      if (streamer != null) {
        streamer.execute();
        try {
          streamer.get();
        } catch (ExecutionException e) {
          if (Debug.DEBUG) {
            Debug.print(ThrowableUtil.cause(e));
          }
        }
      }
    }

    boolean isStopped() {
      return isStopped.get();
    }

    @Override
    public boolean canOpen() {
      return torrent != null;
    }

    @Override
    public void open() {
      if (!canOpen()) {
        return;
      }

      String savePath;
      final File file = (item == null ? new File((savePath = guiListener.getPlaylistSaveDir()).isEmpty() ? Constant.DESKTOP_DIR : savePath, uri)
              : item.getFile(false));
      if (!file.exists()) {
        guiListener.playlistError(Str.str("openFailure", name));
        return;
      }

      (new Worker() {
        @Override
        protected void doWork() {
          if (guiListener.canPlayWithDefaultApp() || !MediaPlayer.open(692, file, false, false)) {
            try {
              IO.open(file);
            } catch (Exception e) {
              guiListener.playlistError(ThrowableUtil.toString(e));
            }
          }
        }
      }).execute();
    }

    @Override
    public String groupID() {
      return groupID;
    }

    @Override
    public String uri() {
      return uri;
    }

    @Override
    public String link() {
      if (torrent == null) {
        return uri;
      }
      try {
        return "magnet:?xt=urn:btih:" + ByteFormatter.nicePrint(TorrentUtils.readFromFile(torrent, false).getHash()).replace(" ", "").toLowerCase(
                Locale.ENGLISH);
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        return "file://" + torrent.getPath();
      }
    }

    @Override
    public File groupFile() {
      return torrent;
    }

    @Override
    public int groupIndex() {
      return groupIndex;
    }

    @Override
    public Long groupDownloadID() {
      String torrentName;
      return torrent == null ? Str.hashCode(uri) : Long.parseLong((torrentName = torrent.getName()).substring(0, torrentName.lastIndexOf('.')));
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public boolean canBan() {
      return true;
    }

    @Override
    public boolean equals(Object obj) {
      PlaylistTorrentItem playlistItem;
      return this == obj || (obj instanceof PlaylistTorrentItem && groupID.equals((playlistItem = (PlaylistTorrentItem) obj).groupID)
              && groupIndex == playlistItem.groupIndex);
    }

    @Override
    public int hashCode() {
      int hash = 7 * 31 + (groupID == null ? 0 : groupID.hashCode());
      return hash * 31 + groupIndex;
    }
  }

  public static FormattedNum size(long size) {
    return new AbstractNum(size, size < 0 ? "" : DisplayFormatters.formatByteCountToKiBEtc(size)) {
      @Override
      public FormattedNum copy(Number newVal) {
        return null;
      }
    };
  }

  public static FormattedNum progress(double progress, final String status) {
    boolean noProgress = (Double.compare(progress, 0) < 0);
    return new AbstractNum(noProgress ? 0.0 : progress, noProgress ? status : Str.percent(progress, 1) + (status.isEmpty() ? "" : " - " + status)) {
      @Override
      public FormattedNum copy(Number newVal) {
        return progress(newVal.doubleValue(), status);
      }
    };
  }

  public static PlaylistItem playlistItem(String groupID, String uri, File groupFile, int groupIndex, String name, boolean isFirstVersion) {
    return new PlaylistTorrentItem(groupFile, isFirstVersion ? uri + Constant.FILE_SEPARATOR + name : uri, groupID, name, groupIndex);
  }

  private abstract static class AbstractNum implements FormattedNum {

    private final Number val;
    private final String name;

    protected AbstractNum(Number val, String name) {
      this.val = val;
      this.name = name;
    }

    @Override
    public Number val() {
      return val;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      AbstractNum num;
      return this == obj || (obj instanceof AbstractNum && val.equals((num = (AbstractNum) obj).val) && name.equals(num.name));
    }

    @Override
    public int hashCode() {
      int hash = 7 * 31 + (val == null ? 0 : val.hashCode());
      return hash * 31 + (name == null ? 0 : name.hashCode());
    }
  }

  private StreamingTorrentUtil() {
  }

  private static String debugStatus(DownloadManager downloadManager, DiskManagerFileInfo fileInfo) {
    StringBuilder status = new StringBuilder(64);
    boolean checking = false;
    switch (downloadManager.getState()) {
      case DownloadManager.STATE_WAITING:
        status.append("Waiting");
        break;
      case DownloadManager.STATE_INITIALIZED:
        status.append("Initialized");
        break;
      case DownloadManager.STATE_READY:
        status.append("Ready");
        break;
      case DownloadManager.STATE_ALLOCATING:
        status.append("Allocating");
        break;
      case DownloadManager.STATE_CHECKING:
        status.append("Checking");
        checking = true;
        break;
      case DownloadManager.STATE_DOWNLOADING:
        status.append("Downloading");
        break;
      case DownloadManager.STATE_SEEDING:
        status.append("Seeding");
        break;
      case DownloadManager.STATE_STOPPED:
        status.append("Stopped");
        break;
      case DownloadManager.STATE_ERROR:
        status.append("Error: ").append(downloadManager.getErrorDetails());
        break;
      default:
        status.append("State: ").append(downloadManager.getState());
        break;
    }
    status.append(" C:");
    status.append(progress(checking ? downloadManager.getStats().getCompleted() / (double) 1000 : progress(fileInfo), ""));
    status.append(" S:");
    status.append(downloadManager.getNbSeeds());
    status.append(" P:");
    status.append(downloadManager.getNbPeers());
    status.append(" DS:");
    status.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(downloadManager.getStats().getSmoothedDataReceiveRate()));
    status.append(" US:");
    status.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(downloadManager.getStats().getSmoothedDataSendRate()));
    return status.toString();
  }
}

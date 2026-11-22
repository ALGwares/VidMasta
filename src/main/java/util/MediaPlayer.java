package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import listener.DomainType;
import str.Str;

public class MediaPlayer {

  private static final File MEDIA_PLAYER_DIR = new File(Constant.APP_DIR, "mediaPlayer"), MEDIA_PLAYER_INDICATOR = new File(Constant.APP_DIR, Str.get(697));
  private static final File FFMPEG_DIR = new File(Constant.APP_DIR, "ffmpeg"), FFMPEG_INDICATOR = new File(Constant.APP_DIR, Str.get(899));
  // ffmpeg (Str 900's build) only runs on 64-bit Windows. Without it, the downloader leaves the video and audio in separate files, which the media player plays
  // together.
  private static final boolean CAN_USE_FFMPEG = Constant.IS_64BIT_WINDOWS;
  private static final Cache<String, Boolean> failedHosts = CacheBuilder.newBuilder().expireAfterWrite(Constant.MS_1HR, TimeUnit.MILLISECONDS).build();
  private static final long MAX_OPENING_NANOS = TimeUnit.SECONDS.toNanos(30);
  // Whether the downloader gets Str 983's arguments, which make it trust other certificates: the Windows certificate store's instead of its own, which fail
  // when antivirus HTTPS scanning re-signs websites, while Windows' can lack a website's root certificate. It's switched when a download fails because of
  // the certificates, for that download's retry and later downloads.
  private static final AtomicBoolean useAltCertificates = new AtomicBoolean();

  public static void install() {
    Str.waitForUpdate();
    if ((!Boolean.parseBoolean(Str.get(692)) && !Boolean.parseBoolean(Str.get(704))) || !Constant.WINDOWS_XP_AND_HIGHER) {
      return;
    }

    if (!MEDIA_PLAYER_INDICATOR.exists()) {
      String zipFile = MEDIA_PLAYER_INDICATOR.getPath() + Constant.ZIP;
      try {
        Connection.saveData(Str.get(761), zipFile, DomainType.UPDATE, false);
        try {
          IO.fileOp(MEDIA_PLAYER_DIR, IO.RM_DIR);
          IO.unzip(zipFile, IO.dir(MEDIA_PLAYER_DIR.getPath()));
          IO.fileOp(MEDIA_PLAYER_INDICATOR, IO.MK_FILE);
        } finally {
          IO.fileOp(zipFile, IO.RM_FILE);
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }

    if (MEDIA_PLAYER_INDICATOR.exists()) {
      if (CAN_USE_FFMPEG && !FFMPEG_INDICATOR.exists()) {
        String zipFile = FFMPEG_INDICATOR.getPath() + Constant.ZIP;
        try {
          Connection.saveData(Str.get(900), zipFile, DomainType.UPDATE, false);
          try {
            IO.fileOp(FFMPEG_DIR, IO.RM_DIR);
            IO.unzip(zipFile, IO.dir(FFMPEG_DIR.getPath()));
            IO.fileOp(FFMPEG_INDICATOR, IO.MK_FILE);
          } finally {
            IO.fileOp(zipFile, IO.RM_FILE);
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
      }
      try {
        File downloader = new File(Constant.APP_DIR, Str.get(Constant.IS_64BIT_WINDOWS ? 820 : 821)), tempDownloader = new File(Constant.APP_DIR,
                downloader.getName() + ".part");
        if (!downloader.exists() || tempDownloader.exists() || IO.isFileTooOld(downloader, Constant.MS_2DAYS)) {
          Connection.saveData(Str.get(Constant.IS_64BIT_WINDOWS ? 822 : 823), tempDownloader.getPath(), DomainType.UPDATE, false);
          IO.write(tempDownloader, downloader);
          IO.fileOp(tempDownloader, IO.RM_FILE);
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
  }

  public static boolean open(int canOpenIndex, File file, boolean playAndExit, boolean startMinimized) {
    return open(canOpenIndex, file, null, playAndExit, startMinimized);
  }

  // Opens a video, with the audio of another file if audio isn't null
  private static boolean open(int canOpenIndex, File video, File audio, boolean playAndExit, boolean startMinimized) {
    String filePath;
    return canOpen(canOpenIndex) && (filePath = video.getPath()).length() < 255 && (audio == null || audio.getPath().length() < 255) && Regex.isMatch(
            video.getName(), 698) && open(filePath, audio, playAndExit, startMinimized, null, null, null, 0, false);
  }

  public static boolean open(int canOpenIndex, String url, int quality, String title, Runnable errorAction) {
    return canOpen(canOpenIndex) && open(url, null, false, false, quality, title, errorAction, 0, false);
  }

  private static boolean canOpen(int canOpenIndex) {
    return Boolean.parseBoolean(Str.get(canOpenIndex)) && Constant.WINDOWS_XP_AND_HIGHER && MEDIA_PLAYER_INDICATOR.exists();
  }

  private static boolean open(String location, File audio, boolean playAndExit, boolean startMinimized, Integer quality, String title,
          Runnable errorAction, int retryCount, boolean isCertificateRetry) {
    try {
      String host = Connection.getShortUrl(location, false);
      if (errorAction != null && Boolean.TRUE.equals(failedHosts.getIfPresent(host))) {
        errorAction.run();
        return true;
      }

      String host2;
      boolean timedOut = false;
      if (Boolean.parseBoolean(Str.get(824)) && Regex.isMatch(location, Str.get(825)) && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2 = host + '2'))
              && (new File(Constant.APP_DIR, Str.get(Constant.IS_64BIT_WINDOWS ? 820 : 821))).exists()) {
        List<String> args = new ArrayList<>(8);
        File downloadDir = new File(Constant.TEMP_DIR, String.valueOf(System.currentTimeMillis()));
        Collections.addAll(args, (new File(Constant.APP_DIR, Str.get(Constant.IS_64BIT_WINDOWS ? 820 : 821))).getPath(), location);
        Collections.addAll(args, Regex.split(901, Constant.SEPARATOR1));
        boolean isAltCertificates = useAltCertificates.get();
        if (isAltCertificates) {
          Collections.addAll(args, Regex.split(983, Constant.SEPARATOR1));
        }
        Collections.addAll(args, Str.get(827), String.format(Str.get(828), downloadDir.getPath() + Constant.FILE_SEPARATOR));
        File ffmpeg;
        if (CAN_USE_FFMPEG && FFMPEG_INDICATOR.exists() && (ffmpeg = IO.findFile(FFMPEG_DIR, Regex.pattern(903))) != null) {
          Collections.addAll(args, Str.get(902), ffmpeg.getPath());
        }
        Collections.addAll(args, Str.get(904), String.format(Str.get(retryCount == 0 ? (quality != null && quality != -1 ? 905 : 906) : 907), quality));
        ProcessBuilder downloaderBuilder = new ProcessBuilder(args);
        downloaderBuilder.redirectErrorStream(true);
        Process downloader = downloaderBuilder.start();
        AtomicBoolean retry = new AtomicBoolean(), isCertificateError = new AtomicBoolean(), isConverting = new AtomicBoolean();
        List<String> downloadNameEnds = new CopyOnWriteArrayList<>(); // Of the downloaded files, in order: such as the video's and then the audio's
        Worker outputReader = Worker.submit(() -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(downloader.getInputStream(), Constant.UTF8))) {
            String line;
            while ((line = br.readLine()) != null) {
              if (Debug.DEBUG) {
                Debug.println((new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aaa")).format(new Date()) + ": " + line);
              }
              if (!Regex.firstMatch(line, 982).isEmpty()) {
                isCertificateError.set(true);
                downloader.destroy();
                return;
              }
              if (!Regex.firstMatch(line, 893).isEmpty() && retryCount < Integer.parseInt(Str.get(894))) {
                retry.set(true); // yt-dlp downloader is a little buggy and sometimes fails non-deterministically so retry
                failedHosts.invalidate(host2);
                downloader.destroy();
                return;
              }
              if (!Regex.firstMatch(line, 831).isEmpty()) {
                if (!Boolean.TRUE.equals(failedHosts.getIfPresent(host2))) {
                  failedHosts.put(host2, true);
                }
                downloader.destroy();
                return;
              }
              if (!isConverting.get() && Regex.isMatch(line, 981)) { // ffmpeg converts (or merges) what was downloaded
                isConverting.set(true);
              }
              // Such as .f140.m4a. Only the end, because the rest of the name can lose characters (such as from the title) that the output's encoding lacks.
              String downloadNameEnd = Regex.firstMatch(line, 984);
              if (!downloadNameEnd.isEmpty() && !downloadNameEnds.contains(downloadNameEnd)) {
                downloadNameEnds.add(downloadNameEnd);
              }
            }
          }
        });

        try {
          String shortUrl = Connection.getShortUrl(location, true);
          Connection.setStatusBar(Str.str("transferring") + ' ' + shortUrl);
          Supplier<Stream<File>> downloads = () -> Arrays.stream(IO.listFiles(downloadDir)).filter(file -> Regex.firstMatch(file.getName(), 897).isEmpty());
          int i = 0, j = Integer.parseInt(Str.get(832)), numBytes = Integer.parseInt(Str.get(833));
          boolean isConversionShown = false;
          for (; i < j && downloads.get().noneMatch(file -> file.length() >= numBytes) && !retry.get() && !isCertificateError.get()
                  && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2)) && downloader.isAlive(); i++) {
            if (!isConversionShown && isConverting.get()) {
              isConversionShown = true;
              Connection.setStatusBar(Str.str("converting") + ' ' + shortUrl);
            }
            Thread.sleep(50);
          }
          if (!downloader.isAlive()) {
            try {
              outputReader.get(1, TimeUnit.SECONDS); // Its last lines can tell why the downloader exited
            } catch (ExecutionException | TimeoutException e) {
              if (Debug.DEBUG) {
                Debug.print(e);
              }
            }
          }
          if (isCertificateError.get() && !isCertificateRetry) {
            downloader.destroy();
            useAltCertificates.compareAndSet(isAltCertificates, !isAltCertificates);
            if (Debug.DEBUG) {
              Debug.println("retrying " + location + " with the " + (isAltCertificates ? "default" : "alternative") + " certificates");
            }
            return open(location, audio, playAndExit, startMinimized, quality, title, errorAction, retryCount, true);
          }
          if (retry.get()) {
            downloader.destroy();
            failedHosts.invalidate(host2);
            return open(location, audio, playAndExit, startMinimized, quality, title, errorAction, retryCount + 1, isCertificateRetry);
          }
          if (!Boolean.TRUE.equals(failedHosts.getIfPresent(host2))) {
            Optional<File> download = downloads.get().findFirst();
            boolean isOpen;
            if (download.isPresent()) {
              isOpen = open(834, download.get(), null, playAndExit, startMinimized);
            } else { // Without ffmpeg (or if merging failed), the downloader leaves the video and audio in separate files, which can be played together
              List<File> videoAndAudio = (downloader.isAlive() ? Collections.<File>emptyList() : unmergedDownloads(downloadDir, downloadNameEnds));
              isOpen = false;
              if (videoAndAudio.size() == 2) {
                if (Debug.DEBUG) {
                  Debug.println("opening the unmerged video " + videoAndAudio.get(0) + " with the audio " + videoAndAudio.get(1));
                }
                isOpen = open(834, videoAndAudio.get(0), videoAndAudio.get(1), playAndExit, startMinimized);
              }
            }
            if (isOpen && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2))) {
              return true;
            }
          }
          if (timedOut = (i == j && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2)))) {
            if (Debug.DEBUG) {
              Debug.println("\n" + host + " downloader timed out for " + location + "\n");
            }
          } else {
            failedHosts.put(host2, true);
          }
          downloader.destroy();
        } finally {
          Connection.unsetStatusBar();
        }
      }

      List<String> args = new ArrayList<String>(16);
      File oldMediaPlayerDir;
      String language = Str.locale().getISO3Language();
      Collections.addAll(args, IO.findFile(MEDIA_PLAYER_DIR.exists() ? MEDIA_PLAYER_DIR : ((oldMediaPlayerDir = new File(Constant.APP_DIR, Str.get(
              762))).exists() ? oldMediaPlayerDir : new File(Constant.APP_DIR)), Regex.pattern(763)).getPath(), location, "--audio-language=" + language,
              "--sub-language=" + language);
      if (audio != null) { // As a URI, since # separates input slaves
        args.add("--input-slave=" + audio.toPath().toUri().toASCIIString());
      }
      Collections.addAll(args, Regex.split(815, Constant.SEPARATOR1));
      if (Boolean.parseBoolean(Str.get(819)) && errorAction != null && failedHosts.getIfPresent(host) == null) {
        failedHosts.put(host, false);
        List<String> testArgs = new ArrayList<>(args);
        Collections.addAll(testArgs, Regex.split(816, Constant.SEPARATOR1));
        boolean error;
        Process mediaPlayer = null;
        try {
          mediaPlayer = open(testArgs, host, true, true, quality, title, true, () -> {
          });
          Connection.setStatusBar(Str.str("transferring") + ' ' + Connection.getShortUrl(location, true));
          error = mediaPlayer.waitFor() != 0;
        } catch (InterruptedException e) {
          failedHosts.invalidate(host);
          throw e;
        } finally {
          if (mediaPlayer != null) {
            mediaPlayer.destroy();
          }
          Connection.unsetStatusBar();
        }

        Runnable showError = () -> Worker.submit(() -> {
          try {
            Connection.setStatusBar(Str.str("mediaPlayerError", host));
            Thread.sleep(5000);
          } finally {
            Connection.unsetStatusBar();
          }
        });
        Boolean failed = failedHosts.getIfPresent(host);
        if (failed == null) {
          failedHosts.put(host, false);
        } else if (failed || error) {
          failedHosts.put(host, !timedOut);
          showError.run();
          errorAction.run();
          return true;
        }
      }

      Process mediaPlayer = open(args, host, playAndExit, startMinimized, quality, title, false, errorAction);
      if (!startMinimized) {
        awaitWindow(mediaPlayer);
      }

      return true;
    } catch (InterruptedException e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      Thread.currentThread().interrupt();
      return true;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    return false;
  }

  // The downloaded files with the given name ends (in the same order), or none if any is missing
  private static List<File> unmergedDownloads(File downloadDir, List<String> nameEnds) {
    File[] files = IO.listFiles(downloadDir);
    List<File> downloads = new ArrayList<>(nameEnds.size());
    for (String nameEnd : nameEnds) {
      Optional<File> download = Arrays.stream(files).filter(file -> file.getName().endsWith(nameEnd)).findFirst();
      if (!download.isPresent()) {
        return Collections.emptyList();
      }
      downloads.add(download.get());
    }
    return downloads;
  }

  // Shows that the media player is opening until its window is visible, since it can take a while to start (or until it exits, or a time limit passes)
  private static void awaitWindow(Process mediaPlayer) throws InterruptedException {
    Connection.setStatusBar(Str.str("openingMediaPlayer") + "...");
    try {
      int processId = WindowsUtil.processId(mediaPlayer);
      long start = System.nanoTime();
      boolean isVisible;
      while (!(isVisible = WindowsUtil.hasVisibleWindow(processId)) && mediaPlayer.isAlive() && System.nanoTime() - start < MAX_OPENING_NANOS) {
        Thread.sleep(100);
      }
      if (Debug.DEBUG) {
        Debug.println("media player " + (isVisible ? "window visible" : (mediaPlayer.isAlive() ? "window still not visible" : "exited")) + " after "
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " ms");
      }
    } catch (InterruptedException e) {
      throw e;
    } catch (Exception | LinkageError e) { // Such as when the process's ID is unavailable, so that its window can't be watched for
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      Connection.unsetStatusBar();
    }
  }

  private static Process open(List<String> args, String host, boolean playAndExit, boolean startMinimized, Integer quality, String title, boolean test,
          final Runnable errorAction) throws Exception {
    if (playAndExit) {
      args.add("--play-and-exit");
    }
    if (startMinimized) {
      args.add("--qt-start-minimized");
    }
    if (quality != null) {
      args.add("--preferred-resolution=" + quality);
    }
    if (title != null) {
      args.add("--meta-title=" + title);
    }

    ProcessBuilder mediaPlayerBuilder = new ProcessBuilder(args);
    if (errorAction != null) {
      mediaPlayerBuilder.redirectErrorStream(true);
    }
    final Process mediaPlayer = mediaPlayerBuilder.start();
    if (errorAction != null) {
      (new Worker() {
        @Override
        protected void doWork() {
          BufferedReader br = null;
          try {
            br = new BufferedReader(new InputStreamReader(mediaPlayer.getInputStream(), Constant.UTF8));
            String line;
            while ((line = br.readLine()) != null) {
              if (Debug.DEBUG) {
                Debug.println(line);
              }
              if (test && !Regex.firstMatch(line, Str.get(818)).isEmpty()) {
                failedHosts.invalidate(host);
                mediaPlayer.destroy();
                return;
              }
              if (!Regex.firstMatch(line, Str.get(817)).isEmpty()) {
                if (!Boolean.TRUE.equals(failedHosts.getIfPresent(host))) {
                  failedHosts.put(host, true);
                }
                mediaPlayer.destroy();
                errorAction.run();
                return;
              }
            }
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          } finally {
            IO.close(br);
          }
        }
      }).execute();
    }

    if (playAndExit) {
      (new Worker() {
        @Override
        protected void doWork() {
          try {
            while (mediaPlayer.isAlive()) {
              Thread.sleep(100);
            }
            for (int i = 0; i < 200 && !WindowsUtil.closeWindow("#32770", "VLC media player"); i++) {
              Thread.sleep(50);
            }
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          }
        }
      }).execute();
    }

    return mediaPlayer;
  }

  private MediaPlayer() {
  }
}

package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;
import listener.DomainType;
import str.Str;

public class MediaPlayer {

  private static final File MEDIA_PLAYER_DIR = new File(Constant.APP_DIR, "mediaPlayer"), MEDIA_PLAYER_INDICATOR = new File(Constant.APP_DIR, Str.get(697));
  private static final File FFMPEG_DIR = new File(Constant.APP_DIR, "ffmpeg"), FFMPEG_INDICATOR = new File(Constant.APP_DIR, Str.get(899));
  private static final Cache<String, Boolean> failedHosts = CacheBuilder.newBuilder().expireAfterWrite(Constant.MS_1HR, TimeUnit.MILLISECONDS).build();

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
      try {
        File script = new File(IO.findFile(MEDIA_PLAYER_DIR, Regex.pattern(789)).getParentFile(), Str.get(790)), tempScript = new File(Constant.APP_DIR,
                script.getName());
        if (!script.exists() || tempScript.exists() || IO.isFileTooOld(script, Constant.MS_1HR)) {
          Connection.saveData(Str.get(788), tempScript.getPath(), DomainType.UPDATE, false);
          IO.write(tempScript, script);
          IO.fileOp(tempScript, IO.RM_FILE);
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.println(e);
        }
      }
      if (!FFMPEG_INDICATOR.exists()) {
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
    String filePath;
    return canOpen(canOpenIndex) && (filePath = file.getPath()).length() < 255 && Regex.isMatch(file.getName(), 698) && open(filePath, playAndExit,
            startMinimized, null, null, null, 0);
  }

  public static boolean open(int canOpenIndex, String url, int quality, String title, Runnable errorAction) {
    return canOpen(canOpenIndex) && open(url, false, false, quality, title, errorAction, 0);
  }

  private static boolean canOpen(int canOpenIndex) {
    return Boolean.parseBoolean(Str.get(canOpenIndex)) && Constant.WINDOWS_XP_AND_HIGHER && MEDIA_PLAYER_INDICATOR.exists();
  }

  private static boolean open(String location, boolean playAndExit, boolean startMinimized, Integer quality, String title, Runnable errorAction,
          int retryCount) {
    try {
      String host = Connection.getShortUrl(location, false);
      if (errorAction != null && Boolean.TRUE.equals(failedHosts.getIfPresent(host))) {
        errorAction.run();
        return true;
      }

      String host2;
      if (Boolean.parseBoolean(Str.get(824)) && Regex.isMatch(location, Str.get(825)) && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2 = host + '2'))
              && (new File(Constant.APP_DIR, Str.get(Constant.IS_64BIT_WINDOWS ? 820 : 821))).exists()) {
        List<String> args = new ArrayList<>(8);
        File downloadDir = new File(Constant.TEMP_DIR, String.valueOf(System.currentTimeMillis()));
        Collections.addAll(args, (new File(Constant.APP_DIR, Str.get(Constant.IS_64BIT_WINDOWS ? 820 : 821))).getPath(), location);
        Collections.addAll(args, Regex.split(901, Constant.SEPARATOR1));
        Collections.addAll(args, Str.get(827), String.format(Str.get(828), downloadDir.getPath() + Constant.FILE_SEPARATOR));
        if (FFMPEG_INDICATOR.exists()) {
          Collections.addAll(args, Str.get(902), IO.findFile(FFMPEG_DIR, Regex.pattern(903)).getPath());
          Collections.addAll(args, Str.get(904), String.format(Str.get(retryCount == 0 ? (quality != null && quality != -1 ? 905 : 906) : 907), quality));
        }
        ProcessBuilder downloaderBuilder = new ProcessBuilder(args);
        downloaderBuilder.redirectErrorStream(true);
        Process downloader = downloaderBuilder.start();
        AtomicBoolean retry = new AtomicBoolean();
        Worker.submit(() -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(downloader.getInputStream(), Constant.UTF8))) {
            String line;
            while ((line = br.readLine()) != null) {
              if (Debug.DEBUG) {
                Debug.println(line);
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
            }
          }
        });

        try {
          Connection.setStatusBar(Str.str("transferring") + ' ' + Connection.getShortUrl(location, true));
          Supplier<Stream<File>> downloads = () -> Arrays.stream(IO.listFiles(downloadDir)).filter(file -> Regex.firstMatch(file.getName(), 897).isEmpty());
          for (int i = 0, j = Integer.parseInt(Str.get(832)), numBytes = Integer.parseInt(Str.get(833)); i < j && downloads.get().noneMatch(file -> file.length()
                  >= numBytes) && !retry.get() && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2)) && downloader.isAlive(); i++) {
            Thread.sleep(50);
          }
          if (retry.get()) {
            downloader.destroy();
            failedHosts.invalidate(host2);
            return open(location, playAndExit, startMinimized, quality, title, errorAction, retryCount + 1);
          }
          Optional<File> download;
          if (!Boolean.TRUE.equals(failedHosts.getIfPresent(host2)) && (download = downloads.get().findFirst()).isPresent() && MediaPlayer.open(834,
                  download.get(), playAndExit, startMinimized) && !Boolean.TRUE.equals(failedHosts.getIfPresent(host2))) {
            return true;
          }
          failedHosts.put(host2, true);
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
        } else if (failed) {
          showError.run();
          errorAction.run();
          return true;
        } else if (error) {
          failedHosts.put(host, true);
          showError.run();
          errorAction.run();
          return true;
        }
      }

      open(args, host, playAndExit, startMinimized, quality, title, false, errorAction);

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

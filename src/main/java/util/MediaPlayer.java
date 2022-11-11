package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import listener.DomainType;
import str.Str;

public class MediaPlayer {

  private static final File MEDIA_PLAYER_DIR = new File(Constant.APP_DIR, "mediaPlayer"), MEDIA_PLAYER_INDICATOR = new File(Constant.APP_DIR, Str.get(697));
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
          Debug.print(e);
        }
      }
    }
  }

  public static boolean open(int canOpenIndex, File file, boolean playAndExit, boolean startMinimized) {
    String filePath;
    return canOpen(canOpenIndex) && (filePath = file.getPath()).length() < 255 && Regex.isMatch(file.getName(), 698) && open(filePath, playAndExit,
            startMinimized, null, null, null);
  }

  public static boolean open(int canOpenIndex, String url, int quality, String title, Runnable errorAction) {
    return canOpen(canOpenIndex) && open(url, false, false, quality, title, errorAction);
  }

  private static boolean canOpen(int canOpenIndex) {
    return Boolean.parseBoolean(Str.get(canOpenIndex)) && Constant.WINDOWS_XP_AND_HIGHER && MEDIA_PLAYER_INDICATOR.exists();
  }

  private static boolean open(String location, boolean playAndExit, boolean startMinimized, Integer quality, String title, Runnable errorAction) {
    try {
      String host = Connection.getShortUrl(location, false);
      if (errorAction != null && Boolean.TRUE.equals(failedHosts.getIfPresent(host))) {
        errorAction.run();
        return true;
      }

      List<String> args = new ArrayList<String>(16);
      File oldMediaPlayerDir;
      String language = Str.locale().getISO3Language();
      Collections.addAll(args, IO.findFile(MEDIA_PLAYER_DIR.exists() ? MEDIA_PLAYER_DIR : ((oldMediaPlayerDir = new File(Constant.APP_DIR, Str.get(
              762))).exists() ? oldMediaPlayerDir : new File(Constant.APP_DIR)), Regex.pattern(763)).getPath(), location, "--no-one-instance",
              "--audio-language=" + language, "--sub-language=" + language, "--avi-index=2", "--no-qt-updates-notif", "--verbose=1");
      if (errorAction != null && failedHosts.getIfPresent(host) == null) {
        failedHosts.put(host, false);
        List<String> testArgs = new ArrayList<>(args);
        Collections.addAll(testArgs, "--run-time=2", "--gain=0", "--no-video", "--intf=dummy", "--qt-notification=0");
        Process mediaPlayer = open(testArgs, host, true, true, quality, title, () -> {
        });
        boolean error = mediaPlayer.waitFor() != 0;
        if (Boolean.TRUE.equals(failedHosts.getIfPresent(host))) {
          errorAction.run();
          return true;
        }
        if (error) {
          failedHosts.put(host, true);
          errorAction.run();
          return true;
        }
      }

      open(args, host, playAndExit, startMinimized, quality, title, errorAction);

      return true;
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    return false;
  }

  private static Process open(List<String> args, String host, boolean playAndExit, boolean startMinimized, Integer quality, String title,
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
              if (!Regex.firstMatch(line, Str.get(739)).isEmpty()) {
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

package util;

import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import listener.DomainType;
import str.Str;

public class MediaPlayer {

    private static final File MEDIA_PLAYER_DIR = new File(Constant.APP_DIR, "mediaPlayer");

    public static void install() {
        Str.waitForUpdate();
        if ((!Boolean.parseBoolean(Str.get(692)) && !Boolean.parseBoolean(Str.get(704))) || !Constant.WINDOWS_XP_AND_HIGHER || (new File(Constant.APP_DIR
                + Str.get(697))).exists()) {
            return;
        }

        String zipFile = Constant.APP_DIR + Str.get(697) + Constant.ZIP;
        try {
            Connection.saveData(Str.get(761), zipFile, DomainType.UPDATE, false);
            try {
                IO.fileOp(MEDIA_PLAYER_DIR, IO.RM_DIR);
                IO.unzip(zipFile, IO.dir(MEDIA_PLAYER_DIR.getPath()));
                IO.fileOp(Constant.APP_DIR + Str.get(697), IO.MK_FILE);
            } finally {
                IO.fileOp(zipFile, IO.RM_FILE);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
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
        return Boolean.parseBoolean(Str.get(canOpenIndex)) && Constant.WINDOWS_XP_AND_HIGHER && (new File(Constant.APP_DIR + Str.get(697))).exists();
    }

    private static boolean open(String location, boolean playAndExit, boolean startMinimized, Integer quality, String title, final Runnable errorAction) {
        try {
            List<String> args = new ArrayList<String>(16);
            File oldMediaPlayerDir;
            String language = Str.locale().getISO3Language();
            Collections.addAll(args, IO.findFile(MEDIA_PLAYER_DIR.exists() ? MEDIA_PLAYER_DIR : ((oldMediaPlayerDir = new File(Constant.APP_DIR, Str.get(
                    762))).exists() ? oldMediaPlayerDir : new File(Constant.APP_DIR)), Regex.pattern(763)).getPath(), location, "--no-one-instance",
                    "--audio-language=" + language, "--sub-language=" + language, "--avi-index=2");
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
                                if (!Regex.firstMatch(line, Str.get(739)).isEmpty()) {
                                    if (Debug.DEBUG) {
                                        Debug.println(line);
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

            return true;
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return false;
    }

    private MediaPlayer() {
    }
}

package util;

import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingWorker;
import listener.DomainType;
import str.Str;

public class VideoPlayer {

    public static void install() {
        Str.waitForUpdate();
        if ((!Boolean.parseBoolean(Str.get(692)) && !Boolean.parseBoolean(Str.get(704))) || !Constant.WINDOWS_XP_AND_HIGHER || (new File(Constant.APP_DIR
                + Str.get(697))).exists()) {
            return;
        }

        String zipFile = Constant.APP_DIR + Str.get(697) + Constant.ZIP;
        try {
            Connection.saveData(Str.get(693), zipFile, DomainType.UPDATE, false);
            try {
                IO.fileOp(Constant.APP_DIR + Str.get(694), IO.RM_DIR);
                IO.unzip(zipFile, Constant.APP_DIR);
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
        return canOpen(canOpenIndex) && open(url, true, false, quality, title, errorAction);
    }

    private static boolean canOpen(int canOpenIndex) {
        return Boolean.parseBoolean(Str.get(canOpenIndex)) && Constant.WINDOWS_XP_AND_HIGHER && (new File(Constant.APP_DIR + Str.get(697))).exists();
    }

    private static boolean open(String location, boolean playAndExit, boolean startMinimized, Integer quality, String title, final Runnable errorAction) {
        try {
            List<String> args = new ArrayList<String>(5);
            Collections.addAll(args, Constant.APP_DIR + Str.get(695).replace(Str.get(696), Constant.FILE_SEPARATOR), location, "--no-one-instance");
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

            ProcessBuilder videoPlayerBuilder = new ProcessBuilder(args);
            if (errorAction != null) {
                videoPlayerBuilder.redirectErrorStream(true);
            }
            final Process videoPlayer = videoPlayerBuilder.start();
            if (errorAction != null) {
                (new SwingWorker<Object, Object>() {
                    @Override
                    protected Object doInBackground() {
                        BufferedReader br = null;
                        try {
                            br = new BufferedReader(new InputStreamReader(videoPlayer.getInputStream(), Constant.UTF8));
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (!Regex.firstMatch(line, Str.get(739)).isEmpty()) {
                                    videoPlayer.destroy();
                                    errorAction.run();
                                    return null;
                                }
                            }
                        } catch (Exception e) {
                            if (Debug.DEBUG) {
                                Debug.print(e);
                            }
                        } finally {
                            IO.close(br);
                        }
                        return null;
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

    private VideoPlayer() {
    }
}

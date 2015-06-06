package util;

import debug.Debug;
import java.io.File;
import listener.DomainType;
import str.Str;

public class VideoPlayer {

    public static void install() {
        Str.waitForUpdate();
        if (!Boolean.parseBoolean(Str.get(692)) || !Constant.WINDOWS_XP_AND_HIGHER || (new File(Constant.APP_DIR + Str.get(697))).exists()) {
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

    public static boolean open(File file) {
        try {
            String filePath;
            if (Boolean.parseBoolean(Str.get(692)) && Constant.WINDOWS_XP_AND_HIGHER && (new File(Constant.APP_DIR + Str.get(697))).exists()
                    && (filePath = file.getPath()).length() < 255 && Regex.isMatch(file.getName(), 698)) {
                (new ProcessBuilder(Constant.APP_DIR + Str.get(695).replace(Str.get(696), Constant.FILE_SEPARATOR), filePath)).start();
                return true;
            }
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

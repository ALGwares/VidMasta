package main;

import debug.Debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;
import listener.DomainType;
import listener.GuiListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.Regex;
import util.UpdateException;

class AppUpdater {

    private static final String APP_UPDATE = "appUpdate" + Constant.TXT, INSTALL = "install.xml";
    private String[] appUpdateStrs;
    static final String APP_UPDATE_FAIL = "updateFail" + Constant.APP_VERSION;

    static void install() {
        try {
            installHelper();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, ExceptionUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
        }
    }

    private static void installHelper() {
        if ((new File(Constant.APP_DIR + APP_UPDATE_FAIL)).exists()) {
            return;
        }
        File update = new File(Constant.APP_DIR + APP_UPDATE);
        if (!update.exists()) {
            return;
        }

        String[] updateStrs;
        try {
            updateStrs = Regex.split(IO.read(update), Constant.SEPARATOR1);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            IO.fileOp(update, IO.RM_FILE_NOW_AND_ON_EXIT);
            return;
        }

        String[] cmd = Regex.split(updateStrs[0], Constant.SEPARATOR2);
        File installer = new File(cmd[cmd.length - 1]), installerScript = new File(Constant.APP_DIR + INSTALL);

        try {
            if (Long.parseLong(updateStrs[1]) != IO.checksum(installer)) {
                throw new UpdateException("auto-setup installer is corrupt");
            }
            IO.write(Constant.APP_DIR + INSTALL, updateStrs[2].replace(Str.get(347), Str.get(348) + IO.parentDir(Constant.PROGRAM_DIR) + Str.get(349)));

            StringBuilder installCmd = new StringBuilder(128);
            for (String cmdPart : cmd) {
                installCmd.append(cmdPart).append(Constant.NEWLINE);
            }
            installCmd.append(Constant.APP_DIR).append(INSTALL);
            restart(installCmd.toString(), Regex.firstMatch(installer.getName(), "\\d++\\.\\d++"), Constant.APP_DIR + APP_UPDATE + Constant.NEWLINE
                    + Constant.APP_DIR + INSTALL + Constant.NEWLINE + cmd[cmd.length - 1]);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            IO.fileOp(update, IO.RM_FILE_NOW_AND_ON_EXIT);
            IO.fileOp(installerScript, IO.RM_FILE_NOW_AND_ON_EXIT);
            IO.fileOp(installer, IO.RM_FILE_NOW_AND_ON_EXIT);
        }
    }

    void update(boolean showConfirmation, GuiListener guiListener) {
        try {
            if (appUpdateStrs == null) {
                appUpdateStrs = Regex.split(Connection.getUpdateFile(Str.get(295), showConfirmation), Constant.NEWLINE);
            }
            if (!Regex.isMatch(appUpdateStrs[0], "\\d++\\.\\d++")) {
                throw new UpdateException("invalid application update version file");
            }
            if (Constant.APP_VERSION < Double.parseDouble(appUpdateStrs[0])) {
                guiListener.updateMsg("<html><head><title></title></head><body><font face=\"tahoma\" size=\"4\">A <a href=\"" + (Constant.WINDOWS
                        ? appUpdateStrs[1] : appUpdateStrs[2]) + "\">newer version</a> (" + appUpdateStrs[0]
                        + ") of the application is available.</font></body></html>");
            } else if (showConfirmation) {
                guiListener.msg("The application (version " + Constant.APP_VERSION + ") is already up to date.", Constant.INFO_MSG);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (showConfirmation) {
                guiListener.msg("There was an error checking for the newest version of the application: " + ExceptionUtil.toString(e), Constant.ERROR_MSG);
            }
        }
    }

    void update(GuiListener guiListener) {
        try {
            if ((new File(Constant.APP_DIR + APP_UPDATE_FAIL)).exists()) {
                throw new UpdateException("application update installation failed");
            }

            appUpdateStrs = Regex.split(Connection.getUpdateFile(Str.get(295), false), Constant.NEWLINE);
            if (!Regex.isMatch(appUpdateStrs[0], "\\d++\\.\\d++")) {
                throw new UpdateException("invalid application update version file");
            }

            double newAppVersion = Double.parseDouble(appUpdateStrs[0]);
            if (Constant.APP_VERSION >= newAppVersion || (new File(Constant.APP_DIR + APP_UPDATE)).exists()) {
                return;
            }

            String installerLink, installerChecksum, installerSuffix;
            if (Constant.WINDOWS) {
                installerLink = appUpdateStrs[3];
                installerChecksum = appUpdateStrs[4];
                installerSuffix = Constant.EXE;
            } else {
                installerLink = appUpdateStrs[5];
                installerChecksum = appUpdateStrs[6];
                installerSuffix = Constant.JAR;
            }

            String installer = Constant.APP_DIR + "auto-vidmasta-setup-" + newAppVersion + installerSuffix;
            Connection.saveData(installerLink, installer, DomainType.UPDATE, false);

            StringBuilder installerXml = new StringBuilder(512);
            for (int i = 7; i < appUpdateStrs.length; i++) {
                installerXml.append(appUpdateStrs[i]).append(Constant.NEWLINE);
            }

            IO.write(Constant.APP_DIR + APP_UPDATE, (installerSuffix.equals(Constant.JAR) ? Constant.JAVA + Constant.SEPARATOR2 + Constant.JAR_OPTION
                    + Constant.SEPARATOR2 : "") + installer + Constant.SEPARATOR1 + installerChecksum + Constant.SEPARATOR1 + installerXml.toString().trim());
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            Connection.updateError(e);
            if (guiListener.canUpdate()) {
                update(false, guiListener);
            }
        }
    }

    private static void restart(final String... args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                List<String> cmd = new ArrayList<String>(16);
                Collections.addAll(cmd, Constant.JAVA, Constant.JAR_OPTION, Constant.PROGRAM_DIR + "updater" + Constant.JAR, Constant.JAVA + Constant.NEWLINE
                        + Constant.JAR_OPTION + Constant.NEWLINE + Constant.PROGRAM_DIR + Constant.PROGRAM_JAR, Constant.APP_TITLE,
                        String.valueOf(System.currentTimeMillis()), Str.get(350), Str.get(351));
                Collections.addAll(cmd, args);
                try {
                    (new ProcessBuilder(cmd)).start();
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    IO.fileOp(Constant.APP_DIR + APP_UPDATE_FAIL, IO.MK_FILE);
                }
            }
        });
        System.exit(0);
    }
}

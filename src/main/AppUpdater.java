package main;

import debug.Debug;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import listener.GuiListener;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.Regex;
import util.UpdateException;
import util.io.Read;
import util.io.Write;

class AppUpdater {

    private static GuiListener guiListener;
    private static final String APP_UPDATE = "appUpdate" + Constant.TXT, INSTALL = "install.xml";
    private String[] appUpdateStrs;
    static final String APP_UPDATE_FAIL = "updateFail" + Constant.APP_VERSION + Constant.TXT;

    static void install() {
        try {
            installHelper();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + ExceptionUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
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
            updateStrs = Regex.split(Read.read(update), Constant.SEPARATOR1);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            Write.fileOp(update, Write.RM_FILE_NOW_AND_ON_EXIT);
            return;
        }

        String[] cmd = Regex.split(updateStrs[0], Constant.SEPARATOR2);
        File installer = new File(cmd[cmd.length - 1]), installerScript = new File(Constant.APP_DIR + INSTALL);

        try {
            if (Long.parseLong(updateStrs[1]) != Read.checksum(installer)) {
                throw new UpdateException("auto-setup installer is corrupt");
            }
            String path = Constant.PROGRAM_DIR;
            String[] pathParts = Constant.FILE_SEPARATOR.compareTo("\\") == 0 ? Regex.split(path.replace('\\', '/'), "/") : Regex.split(path,
                    Constant.FILE_SEPARATOR);
            StringBuilder pathBuf = new StringBuilder(32);
            for (int i = 0; i < pathParts.length - 1; i++) {
                pathBuf.append(pathParts[i]).append(Constant.FILE_SEPARATOR);
            }
            Write.write(Constant.APP_DIR + INSTALL, updateStrs[2].replace(Str.get(347), Str.get(348) + pathBuf.toString()
                    + Str.get(349)));

            StringBuilder installCmd = new StringBuilder(128);
            for (String cmdPart : cmd) {
                installCmd.append(cmdPart).append(Constant.NEWLINE);
            }
            installCmd.append(Constant.APP_DIR).append(INSTALL);
            restart(new String[]{installCmd.toString(), Regex.match(installer.getName(), "\\d++\\.\\d++"), Constant.APP_DIR + APP_UPDATE + Constant.NEWLINE
                + Constant.APP_DIR + INSTALL + Constant.NEWLINE + cmd[cmd.length - 1]});
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            Write.fileOp(update, Write.RM_FILE_NOW_AND_ON_EXIT);
            Write.fileOp(installerScript, Write.RM_FILE_NOW_AND_ON_EXIT);
            Write.fileOp(installer, Write.RM_FILE_NOW_AND_ON_EXIT);
        }
    }

    void update(boolean showConfirmation) {
        try {
            if (appUpdateStrs == null) {
                appUpdateStrs = Regex.split(Connection.getUpdateFile(Str.get(295), showConfirmation), Constant.NEWLINE);
            }
            if (!Regex.isMatch(appUpdateStrs[0], "\\d++\\.\\d++")) {
                throw new UpdateException("invalid app update version file");
            }
            if (Constant.APP_VERSION < Double.parseDouble(appUpdateStrs[0])) {
                String link = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win") ? appUpdateStrs[1] : appUpdateStrs[2];
                guiListener.updateMsg("<html><head><title></title></head><body><font face=\"tahoma\" size=\"4\">A <a href=\"" + link + "\">newer version</a> ("
                        + appUpdateStrs[0] + ") of the application is available.</font></body></html>");
            } else if (showConfirmation) {
                guiListener.msg("The application is already up to date.", Constant.INFO_MSG);
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

    void update() {
        try {
            if ((new File(Constant.APP_DIR + APP_UPDATE_FAIL)).exists()) {
                throw new UpdateException("app update installation failed");
            }

            appUpdateStrs = Regex.split(Connection.getUpdateFile(Str.get(295), false), Constant.NEWLINE);
            if (!Regex.isMatch(appUpdateStrs[0], "\\d++\\.\\d++")) {
                throw new UpdateException("invalid app update version file");
            }

            double newAppVersion = Double.parseDouble(appUpdateStrs[0]);
            if (Constant.APP_VERSION >= newAppVersion || (new File(Constant.APP_DIR + APP_UPDATE)).exists()) {
                return;
            }

            String installerLink, installerChecksum, installerSuffix;
            if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
                installerLink = appUpdateStrs[3];
                installerChecksum = appUpdateStrs[4];
                installerSuffix = Constant.EXE;
            } else {
                installerLink = appUpdateStrs[5];
                installerChecksum = appUpdateStrs[6];
                installerSuffix = Constant.JAR;
            }

            String installer = Constant.APP_DIR + "auto-vidmasta-setup-" + newAppVersion + installerSuffix;
            Connection.saveData(installerLink, installer, Connection.UPDATE, false);

            StringBuilder installerXml = new StringBuilder(512);
            for (int i = 7; i < appUpdateStrs.length; i++) {
                installerXml.append(appUpdateStrs[i]).append(Constant.NEWLINE);
            }

            Write.write(Constant.APP_DIR + APP_UPDATE, (installerSuffix.equals(Constant.JAR) ? Constant.JAVA + Constant.SEPARATOR2 + Constant.JAR_OPTION
                    + Constant.SEPARATOR2 : "") + installer + Constant.SEPARATOR1 + installerChecksum + Constant.SEPARATOR1 + installerXml.toString().trim());
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            Updater.startUpError(e);
            if (guiListener.canUpdate()) {
                update(false);
            }
        }
    }

    private static void restart(final String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                List<String> command = new ArrayList<String>(16);
                command.add(Constant.JAVA);
                command.add(Constant.JAR_OPTION);
                command.add(Constant.PROGRAM_DIR + "updater" + Constant.JAR);
                command.add(Constant.JAVA + Constant.NEWLINE + Constant.JAR_OPTION + Constant.NEWLINE + Constant.PROGRAM_DIR + Constant.PROGRAM_JAR);
                command.add(Constant.APP_TITLE);
                command.add(String.valueOf(System.currentTimeMillis()));
                command.add(Str.get(350));
                command.add(Str.get(351));
                command.addAll(Arrays.asList(args));
                try {
                    (new ProcessBuilder(command)).start();
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    try {
                        Write.write(Constant.APP_DIR + APP_UPDATE_FAIL, "");
                    } catch (Exception e2) {
                        if (Debug.DEBUG) {
                            Debug.print(e2);
                        }
                    }
                }
            }
        });
        System.exit(0);
    }

    static void setGuiListener(GuiListener listener) {
        guiListener = listener;
    }
}

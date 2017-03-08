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
import util.IO;
import util.Regex;
import util.ThrowableUtil;
import util.UpdateException;
import util.WindowsUtil;

class AppUpdater {

    private static final String APP_UPDATE = "appUpdate" + Constant.TXT, INSTALL = "install.xml";
    public static final String APP_UPDATE_FAIL = "updateFail" + Constant.APP_VERSION;
    private String[] appUpdateStrs;

    static void install() {
        try {
            installHelper();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, ThrowableUtil.toString(e), Constant.APP_TITLE, Constant.ERROR_MSG);
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
            IO.fileOp(update, IO.RM_FILE);
            return;
        }

        String[] cmd = Regex.split(updateStrs[0], Constant.SEPARATOR2);
        File installer = new File(cmd[cmd.length - 1]), installerScript = new File(Constant.APP_DIR + INSTALL);

        try {
            if (Long.parseLong(updateStrs[1]) != IO.checksum(installer)) {
                throw new UpdateException("auto-setup installer is corrupt");
            }

            IO.write(installerScript, updateStrs[2].replace(Str.get(347), Str.get(348) + IO.parentDir(Constant.PROGRAM_DIR) + Str.get(349)));

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
            IO.fileOp(update, IO.RM_FILE);
            IO.fileOp(installerScript, IO.RM_FILE_NOW_AND_ON_EXIT);
            IO.fileOp(installer, IO.RM_FILE);
        }
    }

    void update(boolean showConfirmation, GuiListener guiListener) {
        try {
            if (appUpdateStrs == null) {
                appUpdateStrs = Regex.split(Connection.getUpdateFile(Str.get(295), showConfirmation), Constant.NEWLINE);
            }
            if (!Regex.isMatch(appUpdateStrs[0], "\\d++\\.\\d++")) {
                throw new UpdateException(Str.str("invalidAppUpdateVersionFile"));
            }
            double version = Double.parseDouble(appUpdateStrs[0]);
            if (Constant.APP_VERSION < version) {
                guiListener.updateMsg("<html><head><title></title></head><body><font face=\"tahoma\" size=\"4\">" + Str.htmlLinkStr("newAppVersion",
                        Constant.WINDOWS ? appUpdateStrs[1] : appUpdateStrs[2], Str.getNumFormat(Constant.VERSION_FORMAT).format(version))
                        + "</font></body></html>");
            } else if (showConfirmation) {
                guiListener.msg(Str.str("appUpToDate", Str.getNumFormat(Constant.VERSION_FORMAT).format(Constant.APP_VERSION)), Constant.INFO_MSG);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (showConfirmation) {
                guiListener.msg(Str.str("appUpdateError") + ' ' + ThrowableUtil.toString(e), Constant.ERROR_MSG);
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
            if (Constant.APP_VERSION >= newAppVersion) {
                return;
            }

            String installerLink, installerChecksum, installerSuffix;
            if (Constant.WINDOWS) {
                installerLink = Regex.firstMatch(appUpdateStrs[appUpdateStrs.length - 1], "(?<=\\<\\!\\-\\-).+?(?=\\-\\-\\>)"); // Workaround
                installerChecksum = appUpdateStrs[4];
                installerSuffix = Constant.EXE;
            } else {
                installerLink = appUpdateStrs[5];
                installerChecksum = appUpdateStrs[6];
                installerSuffix = Constant.JAR;
            }

            String installerPath = Constant.APP_DIR + "auto-vidmasta-setup-" + newAppVersion + installerSuffix;
            File installer = new File(installerPath);
            if (!installer.exists()) {
                Connection.saveData(installerLink, installerPath, DomainType.UPDATE, false);
            }
            if (Long.parseLong(installerChecksum) != IO.checksum(installer)) {
                IO.fileOp(installer, IO.RM_FILE);
                throw new UpdateException("auto-setup installer is corrupt");
            }

            StringBuilder installerXml = new StringBuilder(512);
            for (int i = 7; i < appUpdateStrs.length - 1; i++) { // Skip last index as part of a workaround
                installerXml.append(appUpdateStrs[i]).append(Constant.NEWLINE);
            }

            IO.write(Constant.APP_DIR + APP_UPDATE, (installerSuffix.equals(Constant.JAR) ? Constant.JAVA + Constant.SEPARATOR2 + Constant.JAR_OPTION
                    + Constant.SEPARATOR2 : "") + installerPath + Constant.SEPARATOR1 + installerChecksum + Constant.SEPARATOR1 + installerXml.toString().trim());
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
                Collections.addAll(cmd, Constant.PROGRAM_DIR + "updater" + Constant.JAR, Constant.JAVA + Constant.NEWLINE + Constant.JAR_OPTION + Constant.NEWLINE
                        + Constant.PROGRAM_DIR + Constant.PROGRAM_JAR, Constant.APP_TITLE, String.valueOf(System.currentTimeMillis()), Str.get(350), Str.get(351));
                Collections.addAll(cmd, args);
                try {
                    WindowsUtil.runJavaAsAdmin(cmd);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    IO.fileOp(Constant.APP_DIR + APP_UPDATE_FAIL, IO.MK_FILE);
                    try {
                        (new ProcessBuilder(Constant.JAVA, Constant.JAR_OPTION, Constant.PROGRAM_DIR + Constant.PROGRAM_JAR)).start();
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
}

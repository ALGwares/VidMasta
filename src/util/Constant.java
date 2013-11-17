package util;

import debug.Debug;
import java.io.File;
import java.util.Locale;
import javax.swing.JOptionPane;

public class Constant {

    public static final int ERROR_MSG = JOptionPane.ERROR_MESSAGE;
    public static final int INFO_MSG = JOptionPane.INFORMATION_MESSAGE;
    public static final int WARNING_MSG = JOptionPane.WARNING_MESSAGE;
    public static final int QUESTION_MSG = JOptionPane.QUESTION_MESSAGE;
    public static final double APP_VERSION = 16.9;
    public static final boolean CAN_PEER_BLOCK;
    public static final int MAX_SUBDIRECTORIES = 100;
    public static final String[] EMPTY_STRS = new String[0];
    public static final String TV_SHOW = "TV Show";
    public static final String IP_FILTER = "ipfilter.dat";
    public static final String PEER_BLOCK_APP_TITLE = "PeerBlock";
    public static final String PEER_BLOCK = "peerblock";
    public static final String CONNECTIVITY = "connectivity";
    public static final String STD_NEWLINE = "\n";
    public static final String NEWLINE = System.getProperty("line.separator", STD_NEWLINE);
    public static final String NEWLINE2 = NEWLINE + NEWLINE;
    public static final String UTF8 = "UTF-8";
    public static final String SEPARATOR1 = ":::", SEPARATOR2 = "~~~", SEPARATOR3 = ";;;";
    public static final String IMAGE_COL = "", ID_COL = "ID", TITLE_COL = "Title", YEAR_COL = "Year", RATING_COL = "Rating", SUMMARY_COL = "Summary";
    public static final int SUMMARY_ACTION = 0, TRAILER_ACTION = 1;
    public static final int TORRENT1_ACTION = 2, TORRENT2_ACTION = 3, TORRENT3_ACTION = 4, STREAM1_ACTION = 5, STREAM2_ACTION = 6;
    public static final String TV_EPISODE_FORMAT = "%02d";
    public static final int TV_EPISODE_PLACEHOLDER_LEN = 100;
    public static final String TV_EPISODE_PLACEHOLDER;
    public static final String HTML_FONT = "<font face=\"Verdana, Geneva, sans-serif\" size=\"4\">";
    public static final String BLANK_HTML_PAGE = "<html><head></head><body marginwidth=\"10\"><br></body></html>";
    public static final String TRUE = Boolean.TRUE.toString(), FALSE = Boolean.FALSE.toString();
    public static final String IMAGES = "images";
    public static final String TXT = ".txt", HTML = ".html", SWF = ".swf";
    public static final String DOWNLOAD_LINK_INFO_PROXY_INDEX = "torrentDbProxyIndex" + Constant.TXT;
    public static final String PROFILES = "profiles" + TXT;
    private static final String UPDATE_FILE_VERSION = "45";
    public static final String UPDATE_FILE = "update" + UPDATE_FILE_VERSION + TXT;
    public static final String UPDATE_BACKUP_FILE = "updateBackup" + UPDATE_FILE_VERSION + TXT;
    public static final int SETTINGS_LEN = 57;
    private static final String SETTINGS_VERSION = "11";
    public static final String PROFILE = "profile" + SETTINGS_VERSION + "_";
    public static final String USER_SETTINGS = "userSettings" + SETTINGS_VERSION + TXT;
    public static final String DEFAULT_SETTINGS = "defaultSettings" + SETTINGS_VERSION + TXT;
    public static final String ANY = "ANY";
    public static final String ANY_GENRE = "ANY GENRE";
    public static final String ANY_LANGUAGE = "ANY LANGUAGE";
    public static final String ANY_COUNTRY = "ANY COUNTRY";
    public static final String DVD = "DVD", HD720 = "720 HD", HD1080 = "1080 HD";
    public static final String NULL = "null";
    public static final String NO_IMAGE = "zzzzz_" + NULL;
    public static final String ZIP = ".zip";
    public static final String PROXY_VERSION = "proxyVersion" + TXT;
    public static final String PROXIES = "proxies" + TXT;
    public static final String NO_PROXY = "NO PROXY";
    public static final String INFINITY = "infinity";
    public static final String CUT = "Cut", COPY = "Copy", PASTE = "Paste";
    public static final String APP_TITLE = "VidMasta";
    public static final String EXE = ".exe", JAR = ".jar", JAR_OPTION = "-jar";
    public static final String PROGRAM_JAR = APP_TITLE + JAR;
    public static final String DEFAULT_PROFILE = "Default Profile";
    public static final String TORRENTS = "torrents";
    public static final String CONNECTING = " Connecting to ";
    public static final String TRANSFERRING = " Transferring data from ";
    public static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH);
    public static final boolean WINDOWS = OS_NAME.startsWith("win"), MAC = OS_NAME.startsWith("mac");
    private static final String WORKING_DIR = System.getProperty("user.dir", ".");
    public static final String FILE_SEPARATOR, PROGRAM_DIR, APP_DIR, CACHE_DIR, TEMP_DIR, TORRENTS_DIR;
    public static final String JAVA;

    static {
        FILE_SEPARATOR = System.getProperty("file.separator", "/");
        String java = System.getProperty("java.home", "java");
        JAVA = java + (java.equals("java") ? "" : FILE_SEPARATOR + "bin" + FILE_SEPARATOR + "java");
        PROGRAM_DIR = initProgramDir();
        APP_DIR = initAppDir();
        CACHE_DIR = APP_DIR + "cache" + FILE_SEPARATOR;
        TEMP_DIR = APP_DIR + "temp" + FILE_SEPARATOR;
        TORRENTS_DIR = APP_DIR + TORRENTS + FILE_SEPARATOR;
        CAN_PEER_BLOCK = WINDOWS && !OS_NAME.equals("windows 95") && !OS_NAME.equals("windows 98") && !OS_NAME.equals("windows me");

        String placeholderChar = "&#8203;";
        StringBuilder placeholder = new StringBuilder(placeholderChar.length() * TV_EPISODE_PLACEHOLDER_LEN);
        for (int i = 0; i < TV_EPISODE_PLACEHOLDER_LEN; i++) {
            placeholder.append(placeholderChar);
        }
        TV_EPISODE_PLACEHOLDER = placeholder.toString();
    }

    private static String initProgramDir() {
        String tempProgramDir = WORKING_DIR + FILE_SEPARATOR;
        try {
            tempProgramDir = (new File(Constant.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath() + FILE_SEPARATOR
                    + "..")).getCanonicalPath() + FILE_SEPARATOR;
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            JOptionPane.showMessageDialog(null, "There was an error locating the installation directory." + NEWLINE + "You must run " + PROGRAM_JAR
                    + " from the installation directory.", APP_TITLE, ERROR_MSG);
            try {
                tempProgramDir = (new File(WORKING_DIR)).getCanonicalPath() + FILE_SEPARATOR;
            } catch (Exception e2) {
                if (Debug.DEBUG) {
                    Debug.print(e2);
                }
            }
        }
        if (Debug.DEBUG) {
            try {
                tempProgramDir = (new File(WORKING_DIR)).getCanonicalPath() + FILE_SEPARATOR;
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
        return tempProgramDir;
    }

    private static String initAppDir() {
        String tempAppDir;
        try {
            String userHome = (new File(System.getProperty("user.home", WORKING_DIR))).getCanonicalPath() + FILE_SEPARATOR;
            if (WINDOWS) {
                tempAppDir = System.getenv("APPDATA");
                if (tempAppDir == null || tempAppDir.isEmpty()) {
                    tempAppDir = userHome + "Application Data";
                }
                tempAppDir += FILE_SEPARATOR + APP_TITLE + FILE_SEPARATOR;
            } else if (MAC) {
                tempAppDir = userHome + "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR + APP_TITLE + FILE_SEPARATOR;
            } else {
                tempAppDir = userHome + "." + APP_TITLE.toLowerCase(Locale.ENGLISH) + FILE_SEPARATOR;
            }

            IO.fileOp(tempAppDir, IO.MK_DIR);
            try {
                copyFileToAppDir(tempAppDir, UPDATE_FILE);
                copyFileToAppDir(tempAppDir, USER_SETTINGS);
                copyFileToAppDir(tempAppDir, PROXY_VERSION);
                copyFileToAppDir(tempAppDir, PROFILES);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                tempAppDir = PROGRAM_DIR;
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            tempAppDir = PROGRAM_DIR;
        }
        return tempAppDir;
    }

    private static void copyFileToAppDir(String appDir, String fileName) throws Exception {
        File file = new File(appDir + fileName);
        if (!file.exists()) {
            IO.write(file, IO.read(PROGRAM_DIR + fileName));
        }
    }

    private Constant() {
    }
}

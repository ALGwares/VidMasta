package util;

import debug.Debug;
import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

public class Constant {

    public static final int ERROR_MSG = JOptionPane.ERROR_MESSAGE;
    public static final int INFO_MSG = JOptionPane.INFORMATION_MESSAGE;
    public static final double APP_VERSION = 17.7;
    public static final boolean CAN_PEER_BLOCK;
    public static final int MAX_SUBDIRECTORIES = 100;
    public static final String[] EMPTY_STRS = new String[0];
    public static final String TV_SHOW = "TV Show";
    public static final String IP_FILTER = "ipfilter.dat";
    public static final String PEER_BLOCK_APP_TITLE = "PeerBlock";
    public static final String PEER_BLOCK = "peerblock";
    public static final String CONNECTIVITY = "connectivity";
    public static final String STD_NEWLINE = IOConstant.STD_NEWLINE, NEWLINE = IOConstant.NEWLINE, NEWLINE2 = NEWLINE + NEWLINE;
    public static final String UTF8 = IOConstant.UTF8;
    public static final String SEPARATOR1 = ":::", SEPARATOR2 = "~~~", SEPARATOR3 = ";;;";
    public static final String IMAGE_COL = "", TITLE_COL = "Title", YEAR_COL = "Year", RATING_COL = "Rating", ID_COL = "0", CURR_TITLE_COL = "1";
    public static final String OLD_TITLE_COL = "2", SUMMARY_COL = "3", IMAGE_LINK_COL = "4", IS_TV_SHOW_COL = "5", IS_TV_SHOW_AND_MOVIE_COL = "6";
    public static final String SEASON_COL = "7", EPISODE_COL = "8";
    public static final String TV_EPISODE_FORMAT = "%02d";
    public static final String ZERO_WIDTH_SPACE;
    public static final int TV_EPISODE_PLACEHOLDER_LEN = 100;
    public static final String TV_EPISODE_PLACEHOLDER, TV_EPISODE_HTML_ID, TV_EPISODE_HTML, TV_EPISODE_HTML_AND_PLACEHOLDER, TV_EPISODE_REGEX;
    public static final String TITLE_INDENT = "&nbsp;&nbsp;&nbsp;";
    public static final int TITLE_INDENT_LEN = TITLE_INDENT.length();
    public static final String HTML_FONT = "<font face=\"Verdana, Geneva, sans-serif\" size=\"4\">";
    public static final String BLANK_HTML_PAGE = "<html><head></head><body marginwidth=\"10\"><br></body></html>";
    public static final String TXT = ".txt", HTML = ".html", SWF = ".swf", TORRENT = ".torrent";
    public static final String DOWNLOAD_LINK_INFO_PROXY_INDEX = "torrentDbProxyIndex" + TXT;
    public static final String PROFILES = "profiles" + TXT;
    public static final int UPDATE_FILE_VERSION = 52;
    public static final String UPDATE_FILE = "update" + UPDATE_FILE_VERSION + TXT;
    public static final String UPDATE_BACKUP_FILE = "updateBackup" + UPDATE_FILE_VERSION + TXT;
    public static final int SETTINGS_LEN = 64;
    public static final int SETTINGS_VERSION = 11;
    public static final String PROFILE = "profile" + SETTINGS_VERSION + "_";
    public static final String USER_SETTINGS = "userSettings" + SETTINGS_VERSION + TXT;
    public static final String DEFAULT_SETTINGS = "defaultSettings" + SETTINGS_VERSION + TXT;
    public static final String ANY = "ANY";
    public static final String ANY_GENRE = "ANY GENRE";
    public static final String ANY_LANGUAGE = "ANY LANGUAGE";
    public static final String ANY_COUNTRY = "ANY COUNTRY";
    public static final String HQ = "HQ", DVD = "DVD", HD720 = "720 HD", HD1080 = "1080 HD";
    public static final String NULL = "null";
    public static final String NO_IMAGE = "zzzzz_" + NULL;
    public static final String ZIP = ".zip";
    public static final String PROXY_VERSION = "proxyVersion" + TXT;
    public static final String PROXIES = "proxies" + TXT;
    public static final String ERROR_LOG = "errorLog" + TXT;
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
    public static final String WORKING_DIR = System.getProperty("user.dir", ".");
    public static final String FILE_SEPARATOR, PROGRAM_DIR, APP_DIR, CACHE_DIR, TEMP_DIR, TORRENTS_DIR;
    public static final String JAVA;

    static {
        FILE_SEPARATOR = IOConstant.FILE_SEPARATOR;
        String java = System.getProperty("java.home", "java");
        JAVA = java + (java.equals("java") ? "" : FILE_SEPARATOR + "bin" + FILE_SEPARATOR + "java");
        PROGRAM_DIR = initProgramDir();
        APP_DIR = initAppDir();
        CACHE_DIR = APP_DIR + "cache" + FILE_SEPARATOR;
        TEMP_DIR = APP_DIR + "temp" + FILE_SEPARATOR;
        TORRENTS_DIR = APP_DIR + TORRENTS + FILE_SEPARATOR;
        CAN_PEER_BLOCK = WINDOWS && !OS_NAME.equals("windows 95") && !OS_NAME.equals("windows 98") && !OS_NAME.equals("windows me");

        int zeroWidthSpaceCharPoint = 8203;
        ZERO_WIDTH_SPACE = new String(new int[]{zeroWidthSpaceCharPoint}, 0, 1);
        String placeholderChar = "&#" + zeroWidthSpaceCharPoint + ";";
        StringBuilder placeholder = new StringBuilder(placeholderChar.length() * TV_EPISODE_PLACEHOLDER_LEN);
        for (int i = 0; i < TV_EPISODE_PLACEHOLDER_LEN; i++) {
            placeholder.append(placeholderChar);
        }
        TV_EPISODE_PLACEHOLDER = placeholder.toString();
        TV_EPISODE_HTML_ID = "nextEpisode";
        TV_EPISODE_HTML = "<b id=\"" + TV_EPISODE_HTML_ID + "\">Next Episode: </b>";
        TV_EPISODE_HTML_AND_PLACEHOLDER = TV_EPISODE_HTML + TV_EPISODE_PLACEHOLDER;
        TV_EPISODE_REGEX = latestEpisode("", "");
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
            } else {
                tempAppDir = userHome + (MAC ? "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR + APP_TITLE : "."
                        + APP_TITLE.toLowerCase(Locale.ENGLISH)) + FILE_SEPARATOR;
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

    public static String latestEpisode(String season, String episode) {
        String latestEpisode = " (Latest Episode: ";
        String rightParenthesis = ")";
        String seasonStr;
        String episodeStr;
        if (season.isEmpty()) {
            latestEpisode = Pattern.quote(latestEpisode);
            rightParenthesis = Pattern.quote(rightParenthesis);
            seasonStr = "\\d{2}+";
            episodeStr = seasonStr;
        } else {
            seasonStr = season;
            episodeStr = episode;
        }
        return latestEpisode + 'S' + seasonStr + 'E' + episodeStr + rightParenthesis;
    }

    public static String aka(String str) {
        return " (AKA: " + str + ')';
    }

    private Constant() {
    }
}

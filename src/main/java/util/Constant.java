package util;

import debug.Debug;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;

public class Constant implements IOConstant {

  public static final int ERROR_MSG = JOptionPane.ERROR_MESSAGE;
  public static final int INFO_MSG = JOptionPane.INFORMATION_MESSAGE;
  public static final int WARN_MSG = JOptionPane.WARNING_MESSAGE;
  public static final String VERSION_FORMAT = "0.0";
  public static final boolean WINDOWS_XP_AND_HIGHER, WINDOWS_VISTA_AND_HIGHER, IS_64BIT_WINDOWS;
  public static final String[] EMPTY_STRS = new String[0];
  public static final long MS_1HR = 3600000, MS_2DAYS = 172_800_000, MS_3DAYS = 259_200_000;
  public static final String TV_SHOW = "TV Show";
  public static final String IP_FILTER = "ipfilter.p2p";
  public static final String PEER_BLOCK = "peerblock", PEER_BLOCK_CONF_VERSION = "peerblockConf" + APP_VERSION, PEER_BLOCK_VERSION;
  public static final String CONNECTIVITY = "connectivity";
  public static final String SEPARATOR1 = ":::", SEPARATOR2 = "~~~", SEPARATOR3 = ";;;";
  public static final String IMAGE_COL = "", ID_COL = "0", CURR_TITLE_COL = "1";
  public static final String OLD_TITLE_COL = "2", SUMMARY_COL = "3", IMAGE_LINK_COL = "4", IS_TV_SHOW_COL = "5", IS_TV_SHOW_AND_MOVIE_COL = "6";
  public static final String PLAYLIST_ITEM_COL = "0";
  public static final String SEASON_COL = "7", EPISODE_COL = "8";
  public static final String STOP_KEY = "stop";
  public static final String TV_EPISODE_FORMAT = "%02d", RATING_FORMAT = "#.0", NO_RATING = "-";
  public static final String STORYLINE_HTML_ID = "storyline", STORYLINE_LINK1_HTML_ID = "storylineLink1", STORYLINE_LINK2_HTML_ID = "storylineLink2";
  public static final String GENRE_HTML_ID = "genre", TV_NEXT_EPISODE_HTML_ID = "nextEpisode", TV_PREV_EPISODE_HTML_ID = "prevEpisode";
  public static final String TITLE_INDENT = "&nbsp;&nbsp;&nbsp;";
  public static final int TITLE_INDENT_LEN = TITLE_INDENT.length();
  public static final String BLANK_HTML_PAGE = "<html><head></head><body marginwidth=\"10\"><br></body></html>";
  public static final String TXT = ".txt", HTML = ".html", TORRENT = ".torrent";
  public static final String DOWNLOAD_LINK_INFO_PROXY_INDEX = "torrentDbProxyIndex" + TXT;
  public static final int UPDATE_FILE_VERSION = 99;
  public static final String UPDATE_FILE = "update" + UPDATE_FILE_VERSION + TXT;
  public static final String UPDATE_BACKUP_FILE = "updateBackup" + UPDATE_FILE_VERSION + TXT;
  public static final int SETTINGS_LEN = 81;
  public static final int SETTINGS_VERSION = 11;
  public static final String PROFILE = "profile" + SETTINGS_VERSION + "_";
  public static final String USER_SETTINGS = "userSettings" + SETTINGS_VERSION + TXT;
  public static final String DEFAULT_SETTINGS = "defaultSettings" + SETTINGS_VERSION + TXT;
  public static final String ANY = "ANY";
  public static final String HQ = "HQ", DVD = "DVD", HD720 = "720 HD", HD1080 = "1080 HD";
  public static final String NULL = "null";
  public static final String NO_IMAGE = "" + Character.MAX_VALUE + Character.MAX_VALUE + Character.MAX_VALUE;
  public static final String ZIP = ".zip";
  public static final String PROXY_VERSION = "proxyVersion" + TXT;
  public static final String PLAYLIST = "playlist2" + TXT;
  public static final String BANNED_TITLES = "bannedTitles" + TXT, BANNED_DOWNLOAD_IDS = "bannedDownloadIDs" + TXT;
  public static final String PROXIES = "proxies" + TXT;
  public static final String ERROR_LOG = "errorLog" + TXT;
  public static final String NO_PROXY = "NO PROXY";
  public static final String INFINITY = "infinity";
  public static final String CUT = "Cut", COPY = "Copy", PASTE = "Paste";
  public static final String EXE = ".exe", JAR = ".jar", JAR_OPTION = "-jar";
  public static final String PROGRAM_JAR = APP_TITLE + JAR;
  public static final String HOME_DIR = System.getProperty("user.home", ""), WORKING_DIR = System.getProperty("user.dir", ".");
  public static final String PROGRAM_DIR, APP_DIR, CACHE_DIR, TEMP_DIR, TORRENTS_DIR, DESKTOP_DIR;
  public static final String JAVA;

  static {
    String java = System.getProperty("java.home", "java");
    JAVA = java + (java.equals("java") ? "" : FILE_SEPARATOR + "bin" + FILE_SEPARATOR + "java");
    PROGRAM_DIR = initProgramDir();
    APP_DIR = initAppDir();
    CACHE_DIR = APP_DIR + "cache" + FILE_SEPARATOR;
    TEMP_DIR = APP_DIR + "temp" + FILE_SEPARATOR;
    TORRENTS_DIR = APP_DIR + "torrents" + FILE_SEPARATOR;
    DESKTOP_DIR = (HOME_DIR.isEmpty() ? "" : HOME_DIR + FILE_SEPARATOR + "Desktop");

    if (WINDOWS) {
      double osVersion;
      try {
        osVersion = Double.parseDouble(System.getProperty("os.version", "0"));
      } catch (NumberFormatException e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        osVersion = 0;
      }
      PEER_BLOCK_VERSION = ((WINDOWS_XP_AND_HIGHER = osVersion > 5) ? PEER_BLOCK + '_' + (osVersion < 6 ? "xp" : "vista") + (System.getenv(
              "ProgramFiles(X86)") == null ? "" : "_64") : null);
      WINDOWS_VISTA_AND_HIGHER = osVersion >= 6;
      String arch = System.getenv("PROCESSOR_ARCHITECTURE"), arch2;
      IS_64BIT_WINDOWS = arch != null && arch.endsWith("64") || (arch2 = System.getenv("PROCESSOR_ARCHITEW6432")) != null && arch2.endsWith("64");
    } else {
      WINDOWS_XP_AND_HIGHER = false;
      WINDOWS_VISTA_AND_HIGHER = false;
      IS_64BIT_WINDOWS = false;
      PEER_BLOCK_VERSION = null;
    }
  }

  private static String initProgramDir() {
    String tempProgramDir = "";
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
    if ((new File("target")).exists()) {
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
      if (WINDOWS) {
        tempAppDir = System.getenv("APPDATA");
        if (tempAppDir == null || tempAppDir.isEmpty()) {
          tempAppDir = userHomeDir() + "Application Data";
        }
        tempAppDir += FILE_SEPARATOR + APP_TITLE + FILE_SEPARATOR;
      } else {
        tempAppDir = userHomeDir() + (MAC ? "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR + APP_TITLE : "."
                + APP_TITLE.toLowerCase(Locale.ENGLISH)) + FILE_SEPARATOR;
      }

      IO.fileOp(tempAppDir, IO.MK_DIR);
      try {
        copyFileToAppDir(tempAppDir, UPDATE_FILE);
        copyFileToAppDir(tempAppDir, USER_SETTINGS);
        copyFileToAppDir(tempAppDir, PROXY_VERSION);
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

  public static String userHomeDir() throws IOException {
    return (new File(HOME_DIR.isEmpty() ? WORKING_DIR : HOME_DIR)).getCanonicalPath() + FILE_SEPARATOR;
  }

  public static String[] appDirs() throws IOException {
    Set<String> appDirs = new TreeSet<String>();
    if (WINDOWS) {
      String appDataDir = System.getenv("APPDATA");
      if (appDataDir != null && !appDataDir.isEmpty()) {
        appDirs.add(appDataDir + FILE_SEPARATOR);
      }
      String localAppDataDir = System.getenv("LOCALAPPDATA");
      if (localAppDataDir != null && !localAppDataDir.isEmpty()) {
        Collections.addAll(appDirs, localAppDataDir + FILE_SEPARATOR, localAppDataDir + "Low" + FILE_SEPARATOR);
      }
    } else {
      String appDir = userHomeDir();
      appDirs.add(MAC ? appDir + "Library" + FILE_SEPARATOR + "Application Support" + FILE_SEPARATOR : appDir);
    }
    return appDirs.toArray(EMPTY_STRS);
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

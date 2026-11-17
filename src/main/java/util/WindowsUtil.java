package util;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.KnownFolders;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShellAPI.SHELLEXECUTEINFO;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinUser;
import debug.Debug;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class WindowsUtil {

  private static final String NOTIFY_ICON_SETTINGS = "Control Panel\\NotifyIconSettings";
  private static final AtomicBoolean isTrayIconPromotionStarted = new AtomicBoolean();

  public static void addMicrosoftRegistryEntry(String path, String type, String name, String val) {
    try {
      (new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\" + path, "/v", name, "/t", "REG_" + type, "/d", val, "/f")).start().waitFor();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  public static void runJavaAsAdmin(List<String> javaArgs) throws Exception {
    List<String> cmd = new ArrayList<String>(2 + javaArgs.size());
    Collections.addAll(cmd, Constant.JAVA, Constant.JAR_OPTION);
    cmd.addAll(javaArgs);
    if (!Constant.WINDOWS_VISTA_AND_HIGHER) {
      (new ProcessBuilder(cmd)).start();
      return;
    }

    String adminPermissionsTesterProgram = Constant.PROGRAM_DIR + "adminPermissionsTester" + Constant.EXE;
    addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", adminPermissionsTesterProgram, "RUNASADMIN");
    try {
      if ((new ProcessBuilder(adminPermissionsTesterProgram)).start().waitFor() == 0) {
        (new ProcessBuilder(cmd)).start();
        return;
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    SHELLEXECUTEINFO shellExecuteInfo = new SHELLEXECUTEINFO();
    shellExecuteInfo.lpVerb = "runas";
    shellExecuteInfo.lpFile = '"' + cmd.get(0) + "w\"";
    StringBuilder params = new StringBuilder(512);
    for (int i = 1, j = cmd.size(); i < j; i++) {
      params.append(" \"").append(cmd.get(i)).append('"');
    }
    shellExecuteInfo.lpParameters = params.toString().trim();
    if (!Shell32.INSTANCE.ShellExecuteEx(shellExecuteInfo)) {
      throw new Exception(Kernel32Util.getLastErrorMessage());
    }
  }

  public static boolean isProcessRunning(String process) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader((new ProcessBuilder("tasklist")).start().getInputStream(), Constant.UTF8));
      String line, processName = process + Constant.EXE;
      while ((line = br.readLine()) != null) {
        if (line.startsWith(processName)) {
          return true;
        }
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      IO.close(br);
    }
    return false;
  }

  public static void startPeerBlock() throws Exception {
    File peerBlock = new File(Constant.APP_DIR + Constant.PEER_BLOCK_VERSION);
    if (!peerBlock.exists()) {
      try {
        IO.unzip(Constant.PROGRAM_DIR + Constant.PEER_BLOCK_VERSION + Constant.ZIP, Constant.APP_DIR);
      } catch (Exception e) {
        IO.fileOp(peerBlock, IO.RM_DIR);
        throw e;
      }
    }
    File confVersion = new File(Constant.APP_DIR, Constant.PEER_BLOCK_CONF_VERSION);
    if (!confVersion.exists()) {
      File conf = new File(peerBlock, "peerblock.conf");
      try {
        String confStr = IO.read(conf);
        int startIndex = confStr.indexOf("<Lists>"), endIndex;
        if (startIndex != -1 && (endIndex = confStr.indexOf("</Lists>")) != -1) {
          IO.write(conf, (new StringBuilder(confStr.substring(0, startIndex).trim())).append(Constant.NEWLINE).append("    <Lists>").append(
                  Constant.NEWLINE).append("        <List><File>").append(Constant.APP_DIR).append(Constant.IP_FILTER).append(
                  "</File><Type>block</Type><Description>Blocked IPs</Description><Enabled>yes</Enabled></List>").append(Constant.NEWLINE).append("    ").append(
                  confStr.substring(endIndex)).toString());
        }
        IO.fileOp(confVersion, IO.MK_FILE);
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
    String peerBlockProgram = Constant.APP_DIR + Constant.PEER_BLOCK_VERSION + Constant.FILE_SEPARATOR + Constant.PEER_BLOCK + Constant.EXE;
    addMicrosoftRegistryEntry("Windows NT\\CurrentVersion\\AppCompatFlags\\Layers", "SZ", peerBlockProgram, "RUNASADMIN");
    runJavaAsAdmin(Arrays.asList(Constant.PROGRAM_DIR + Constant.PEER_BLOCK + Constant.JAR, peerBlockProgram, Constant.APP_TITLE, Constant.APP_DIR
            + Constant.PEER_BLOCK + "Running", Constant.APP_DIR + Constant.PEER_BLOCK + "Exit"));
  }

  public static boolean closeWindow(String className, String windowName) {
    HWND hwnd = User32.INSTANCE.FindWindow(className, windowName);
    if (hwnd != null) {
      User32.INSTANCE.PostMessage(hwnd, WinUser.WM_QUIT, null, null);
      return true;
    }
    return false;
  }

  public static void addTrayIcon(TrayIcon trayIcon) throws AWTException {
    SystemTray.getSystemTray().add(trayIcon);
    if (isTrayIconPromotionStarted.compareAndSet(false, true)) {
      Worker.submit(WindowsUtil::promoteTrayIcon);
    }
  }

  private static void promoteTrayIcon() throws Exception {
    // Windows 11 has no setting to always show all notification area icons. It hides a new icon in the taskbar corner overflow menu (or entirely
    // if that menu is turned off) until the icon's IsPromoted setting is 1. Windows adds the settings for an executable's first icon and uses them
    // for the executable's later icons whatever their IDs. AWT's icons belong to java.exe or javaw.exe, so all Java programs run by the same Java
    // installation share them.
    if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, NOTIFY_ICON_SETTINGS)) {
      return;
    }

    String exe = Kernel32Util.QueryFullProcessImageName(Kernel32.INSTANCE.GetCurrentProcess(), 0);
    Preferences preferences = Preferences.userNodeForPackage(WindowsUtil.class);
    // Windows adds the settings just after the icon (and keeps them after the icon is removed) and applies a change to them right away. Check that
    // IsPromoted stays 1 in case Windows overwrites it while adding them.
    for (int i = 0; i < 20; i++) {
      List<String> settingsKeys = trayIconSettings(exe);
      boolean isPromoted = !settingsKeys.isEmpty();
      for (String settings : settingsKeys) {
        try {
          String promotedKey = "promotedTrayIcon" + Integer.toHexString(settings.hashCode()); // Preferences keys have at most 80 characters
          if (preferences.getBoolean(promotedKey, false)) {
            continue; // Promoted before, so keep the user's choice since then
          }
          String key = NOTIFY_ICON_SETTINGS + '\\' + settings;
          if (Integer.valueOf(1).equals(Advapi32Util.registryGetValue(WinReg.HKEY_CURRENT_USER, key, "IsPromoted"))) {
            preferences.putBoolean(promotedKey, true);
          } else {
            Advapi32Util.registrySetIntValue(WinReg.HKEY_CURRENT_USER, key, "IsPromoted", 1);
            isPromoted = false;
          }
        } catch (Exception e) {
          isPromoted = false;
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
      }
      if (isPromoted) {
        return;
      }
      Thread.sleep(500);
    }
    isTrayIconPromotionStarted.set(false); // Try again when the icon is next added
  }

  // Returns the names of the registry keys with Windows' settings for this process's tray icons
  private static List<String> trayIconSettings(String exe) {
    List<String> settingsKeys = new ArrayList<String>(1);
    for (String settings : Advapi32Util.registryGetKeys(WinReg.HKEY_CURRENT_USER, NOTIFY_ICON_SETTINGS)) {
      String key = NOTIFY_ICON_SETTINGS + '\\' + settings;
      try {
        Object path;
        // Skip the settings of icons with a GUID instead of an ID (AWT's icons have IDs)
        if (Advapi32Util.registryGetValue(WinReg.HKEY_CURRENT_USER, key, "UID") instanceof Integer && (path = Advapi32Util.registryGetValue(
                WinReg.HKEY_CURRENT_USER, key, "ExecutablePath")) instanceof String && exe.equalsIgnoreCase(path((String) path))) {
          settingsKeys.add(settings);
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
    return settingsKeys;
  }

  private static String path(String notifyIconPath) {
    // Windows writes a path in a known folder with the folder's ID, as in {6D809377-6AF0-444B-8957-A3773F02200E}\Java\jdk-21\bin\javaw.exe
    int end;
    if (!notifyIconPath.startsWith("{") || (end = notifyIconPath.indexOf('}')) == -1) {
      return notifyIconPath;
    }
    GUID folderID = new GUID(notifyIconPath.substring(0, end + 1));
    String folder = null;
    // A 32-bit process on 64-bit Windows gets the 32-bit program folders instead of the 64-bit ones, or an error
    if (folderID.equals(KnownFolders.FOLDERID_ProgramFiles) || folderID.equals(KnownFolders.FOLDERID_ProgramFilesX64)) {
      folder = System.getenv("ProgramW6432");
    } else if (folderID.equals(KnownFolders.FOLDERID_ProgramFilesCommon) || folderID.equals(KnownFolders.FOLDERID_ProgramFilesCommonX64)) {
      folder = System.getenv("CommonProgramW6432");
    }
    return (folder == null ? Shell32Util.getKnownFolderPath(folderID) : folder) + notifyIconPath.substring(end + 1);
  }

  private WindowsUtil() {
  }
}

package proxy;

import debug.Debug;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import listener.GuiListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;
import util.Worker;

public class ProxyListDownloader extends Worker {

  private GuiListener guiListener;
  private String proxyFile;

  public ProxyListDownloader(GuiListener guiListener) {
    this.guiListener = guiListener;
  }

  @Override
  protected void doWork() {
    guiListener.proxyListDownloadStarted();
    try {
      download();
    } catch (Exception e) {
      guiListener.error(e);
    }
    guiListener.proxyListDownloadStopped();
  }

  private void download() throws Exception {
    int latestVersion;
    String latestDate;
    try {
      String[] versionStrs = Regex.split(Connection.getUpdateFile(Str.get(291)), Constant.NEWLINE);
      latestVersion = Integer.parseInt(versionStrs[0]);
      latestDate = versionStrs[1];
      proxyFile = versionStrs[2];
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      guiListener.msg(Str.str("proxyDownloadError"), Constant.ERROR_MSG);
      return;
    }

    if (!(new File(Constant.APP_DIR + Constant.PROXIES)).exists()) {
      addProxies(latestVersion);
      return;
    }

    int currVersion = 0;
    try {
      currVersion = Integer.parseInt(IO.read(Constant.APP_DIR + Constant.PROXY_VERSION));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    if (currVersion < latestVersion) {
      try {
        latestDate = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.DEFAULT).format((new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",
                Locale.ENGLISH)).parse(latestDate));
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      if (guiListener.isConfirmed(Str.str("newerProxies", latestDate) + ' ' + Str.str("downloadProxies"))) {
        addProxies(latestVersion);
      }
    } else if (!hasLatestProxies() || guiListener.isConfirmed(Str.str("proxiesUpToDate") + ' ' + Str.str("downloadProxiesAgain"))) {
      addProxies(latestVersion);
    }
  }

  private static boolean hasLatestProxies() {
    try {
      Collection<String> proxies = new ArrayList<String>(Arrays.asList(Regex.split(IO.read(Constant.APP_DIR + "bk_" + Constant.PROXIES), Constant.NEWLINE)));
      proxies.removeAll(Arrays.asList(Regex.split(IO.read(Constant.APP_DIR + Constant.PROXIES), Constant.NEWLINE)));
      return proxies.isEmpty();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      return true;
    }
  }

  private void addProxies(int latestVersion) throws Exception {
    String[] newProxies = Regex.split(Connection.getUpdateFile(proxyFile), Constant.NEWLINE);
    String[] oldProxies = new File(Constant.APP_DIR + Constant.PROXIES).exists() ? Regex.split(IO.read(Constant.APP_DIR + Constant.PROXIES),
            Constant.NEWLINE) : Constant.EMPTY_STRS;

    Collection<String> proxies = new ArrayList<String>(newProxies.length + oldProxies.length);
    StringBuilder proxiesStr = new StringBuilder((newProxies.length + oldProxies.length) * 32);

    for (String newProxy : newProxies) {
      String proxy = newProxy.trim();
      if (!proxy.isEmpty() && !proxies.contains(newProxy)) {
        proxies.add(newProxy);
        proxiesStr.append(newProxy).append(Constant.NEWLINE);
      }
    }

    int numOldProxies = 0;
    for (String oldProxy : oldProxies) {
      String proxy = oldProxy.trim();
      if (!proxy.isEmpty()) {
        numOldProxies++;
        if (!proxies.contains(oldProxy)) {
          proxies.add(oldProxy);
          proxiesStr.append(oldProxy).append(Constant.NEWLINE);
        }
      }
    }

    File proxiesFile = new File(Constant.APP_DIR + Constant.PROXIES);
    IO.write(proxiesFile, proxiesStr.toString().trim());
    IO.write(proxiesFile, new File(Constant.APP_DIR + "bk_" + Constant.PROXIES));
    IO.write(Constant.APP_DIR + Constant.PROXY_VERSION, String.valueOf(latestVersion));

    guiListener.newProxies(proxies);

    int numNewProxies = proxies.size() - numOldProxies;
    guiListener.msg(numNewProxies == 0 ? Str.str("noProxiesAdded") : (numNewProxies == 1 ? Str.str("proxyAdded") : Str.str("proxiesAdded", numNewProxies)),
            Constant.INFO_MSG);
  }
}

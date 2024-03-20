package torrent;

import com.biglybt.core.Core;
import com.biglybt.core.CoreFactory;
import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.instancemanager.ClientInstance;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.ipfilter.IpFilter;
import com.biglybt.core.ipfilter.impl.IPAddressRangeManagerV4;
import com.biglybt.core.ipfilter.impl.IpFilterImpl;
import com.biglybt.core.ipfilter.impl.IpRangeV4Impl;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.core.util.SystemProperties;
import com.biglybt.core.util.UrlUtils;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.PluginManagerDefaults;
import com.biglybt.pif.PluginState;
import com.biglybt.plugin.dht.DHTPlugin;
import com.biglybt.plugin.magnet.MagnetPlugin;
import com.biglybt.plugin.magnet.MagnetPluginProgressListener;
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import listener.DomainType;
import listener.GuiListener;
import org.apache.commons.lang3.StringUtils;
import str.Str;
import util.AbstractWorker;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;
import util.ThrowableUtil;
import util.Worker;

public class Magnet extends Thread {

  private static final Object saveTorrentLock = new Object(), azureusConfigLock = new Object();
  private static final CountDownLatch ipFilterInitializerStartSignal = new CountDownLatch(1);
  private static final AtomicBoolean isAzureusConfigured = new AtomicBoolean();
  public static final String VUZE_VERSION = "biglybt" + Constants.BIGLYBT_VERSION.replace(".", "");
  private static final String VUZE_DIR = Constant.APP_DIR + VUZE_VERSION + Constant.FILE_SEPARATOR + "biglybt" + Constant.FILE_SEPARATOR;
  private static volatile Core core;
  public final String magnetLink;
  public final File torrent;
  private final AtomicBoolean isDoneDownloading = new AtomicBoolean(), isDoneSaving = new AtomicBoolean();
  private static final ConcurrentMap<String, Thread> downloaders = new ConcurrentHashMap<String, Thread>(16);
  private static volatile Worker azureusStarter;
  private static Thread ipFilterInitializer;

  public Magnet(String magnetLink) {
    this.magnetLink = magnetLink;
    IO.fileOp(Constant.TORRENTS_DIR, IO.MK_DIR);
    torrent = new File(Constant.TORRENTS_DIR + Str.hashCode(magnetLink) + Constant.TORRENT);
  }

  public boolean download(GuiListener guiListener, Future<?> parent, boolean runInBackground) throws Exception {
    if (torrentExists()) {
      return true;
    }

    start();

    if (runInBackground) {
      return torrentExists();
    }

    for (int i = guiListener.getDownloadLinkTimeout(); i > 0; i--) {
      try {
        if (isDHTConnecting()) {
          Connection.setStatusBar(Str.str("connecting2") + Str.str("connecting3") + guiListener.getNumBlockedIpsMsg());
        } else {
          Connection.setStatusBar(Str.str("transferring2") + Str.str("connecting3") + guiListener.getNumBlockedIpsMsg());
        }
        join(1000);
        if ((!isDoneDownloading.get() && torrentExists()) || isDoneSaving.get() || !isAlive() || parent.isCancelled()) {
          break;
        }
      } finally {
        Connection.unsetStatusBar();
      }
    }

    return torrentExists();
  }

  @Override
  public void run() {
    if (!torrentExists()) {
      try {
        download();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
  }

  private void download() throws Exception {
    Thread downloader = downloaders.putIfAbsent(magnetLink, this);
    if (downloader != null) {
      downloader.join();
      return;
    }
    try {
      byte[] torrentBytes;
      String magnetLinkHash = Regex.replaceAllRepeatedly(magnetLink, 801);
      try {
        String tempTorrent = Constant.TEMP_DIR + magnetLinkHash + ".torrent";
        Connection.saveData(String.format(Locale.ENGLISH, Str.get(803), magnetLinkHash), tempTorrent, DomainType.DOWNLOAD_LINK_INFO);
        torrentBytes = Files.readAllBytes(Paths.get(tempTorrent));
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        Objects.requireNonNull(torrentBytes = ((MagnetPlugin) CoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(
                MagnetPlugin.class).getPlugin()).download(new MagnetPluginProgressListener() {
                  @Override
                  public void reportSize(long size) {
                  }

                  @Override
                  public void reportActivity(String str) {
                    if (Debug.DEBUG) {
                      Debug.println(Str.get(388) + magnetLinkHash + ' ' + str);
                    }
                  }

                  @Override
                  public void reportCompleteness(int percent) {
                    if (Debug.DEBUG) {
                      Debug.println(Str.get(388) + magnetLinkHash + " " + percent + "% complete");
                    }
                  }

                  @Override
                  public void reportContributor(InetSocketAddress address) {
                  }

                  @Override
                  public boolean verbose() {
                    return true;
                  }

                  @Override
                  public boolean cancelled() {
                    return false;
                  }
                }, UrlUtils.decodeTruncatedHashFromMagnetURI(magnetLinkHash.toUpperCase(Locale.ENGLISH)), StringUtils.substringAfter(magnetLink, "&"),
                        new InetSocketAddress[0], Collections.emptyList(), Collections.emptyMap(), 90_000, MagnetPlugin.FL_NONE),
                "magnet link download failed for: " + magnetLink);
      }
      isDoneDownloading.set(true);

      if (torrentExists()) {
        return;
      }
      synchronized (saveTorrentLock) {
        if (torrent.exists()) {
          return;
        }
        IO.write(torrent, torrentBytes);
      }

      if (Debug.DEBUG) {
        Debug.println(torrent.getName() + " converted");
      }
      isDoneSaving.set(true);
    } finally {
      downloaders.remove(magnetLink);
    }
  }

  private boolean torrentExists() {
    synchronized (saveTorrentLock) {
      return torrent.exists();
    }
  }

  public static void waitForAzureusToStart() throws Exception {
    if (azureusStarter == null) {
      return;
    }
    AbstractWorker.get(azureusStarter);
  }

  public static void initIpFilter() {
    configAzureus();
    ipFilterInitializer = new IpFilterInitializer();
    ipFilterInitializer.setPriority(Thread.MIN_PRIORITY);
    ipFilterInitializer.start();
    ipFilterInitializerStartSignal.countDown();
  }

  // Intentionally un-synchronized because a caller is the event dispatch thread
  public static void startAzureus(final GuiListener guiListener) {
    if (core != null) {
      return;
    }
    (azureusStarter = new Worker() {
      @Override
      protected void doWork() {
        initAzureus(guiListener);
      }
    }).execute();
  }

  private static void configAzureus() {
    synchronized (azureusConfigLock) {
      if (isAzureusConfigured.get()) {
        return;
      }

      Str.waitForUpdate(); // Attempt a HTTPS connection to ensure HTTPS connections are not possibly prevented by Azureus javax.net.ssl.trustStore bug

      System.setProperty(SystemProperties.SYSPROP_SECURITY_MANAGER_INSTALL, "0");
      System.setProperty(SystemProperties.SYSPROP_SECURITY_MANAGER_PERMITEXIT, "1");
      System.setProperty("MULTI_INSTANCE", String.valueOf(true));
      System.setProperty(SystemProperties.SYSPROP_PLATFORM_MANAGER_DISABLE, String.valueOf(true));
      IO.fileOp(VUZE_DIR, IO.MK_DIR);
      System.setProperty(SystemProperties.SYSPROP_INSTALL_PATH, VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_CONFIG_PATH, VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_PORTABLE_ROOT, VUZE_DIR);
      SystemProperties.setUserPath(VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_DOC_PATH, VUZE_DIR + "Documents");

      COConfigurationManager.initialise();
      Connection.setAuthenticator();
      COConfigurationManager.setParameter("max active torrents", 256);
      COConfigurationManager.setParameter("max downloads", 256);
      COConfigurationManager.setParameter("Ip Filter Enabled", true);
      COConfigurationManager.setParameter("Ip Filter Allow", false);
      COConfigurationManager.setParameter("Ip Filter Enable Banning", true);
      COConfigurationManager.setParameter("Ip Filter Ban Block Limit", (long) Integer.MAX_VALUE);
      COConfigurationManager.setParameter("Ip Filter Ban Discard Ratio", "5.0");
      COConfigurationManager.setParameter("Ip Filter Ban Discard Min KB", 4L);
      COConfigurationManager.setParameter("Ip Filter Banning Persistent", false);
      COConfigurationManager.setParameter("Ip Filter Enable Description Cache", false);
      COConfigurationManager.setParameter("Ip Filter Autoload File", "");
      COConfigurationManager.setParameter("Ip Filter Clear On Reload", false);
      COConfigurationManager.setParameter("network.transport.encrypted.require", true);
      COConfigurationManager.setParameter("network.transport.encrypted.min_level", "RC4");
      COConfigurationManager.setParameter("network.transport.encrypted.fallback.outgoing", true);
      COConfigurationManager.setParameter("network.transport.encrypted.fallback.incoming", true);
      COConfigurationManager.setParameter("network.transport.encrypted.use.crypto.port", false);
      COConfigurationManager.setParameter("Enable incremental file creation", true);

      changeLocale();

      isAzureusConfigured.set(true);
    }
  }

  private static synchronized void initAzureus(GuiListener guiListener) {
    if (core != null) {
      return;
    }

    try {
      Connection.setStatusBar(Str.str("connecting4"));
      configAzureus();

      int originalPort = guiListener.getPort();
      int port = originalPort;
      for (int i = 0, j = 11; i <= j; i++, port = guiListener.setRandomPort()) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
          ss = new ServerSocket(port);
          ss.setReuseAddress(true);
          ds = new DatagramSocket(port);
          ds.setReuseAddress(true);
          if (port != originalPort && !guiListener.canRandomizePort()) {
            guiListener.msg(Str.str("portChanged", port, originalPort), Constant.WARN_MSG);
          }
          break;
        } catch (IOException e) {
          if (i == j) {
            guiListener.setPort(port = originalPort);
            guiListener.error(new IOException(Str.str("portError", port) + ' ' + ThrowableUtil.toString(e)));
            break;
          }
        } finally {
          IO.close(ds, ss);
        }
      }
      setPorts(port);

      core = CoreFactory.create();

      try {
        ipFilterInitializerStartSignal.await();
        ipFilterInitializer.join();
        Field rangeManagerField = IpFilterImpl.class.getDeclaredField("range_manager_v4");
        rangeManagerField.setAccessible(true);
        Object rangeManager = rangeManagerField.get(IpFilterImpl.getInstance());
        Method checkRebuildMethod = IPAddressRangeManagerV4.class.getDeclaredMethod("checkRebuild");
        checkRebuildMethod.setAccessible(true);
        checkRebuildMethod.invoke(rangeManager);
        Field totalSpanField = IPAddressRangeManagerV4.class.getDeclaredField("total_span");
        totalSpanField.setAccessible(true);
        guiListener.setPlaylistPlayHint((Long) totalSpanField.get(rangeManager));
        if (Debug.DEBUG) {
          Debug.println(guiListener.getNumBlockedIpsMsg().trim());
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }

      Connection.setStatusBar(Str.str("connecting4") + guiListener.getNumBlockedIpsMsg());

      if (Debug.DEBUG) {
        ClientInstance instance = core.getInstanceManager().getMyInstance();
        Debug.println("TCP Port: " + instance.getTCPListenPort() + "\nUDP Port: " + instance.getUDPListenPort() + "\nUDP Non-Data Port: "
                + instance.getUDPNonDataListenPort());
      }

      Collection<String> enabledPluginNames = new ArrayList<String>(16), enabledPluginIDs = new ArrayList<String>(8);
      Collections.addAll(enabledPluginNames, "DHT", "DHT Tracker", "Local Tracker", "Tracker Peer Auth", "uTP Plugin", "Distributed DB",
              "Distributed Tracker", "Magnet URI Handler", "External Seed", "LAN Peer Finder", "Client Identification");
      Collections.addAll(enabledPluginIDs, "azutp", "azbpdht", "azbpdhdtracker", "azbpmagnet", "azextseed", "azlocaltracker", "bgclientid");

      PluginManagerDefaults pluginManagerDefaults = core.getPluginManagerDefaults();
      for (String pluginName : pluginManagerDefaults.getDefaultPlugins()) {
        boolean isEnabled = enabledPluginNames.contains(pluginName);
        if (Debug.DEBUG) {
          Debug.println((isEnabled ? "Enabled" : "Disabled") + " (Default): '" + pluginName + "'");
        }
        pluginManagerDefaults.setDefaultPluginEnabled(pluginName, isEnabled);
      }

      core.start();
      if (Debug.DEBUG) {
        Debug.println("Azureus core started");
      }

      for (PluginInterface pluginInterface : core.getPluginManager().getPlugins()) {
        PluginState state = pluginInterface.getPluginState();
        if (enabledPluginIDs.contains(pluginInterface.getPluginID())) {
          if (state.isDisabled()) {
            state.setDisabled(false);
            if (Debug.DEBUG) {
              Debug.println("Enabled: '" + pluginInterface.getPluginName() + "'");
            }
            state.setLoadedAtStartup(true);
            if (state.isUnloadable()) {
              state.reload();
              if (Debug.DEBUG) {
                Debug.println("Reloaded: '" + pluginInterface.getPluginName() + "'");
              }
            }
          }
        } else if (!state.isDisabled()) {
          state.setDisabled(true);
          if (Debug.DEBUG) {
            Debug.println("Disabled: '" + pluginInterface.getPluginName() + "'");
          }
          state.setLoadedAtStartup(false);
          if (state.isUnloadable()) {
            state.unload();
            if (Debug.DEBUG) {
              Debug.println("Unloaded: '" + pluginInterface.getPluginName() + "'");
            }
          }
        }
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      Connection.unsetStatusBar();
    }
  }

  public static synchronized void stopAzureus() {
    if (core != null) {
      try {
        core.stop();
        if (Debug.DEBUG) {
          Debug.println("Azureus core stopped");
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
  }

  // Intentionally un-synchronized because caller is event dispatch thread
  public static void changePorts(int port) {
    if (core != null) {
      setPorts(port);
    }
  }

  // Intentionally un-synchronized because a caller is the event dispatch thread
  private static void setPorts(int port) {
    COConfigurationManager.setParameter("TCP.Listen.Port", port);
    COConfigurationManager.setParameter("TCP.Listen.Port.Enable", true);
    COConfigurationManager.setParameter("UDP.Listen.Port", port);
    COConfigurationManager.setParameter("UDP.Listen.Port.Enable", true);
    COConfigurationManager.setParameter("UDP.NonData.Listen.Port", port);
    COConfigurationManager.setParameter("UDP.NonData.Listen.Port.Same", true);
    COConfigurationManager.setParameter("Listen.Port.Randomize.Enable", false);
  }

  public static void localeChanged() {
    if (isAzureusConfigured.get()) {
      changeLocale();
    }
  }

  private static void changeLocale() {
    Locale defaultLocale = Locale.getDefault();
    try {
      COConfigurationManager.setParameter("locale", ("en".equals(defaultLocale.getLanguage()) ? Locale.ENGLISH : defaultLocale).toString());
      MessageText.loadBundle();
      DisplayFormatters.setUnits();
      DisplayFormatters.loadMessages();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      Locale.setDefault(defaultLocale);
    }
  }

  private static synchronized boolean isDHTConnecting() {
    if (core == null) {
      return false;
    }

    try {
      PluginInterface dhtPluginInterface = core.getPluginManager().getPluginInterfaceByClass(DHTPlugin.class);
      if (dhtPluginInterface == null) {
        return false;
      }
      DHTPlugin dhtPlugin = (DHTPlugin) dhtPluginInterface.getPlugin();
      if (dhtPlugin == null) {
        return false;
      }
      int dhtStatus = dhtPlugin.getStatus();
      if (dhtStatus == DHTPlugin.STATUS_RUNNING) {
        long numUsers = dhtPlugin.getDHTs()[0].getControl().getStats().getEstimatedDHTSize(), minNumUsers = Long.parseLong(Str.get(395));
        return numUsers < minNumUsers;
      } else if (dhtStatus == DHTPlugin.STATUS_INITALISING) {
        return true;
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }

    return false;
  }

  private static class IpFilterInitializer extends Thread {

    @Override
    public void run() {
      try {
        initIpFilter();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      } catch (OutOfMemoryError e) {
        System.gc();
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }

    private static void initIpFilter() throws Exception {
      File ipfilter = new File(Constant.APP_DIR, Constant.IP_FILTER), ipfilterZip = new File(ipfilter.getPath() + Constant.ZIP);
      if (!ipfilter.exists() || ipfilterZip.exists() || IO.isFileTooOld(ipfilter, Long.parseLong(Str.get(794)))) {
        Connection.saveData(Str.get(795), ipfilterZip.getPath(), DomainType.UPDATE, false);
        IO.unzip(ipfilterZip.getPath(), Constant.APP_DIR);
        IO.fileOp(ipfilterZip, IO.RM_FILE);
      }

      String line;
      BufferedReader br = null;
      Pattern ipPattern = Regex.pattern("\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+");
      IpFilter ipFilter = IpFilterImpl.getInstance();

      try {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(ipfilter), Constant.UTF8));
        while ((line = br.readLine()) != null) {
          Matcher ipMatcher = ipPattern.matcher(line);
          while (!ipMatcher.hitEnd()) {
            if (!ipMatcher.find()) {
              continue;
            }

            String startIp = ipMatcher.group(), endIp = startIp;
            int index = line.indexOf('-', ipMatcher.end());

            if (index != -1) {
              ipMatcher = ipPattern.matcher(line.substring(index + 1));
              while (!ipMatcher.hitEnd()) {
                if (ipMatcher.find()) {
                  endIp = ipMatcher.group();
                  break;
                }
              }
            }

            ipFilter.addRange(new IpRangeV4Impl("", startIp, endIp, true));
            break;
          }
        }
      } finally {
        IO.close(br);
      }
    }
  }

  public static String getIp(GuiListener guiListener) throws Exception {
    startAzureus(guiListener);
    waitForAzureusToStart();
    return core.getInstanceManager().getMyInstance().getExternalAddress().getHostAddress();
  }
}

package torrent;

import com.biglybt.core.Core;
import com.biglybt.core.CoreFactory;
import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.config.impl.ConfigurationManager;
import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.download.DownloadManagerState;
import com.biglybt.core.download.impl.DownloadManagerAdapter;
import com.biglybt.core.global.GlobalManager;
import com.biglybt.core.global.GlobalManagerAdapter;
import com.biglybt.core.instancemanager.ClientInstance;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.ipfilter.IpFilter;
import com.biglybt.core.ipfilter.impl.IPAddressRangeManagerV4;
import com.biglybt.core.ipfilter.impl.IpFilterImpl;
import com.biglybt.core.ipfilter.impl.IpRangeV4Impl;
import com.biglybt.core.torrent.TOTorrent;
import com.biglybt.core.torrent.impl.TOTorrentDeserialiseImpl;
import com.biglybt.core.util.AEDiagnostics;
import com.biglybt.core.util.AEMonitor;
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
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import listener.DomainType;
import listener.GuiListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import str.Str;
import util.AbstractWorker;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;
import util.ThreadUtil;
import util.ThrowableUtil;
import util.Worker;

public class Magnet extends Thread {

  private static final Object saveTorrentLock = new Object(), azureusConfigLock = new Object();
  private static final CountDownLatch ipFilterInitializerStartSignal = new CountDownLatch(1);
  private static final AtomicBoolean isAzureusConfigured = new AtomicBoolean();
  public static final String VUZE_VERSION = "biglybt" + Constants.BIGLYBT_VERSION.replace(".", "");
  private static final String VUZE_DIR = Constant.APP_DIR + VUZE_VERSION + Constant.FILE_SEPARATOR + "biglybt" + Constant.FILE_SEPARATOR;
  private static final Pattern VERSIONED_PLUGIN_FILE = Pattern.compile("(.+?)_(\\d[\\d.]*)(\\.\\w+)"); // Such as mlDHT_2.6.14.jar
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
      configAzureus(); // Prevent this thread's first use of BiglyBT from racing (and deadlocking with) BiglyBT's initialization in configAzureus()
      byte[] torrentBytes = null;
      String magnetLinkHash = Regex.replaceAllRepeatedly(magnetLink, 801);
      byte[] infoHash = UrlUtils.decodeTruncatedHashFromMagnetURI(magnetLinkHash.toUpperCase(Locale.ENGLISH));
      try {
        String tempTorrent = Constant.TEMP_DIR + magnetLinkHash + ".torrent";
        for (String urlFormat : Regex.split(890, Constant.SEPARATOR1)) {
          try {
            String url = String.format(Locale.ENGLISH, urlFormat, magnetLinkHash);
            Connection.saveData(url, tempTorrent, DomainType.DOWNLOAD_LINK_INFO);
            byte[] bytes = Files.readAllBytes(Paths.get(tempTorrent));
            String torrentData = (new String(bytes, Constant.UTF8)).trim();
            if (torrentData.isEmpty() || !Regex.firstMatch(torrentData, 888).isEmpty()) {
              throw new IOException("Bad torrent for: " + url);
            }
            TOTorrent cachedTorrent = new TOTorrentDeserialiseImpl(new File(tempTorrent));
            Validate.isTrue(cachedTorrent.getFileCount() > 0, "No files in torrent for: %s", url);
            Validate.isTrue(Arrays.equals(cachedTorrent.getHash(), infoHash), "Torrent with another info hash for: %s", url); // Such as a wrong cached file
            torrentBytes = bytes; // Set only once valid: otherwise an invalid torrent from one URL is saved if the remaining URLs fail
            break;
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          }
        }
        if (torrentBytes == null) {
          throw new IOException("No torrent for magnet link: " + magnetLink);
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        waitForAzureusToStart(); // BiglyBT starts asynchronously (startAzureus()), and until it has, MagnetPlugin isn't available
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

                  @Override
                  public void setupComplete(long addedTime) {
                  }
                }, infoHash, StringUtils.substringAfter(magnetLink, "&"),
                        new InetSocketAddress[0], Collections.emptyList(), Collections.emptyMap(), 60_000, MagnetPlugin.FL_NONE),
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

  // Call before using BiglyBT outside this class. BiglyBT's first use initializes it, which has to happen in configAzureus(): that sets BiglyBT's paths and
  // system properties first (otherwise BiglyBT uses its default profile), and two threads initializing BiglyBT at once can deadlock. Returns false without
  // waiting if BiglyBT is not configured yet and this is the event dispatch thread, because configuring can wait 20 seconds for the update check.
  public static boolean awaitAzureusConfig() {
    if (!isAzureusConfigured.get() && !EventQueue.isDispatchThread()) {
      try {
        configAzureus();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
    }
    return isAzureusConfigured.get();
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

      // BiglyBT never installs its security manager on Java 18+. There, this property's only effect would be a Debug call inside BiglyBT's
      // configuration initialization, which is what lets another thread's first use of BiglyBT deadlock with configInit below.
      if (!Constants.isJava18OrHigher) {
        System.setProperty(SystemProperties.SYSPROP_SECURITY_MANAGER_INSTALL, "0");
      }
      System.setProperty(SystemProperties.SYSPROP_SECURITY_MANAGER_PERMITEXIT, "1");
      System.setProperty("MULTI_INSTANCE", String.valueOf(true));
      System.setProperty(SystemProperties.SYSPROP_PLATFORM_MANAGER_DISABLE, String.valueOf(true));
      // BiglyBT's instance manager (which looks for other BiglyBT instances on the local network, so the LAN Peer Finder plugin is disabled too) has a multicast
      // socket on every network adapter, including virtual ones such as VMware's and VirtualBox's, and sends packets through them, also as BiglyBT stops. Windows
      // closes a socket only once its sends are finished, so a network driver that never finishes one keeps the JVM's process, with all of its memory, from
      // ending after VidMasta exits.
      System.setProperty("az.instance.manager.enable", "0");
      IO.fileOp(VUZE_DIR, IO.MK_DIR);
      System.setProperty(SystemProperties.SYSPROP_INSTALL_PATH, VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_CONFIG_PATH, VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_PORTABLE_ROOT, VUZE_DIR);
      SystemProperties.setUserPath(VUZE_DIR);
      System.setProperty(SystemProperties.SYSPROP_DOC_PATH, VUZE_DIR + "Documents");

      Thread configInit = new Thread(() -> {
        try {
          cacheAzureusJar();
          AzureusConfigLock configLock = (Constants.isJava18OrHigher ? null : AzureusConfigLock.hold());
          try {
            // Debug's initializer starts BiglyBT's diagnostics and configuration, so initializing it first makes this thread hold Debug's
            // initialization for all of that. BiglyBT threads and JVM-wide hooks (stdout/stderr redirect, DNS resolver) that BiglyBT installs meanwhile
            // then wait for Debug instead of starting a second, conflicting initialization.
            com.biglybt.core.util.Debug.outNoStack("");
            (new File("biglybt_error.log")).delete();
          } catch (Exception e) {
            if (Debug.DEBUG) {
              Debug.print(e);
            }
          } finally {
            if (configLock != null) {
              configLock.release();
            }
          }
          COConfigurationManager.initialise();
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
        }
      }, "Azureus config init");
      // Get this before configInit starts: BiglyBT initializes the JDK's management classes too, and if configInit got stuck in them, so would this thread
      ThreadMXBean threadMXBean = ThreadUtil.threadMXBean();
      configInit.start();
      File deadlock = new File(Constant.APP_DIR, "deadlock");
      try {
        String stuckReason = awaitAzureusConfigInit(configInit, threadMXBean);
        if (stuckReason != null) {
          Exception e = new Exception("deadlock: " + stuckReason + Constant.NEWLINE + ThreadUtil.threadDump(threadMXBean));
          e.setStackTrace(configInit.getStackTrace());
          IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
          if (deadlock.exists()) {
            JOptionPane.showMessageDialog(null, Str.str("deadlockError", Constant.WINDOWS ? Constant.EXE : "", Constant.APP_TITLE), Constant.APP_TITLE,
                    Constant.ERROR_MSG);
          } else {
            IO.fileOp(deadlock, IO.MK_FILE);
            Connection.clearStatusBar();
            Connection.setStatusBar(Str.str("deadlockError", Constant.WINDOWS ? Constant.EXE : "", Constant.APP_TITLE));
            (new ProcessBuilder(Constant.JAVA, "-cp", System.getProperty("java.class.path"), Constant.APP_TITLE)).start();
            Runtime.getRuntime().halt(-1);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      IO.fileOp(deadlock, IO.RM_FILE);

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
      // A new download file grows as pieces arrive, so on NTFS a piece written past the end of the data so far first has the whole gap zero-filled, which
      // blocked BiglyBT's disk writes (a 16 KB write past 1 GB took 57 seconds) and with them downloads and shutdown. A sparse file has no gap to fill.
      COConfigurationManager.setParameter("Enable Sparse Files", true);

      changeLocale();

      isAzureusConfigured.set(true);
    }
  }

  // Reading BiglyBT's jar sequentially puts it in the OS file cache. Otherwise, after a cold boot, loading BiglyBT's classes one at a time takes thousands
  // of random reads, which on a hard disk made BiglyBT's initialization take 6 to 10 seconds instead of 1.
  private static void cacheAzureusJar() {
    try {
      File jar = new File(SystemProperties.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!jar.isFile()) {
        return;
      }
      byte[] buffer = new byte[1_048_576];
      try (InputStream is = new FileInputStream(jar)) {
        while (is.read(buffer) != -1) {
          // Discard: only the OS file cache is wanted
        }
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  // On Java 8-17, BiglyBT's configuration initialization calls Debug while it holds ConfigurationManager's lock (class_mon), and Debug's initializer takes
  // that lock to start the configuration. So a thread that starts BiglyBT through another class can deadlock with configInit, which starts it through
  // Debug. To rule that out, configInit takes class_mon before Debug's initializer and lets go of it as soon as the configuration initialization inside
  // that initializer releases its own hold, i.e. once the configuration is ready. Until then, a thread starting BiglyBT through another class waits for
  // class_mon before it can reach Debug. This replaces class_mon's lock with this wrapper, which delegates to the original lock, so threads using either
  // object still share one lock (only the final getQueueLength() and hasQueuedThreads() can't delegate, and nothing calls them on class_mon). Java 18+
  // doesn't need this (see SYSPROP_SECURITY_MANAGER_INSTALL in configAzureus()), and newer JDKs warn about, then refuse, setting final fields reflectively.
  private static final class AzureusConfigLock extends ReentrantLock {

    private static final long serialVersionUID = 1L;
    private final transient AEMonitor monitor;
    private final transient Field monitorLockField;
    private final transient ReentrantLock delegate;
    private transient volatile Thread holder;

    private AzureusConfigLock(AEMonitor monitor, Field monitorLockField, ReentrantLock delegate) {
      this.monitor = monitor;
      this.monitorLockField = monitorLockField;
      this.delegate = delegate;
    }

    // Returns null if BiglyBT's internals have changed
    static AzureusConfigLock hold() {
      try {
        Field classMonField = ConfigurationManager.class.getDeclaredField("class_mon");
        classMonField.setAccessible(true);
        AEMonitor monitor = (AEMonitor) classMonField.get(null);
        Field monitorLockField = AEMonitor.class.getDeclaredField("lock");
        monitorLockField.setAccessible(true);
        AzureusConfigLock configLock = new AzureusConfigLock(monitor, monitorLockField, (ReentrantLock) monitorLockField.get(monitor));
        monitorLockField.set(monitor, configLock);
        synchronized (AEDiagnostics.class) {
          // Let a Debug initialization that another thread already started finish first: it holds this monitor until its configuration is ready. That only
          // narrows the race: one that hasn't reached AEDiagnostics.getLogger() yet (or starts before configInit's Debug call) can still deadlock with
          // configInit, so BiglyBT's first use outside this class still has to wait for configAzureus() (awaitAzureusConfig()).
        }
        configLock.delegate.lock();
        configLock.holder = Thread.currentThread();
        return configLock;
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        return null;
      }
    }

    void release() {
      try {
        monitorLockField.set(monitor, delegate);
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      if (holder == Thread.currentThread()) {
        holder = null;
        delegate.unlock();
      }
    }

    @Override
    public void lock() {
      delegate.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      delegate.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
      return delegate.tryLock();
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
      return delegate.tryLock(timeout, unit);
    }

    @Override
    public void unlock() {
      delegate.unlock();
      if (holder == Thread.currentThread() && delegate.getHoldCount() == 1) { // The configuration initialization released its hold
        holder = null;
        delegate.unlock();
      }
    }

    @Override
    public Condition newCondition() {
      return delegate.newCondition();
    }

    @Override
    public int getHoldCount() {
      return delegate.getHoldCount();
    }

    @Override
    public boolean isHeldByCurrentThread() {
      return delegate.isHeldByCurrentThread();
    }

    @Override
    public boolean isLocked() {
      return delegate.isLocked();
    }
  }

  // Returns null once configInit finishes, or why it is considered deadlocked: for 10 seconds, neither configInit nor any thread it waits for (the owner of
  // the lock it waits on, that thread's lock owner, and so on) made progress. A thread waiting for another thread's class initialization shows no lock
  // owner, so then only its own progress counts.
  private static String awaitAzureusConfigInit(Thread configInit, ThreadMXBean threadMXBean) throws InterruptedException {
    long maxStallNanos = TimeUnit.SECONDS.toNanos(10), maxNanos = TimeUnit.SECONDS.toNanos(120), start = System.nanoTime(), lastProgress = start;
    Map<Long, List<StackTraceElement>> lastStacks = new HashMap<Long, List<StackTraceElement>>(8);
    while (true) {
      configInit.join(250);
      if (!configInit.isAlive()) {
        return null;
      }
      long now = System.nanoTime();
      boolean progress = false;
      Map<Long, List<StackTraceElement>> stacks = new HashMap<Long, List<StackTraceElement>>(8);
      if (threadMXBean == null) {
        progress = isProgressing(configInit.getId(), configInit.getState(), configInit.getStackTrace(), stacks, lastStacks);
      } else {
        for (long id = configInit.getId(); id != -1 && !stacks.containsKey(id);) {
          ThreadInfo info = threadMXBean.getThreadInfo(id, Integer.MAX_VALUE);
          if (info == null) {
            break;
          }
          progress |= isProgressing(id, info.getThreadState(), info.getStackTrace(), stacks, lastStacks);
          id = info.getLockOwnerId();
        }
      }
      lastStacks = stacks;
      if (progress) {
        lastProgress = now;
      } else if (now - lastProgress >= maxStallNanos) {
        return "no progress for " + TimeUnit.NANOSECONDS.toSeconds(maxStallNanos) + " seconds";
      }
      if (now - start >= maxNanos) {
        return "not finished after " + TimeUnit.NANOSECONDS.toSeconds(maxNanos) + " seconds";
      }
    }
  }

  // Records a thread's stack and returns whether the thread made progress: its stack changed, or it is in a state a deadlocked thread never is, a timed wait
  // (which ends by itself) or native code such as a file read
  private static boolean isProgressing(long id, Thread.State state, StackTraceElement[] stack, Map<Long, List<StackTraceElement>> stacks,
          Map<Long, List<StackTraceElement>> lastStacks) {
    List<StackTraceElement> frames = Arrays.asList(stack);
    stacks.put(id, frames);
    return !frames.equals(lastStacks.get(id)) || state == Thread.State.TIMED_WAITING || (state == Thread.State.RUNNABLE && stack.length != 0
            && stack[0].isNativeMethod());
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

      Arrays.stream(IO.listFiles(Constant.PROGRAM_DIR + "plugins")).forEach(plugin -> {
        File dest = new File(VUZE_DIR + "plugins", plugin.getName());
        boolean isNew = !dest.exists();
        try {
          installPlugin(plugin, dest);
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          if (isNew) {
            FileUtils.deleteQuietly(dest);
          }
        }
      });

      Collection<String> enabledPluginNames = new ArrayList<String>(16), enabledPluginIDs = new ArrayList<String>(8);
      Collections.addAll(enabledPluginNames, "DHT", "DHT Tracker", "Tracker Peer Auth", "uTP Plugin", "Distributed DB", "Distributed Tracker",
              "Magnet URI Handler", "External Seed", "Client Identification", "Location Provider", "Swarm Discoveries", "uTP Plugin", "mlDHT");
      Collections.addAll(enabledPluginIDs, "azutp", "azbpdht", "azbpdhdtracker", "azbpmagnet", "azextseed", "bgclientid", "azlocprov", "aercm", "azutp",
              "mldht");

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
      startMetadataDownloads(core.getGlobalManager());

      if (Debug.DEBUG) {
        System.err.println("mlDHT plugin does IPv6 connection test, which may print to System.err ignorable exception: java.net.SocketException: Network is"
                + " unreachable: connect");
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    } finally {
      Connection.unsetStatusBar();
    }
  }

  // Copies a plugin to BiglyBT's folder, where a previous version of it can be (VidMasta's updates keep BiglyBT's folder, and only add files to the program's
  // plugins). BiglyBT's folder for a plugin also has the plugin's own data (such as mlDHT's node table), so only files with another version in their name (such
  // as the previous version's mlDHT_2.6.7.jar) are removed.
  private static void installPlugin(File plugin, File dest) throws IOException {
    Map<String, String> newestVersions = new HashMap<>();
    for (File file : IO.listFiles(plugin)) {
      Matcher matcher = VERSIONED_PLUGIN_FILE.matcher(file.getName());
      if (matcher.matches()) {
        newestVersions.merge(matcher.group(1) + matcher.group(3), matcher.group(2), (version1, version2) -> Constants.compareVersions(version1, version2) < 0
                ? version2 : version1);
      }
    }
    FileFilter isOtherVersion = file -> {
      Matcher matcher = VERSIONED_PLUGIN_FILE.matcher(file.getName());
      String newestVersion = matcher.matches() ? newestVersions.get(matcher.group(1) + matcher.group(3)) : null;
      return newestVersion != null && !newestVersion.equals(matcher.group(2));
    };
    if (Arrays.stream(IO.listFiles(plugin)).filter(file -> !isOtherVersion.accept(file)).allMatch(file -> {
      File installed = new File(dest, file.getName());
      return installed.exists() && installed.length() == file.length();
    })) {
      return;
    }
    FileUtils.copyDirectory(plugin, dest, file -> !isOtherVersion.accept(file));
    Arrays.stream(IO.listFiles(dest)).filter(isOtherVersion::accept).forEach(FileUtils::deleteQuietly);
  }

  // MagnetPlugin gets a magnet link's torrent from peers with a metadata download that it only force starts, leaving the Start/Stop Rules plugin to initialize
  // and start it. That plugin is disabled, so do that here, as StreamingTorrentUtil does for its downloads. Otherwise metadata downloads never start, and
  // MagnetPlugin only looks for the torrent from other BiglyBT clients, which is slow and often finds nothing.
  private static void startMetadataDownloads(GlobalManager globalManager) {
    globalManager.addListener(new GlobalManagerAdapter() {
      @Override
      public void downloadManagerAdded(DownloadManager download) {
        if (!download.getDownloadState().getFlag(DownloadManagerState.FLAG_METADATA_DOWNLOAD)) {
          return;
        }
        download.addListener(new DownloadManagerAdapter() {
          @Override
          public void stateChanged(DownloadManager manager, int state) {
            if (state != DownloadManager.STATE_WAITING && state != DownloadManager.STATE_READY) {
              return;
            }
            Worker.submit(() -> { // Not during BiglyBT's state change notification
              synchronized (this) { // One at a time: initializing or starting a download twice makes BiglyBT fail it
                int currState = manager.getState();
                if (currState == DownloadManager.STATE_WAITING) {
                  manager.initialize();
                } else if (currState == DownloadManager.STATE_READY) {
                  manager.startDownload();
                }
              }
            });
          }
        }, true);
      }
    }, true);
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

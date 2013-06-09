package torrent;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import debug.Debug;
import gnu.trove.iterator.TIntIterator;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.impl.IpFilterImpl;
import org.gudy.azureus2.core3.ipfilter.impl.IpRangeImpl;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManagerDefaults;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import search.util.SwingWorkerUtil;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.io.CleanUp;
import util.io.Write;

public class Magnet extends Thread {

    private static GuiListener guiListener;
    private static final Object saveTorrentLock = new Object(), azureusConfigLock = new Object();
    private static final CountDownLatch ipInitializerStartSignal = new CountDownLatch(1);
    private static boolean isAzureusConfigured;
    private static volatile AzureusCore core;
    private String magnetLink;
    private static final AtomicBoolean isPortPossiblyBlocked = new AtomicBoolean(true), magnetDownloadAttempted = new AtomicBoolean();
    private final AtomicBoolean isDoneDownloading = new AtomicBoolean(), isDoneSaving = new AtomicBoolean();
    private static volatile String ipBlockMsg = "";
    private static SwingWorker<?, ?> azureusStarter;
    private static IpInitializer ipInitializer;
    public final File torrentFile;

    public Magnet(String magnetLink, String torrentName) {
        this.magnetLink = magnetLink;
        Write.fileOp(Constant.TORRENTS_DIR, Write.MK_DIR);
        torrentFile = new File(Constant.TORRENTS_DIR + torrentName + ".torrent");
    }

    public void download(SwingWorker<?, ?> parent) throws Exception {
        if (torrentExists()) {
            return;
        }

        int timeout = guiListener.getTimeout(), waitTime = Integer.parseInt(Str.get(391)), numChecks, milliSeconds = 1000;
        if (timeout < waitTime) {
            numChecks = waitTime / milliSeconds;
        } else {
            numChecks = timeout / milliSeconds;
        }

        start();
        for (int i = 0; i < numChecks; i++) {
            try {
                if (isDHTConnecting()) {
                    Connection.setStatusBar(Constant.CONNECTING + "torrent network to convert magnet link to torrent file...it may take many seconds"
                            + ipBlockMsg);
                } else {
                    Connection.setStatusBar(Constant.TRANSFERRING + "torrent network to convert magnet link to torrent file...it may take a few seconds"
                            + ipBlockMsg);
                }
                join(milliSeconds);
                if ((!isDoneDownloading.get() && torrentExists()) || isDoneSaving.get() || parent.isCancelled()) {
                    break;
                }
            } finally {
                Connection.unsetStatusBar();
            }
        }
        interrupt();

        if (!torrentExists()) {
            throw new ConnectionException(Connection.error("downloading a torrent", "", ""));
        }
    }

    @Override
    public void run() {
        try {
            download();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    private void download() throws Exception {
        magnetDownloadAttempted.set(true);
        TorrentImpl torrent = (TorrentImpl) TorrentManagerImpl.getSingleton().getURLDownloader(new URL(magnetLink)).download();
        isPortPossiblyBlocked.set(false);
        isDoneDownloading.set(true);

        if (torrentExists()) {
            return;
        }
        synchronized (saveTorrentLock) {
            if (torrentFile.exists()) {
                return;
            }

            TOTorrent toTorrent = torrent.getTorrent();
            if (toTorrent.isCreated()) {
                TorrentUtils.addCreatedTorrent(toTorrent);
            }

            Method serialiseToByteArray = TOTorrentImpl.class.getDeclaredMethod("serialiseToByteArray");
            serialiseToByteArray.setAccessible(true);
            byte[] torrentBytes = (byte[]) serialiseToByteArray.invoke(toTorrent);

            String vuzeDir = System.getProperty(Str.get(564));
            if (vuzeDir != null && (new String(torrentBytes, Constant.UTF8)).contains(vuzeDir)) {
                throw new Exception(torrentFile.getName() + " is corrupt");
            }

            OutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(torrentFile), 8192);
                bos.write(torrentBytes);
                bos.flush();
            } finally {
                CleanUp.close(bos);
            }
        }

        if (Debug.DEBUG) {
            Debug.println(torrentFile.getName() + " converted");
        }
        isDoneSaving.set(true);
    }

    public static boolean isPortPossiblyBlocked() {
        return magnetDownloadAttempted.get() && isPortPossiblyBlocked.get();
    }

    private boolean torrentExists() {
        synchronized (saveTorrentLock) {
            return torrentFile.exists();
        }
    }

    public static void waitForAzureusToStart() throws Exception {
        if (azureusStarter == null) {
            return;
        }
        SwingWorkerUtil.waitFor(azureusStarter);
    }

    public static void initIPs() {
        configAzureus();
        ipInitializer = new IpInitializer();
        ipInitializer.setPriority(Thread.MIN_PRIORITY);
        ipInitializer.start();
        ipInitializerStartSignal.countDown();
    }

    public static synchronized void startAzureus() {
        if (core != null) {
            return;
        }
        (azureusStarter = new SwingWorker<Object, Object[]>() {
            @Override
            protected Object doInBackground() {
                initAzureus();
                return null;
            }
        }).execute();
    }

    private static void configAzureus() {
        synchronized (azureusConfigLock) {
            if (isAzureusConfigured) {
                return;
            }

            Str.waitForUpdate(); // Attempt a HTTPS connection to ensure HTTPS connections are not possibly prevented by Azureus javax.net.ssl.trustStore bug

            System.setProperty("azureus.security.manager.install", "0");
            System.setProperty("azureus.security.manager.permitexit", "1");
            System.setProperty("MULTI_INSTANCE", Constant.TRUE);
            String vuzeDir = Constant.APP_DIR + "vuze" + Constants.AZUREUS_VERSION.replace(".", "") + Constant.FILE_SEPARATOR + "vuze" + Constant.FILE_SEPARATOR;
            Write.fileOp(vuzeDir, Write.MK_DIR);
            System.setProperty("azureus.install.path", vuzeDir);
            System.setProperty("azureus.config.path", vuzeDir);
            System.setProperty("azureus.portable.root", vuzeDir);
            SystemProperties.setUserPath(vuzeDir);

            COConfigurationManager.initialise();
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

            isAzureusConfigured = true;
        }
    }

    private static synchronized void initAzureus() {
        try {
            Connection.setStatusBar(Constant.CONNECTING + "torrent network to convert magnet link to torrent file");

            configAzureus();
            setPorts(guiListener.getPort());

            core = AzureusCoreFactory.create();
            initIpFilter();

            Connection.setStatusBar(Constant.CONNECTING + "torrent network to convert magnet link to torrent file" + ipBlockMsg);

            AZInstance instance = core.getInstanceManager().getMyInstance();
            if (Debug.DEBUG) {
                Debug.println("TCP Port: " + instance.getTCPListenPort() + "\nUDP Port: " + instance.getUDPListenPort() + "\nUDP Non-Data Port: "
                        + instance.getUDPNonDataListenPort());
            }

            Collection<String> enabledPluginNames = new ArrayList<String>(16), enabledPluginIDs = new ArrayList<String>(8);
            Collections.addAll(enabledPluginNames, "DHT", "DHT Tracker", "Local Tracker", "Tracker Peer Auth", "uTP Plugin", "Distributed DB",
                    "Distributed Tracker", "Magnet URI Handler", "External Seed", "LAN Peer Finder");
            Collections.addAll(enabledPluginIDs, "azutp", "azbpdht", "azbpdhdtracker", "azbpmagnet", "azextseed", "azlocaltracker");

            PluginManagerDefaults pluginManagerDefaults = core.getPluginManagerDefaults();
            String[] pluginNames = pluginManagerDefaults.getDefaultPlugins();
            for (String pluginName : pluginNames) {
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

            PluginInterface[] pluginInterfaces = core.getPluginManager().getPlugins();
            for (PluginInterface pluginInterface : pluginInterfaces) {
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

    public static void setGuiListener(GuiListener listener) {
        guiListener = listener;
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

    private static synchronized void initIpFilter() {
        try {
            ipInitializerStartSignal.await();
            ipInitializer.join();

            IpFilter ipFilter = IpFilterImpl.getInstance();

            TIntIterator iterator = ipInitializer.ips.iterator();
            while (iterator.hasNext()) {
                final int ip = iterator.next();
                ipFilter.addRange(new IpRangeImpl("", ip, ip, true));
            }
            ipInitializer.ips.clear(0);

            iterator = ipInitializer.ipRanges.iterator();
            while (iterator.hasNext()) {
                ipFilter.addRange(new IpRangeImpl("", iterator.next(), iterator.next(), true));
            }
            ipInitializer.ipRanges.clear(0);

            setIpBlockMsg(ipFilter);
            if (Debug.DEBUG) {
                Debug.println(ipBlockMsg);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    public static synchronized void enableIpFilter(boolean enable) {
        if (core == null) {
            return;
        }
        IpFilter ipFilter = IpFilterImpl.getInstance();
        if (ipFilter.isEnabled() != enable) {
            ipFilter.setInRangeAddressesAreAllowed(!enable);
            ipFilter.setEnabled(enable);
            setIpBlockMsg(enable ? ipFilter : null);
        }
    }

    private static void setIpBlockMsg(IpFilter ipFilter) {
        long numBlockedIps;
        ipBlockMsg = (ipFilter != null && (numBlockedIps = ipFilter.getTotalAddressesInRange()) > 0 ? " (blocking "
                + (new DecimalFormat("#,###")).format(numBlockedIps) + " untrusty IPs with " + Constant.IP_FILTER + ")" : "");
    }
}

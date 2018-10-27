package torrent;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import listener.GuiListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.impl.IpFilterImpl;
import org.gudy.azureus2.core3.ipfilter.impl.IpRangeImpl;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManagerDefaults;
import org.gudy.azureus2.plugins.PluginState;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;
import str.Str;
import util.AbstractWorker;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;
import util.Worker;

public class Magnet extends Thread {

    private static final Object saveTorrentLock = new Object(), azureusConfigLock = new Object();
    private static final CountDownLatch ipFilterInitializerStartSignal = new CountDownLatch(1);
    private static final AtomicBoolean isAzureusConfigured = new AtomicBoolean();
    public static final String IP_FILTER_VERSION = "ipfilter" + Constant.APP_VERSION, VUZE_VERSION = "vuze" + Constants.AZUREUS_VERSION.replace(".", "");
    private static final String VUZE_DIR = Constant.APP_DIR + VUZE_VERSION + Constant.FILE_SEPARATOR + "vuze" + Constant.FILE_SEPARATOR, IP_FILTER_TOGGLE
            = "Ip Filter Enabled";
    private static volatile AzureusCore core;
    public final String MAGNET_LINK;
    public final File TORRENT;
    private final AtomicBoolean isDoneDownloading = new AtomicBoolean(), isDoneSaving = new AtomicBoolean();
    private static final ConcurrentMap<String, Thread> downloaders = new ConcurrentHashMap<String, Thread>(16);
    private static volatile String ipBlockMsg = "";
    private static volatile Worker azureusStarter;
    private static Thread ipFilterInitializer;

    public Magnet(String magnetLink) {
        MAGNET_LINK = magnetLink;
        IO.fileOp(Constant.TORRENTS_DIR, IO.MK_DIR);
        TORRENT = new File(Constant.TORRENTS_DIR + Str.hashCode(magnetLink) + Constant.TORRENT);
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
                    Connection.setStatusBar(Str.str("connecting2") + Str.str("connecting3") + ipBlockMsg);
                } else {
                    Connection.setStatusBar(Str.str("transferring2") + Str.str("connecting3") + ipBlockMsg);
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
        Thread downloader = downloaders.putIfAbsent(MAGNET_LINK, this);
        if (downloader != null) {
            downloader.join();
            return;
        }
        try {
            byte[] torrentBytes = FileUtil.readInputStreamAsByteArray(ResourceDownloaderFactoryImpl.getSingleton().create(new URL(MAGNET_LINK)).download(),
                    BDecoder.MAX_BYTE_ARRAY_SIZE);
            isDoneDownloading.set(true);

            if (torrentExists()) {
                return;
            }
            synchronized (saveTorrentLock) {
                if (TORRENT.exists()) {
                    return;
                }
                IO.write(TORRENT, torrentBytes);

                if (!COConfigurationManager.getBooleanParameter(IP_FILTER_TOGGLE)) {
                    COConfigurationManager.setParameter(IP_FILTER_TOGGLE, true);
                }
            }

            if (Debug.DEBUG) {
                Debug.println(TORRENT.getName() + " converted");
            }
            isDoneSaving.set(true);
        } finally {
            downloaders.remove(MAGNET_LINK);
        }
    }

    private boolean torrentExists() {
        synchronized (saveTorrentLock) {
            return TORRENT.exists();
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

    public static boolean canFilterIpsWithoutBlocking() {
        return IO.listFiles(VUZE_DIR + "torrents").length != 0;
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

            System.setProperty("azureus.security.manager.install", "0");
            System.setProperty("azureus.security.manager.permitexit", "1");
            System.setProperty("MULTI_INSTANCE", String.valueOf(true));
            System.setProperty("azureus.platform.manager.disable", String.valueOf(true));
            boolean canFilterIpsWithoutBlocking = canFilterIpsWithoutBlocking();
            IO.fileOp(VUZE_DIR, IO.MK_DIR);
            System.setProperty("azureus.install.path", VUZE_DIR);
            System.setProperty("azureus.config.path", VUZE_DIR);
            System.setProperty("azureus.portable.root", VUZE_DIR);
            SystemProperties.setUserPath(VUZE_DIR);

            COConfigurationManager.initialise();
            Connection.setAuthenticator();
            COConfigurationManager.setParameter("max active torrents", 256);
            COConfigurationManager.setParameter("max downloads", 256);
            COConfigurationManager.setParameter(IP_FILTER_TOGGLE, canFilterIpsWithoutBlocking);
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
            setPorts(guiListener.getPort());

            core = AzureusCoreFactory.create();

            try {
                ipFilterInitializerStartSignal.await();
                ipFilterInitializer.join();
                setIpBlockMsg(IpFilterImpl.getInstance());
                guiListener.setPlaylistPlayHint(ipBlockMsg);
                if (Debug.DEBUG) {
                    Debug.println(ipBlockMsg.trim());
                }
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }

            Connection.setStatusBar(Str.str("connecting4") + ipBlockMsg);

            if (Debug.DEBUG) {
                AZInstance instance = core.getInstanceManager().getMyInstance();
                Debug.println("TCP Port: " + instance.getTCPListenPort() + "\nUDP Port: " + instance.getUDPListenPort() + "\nUDP Non-Data Port: "
                        + instance.getUDPNonDataListenPort());
            }

            Collection<String> enabledPluginNames = new ArrayList<String>(16), enabledPluginIDs = new ArrayList<String>(8);
            Collections.addAll(enabledPluginNames, "DHT", "DHT Tracker", "Local Tracker", "Tracker Peer Auth", "uTP Plugin", "Distributed DB",
                    "Distributed Tracker", "Magnet URI Handler", "External Seed", "LAN Peer Finder");
            Collections.addAll(enabledPluginIDs, "azutp", "azbpdht", "azbpdhdtracker", "azbpmagnet", "azextseed", "azlocaltracker");

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

    private static void setIpBlockMsg(IpFilter ipFilter) {
        long numBlockedIps;
        ipBlockMsg = (ipFilter != null && (numBlockedIps = ipFilter.getTotalAddressesInRange()) > 0 ? ' ' + Str.str("ipFiltering", Str.getNumFormat(
                "#,###").format(numBlockedIps)) : "");
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
            File blockedIPs = new File(Constant.APP_DIR + Constant.IP_FILTER), ipFilterVersion = new File(Constant.APP_DIR + IP_FILTER_VERSION);
            if (!blockedIPs.exists() || !ipFilterVersion.exists()) {
                try {
                    IO.unzip(Constant.PROGRAM_DIR + Constant.IP_FILTER + Constant.ZIP, Constant.APP_DIR);
                } catch (Exception e) {
                    IO.fileOp(blockedIPs, IO.RM_FILE);
                    throw e;
                }
                IO.fileOp(ipFilterVersion, IO.MK_FILE);
            }

            String line;
            BufferedReader br = null;
            Pattern ipPattern = Regex.pattern("\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+\\.\\d{1,3}+");
            IpFilter ipFilter = IpFilterImpl.getInstance();

            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(blockedIPs), Constant.UTF8));
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

                        ipFilter.addRange(new IpRangeImpl("", startIp, endIp, true));
                        break;
                    }
                }
            } finally {
                IO.close(br);
            }
        }
    }

    public static String getIP(GuiListener guiListener) throws Exception {
        startAzureus(guiListener);
        waitForAzureusToStart();
        return core.getInstanceManager().getMyInstance().getExternalAddress().getHostAddress();
    }
}

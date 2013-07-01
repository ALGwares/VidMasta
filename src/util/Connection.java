package util;

import debug.Debug;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import listener.GuiListener;
import main.Str;
import main.Str.UpdateListener;
import util.io.CleanUp;
import util.io.Read;
import util.io.Write;

public class Connection {

    private static GuiListener guiListener;
    public static final int DOWNLOAD_LINK_INFO = 0, VIDEO_INFO = 1, SEARCH_ENGINE = 2, TRAILER = 3, VIDEO_STREAMER = 4, UPDATE = 5, SUBTITLE = 6;
    private static final Queue<Msg> statusBarMessages = new ConcurrentLinkedQueue<Msg>();
    private static final StatusBar statusBar = new StatusBar();
    private static final Collection<Long> cache = new ConcurrentSkipListSet<Long>();
    private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
    private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
    private static volatile String downloadLinkInfoFailUrl;

    static {
        Str.addListener(new UpdateListener() {
            @Override
            public void update(String[] strs) {
                try {
                    updateDownloadLinkInfoProxy(strs);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
            }
        });
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String msg = "A login is requested by " + getShortUrl(getRequestingURL().toString(), false) + ".";
                String prompt = getRequestingPrompt().trim();
                if (!prompt.isEmpty()) {
                    msg += " The site says: ''" + prompt + "''";
                }
                if (guiListener.isAuthorizationConfirmed(msg)) {
                    char[] password = guiListener.getAuthorizationPassword();
                    PasswordAuthentication passwordAuthentication = new PasswordAuthentication(guiListener.getAuthorizationUsername(), password);
                    for (int i = 0; i < password.length; i++) {
                        password[i] = '\0';
                    }
                    return passwordAuthentication;
                }
                return null;
            }
        });
    }

    public static String getUpdateFile(String file) throws Exception {
        return getUpdateFile(file, true);
    }

    public static String getUpdateFile(String file, boolean showStatus) throws Exception {
        return getSource(file, UPDATE, showStatus, true, false, true).trim();
    }

    public static String getSourceCode(String url, int connectionType) throws Exception {
        return getSourceCode(url, connectionType, true, false);
    }

    public static String getSourceCode(String url, int connectionType, boolean showStatus) throws Exception {
        return getSourceCode(url, connectionType, showStatus, false);
    }

    public static String getSourceCode(String url, int connectionType, boolean showStatus, boolean emptyOK) throws Exception {
        if (url == null || url.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("Internal error: the URL is null or empty.");
            }
            return "";
        }

        Long urlHashCode = Str.hashCode(url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        String sourceCode, sourceCodePath = Constant.CACHE_DIR + urlHashCode.toString() + Constant.HTML;
        if (cache.contains(urlHashCode)) {
            if (Debug.DEBUG) {
                Debug.println("fetching " + url);
            }
            try {
                sourceCode = Read.read(sourceCodePath);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                Write.fileOp(sourceCodePath, Write.RM_FILE);
                sourceCode = getSource(url, connectionType, showStatus, emptyOK, true, false);
                addToCache(sourceCode, sourceCodePath, urlHashCode);
            }
        } else {
            sourceCode = getSource(url, connectionType, showStatus, emptyOK, true, false);
            addToCache(sourceCode, sourceCodePath, urlHashCode);
        }

        return sourceCode;
    }

    private static String getSource(String url, int connectionType, boolean showStatus, boolean emptyOK, boolean compress, boolean throwException)
            throws Exception {
        if (Debug.DEBUG) {
            Debug.println(url);
        }
        HttpURLConnection connection = null;
        BufferedReader br = null;
        StringBuilder source = new StringBuilder(262144);
        try {
            Proxy proxy = getProxy(connectionType);
            String statusMsg = checkProxy(proxy, url, showStatus);
            connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
            setConnectionProperties(connection, compress);
            br = new BufferedReader(new InputStreamReader(connect(connection), Constant.UTF8));

            if (showStatus) {
                setStatusBar(Constant.TRANSFERRING + statusMsg);
            }

            String line;
            while ((line = br.readLine()) != null) {
                source.append(line).append(Constant.NEWLINE);
            }
        } catch (IOException e) {
            if (throwException) {
                throw e;
            }
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (showStatus) {
                String downloadLinkInfoUrl = deproxyDownloadLinkInfoProxyUrl(url);
                if (downloadLinkInfoUrl != null) {
                    selectNextDownloadLinkInfoProxy();
                    return getSourceCode(downloadLinkInfoUrl, connectionType, showStatus, emptyOK);
                } else if (url.startsWith(Str.get(467))) {
                    downloadLinkInfoFail.set(true);
                }
            }
            throw new ConnectionException(error("", "", url), connection == null ? null : connection.getURL().toString());
        } finally {
            if (showStatus) {
                unsetStatusBar();
            }
            CleanUp.close(connection, br);
        }

        if (!emptyOK && source.length() == 0) {
            throw new ConnectionException(error("", "", url));
        }

        return source.toString();
    }

    public static String error(String problem, String solution, String url) {
        return (problem == null ? "" : "There was a problem " + (problem.isEmpty() ? "connecting to " : problem)) + (url == null ? "" : getShortUrl(url, false))
                + (solution == null ? "." : ". Try " + solution + "increasing the connection timeout under the search menu.");
    }

    public static String error(String url) {
        return getShortUrl(url, false) + " is experiencing technical issues. Please retry.";
    }

    private static String deproxyDownloadLinkInfoProxyUrl(String downloadLinkInfoProxyUrl) {
        return downloadLinkInfoProxyUrl.startsWith(Str.get(466)) ? Str.get(467) + downloadLinkInfoProxyUrl.substring(Str.get(466).length()) : null;
    }

    public static void failDownloadLinkInfo() {
        downloadLinkInfoFail.set(true);
        selectNextDownloadLinkInfoProxy();
    }

    private static void selectNextDownloadLinkInfoProxy() {
        String proxies = Str.get(516);
        if (proxies.isEmpty()) {
            return;
        }

        if (downloadLinkInfoProxyLock.tryLock()) {
            try {
                File proxyIndexFile = new File(Constant.APP_DIR + Constant.DOWNLOAD_LINK_INFO_PROXY_INDEX);
                int proxyIndex = (proxyIndexFile.exists() ? Integer.parseInt(Read.read(proxyIndexFile)) : 0);
                Write.write(proxyIndexFile, String.valueOf(++proxyIndex >= Regex.split(proxies, Constant.SEPARATOR1).length ? 0 : proxyIndex));
                Str.update();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            } finally {
                downloadLinkInfoProxyLock.unlock();
            }
        }
    }

    private static void updateDownloadLinkInfoProxy(String[] strs) throws Exception {
        File proxyIndexFile;
        if (strs[516].isEmpty() || !(proxyIndexFile = new File(Constant.APP_DIR + Constant.DOWNLOAD_LINK_INFO_PROXY_INDEX)).exists()) {
            return;
        }

        int proxyIndex = Integer.parseInt(Read.read(proxyIndexFile));
        String[] proxies = Regex.split(strs[516], Constant.SEPARATOR1);
        String nextProxy = proxies[proxyIndex >= proxies.length ? 0 : proxyIndex];
        downloadLinkInfoFailUrl = strs[518];
        int downloadLinkInfoFailUrlLen = downloadLinkInfoFailUrl.length();

        for (String indexToUpdate : Regex.split(strs[517], ",")) {
            int indexToUpdateNum = Integer.parseInt(indexToUpdate);
            strs[indexToUpdateNum] = nextProxy + strs[indexToUpdateNum].substring(downloadLinkInfoFailUrlLen);
        }
    }

    public static String downloadLinkInfoFailUrl() {
        return downloadLinkInfoFailUrl == null ? Str.get(505) : downloadLinkInfoFailUrl;
    }

    public static boolean downloadLinkInfoFail() {
        return downloadLinkInfoFail.get();
    }

    public static void downloadLinkInfoUnFail() {
        downloadLinkInfoFail.set(false);
    }

    private static InputStream connect(HttpURLConnection connection) throws Exception {
        connection.connect();
        String encoding = connection.getContentEncoding();
        InputStream is = connection.getInputStream();
        if (encoding != null) {
            encoding = encoding.toLowerCase(Locale.ENGLISH);
            if (encoding.equals("gzip")) {
                is = new GZIPInputStream(is, 512);
            } else if (encoding.equals("deflate")) {
                is = new InflaterInputStream(is, new Inflater(), 512);
            }
        }
        return is;
    }

    public static void setConnectionProperties(HttpURLConnection connection) {
        setConnectionProperties(connection, false);
    }

    public static void setConnectionProperties(HttpURLConnection connection, boolean compress) {
        connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1;q=0.9, us-ascii;q=0.8, *;q=0.7");
        connection.setRequestProperty("Accept-Language", "en-US, en-GB;q=0.9, en;q=0.8, *;q=0.7");
        if (compress) {
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        }
        connection.setRequestProperty("User-Agent", Str.get(301));
        int timeout = guiListener.getTimeout();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
    }

    private static void addToCache(String sourceCode, String sourceCodePath, Long urlHashCode) {
        try {
            Write.write(sourceCodePath, sourceCode);
            cache.add(urlHashCode);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            Write.fileOp(sourceCodePath, Write.RM_FILE);
        }
    }

    public static void removeDownloadLinkInfoProxyUrlFromCache(String downloadLinkInfoProxyUrl) {
        removeFromCache(downloadLinkInfoProxyUrl);
        String downloadLinkInfoUrl = deproxyDownloadLinkInfoProxyUrl(downloadLinkInfoProxyUrl);
        if (downloadLinkInfoUrl != null) {
            removeFromCache(downloadLinkInfoUrl);
        }
    }

    public static void removeFromCache(String url) {
        Long urlHashCode = Str.hashCode(url);
        Write.fileOp(Constant.CACHE_DIR + urlHashCode.toString() + Constant.HTML, Write.RM_FILE);
        cache.remove(urlHashCode);
    }

    public static void clearCache() {
        File[] files = (new File(Constant.CACHE_DIR)).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(Constant.HTML)) {
                    Write.fileOp(file, Write.RM_FILE);
                }
            }
        }
        cache.clear();
        downloadLinkInfoFail.set(false);
    }

    public static void saveData(String url, String outputPath, int connectionType) throws Exception {
        saveData(url, outputPath, connectionType, true);
    }

    public static void saveData(String url, String outputPath, int connectionType, boolean showStatus) throws Exception {
        if (Debug.DEBUG) {
            Debug.println(url);
        }
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            Proxy proxy = getProxy(connectionType);
            String statusMsg = checkProxy(proxy, url, showStatus);
            connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
            setConnectionProperties(connection);
            is = connection.getInputStream();
            os = new FileOutputStream(outputPath);

            if (showStatus) {
                setStatusBar(Constant.TRANSFERRING + statusMsg);
            }

            Write.write(is, os);
        } finally {
            if (showStatus) {
                unsetStatusBar();
            }
            CleanUp.close(connection, is, os);
        }
    }

    public static String getShortUrl(String url, boolean showDots) {
        String host = Regex.match(url, Str.get(302)), dots = Str.get(303);
        if (host.isEmpty()) {
            host = url;
            dots = Str.get(304);
        }
        return Regex.replaceFirst(Regex.replaceFirst(host, Str.get(305), Str.get(306)), Str.get(307), Str.get(308)) + (showDots ? dots : Str.get(309));
    }

    public static Proxy getProxy(int connectionType) throws Exception {
        String selectedProxy = guiListener.getSelectedProxy();
        if (selectedProxy.equals(Constant.NO_PROXY)
                || !((guiListener.canProxyDownloadLinkInfo() && connectionType == DOWNLOAD_LINK_INFO)
                || (guiListener.canProxyVideoInfo() && connectionType == VIDEO_INFO)
                || (guiListener.canProxySearchEngines() && connectionType == SEARCH_ENGINE)
                || (guiListener.canProxyTrailers() && connectionType == TRAILER)
                || (guiListener.canProxyVideoStreamers() && connectionType == VIDEO_STREAMER)
                || (guiListener.canProxyUpdates() && connectionType == UPDATE)
                || (guiListener.canProxySubtitles() && connectionType == SUBTITLE))) {
            return Proxy.NO_PROXY;
        }

        String[] ipPort = Regex.split(getProxy(selectedProxy), Str.get(256));
        return new Proxy(Type.HTTP, new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])));
    }

    public static String checkProxy(Proxy proxy, String url, boolean showStatus) throws Exception {
        String statusMsg = (showStatus ? getShortUrl(url, true) : null);
        if (proxy != Proxy.NO_PROXY) {
            InputStream is = null;
            HttpURLConnection connection = null;
            try {
                if (showStatus) {
                    InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
                    statusMsg += " (using proxy " + socketAddress.getAddress().getHostAddress() + ':' + socketAddress.getPort() + ')';
                    setStatusBar(Constant.CONNECTING + statusMsg);
                }

                connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
                setConnectionProperties(connection);
                is = connection.getInputStream();

                if (showStatus) {
                    setStatusBar(Constant.TRANSFERRING + statusMsg);
                }

                is.read();
            } catch (IOException e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                throw new ProxyException("Proxy failed for " + error(null, "using a different proxy or ", url));
            } finally {
                if (showStatus) {
                    unsetStatusBar();
                }
                CleanUp.close(connection, is);
            }
        }

        if (showStatus) {
            setStatusBar(Constant.CONNECTING + statusMsg);
        }

        return statusMsg;
    }

    public static String getProxy(String proxy) {
        if (!Regex.isMatch(proxy, Str.get(250))) {
            return null;
        }

        String[] ipPort = Regex.split(Regex.match(proxy, Str.get(253)), Str.get(254));
        StringBuilder ip = new StringBuilder(16);
        String[] ipParts = Regex.split(ipPort[0], Str.get(255));

        for (int i = 0; i < 4; i++) {
            int ipPartNum = Integer.parseInt(ipParts[i].trim());
            if (ipPartNum < 0 || ipPartNum > 255) {
                return null;
            }
            if (i < 3) {
                ip.append(ipPartNum).append('.');
            } else {
                ip.append(ipPartNum).append(':');
            }
        }

        String port = ipPort[1].trim();
        int portNum = Integer.parseInt(port);
        if (portNum < 0 || portNum > 65535) {
            return null;
        }

        return Regex.replaceAll(ip.toString() + port, Str.get(251), Str.get(252));
    }

    public static boolean isPeerBlockRunning() {
        BufferedReader br = null;
        try {
            Process tasklist = (new ProcessBuilder("tasklist")).start();
            br = new BufferedReader(new InputStreamReader(tasklist.getInputStream(), Constant.UTF8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(Constant.PEER_BLOCK + Constant.EXE)) {
                    return true;
                }
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        } finally {
            CleanUp.close(br);
        }
        return false;
    }

    public static void setGuiListener(GuiListener listener) {
        guiListener = listener;
    }

    public static void startStatusBar() {
        statusBar.setPriority(Thread.MIN_PRIORITY);
        statusBar.start();
    }

    public static void stopStatusBar() {
        statusBar.interrupt();
    }

    public static void setStatusBar(String str) {
        Msg msg = new Msg(Thread.currentThread(), str);
        statusBarMessages.remove(msg);
        statusBarMessages.add(msg);
    }

    public static void unsetStatusBar() {
        statusBarMessages.remove(new Msg(Thread.currentThread(), null));
    }

    private static class StatusBar extends Thread {

        StatusBar() {
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        Msg msg = statusBarMessages.peek();
                        if (msg == null) {
                            guiListener.clearStatusBar();
                        } else {
                            if (!msg.thread.isAlive()) {
                                statusBarMessages.remove(msg);
                                if (Debug.DEBUG) {
                                    Debug.println("status bar message thread died");
                                }
                                continue;
                            }
                            guiListener.setStatusBar(msg.msg);
                        }
                    } catch (Exception e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                    Thread.sleep(250);
                }
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.println("status bar stopped: " + e.toString());
                }
            }
        }
    }

    private static class Msg {

        Thread thread;
        String msg;

        Msg(Thread thread, String msg) {
            this.thread = thread;
            this.msg = msg;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Msg)) {
                return false;
            }
            Msg msgObj = (Msg) obj;
            return thread.equals(msgObj.thread);
        }

        @Override
        public int hashCode() {
            return 7 * 31 + (thread == null ? 0 : thread.hashCode());
        }
    }

    public static void browse(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
        } else {
            guiListener.msg("Update Java on your computer at http://www.java.com in order to complete your action. Or manually enter the following URL into a"
                    + " web browser:" + Constant.NEWLINE2 + url + Constant.NEWLINE, Constant.ERROR_MSG);
        }
    }

    public static void browseFile(String file) throws Exception {
        try {
            String command;
            if (Constant.WINDOWS) {
                command = "rundll32 url.dll,FileProtocolHandler";
            } else if (Constant.MAC) {
                command = "open";
            } else {
                command = "xdg-open";
            }
            Runtime.getRuntime().exec(command + " " + file);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN)) {
                Desktop.getDesktop().open(new File(file));
            } else {
                guiListener.msg("Update Java on your computer at http://www.java.com in order to complete your action. Or manually enter the following file"
                        + " location into a web browser:" + Constant.NEWLINE2 + "file://" + file + Constant.NEWLINE, Constant.ERROR_MSG);
            }
        }
    }

    private Connection() {
    }
}

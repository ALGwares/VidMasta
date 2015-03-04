package util;

import debug.Debug;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import listener.DomainType;
import listener.GuiListener;
import listener.StrUpdateListener.UpdateListener;
import str.Str;
import util.RunnableUtil.AbstractWorker;

public class Connection {

    private static GuiListener guiListener;
    private static final StatusBar statusBar = new StatusBar();
    private static final Collection<Long> cache = new ConcurrentSkipListSet<Long>();
    private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
    private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
    private static volatile String downloadLinkInfoFailUrl;

    public static void init(GuiListener listener) {
        guiListener = listener;
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
                    Arrays.fill(password, '\0');
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
        return getSourceCode(file, DomainType.UPDATE, showStatus, true, false, true).trim();
    }

    public static String getSourceCode(String url, DomainType domainType) throws Exception {
        return getSourceCode(url, domainType, true, false);
    }

    public static String getSourceCode(String url, DomainType domainType, boolean showStatus) throws Exception {
        return getSourceCode(url, domainType, showStatus, false);
    }

    public static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK) throws Exception {
        if (url == null || url.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("Internal error: the URL is null or empty.");
            }
            return "";
        }

        Long urlHashCode = Str.hashCode(url);
        String sourceCode, sourceCodePath = Constant.CACHE_DIR + urlHashCode + Constant.HTML;
        if (cache.contains(urlHashCode)) {
            if (Debug.DEBUG) {
                Debug.println("fetching " + url);
            }
            try {
                sourceCode = IO.read(sourceCodePath);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                IO.fileOp(sourceCodePath, IO.RM_FILE);
                addToCache(sourceCode = getSourceCode(url, domainType, showStatus, emptyOK, true, false), sourceCodePath, urlHashCode);
            }
        } else {
            addToCache(sourceCode = getSourceCode(url, domainType, showStatus, emptyOK, true, false), sourceCodePath, urlHashCode);
        }

        return sourceCode;
    }

    public static String getSourceCode(final String url, final DomainType domainType, final boolean showStatus, final boolean emptyOK, final boolean compress,
            final boolean throwIOException) throws Exception {
        if (Debug.DEBUG) {
            Debug.println(url);
        }
        return (new AbstractWorker<String>() {
            @Override
            protected String call() throws Exception {
                HttpURLConnection connection = null;
                BufferedReader br = null;
                StringBuilder source = new StringBuilder(262144);
                try {
                    Proxy proxy = getProxy(domainType);
                    String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this);
                    if (isCancelled()) {
                        return "";
                    }

                    connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
                    if (isCancelled()) {
                        return "";
                    }

                    setConnectionProperties(connection, compress);
                    br = new BufferedReader(new InputStreamReader(connect(connection), Constant.UTF8));
                    if (isCancelled()) {
                        return "";
                    }

                    if (showStatus) {
                        setStatusBar(Constant.TRANSFERRING + statusMsg);
                    }

                    String line;
                    while ((line = br.readLine()) != null) {
                        if (isCancelled()) {
                            return "";
                        }
                        source.append(line).append(Constant.NEWLINE);
                    }

                    checkConnectionResponse(connection, url);
                    if (!emptyOK && source.length() == 0) {
                        throw new IOException("empty source code");
                    }
                } catch (IOException e) {
                    IO.consumeErrorStream(connection);
                    if (throwIOException) {
                        throw e;
                    }
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    if (domainType == DomainType.DOWNLOAD_LINK_INFO && showStatus && !isIgnorable(e)) {
                        String downloadLinkInfoUrl = deproxyDownloadLinkInfoProxyUrl(url);
                        if (downloadLinkInfoUrl != null) {
                            selectNextDownloadLinkInfoProxy();
                            return getSourceCode(downloadLinkInfoUrl, domainType, showStatus, emptyOK);
                        } else if (url.startsWith(Str.get(467))) {
                            downloadLinkInfoFail.set(true);
                        }
                    }
                    throw new ConnectionException(error("", "", url), e, connection == null ? null : connection.getURL().toString());
                } finally {
                    if (showStatus) {
                        unsetStatusBar();
                    }
                    IO.close(connection, br);
                }

                return source.toString();
            }
        }).runAndWaitFor();
    }

    public static boolean isIgnorable(Throwable t) {
        return t instanceof FileNotFoundException && Regex.isMatch(String.valueOf(HttpURLConnection.HTTP_NOT_FOUND), 599);
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
                int proxyIndex = (proxyIndexFile.exists() ? Integer.parseInt(IO.read(proxyIndexFile)) : 0);
                IO.write(proxyIndexFile, String.valueOf(++proxyIndex >= Regex.split(proxies, Constant.SEPARATOR1).length ? 0 : proxyIndex));
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

        int proxyIndex = Integer.parseInt(IO.read(proxyIndexFile));
        String[] proxies = Regex.split(strs[516], Constant.SEPARATOR1);
        String nextProxy = proxies[proxyIndex >= proxies.length ? 0 : proxyIndex];
        downloadLinkInfoFailUrl = strs[518];
        int downloadLinkInfoFailUrlLen = downloadLinkInfoFailUrl.length();

        for (String indexToUpdate : Regex.split(strs[671], ",")) {
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

    public static void unfailDownloadLinkInfo() {
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
        int timeout = guiListener.getTimeout() * 1000;
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
    }

    public static void checkConnectionResponse(HttpURLConnection connection, String url) throws IOException {
        if (!Regex.isMatch(String.valueOf(connection.getResponseCode()), 599)) {
            if (Debug.DEBUG) {
                Debug.println("'" + url + "' response: '" + connection.getResponseMessage() + "'");
            }
            throw new IOException(error("", "", url));
        }
    }

    private static void addToCache(String sourceCode, String sourceCodePath, Long urlHashCode) {
        try {
            IO.write(sourceCodePath, sourceCode);
            cache.add(urlHashCode);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            IO.fileOp(sourceCodePath, IO.RM_FILE);
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
        IO.fileOp(Constant.CACHE_DIR + urlHashCode + Constant.HTML, IO.RM_FILE);
        cache.remove(urlHashCode);
    }

    public static void clearCache() {
        for (File file : IO.listFiles(Constant.CACHE_DIR)) {
            if (file.getName().endsWith(Constant.HTML)) {
                IO.fileOp(file, IO.RM_FILE);
            }
        }
        cache.clear();
        downloadLinkInfoFail.set(false);
    }

    public static void saveData(String url, String outputPath, DomainType domainType) throws Exception {
        saveData(url, outputPath, domainType, true);
    }

    public static void saveData(final String url, final String outputPath, final DomainType domainType, final boolean showStatus) throws Exception {
        if (Debug.DEBUG) {
            Debug.println(url);
        }
        (new AbstractWorker<Object>() {
            @Override
            protected Object call() throws Exception {
                HttpURLConnection connection = null;
                InputStream is = null;
                OutputStream os = null;
                boolean outputStarted = false;
                try {
                    Proxy proxy = getProxy(domainType);
                    String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this);
                    if (isCancelled()) {
                        return null;
                    }

                    connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
                    if (isCancelled()) {
                        return null;
                    }

                    setConnectionProperties(connection);
                    is = connection.getInputStream();
                    if (isCancelled()) {
                        return null;
                    }

                    os = new BufferedOutputStream(new FileOutputStream(outputPath)) {
                        @Override
                        public synchronized void write(byte[] bytes, int startOffset, int numBytes) throws IOException {
                            if (isCancelled()) {
                                throw new CancellationException();
                            }
                            super.write(bytes, startOffset, numBytes);
                        }
                    };

                    if (showStatus) {
                        setStatusBar(Constant.TRANSFERRING + statusMsg);
                    }

                    outputStarted = true;
                    IO.write(is, os);

                    checkConnectionResponse(connection, url);
                } catch (Exception e) {
                    IO.consumeErrorStream(connection);
                    if (outputStarted) {
                        IO.close(os);
                        IO.fileOp(outputPath, IO.RM_FILE);
                    }
                    throw e;
                } finally {
                    if (showStatus) {
                        unsetStatusBar();
                    }
                    IO.close(connection, is, os);
                }
                return null;
            }
        }).runAndWaitFor();
    }

    public static String getShortUrl(String url, boolean showDots) {
        String host = Regex.firstMatch(url, 302), dots = Str.get(303);
        if (host.isEmpty()) {
            host = url;
            dots = Str.get(304);
        }
        return Regex.replaceFirst(Regex.replaceFirst(host, 305), 307) + (showDots ? dots : Str.get(309));
    }

    public static Proxy getProxy(DomainType domainType) {
        String selectedProxy = guiListener.getSelectedProxy();
        if (selectedProxy.equals(Constant.NO_PROXY)
                || !((guiListener.canProxyDownloadLinkInfo() && domainType == DomainType.DOWNLOAD_LINK_INFO)
                || (guiListener.canProxyVideoInfo() && domainType == DomainType.VIDEO_INFO)
                || (guiListener.canProxySearchEngines() && domainType == DomainType.SEARCH_ENGINE)
                || (guiListener.canProxyTrailers() && domainType == DomainType.TRAILER)
                || (guiListener.canProxyVideoStreamers() && domainType == DomainType.VIDEO_STREAMER)
                || (guiListener.canProxyUpdates() && domainType == DomainType.UPDATE)
                || (guiListener.canProxySubtitles() && domainType == DomainType.SUBTITLE))) {
            return Proxy.NO_PROXY;
        }

        String[] ipPort = Regex.split(getProxy(selectedProxy), 256);
        return new Proxy(Type.HTTP, new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])));
    }

    private static String checkProxyAndSetStatusBar(Proxy proxy, String url, boolean showStatus, AbstractWorker<?> callingWorker) throws Exception {
        String statusMsg;
        if (showStatus) {
            final Thread runner = Thread.currentThread();
            callingWorker.doneListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    statusBar.unset(runner);
                }
            };
            statusMsg = getShortUrl(url, true);
        } else {
            statusMsg = null;
        }

        if (!proxy.equals(Proxy.NO_PROXY)) {
            InputStream is = null;
            HttpURLConnection connection = null;
            try {
                if (showStatus) {
                    InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
                    statusMsg += " (using proxy " + socketAddress.getAddress().getHostAddress() + ':' + socketAddress.getPort() + ')';
                    setStatusBar(Constant.CONNECTING + statusMsg);
                }

                connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
                if (callingWorker.isCancelled()) {
                    return "";
                }

                setConnectionProperties(connection);
                is = connection.getInputStream();
                if (callingWorker.isCancelled()) {
                    return "";
                }

                if (showStatus) {
                    setStatusBar(Constant.TRANSFERRING + statusMsg);
                }

                is.read();
                if (callingWorker.isCancelled()) {
                    return "";
                }

                checkConnectionResponse(connection, url);
            } catch (IOException e) {
                IO.consumeErrorStream(connection);
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                throw new ProxyException("Proxy failed for " + error(null, "using a different proxy or ", url));
            } finally {
                if (showStatus) {
                    unsetStatusBar();
                }
                IO.close(connection, is);
            }
        }

        if (showStatus) {
            setStatusBar(Constant.CONNECTING + statusMsg);
        }

        return statusMsg;
    }

    public static String getProxy(String proxy) {
        if (!Regex.isMatch(proxy, 250)) {
            return null;
        }

        String[] ipPort = Regex.split(Regex.firstMatch(proxy, 253), 254), ipParts = Regex.split(ipPort[0], 255);
        StringBuilder ip = new StringBuilder(16);

        for (int i = 0; i < 4; i++) {
            int ipPartNum = Integer.parseInt(ipParts[i].trim());
            if (ipPartNum < 0 || ipPartNum > 255) {
                return null;
            }
            ip.append(ipPartNum).append(i < 3 ? '.' : ':');
        }

        String port = ipPort[1].trim();
        int portNum = Integer.parseInt(port);
        if (portNum < 0 || portNum > 65535) {
            return null;
        }

        return Regex.replaceAll(ip + port, 251);
    }

    public static void startStatusBar() {
        statusBar.setPriority(Thread.MIN_PRIORITY);
        statusBar.start();
    }

    public static void stopStatusBar() {
        statusBar.interrupt();
    }

    public static void setStatusBar(String str) {
        statusBar.set(Thread.currentThread(), ' ' + str);
    }

    public static void unsetStatusBar() {
        statusBar.unset(Thread.currentThread());
    }

    private static class StatusBar extends Thread {

        private final Queue<Msg> msgs = new ConcurrentLinkedQueue<Msg>();

        StatusBar() {
        }

        void set(Thread thread, String str) {
            Msg msg = new Msg(thread, str);
            msgs.remove(msg);
            msgs.add(msg);
        }

        void unset(Thread thread) {
            msgs.remove(new Msg(thread, null));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        Msg msg = msgs.peek();
                        if (msg == null) {
                            guiListener.clearStatusBar();
                        } else {
                            if (!msg.thread.isAlive()) {
                                msgs.remove(msg);
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
                    Debug.println("status bar stopped: " + e);
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
                return this == obj || (obj instanceof Msg && thread.equals(((Msg) obj).thread));
            }

            @Override
            public int hashCode() {
                return 7 * 31 + (thread == null ? 0 : thread.hashCode());
            }
        }
    }

    public static void email(String subject, String body) throws IOException {
        email("mailto:?subject=" + encodeMailtoArg(subject) + "&body=" + encodeMailtoArg(body));
    }

    public static void email(String url) throws IOException {
        browse(url, "an email client", "mailto");
    }

    private static String encodeMailtoArg(String arg) throws IOException {
        return URLEncoder.encode(arg, Constant.UTF8).replace("+", "%20");
    }

    public static void browse(String url) throws IOException {
        browse(url, "a web browser", "HTTP");
    }

    public static void browse(String url, String applicationType, String linkType) throws IOException {
        Desktop desktop;
        if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Action.BROWSE)) {
            try {
                desktop.browse(URI.create(url));
            } catch (IOException e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                throw new IOException("Associate/install an application (e.g. " + applicationType + ") for " + linkType + " links and then retry.");
            }
        } else {
            throw new IOException(
                    "Update Java on your computer at http://www.java.com in order to complete your action. Or manually open the following link with "
                    + applicationType + ':' + Constant.NEWLINE2 + url + Constant.NEWLINE);
        }
    }

    public static void updateError(final Exception e) {
        if (e instanceof UpdateException) {
            return;
        }

        Thread errorNotifier = new Thread() {
            @Override
            public void run() {
                try {
                    setStatusBar("Update error: " + ExceptionUtil.toString(e));
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e2) {
                        if (Debug.DEBUG) {
                            Debug.print(e2);
                        }
                    }
                } finally {
                    unsetStatusBar();
                }
            }
        };
        errorNotifier.setPriority(Thread.MIN_PRIORITY);
        errorNotifier.start();
    }

    private Connection() {
    }
}

package util;

import debug.Debug;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import listener.DomainType;
import listener.GuiListener;
import listener.StrUpdateListener.UpdateListener;
import str.Str;

public class Connection {

  private static GuiListener guiListener;
  private static final StatusBar statusBar = new StatusBar();
  private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
  private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
  private static int numDownloadLinkInfoDeproxiers;
  private static UpdateListener deproxyDownloadLinkInfo;
  private static boolean reproxyDownloadLinkInfoUrlSet;
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
    setAuthenticator();
  }

  public static void setAuthenticator() {
    try {
      Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          String host = getRequestingHost();
          if (host == null) {
            URL url = getRequestingURL();
            if (url == null) {
              InetAddress site = getRequestingSite();
              if (site != null) {
                host = site.getHostName();
              }
            } else {
              host = url.getHost();
            }
          }
          String msg = Str.str("loginRequested", host == null ? "" : host);
          String prompt = getRequestingPrompt().trim();
          if (!prompt.isEmpty()) {
            msg += ' ' + Str.str("websiteMsg") + ' ' + prompt;
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
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
  }

  public static String getUpdateFile(String file) throws Exception {
    return getUpdateFile(file, true);
  }

  public static String getUpdateFile(String file, boolean showStatus) throws Exception {
    return getSourceCode(file, DomainType.UPDATE, showStatus, true, false, Constant.MS_1HR, IOException.class).trim();
  }

  public static String getSourceCode(String url, DomainType domainType, long cacheExpirationMs) throws Exception {
    return getSourceCode(url, domainType, true, false, true, cacheExpirationMs);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, long cacheExpirationMs) throws Exception {
    return getSourceCode(url, domainType, showStatus, false, true, cacheExpirationMs);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK, boolean compress, long cacheExpirationMs,
          Class<?>... throwables) throws Exception {
    if (url == null || url.isEmpty()) {
      if (Debug.DEBUG) {
        Debug.println("Internal error: the URL is null or empty.");
      }
      return "";
    }

    Long urlHashCode = Str.hashCode(url);
    String sourceCode;
    File sourceCodeFile = new File(Constant.CACHE_DIR + urlHashCode + Constant.HTML);
    if (sourceCodeFile.exists()) {
      if (IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
        try {
          addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, compress, cacheExpirationMs, throwables), sourceCodeFile);
        } catch (InterruptedException | CancellationException e) {
          throw e;
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.println("fetching (due to " + ThrowableUtil.cause(e) + ") " + url);
          }
          sourceCode = IO.read(sourceCodeFile);
        }
      } else {
        if (Debug.DEBUG) {
          Debug.println("fetching " + url);
        }
        try {
          sourceCode = IO.read(sourceCodeFile);
        } catch (InterruptedException | CancellationException e) {
          throw e;
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, compress, cacheExpirationMs, throwables), sourceCodeFile);
        }
      }
    } else {
      addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, compress, cacheExpirationMs, throwables), sourceCodeFile);
    }

    return sourceCode;
  }

  private static String getSourceCodeHelper(final String url, final DomainType domainType, final boolean showStatus, final boolean emptyOK,
          final boolean compress, final long cacheExpirationMs, final Class<?>... throwables) throws Exception {
    if (Debug.DEBUG) {
      Debug.println(url);
    }
    return (new AbstractWorker<String>() {
      @Override
      protected String doInBackground() throws Exception {
        HttpURLConnection connection = null;
        BufferedReader br = null;
        StringBuilder source = new StringBuilder(8192);
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

          setConnectionProperties(connection, compress, null);
          connection.connect();
          br = IO.bufferedReader(connection.getContentEncoding(), connection.getInputStream());
          if (isCancelled()) {
            return "";
          }

          if (showStatus) {
            setStatusBar(Str.str("transferring") + ' ' + statusMsg);
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
          String errorMsg = IO.consumeErrorStream(connection);
          for (Class<?> throwable : throwables) {
            if (throwable.isInstance(e)) {
              throw new IOException2(e, errorMsg);
            }
          }
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          if (domainType == DomainType.DOWNLOAD_LINK_INFO && showStatus) {
            String proxy, proxies; // Store because variables are concurrently set
            if (url.startsWith(Str.get(731))) {
              downloadLinkInfoFail.set(true);
            } else if (url.startsWith(proxy = Str.get(723))) {
              selectNextDownloadLinkInfoProxy();
              return getSourceCode(Str.get(731) + url.substring(proxy.length()), domainType, showStatus, emptyOK, compress, cacheExpirationMs, throwables);
            } else if (!(proxies = Str.get(726)).isEmpty()) {
              for (String currProxy : Regex.split(proxies, Constant.SEPARATOR1)) {
                if (url.startsWith(currProxy)) {
                  return getSourceCode(Str.get(731) + url.substring(currProxy.length()), domainType, showStatus, emptyOK, compress, cacheExpirationMs,
                          throwables);
                }
              }
            }
          }
          throw new ConnectionException(error(url), e, connection == null ? null : connection.getURL().toString());
        } finally {
          if (showStatus) {
            unsetStatusBar();
          }
          IO.close(br);
        }

        return domainType == DomainType.UPDATE ? source.toString() : Regex.replaceAll(source.toString(), 741);
      }
    }).executeAndGet();
  }

  public static String error(String url) {
    return Str.str("connectionProblem", getShortUrl(url, false)) + ' ' + Str.str("connectionSolution");
  }

  public static String serverError(String url) {
    return Str.str("serverProblem", getShortUrl(url, false)) + ' ' + Str.str("pleaseRetry");
  }

  public static void runDownloadLinkInfoDeproxier(Task deproxier) throws Exception {
    try {
      downloadLinkInfoProxyLock.lock();
      try {
        ++numDownloadLinkInfoDeproxiers;
      } finally {
        downloadLinkInfoProxyLock.unlock();
      }
      deproxier.run();
    } finally {
      downloadLinkInfoProxyLock.lock();
      try {
        if (--numDownloadLinkInfoDeproxiers == 0 && Str.containsListener(deproxyDownloadLinkInfo)) {
          Str.removeListener(deproxyDownloadLinkInfo);
          deproxyDownloadLinkInfo = null;
          if (!reproxyDownloadLinkInfoUrlSet) {
            selectNextDownloadLinkInfoProxy();
          }
          reproxyDownloadLinkInfoUrlSet = false;
        }
      } finally {
        downloadLinkInfoProxyLock.unlock();
      }
    }
  }

  public static boolean deproxyDownloadLinkInfo() {
    downloadLinkInfoProxyLock.lock();
    try {
      if (Str.containsListener(deproxyDownloadLinkInfo)) {
        return false;
      }

      Str.addListener(deproxyDownloadLinkInfo = new UpdateListener() {
        @Override
        public void update(String[] strs) {
          int downloadLinkInfoUrlLen = strs[727].length();
          for (String indexToUpdate : Regex.split(strs[732], ",")) {
            int indexToUpdateNum = Integer.parseInt(indexToUpdate);
            strs[indexToUpdateNum] = strs[731] + strs[indexToUpdateNum].substring(downloadLinkInfoUrlLen);
          }
        }
      });

      if (Debug.DEBUG) {
        Debug.print(new ConnectionException("Download link info deproxied."));
      }

      return true;
    } finally {
      downloadLinkInfoProxyLock.unlock();
    }
  }

  public static boolean isDownloadLinkInfoDeproxied() {
    downloadLinkInfoProxyLock.lock();
    try {
      return Str.containsListener(deproxyDownloadLinkInfo);
    } finally {
      downloadLinkInfoProxyLock.unlock();
    }
  }

  private static void selectNextDownloadLinkInfoProxy() {
    String proxies = Str.get(726);
    if (proxies.isEmpty()) {
      return;
    }

    if (downloadLinkInfoProxyLock.tryLock()) {
      try {
        File proxyIndexFile = new File(Constant.APP_DIR + Constant.DOWNLOAD_LINK_INFO_PROXY_INDEX);
        int proxyIndex = (proxyIndexFile.exists() ? Integer.parseInt(IO.read(proxyIndexFile)) : 0);
        IO.write(proxyIndexFile, String.valueOf(++proxyIndex >= Regex.split(proxies, Constant.SEPARATOR1).length ? 0 : proxyIndex));
        Str.update();
        reproxyDownloadLinkInfoUrlSet = true;
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
    if (strs[726].isEmpty() || !(proxyIndexFile = new File(Constant.APP_DIR + Constant.DOWNLOAD_LINK_INFO_PROXY_INDEX)).exists()) {
      return;
    }

    int proxyIndex = Integer.parseInt(IO.read(proxyIndexFile));
    String[] proxies = Regex.split(strs[726], Constant.SEPARATOR1);
    String nextProxy = proxies[proxyIndex >= proxies.length ? 0 : proxyIndex];
    downloadLinkInfoFailUrl = strs[727];
    int downloadLinkInfoFailUrlLen = downloadLinkInfoFailUrl.length();

    for (String indexToUpdate : Regex.split(strs[732], ",")) {
      int indexToUpdateNum = Integer.parseInt(indexToUpdate);
      strs[indexToUpdateNum] = nextProxy + strs[indexToUpdateNum].substring(downloadLinkInfoFailUrlLen);
    }
  }

  public static String downloadLinkInfoFailUrl() {
    return downloadLinkInfoFailUrl == null ? Str.get(725) : downloadLinkInfoFailUrl;
  }

  public static boolean downloadLinkInfoFail() {
    return downloadLinkInfoFail.get();
  }

  public static void unfailDownloadLinkInfo() {
    downloadLinkInfoFail.set(false);
  }

  public static void setConnectionProperties(HttpURLConnection connection, boolean compress, String referer) {
    connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1;q=0.9, us-ascii;q=0.8, *;q=0.7");
    connection.setRequestProperty("Accept-Language", "en-US, en-GB;q=0.9, en;q=0.8, *;q=0.7");
    if (compress) {
      connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
    }
    connection.setRequestProperty("User-Agent", Str.get(301));
    if (referer != null) {
      connection.setRequestProperty("Referer", referer);
    }
    int timeout = guiListener.getTimeout() * 1000;
    connection.setConnectTimeout(timeout);
    connection.setReadTimeout(timeout);
  }

  public static void checkConnectionResponse(HttpURLConnection connection, String url) throws IOException {
    int responseCode = connection.getResponseCode();
    if (!Regex.isMatch(String.valueOf(responseCode), 737)) {
      if (Debug.DEBUG) {
        Debug.println(url + " response: " + responseCode + " " + connection.getResponseMessage() + " " + connection.getHeaderFields());
      }
      throw responseCode == HttpURLConnection.HTTP_NOT_FOUND ? new FileNotFoundException(error(url)) : new IOException(error(url));
    }
  }

  private static void addToCache(String sourceCode, File sourceCodeFile) throws Exception {
    try {
      IO.write(sourceCodeFile, sourceCode);
    } catch (Exception e) {
      IO.write(sourceCodeFile, sourceCode);
      throw e;
    }
  }

  public static void removeDownloadLinkInfoFromCache(String url) {
    removeFromCache(url);
    if (!url.startsWith(Str.get(731))) {
      String proxy = Str.get(723), proxies; // Store because variables are concurrently set
      if (url.startsWith(proxy)) {
        removeFromCache(Str.get(731) + url.substring(proxy.length()));
      } else if (!(proxies = Str.get(726)).isEmpty()) {
        for (String currProxy : Regex.split(proxies, Constant.SEPARATOR1)) {
          if (url.startsWith(currProxy)) {
            removeFromCache(Str.get(731) + url.substring(currProxy.length()));
            return;
          }
        }
      }
    }
  }

  public static void removeFromCache(String url) {
    IO.fileOp(Constant.CACHE_DIR + Str.hashCode(url) + Constant.HTML, IO.RM_FILE);
  }

  public static void saveData(String url, String outputPath, DomainType domainType) throws Exception {
    saveData(url, outputPath, domainType, true);
  }

  public static void saveData(final String url, final String outputPath, final DomainType domainType, final boolean showStatus) throws Exception {
    saveData(url, outputPath, domainType, showStatus, null);
  }

  public static void saveData(final String url, final String outputPath, final DomainType domainType, final boolean showStatus, final String referer)
          throws Exception {
    if (Debug.DEBUG) {
      Debug.println(url);
    }
    (new Worker() {
      @Override
      protected void doWork() throws Exception {
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        boolean outputStarted = false;
        try {
          Proxy proxy = getProxy(domainType);
          String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this);
          if (isCancelled()) {
            return;
          }

          connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
          if (isCancelled()) {
            return;
          }

          setConnectionProperties(connection, false, referer);
          is = connection.getInputStream();
          if (isCancelled()) {
            return;
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
            setStatusBar(Str.str("transferring") + ' ' + statusMsg);
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
          IO.close(is, os);
        }
      }
    }).executeAndGet();
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
      callingWorker.doneAction = new Runnable() {
        @Override
        public void run() {
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
          statusMsg += ' ' + Str.str("proxing", socketAddress.getAddress().getHostAddress() + ':' + socketAddress.getPort());
          setStatusBar(Str.str("connecting") + ' ' + statusMsg);
        }

        connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
        if (callingWorker.isCancelled()) {
          return "";
        }

        setConnectionProperties(connection, false, null);
        is = connection.getInputStream();
        if (callingWorker.isCancelled()) {
          return "";
        }

        if (showStatus) {
          setStatusBar(Str.str("transferring") + ' ' + statusMsg);
        }

        is.read();
        if (callingWorker.isCancelled()) {
          return "";
        }

        checkConnectionResponse(connection, url);
      } catch (Exception e) {
        IO.consumeErrorStream(connection);
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        throw new ProxyException(Str.str("proxyProblem", getShortUrl(url, false)) + ' ' + Str.str("proxySolution"));
      } finally {
        if (showStatus) {
          unsetStatusBar();
        }
        IO.close(is);
      }
    }

    if (showStatus) {
      setStatusBar(Str.str("connecting") + ' ' + statusMsg);
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
    browse(url, "emailClient");
  }

  private static String encodeMailtoArg(String arg) throws IOException {
    return URLEncoder.encode(arg, Constant.UTF8).replace("+", "%20");
  }

  public static void browse(String url) throws IOException {
    browse(url, "webBrowser");
  }

  public static void browse(String url, String applicationType) throws IOException {
    Desktop desktop;
    if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Action.BROWSE)) {
      try {
        desktop.browse(URI.create(url));
      } catch (IOException e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        throw new IOException(Str.str(applicationType + "Needed"));
      }
    } else {
      throw new IOException(Str.str("updateJava") + ' ' + Str.str(applicationType + "Alternative") + Constant.NEWLINE2 + url + Constant.NEWLINE);
    }
  }

  public static void updateError(final Exception e) {
    if (e instanceof UpdateException) {
      return;
    }

    (new Worker() {
      @Override
      public void doWork() {
        try {
          String msg = Str.str("updateError") + ' ' + ThrowableUtil.toString(e);
          setStatusBar(msg);
          try {
            Thread.sleep(Regex.split(msg, " ").length * 400L);
          } catch (InterruptedException e2) {
            if (Debug.DEBUG) {
              Debug.print(e2);
            }
          }
        } finally {
          unsetStatusBar();
        }
      }
    }).execute();
  }

  private Connection() {
  }
}

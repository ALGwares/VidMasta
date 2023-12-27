package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.io.PrintStream;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import listener.DomainType;
import listener.GuiListener;
import listener.StrUpdateListener.UpdateListener;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Event;
import org.openqa.selenium.devtools.v113.network.Network;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriver.SystemProperty;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import str.Str;

public class Connection {

  private static GuiListener guiListener;
  private static final StatusBar statusBar = new StatusBar();
  private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
  private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
  private static final Cache<String, Boolean> shortTimeoutUrls = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  private static int numDownloadLinkInfoDeproxiers;
  private static UpdateListener deproxyDownloadLinkInfo;
  private static boolean reproxyDownloadLinkInfoUrlSet;
  private static volatile String downloadLinkInfoFailUrl;
  private static volatile Consumer<String> webBrowserRequestListener;
  private static Proxy webBrowserProxy = Proxy.NO_PROXY;
  private static final Lock webBrowserLock = new ReentrantLock();
  private static final ThrowingRunnable webBrowserSleep = () -> Thread.sleep(Integer.parseInt(Str.get(850)));
  private static final AtomicBoolean webBrowserInitShowStatus = new AtomicBoolean();
  private static final AtomicReference<String> webBrowserInitStatusMsg = new AtomicReference<>();
  private static final LazyInitializer<FirefoxDriver> webBrowserDriver = new LazyInitializer<FirefoxDriver>() {
    @Override
    public FirefoxDriver initialize() {
      File firefoxIndicator = new File(Constant.APP_DIR, Str.get(Constant.WINDOWS ? 836 : (Constant.MAC ? 840 : 844)));
      File webBrowserDir = new File(Constant.APP_DIR, "webBrowser");
      try {
        if (!firefoxIndicator.exists()) {
          String zipFile = firefoxIndicator.getPath() + Constant.ZIP;
          try {
            saveData(Str.get(Constant.WINDOWS ? 839 : (Constant.MAC ? 843 : 847)), zipFile, DomainType.UPDATE, webBrowserInitShowStatus.get());
            if (webBrowserInitShowStatus.get()) {
              setStatusBar(webBrowserInitStatusMsg.updateAndGet(prev -> Str.str("initializing") + "..."));
            }
            try {
              IO.fileOp(webBrowserDir, IO.RM_DIR);
              IO.unzip(zipFile, IO.dir(webBrowserDir.getPath()));
              Files.walk(webBrowserDir.toPath()).map(Path::toFile).filter(File::isFile).forEach(file -> file.setExecutable(true));
              IO.fileOp(firefoxIndicator, IO.MK_FILE);
            } finally {
              IO.fileOp(zipFile, IO.RM_FILE);
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }

        if (webBrowserInitShowStatus.get()) {
          setStatusBar(webBrowserInitStatusMsg.updateAndGet(prev -> Str.str("initializing") + "..."));
        }
        File firefoxDriver = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 837 : (Constant.MAC ? 841 : 845)));
        File firefoxBinary = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 838 : (Constant.MAC ? 842 : 846)));
        System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, firefoxDriver.getPath());
        System.setProperty(SystemProperty.BROWSER_BINARY, firefoxBinary.getPath());
        FirefoxOptions options = new FirefoxOptions();
        options.setBinary(firefoxBinary.getPath());
        options.addArguments(Regex.split(848, Constant.SEPARATOR1));
        FirefoxProfile profile = new FirefoxProfile();
        Arrays.stream(Regex.split(849, Constant.SEPARATOR2)).map(pref -> Regex.split(pref, Constant.SEPARATOR1)).forEach(pref -> profile.setPreference(pref[0],
                Regex.isMatch(pref[1], "(true)|(false)") ? Boolean.parseBoolean(pref[1]) : (Regex.isMatch(pref[1], "\\d++") ? Integer.parseInt(pref[1])
                : pref[1])));
        options.setProfile(profile);
        AtomicReference<FirefoxDriver> driverRef = new AtomicReference<>();
        AtomicBoolean quit = new AtomicBoolean();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          FirefoxDriver tempDriver = driverRef.get();
          if (tempDriver != null) {
            quit.set(true);
            tempDriver.quit();
          }
        }));

        System.setErr(new PrintStream(new OutputStream() {
          private final StringBuffer line = new StringBuffer(1024);

          @Override
          public void write(int b) {
            if (quit.get()) {
              return;
            }
            if (b == '\n') {
              String str = line.toString().toUpperCase(Locale.ENGLISH);
              if (StringUtils.containsAny(str, "FATAL:", "FATAL\t", "SEVERE:", "SEVERE\t", "ERROR:", "ERROR\t", "WARNING:", "WARNING\t", "WARN:", "WARN\t")
                      && !StringUtils.containsAny(str, "CONSOLE.ERROR:", "CONSOLE.WARN:", "JAVASCRIPT ERROR:", "JAVASCRIPT WARNING:",
                              "\"MESSAGE\":\"LOG.CLEAR\"", "UNKNOWNMETHODERROR: LOG.CLEAR:", "\tTLS CERTIFICATE ERRORS ",
                              "\tINVALID BROWSER PREFERENCES FOR CDP.")) {
                System.out.print(line);
              }
              line.setLength(0);
            } else {
              line.append((char) b);
            }
          }
        }));

        driverRef.set(new FirefoxDriver(options));
        FirefoxDriver driver = driverRef.get();
        DevTools devTools = driver.getDevTools();
        devTools.createSessionIfThereIsNotOne();
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.addListener(new Event<>("Network.requestWillBeSent", input -> {
          String requestUrl = "";
          input.beginObject();
          while (input.hasNext()) {
            switch (input.nextName()) {
              case "request":
                input.beginObject();
                while (input.hasNext()) {
                  switch (input.nextName()) {
                    case "url":
                      requestUrl = input.nextString();
                      break;
                    default:
                      input.skipValue();
                      break;
                  }
                }
                input.endObject();
                break;
              default:
                input.skipValue();
                break;
            }
          }
          input.endObject();
          return requestUrl;
        }), requestUrl -> {
          if (webBrowserRequestListener != null) {
            webBrowserRequestListener.accept(requestUrl);
          }
        });
        return driver;
      } finally {
        webBrowserInitStatusMsg.set(null);
        unsetStatusBar();
      }
    }
  };

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
    return getSourceCode(file, DomainType.UPDATE, showStatus, true, Constant.MS_1HR, IOException.class).trim();
  }

  public static String getSourceCode(String url, DomainType domainType, long cacheExpirationMs) throws Exception {
    return getSourceCode(url, domainType, true, false, cacheExpirationMs);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, long cacheExpirationMs) throws Exception {
    return getSourceCode(url, domainType, showStatus, false, cacheExpirationMs);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs, Class<?>... throwables)
          throws Exception {
    return getSourceCode(url, domainType, showStatus, emptyOK, cacheExpirationMs, 2, null, throwables);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          WebBrowserRequest webBrowserRequest) throws Exception {
    return getSourceCode(url, domainType, showStatus, emptyOK, cacheExpirationMs, 2, webBrowserRequest);
  }

  private static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs, int maxNumRedirects,
          WebBrowserRequest webBrowserRequest, Class<?>... throwables) throws Exception {
    if (url == null || url.isEmpty()) {
      if (Debug.DEBUG) {
        Debug.println("Internal error: the URL is null or empty.");
      }
      return "";
    }

    if (cacheExpirationMs <= 0) {
      return getSourceCodeHelper(url, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables);
    }

    String sourceCode;
    File sourceCodeFile = new File(Constant.CACHE_DIR + Str.hashPath(url) + Constant.HTML);
    if (sourceCodeFile.exists()) {
      if (IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
        try {
          addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables),
                  sourceCodeFile);
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
          addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables),
                  sourceCodeFile);
        }
      }
    } else {
      addToCache(sourceCode = getSourceCodeHelper(url, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables),
              sourceCodeFile);
    }

    return sourceCode;
  }

  private static String getSourceCodeHelper(String url, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          int maxNumRedirects, WebBrowserRequest webBrowserRequest, Class<?>... throwables) throws Exception {
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
          if (webBrowserRequest != null) {
            webBrowserInitShowStatus.set(showStatus);
            if (showStatus) {
              String msg = webBrowserInitStatusMsg.get();
              if (msg != null) {
                setStatusBar(msg);
              }
            }
            webBrowserDriver.get();
          }
          Proxy proxy = getProxy(domainType);
          String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this);
          if (isCancelled()) {
            return "";
          }

          if (webBrowserRequest == null) {
            connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
            if (isCancelled()) {
              return "";
            }

            setConnectionProperties(connection, null, 2);
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

            if (maxNumRedirects > 0 && Regex.isMatch(String.valueOf(connection.getResponseCode()), "30[12378]")) {
              String newUrl = connection.getHeaderField("Location");
              if (!Regex.isMatch(newUrl, "(?i)https?+:.+")) {
                URL oldUrl = new URL(url);
                newUrl = oldUrl.getProtocol() + "://" + oldUrl.getHost() + newUrl;
              }
              if (Debug.DEBUG) {
                Debug.println("following redirect from " + url + " to " + newUrl);
              }
              return getSourceCode(newUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects - 1, webBrowserRequest, throwables);
            }
            checkConnectionResponse(connection, url);
          } else {
            webBrowserLock.lockInterruptibly();
            try {
              FirefoxDriver driver = webBrowserDriver.get();
              driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(shortTimeoutUrls.getIfPresent((new URL(url)).getHost()) == null
                      ? guiListener.getTimeout() : 2));
              if (!webBrowserProxy.equals(proxy)) {
                String setProxyScript;
                if (proxy.equals(Proxy.NO_PROXY)) {
                  setProxyScript = Str.get(852);
                } else {
                  InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
                  setProxyScript = String.format(Str.get(853), socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
                }
                driver.get(Str.get(851));
                webBrowserSleep.run();
                webBrowserProxy = proxy;
                driver.executeScript(setProxyScript);
                webBrowserSleep.run();
              }
              if (showStatus) {
                setStatusBar(Str.str("transferring") + ' ' + statusMsg);
              }
              source.append(webBrowserRequest.get(url, driver, webBrowserSleep));
            } catch (Exception e) {
              throw new IOException(e);
            } finally {
              webBrowserLock.unlock();
            }
          }

          if (!emptyOK && source.length() == 0) {
            throw new IOException("empty source code");
          }
        } catch (IOException exception) {
          IOException e = exception;
          Throwable t = ThrowableUtil.rootCause(e);
          if (t instanceof WebDriverException) {
            e = new IOException(t.getClass().getName() + ": " + ((WebDriverException) t).getRawMessage());
            e.setStackTrace(t.getStackTrace());
          }
          String errorMsg = IO.consumeErrorStream(connection);
          addShortTimeoutUrls(url);
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
              addShortTimeoutUrls();
              downloadLinkInfoFail.set(true);
            } else if (url.startsWith(proxy = Str.get(723))) {
              addShortTimeoutUrls();
              selectNextDownloadLinkInfoProxy();
              return getSourceCode(Str.get(731) + url.substring(proxy.length()), domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects,
                      webBrowserRequest, throwables);
            } else if (!(proxies = Str.get(726)).isEmpty()) {
              for (String currProxy : Regex.split(proxies, Constant.SEPARATOR1)) {
                if (url.startsWith(currProxy)) {
                  addShortTimeoutUrls();
                  return getSourceCode(Str.get(731) + url.substring(currProxy.length()), domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects,
                          webBrowserRequest, throwables);
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
    return Str.str("connectionProblem", getShortUrl(url, false)) + ' ' + Str.str("pleaseRetry");
  }

  public static String serverError(String url) {
    return Str.str("serverProblem", getShortUrl(url, false)) + ' ' + Str.str("pleaseRetry");
  }

  public static void runDownloadLinkInfoDeproxier(ThrowingRunnable deproxier) throws Exception {
    try {
      downloadLinkInfoProxyLock.lockInterruptibly();
      try {
        ++numDownloadLinkInfoDeproxiers;
      } finally {
        downloadLinkInfoProxyLock.unlock();
      }
      deproxier.run();
    } finally {
      downloadLinkInfoProxyLock.lockInterruptibly();
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

  public static boolean deproxyDownloadLinkInfo() throws Exception {
    downloadLinkInfoProxyLock.lockInterruptibly();
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

  public static boolean isDownloadLinkInfoDeproxied() throws Exception {
    downloadLinkInfoProxyLock.lockInterruptibly();
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

  private static void addShortTimeoutUrls() throws Exception {
    addShortTimeoutUrls(Stream.concat(Arrays.asList(Str.get(731), Str.get(723)).stream(), Arrays.stream(Str.get(726).isEmpty() ? Constant.EMPTY_STRS
            : Regex.split(Str.get(726), Constant.SEPARATOR1)
    )).toArray(String[]::new));
  }

  private static void addShortTimeoutUrls(String... urls) throws Exception {
    for (String url : urls) {
      shortTimeoutUrls.put((new URL(url)).getHost(), true);
    }
  }

  public static void setConnectionProperties(HttpURLConnection connection, String referer, int shortTimeoutSecs) {
    connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1;q=0.9, us-ascii;q=0.8, *;q=0.7");
    connection.setRequestProperty("Accept-Language", "en-US, en-GB;q=0.9, en;q=0.8, *;q=0.7");
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
    connection.setRequestProperty("User-Agent", Str.get(301));
    if (referer != null) {
      connection.setRequestProperty("Referer", referer);
    }
    int timeout = (shortTimeoutUrls.getIfPresent(connection.getURL().getHost()) == null ? guiListener.getTimeout() : shortTimeoutSecs) * 1000;
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
    IO.fileOp(Constant.CACHE_DIR + Str.hashPath(url) + Constant.HTML, IO.RM_FILE);
  }

  public static void saveData(String url, String outputPath, DomainType domainType) throws Exception {
    saveData(url, outputPath, domainType, true);
  }

  public static void saveData(String url, String outputPath, DomainType domainType, boolean showStatus) throws Exception {
    saveData(url, outputPath, domainType, showStatus, null);
  }

  public static void saveData(String url, String outputPath, DomainType domainType, boolean showStatus, String referer) throws Exception {
    saveData(url, outputPath, domainType, showStatus, referer, 2, null);
  }

  public static void saveData(String url, String outputPath, DomainType domainType, boolean showStatus, String referer, int maxNumRedirects, String cookie)
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

          setConnectionProperties(connection, referer, 5);
          if (cookie != null) {
            connection.setRequestProperty("Cookie", cookie);
          }
          connection.connect();

          if (maxNumRedirects > 0 && Regex.isMatch(String.valueOf(connection.getResponseCode()), "30[12378]")) {
            String newUrl = connection.getHeaderField("Location");
            if (!Regex.isMatch(newUrl, "(?i)https?+:.+")) {
              URL oldUrl = new URL(url);
              newUrl = oldUrl.getProtocol() + "://" + oldUrl.getHost() + newUrl;
            }
            if (Debug.DEBUG) {
              Debug.println("following redirect from " + url + " to " + newUrl);
            }
            List<String> cookies = Stream.concat(Arrays.asList(cookie).stream(), ObjectUtils.defaultIfNull(connection.getHeaderFields().get("Set-Cookie"),
                    Collections.<String>emptyList()).stream()).filter(currCookie -> currCookie != null).collect(Collectors.toList());
            saveData(newUrl, outputPath, domainType, showStatus, referer, maxNumRedirects - 1, cookies.isEmpty() ? null : cookies.stream().collect(
                    Collectors.joining(";")));
            return;
          }

          is = IO.inputStream(connection.getContentEncoding(), connection.getInputStream());
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
      Thread runner = Thread.currentThread();
      callingWorker.doneAction = () -> statusBar.unset(runner);
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

        setConnectionProperties(connection, null, 5);
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

  public static void updateError(Exception e) {
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

  public static class WebBrowserRequest {

    private final Integer subrequestUrlRegexIndex;
    public final AtomicReference<String> subrequestUrl = new AtomicReference<>();
    public final AtomicReference<String> cookies = new AtomicReference<>();

    public WebBrowserRequest(Integer subrequestUrlRegexIndex) {
      this.subrequestUrlRegexIndex = subrequestUrlRegexIndex;
    }

    public String get(String url, FirefoxDriver driver, ThrowingRunnable sleep) throws Exception {
      if (subrequestUrlRegexIndex == null) {
        driver.get(url);
        return driver.getPageSource();
      }

      final LinkedBlockingDeque<String> subrequestUrlQueue = new LinkedBlockingDeque<>();
      try {
        webBrowserRequestListener = request -> {
          if (Regex.isMatch(request, subrequestUrlRegexIndex)) {
            subrequestUrlQueue.add(request);
          }
        };
        driver.get(url);
        triggerSubRequest(driver, sleep);
        subrequestUrl.set(Objects.requireNonNull(subrequestUrlQueue.pollFirst(driver.manage().timeouts().getPageLoadTimeout().getSeconds(), TimeUnit.SECONDS)));
        cookies.set(driver.manage().getCookies().stream().map(Cookie::toString).collect(Collectors.joining(";")));
      } finally {
        webBrowserRequestListener = null;
      }
      return "<subrequest/>";
    }

    protected void triggerSubRequest(FirefoxDriver driver, ThrowingRunnable sleep) throws Exception {
    }
  }

  private Connection() {
  }
}

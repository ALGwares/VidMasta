package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import java.io.InputStreamReader;
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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import listener.DomainType;
import listener.GuiListener;
import listener.StrUpdateListener.UpdateListener;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.function.TriFunction;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Event;
import org.openqa.selenium.devtools.v113.network.Network;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriver.SystemProperty;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rauschig.jarchivelib.ArchiverFactory;
import str.Str;

public class Connection {

  private static GuiListener guiListener;
  private static final StatusBar statusBar = new StatusBar();
  public static final int MAX_NUM_REDIRECTS = 3;
  private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
  private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
  private static final Cache<String, Boolean> shortTimeoutUrls = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  private static int numDownloadLinkInfoDeproxiers;
  private static UpdateListener deproxyDownloadLinkInfo;
  private static boolean reproxyDownloadLinkInfoUrlSet;
  private static volatile String downloadLinkInfoFailUrl;
  private static final AtomicReference<Consumer<String>> webBrowserRequestListener = new AtomicReference<>();
  private static final AtomicReference<Consumer<String>> webBrowserResponseListener = new AtomicReference<>();
  private static Proxy webBrowserProxy = Proxy.NO_PROXY;
  private static final Lock webBrowserLock = new ReentrantLock();
  private static final ThrowingRunnable webBrowserSleep = () -> Thread.sleep(Integer.parseInt(Str.get(896)));
  private static final AtomicBoolean webBrowserInitShowStatus = new AtomicBoolean(), curlInitShowStatus = new AtomicBoolean();
  private static final AtomicReference<String> webBrowserInitStatusMsg = new AtomicReference<>();
  private static final AtomicReference<LazyInitializer<FirefoxDriver>> webBrowserDriver = new AtomicReference<>();
  private static final LazyInitializer<String> curl = new LazyInitializer<String>() {
    @Override
    protected String initialize() {
      try {
        File curlDir = new File(Constant.APP_DIR, "curl"), curlIndicator = new File(Constant.APP_DIR, Str.get(911));
        if (!curlIndicator.exists()) {
          File curlCompressed = new File(Constant.APP_DIR, Str.get(925));
          Connection.saveData(Str.get(Constant.WINDOWS ? 908 : (Constant.MAC ? 909 : 910)), curlCompressed.getPath(), DomainType.UPDATE,
                  curlInitShowStatus.get());
          try {
            IO.fileOp(curlDir, IO.RM_DIR);
            ArchiverFactory.createArchiver(curlCompressed).extract(curlCompressed, new File(curlDir, curlIndicator.getName()));
            IO.fileOp(curlIndicator, IO.MK_FILE);
          } finally {
            IO.fileOp(curlCompressed, IO.RM_FILE);
          }
        }
        String curlCmd = IO.findFile(curlDir, Regex.pattern(912)).getPath();
        List<String> curlArgs = Lists.newArrayList(curlCmd);
        Collections.addAll(curlArgs, Regex.split(929, Constant.SEPARATOR1));
        ProcessBuilder curlBuilder = new ProcessBuilder(curlArgs);
        curlBuilder.redirectErrorStream(true);
        Process curlProcess = curlBuilder.start();
        StringBuffer curlOutput = new StringBuffer(512);
        Worker.submit(() -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(curlProcess.getInputStream(), Constant.UTF8))) {
            String line;
            while ((line = br.readLine()) != null) {
              curlOutput.append(line).append(Constant.NEWLINE);
            }
          }
        });
        if (!curlProcess.waitFor(10, TimeUnit.SECONDS)) {
          curlProcess.destroy();
          throw new RuntimeException("\n" + curlOutput + "\nCurl initialization timed out.");
        }
        int exitValue = curlProcess.exitValue();
        if (exitValue != 0) {
          throw new RuntimeException("\n" + curlOutput + "\nCurl initialization bad exit value: " + exitValue + ".");
        }
        if (Debug.DEBUG) {
          Debug.println("curl initialized");
        }
        return curlCmd;
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        Throwable cause = ThrowableUtil.rootCause(e);
        if (cause instanceof InterruptedException || cause instanceof CancellationException) {
          throw new RuntimeException(e);
        }
        return null;
      }
    }
  };

  private static FirefoxDriver createWebBrowser(boolean restart) throws Exception {
    if (restart) {
      webBrowserDriver.set(null);
    }
    webBrowserDriver.compareAndSet(null, new LazyInitializer<FirefoxDriver>() {
      @Override
      protected FirefoxDriver initialize() {
        try {
          File webBrowserDir = new File(Constant.APP_DIR, "webBrowser");
          File firefoxBinary = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 838 : (Constant.MAC ? 842 : 846)));
          File firefoxDriver = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 837 : (Constant.MAC ? 841 : 845)));
          Consumer<Boolean> killWebBrowser = wait -> Arrays.asList(firefoxBinary, firefoxDriver).forEach(exe -> {
            try {
              Process process = (new ProcessBuilder(Constant.WINDOWS ? Arrays.asList("powershell", "get-process | where-object {$_.path -eq '" + exe.getPath()
                      + "' -or ($_.name -eq '" + StringUtils.substringBeforeLast(exe.getName(), ".") + "' -and $_.path -eq $null)} | stop-process -force")
                      : Arrays.asList("pkill", "-9", "-f", exe.getPath()))).start();
              if (wait) {
                process.waitFor();
              }
            } catch (Exception e) {
              if (Debug.DEBUG) {
                Debug.print(e);
              }
            }
          });
          killWebBrowser.accept(true);

          File firefoxIndicator = new File(Constant.APP_DIR, Str.get(Constant.WINDOWS ? 836 : (Constant.MAC ? 840 : 844)));
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
          System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, firefoxDriver.getPath());
          System.setProperty(SystemProperty.BROWSER_BINARY, firefoxBinary.getPath());
          FirefoxOptions options = new FirefoxOptions();
          options.setBinary(firefoxBinary.getPath());
          options.setAcceptInsecureCerts(true);
          options.addArguments(Regex.split(848, Constant.SEPARATOR1));
          FirefoxProfile profile = new FirefoxProfile();
          Arrays.stream(Regex.split(849, Constant.SEPARATOR2)).map(pref -> Regex.split(pref, Constant.SEPARATOR1)).forEach(pref -> profile.setPreference(pref[0],
                  Regex.isMatch(pref[1], "(true)|(false)") ? Boolean.parseBoolean(pref[1]) : (Regex.isMatch(pref[1], "\\d++") ? Integer.parseInt(pref[1])
                  : pref[1])));
          Arrays.asList(869, 870).forEach(i -> profile.setPreference(Str.get(i), Constant.TEMP_DIR));
          options.setProfile(profile);
          AtomicReference<FirefoxDriver> driverRef = new AtomicReference<>();
          AtomicBoolean quit = new AtomicBoolean();
          Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FirefoxDriver tempDriver = driverRef.get();
            if (tempDriver != null) {
              quit.set(true);
              try {
                tempDriver.quit();
              } catch (Exception e) {
                if (Debug.DEBUG) {
                  Debug.print(e);
                }
              }
            }
            killWebBrowser.accept(false);
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
          TriFunction<String, String, AtomicReference<Consumer<String>>, Void> addUrlListener = (evtName, urlType, urlHandler) -> {
            devTools.addListener(new Event<>("Network." + evtName, input -> {
              String url = "";
              input.beginObject();
              while (input.hasNext()) {
                if (input.nextName().equals(urlType)) {
                  input.beginObject();
                  while (input.hasNext()) {
                    switch (input.nextName()) {
                      case "url":
                        url = input.nextString();
                        break;
                      default:
                        input.skipValue();
                        break;
                    }
                  }
                  input.endObject();
                } else {
                  input.skipValue();
                }
              }
              input.endObject();
              return url;
            }), url -> Optional.ofNullable(urlHandler.get()).ifPresent(handler -> handler.accept(url)));
            return null;
          };
          addUrlListener.apply("requestWillBeSent", "request", webBrowserRequestListener);
          addUrlListener.apply("responseReceived", "response", webBrowserResponseListener);
          if (Debug.DEBUG) {
            Debug.println("web browser " + (restart ? "re" : "") + "created");
          }
          return driver;
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
          Throwable cause = ThrowableUtil.rootCause(e);
          if (cause instanceof InterruptedException || cause instanceof CancellationException) {
            throw e;
          }
          return null;
        } finally {
          webBrowserInitStatusMsg.set(null);
          unsetStatusBar();
        }
      }
    });
    return webBrowserDriver.get().get();
  }

  public static void startHttpClients() {
    if (Boolean.parseBoolean(Str.get(926))) {
      Worker.submit(() -> curl.get());
    } else {
      curlInitShowStatus.set(true);
    }
    if (Boolean.parseBoolean(Str.get(927))) {
      Worker.submit(() -> createWebBrowser(false));
    } else {
      webBrowserInitShowStatus.set(true);
    }
  }

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
    return getSourceCode(url, url, domainType, showStatus, emptyOK, cacheExpirationMs, MAX_NUM_REDIRECTS, null, throwables);
  }

  public static String getSourceCode(String url, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          WebBrowserRequest webBrowserRequest) throws Exception {
    return getSourceCode(url, url, domainType, showStatus, emptyOK, cacheExpirationMs, MAX_NUM_REDIRECTS, webBrowserRequest);
  }

  private static String getSourceCode(String url, String originalUrl, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          int maxNumRedirects, WebBrowserRequest webBrowserRequest, Class<?>... throwables) throws Exception {
    if (url == null || url.isEmpty()) {
      if (Debug.DEBUG) {
        Debug.println("Internal error: the URL is null or empty.");
      }
      return "";
    }

    if (cacheExpirationMs <= 0) {
      return getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables);
    }

    String sourceCode;
    File sourceCodeFile = new File(Constant.CACHE_DIR + Str.hashPath(url) + Constant.HTML);
    if (sourceCodeFile.exists()) {
      if (IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
        try {
          addToCache(sourceCode = getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest,
                  throwables), sourceCodeFile);
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
          addToCache(sourceCode = getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest,
                  throwables), sourceCodeFile);
        }
      }
    } else {
      addToCache(sourceCode = getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest,
              throwables), sourceCodeFile);
    }

    return sourceCode;
  }

  private static String getSourceCodeHelper(String url, String originalUrl, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          int maxNumRedirects, WebBrowserRequest webBrowserRequest, Class<?>... throwables) throws Exception {
    boolean useCurl = Regex.isMatch(url, 923) && curl.get() != null;
    if (webBrowserRequest == null && Regex.isMatch(url, 871) && !useCurl && createWebBrowser(false) != null) {
      if (!Regex.isMatch(url, 873)) {
        return getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, new WebBrowserRequest(), throwables);
      }
      File temp = new File(Constant.TEMP_DIR, UUID.randomUUID().toString());
      saveData(url, temp.getPath(), domainType, showStatus, null, MAX_NUM_REDIRECTS, null, true);
      String str = IO.read(temp);
      IO.fileOp(temp, IO.RM_FILE);
      return str;
    }

    return (new AbstractWorker<String>() {
      @Override
      protected String doInBackground() throws Exception {
        HttpURLConnection connection = null;
        BufferedReader br = null;
        StringBuilder source = new StringBuilder(8192);
        try {
          boolean useWebBrowser = false;
          if (webBrowserRequest != null) {
            webBrowserInitShowStatus.set(showStatus);
            if (showStatus) {
              String msg = webBrowserInitStatusMsg.get();
              if (msg != null) {
                setStatusBar(msg);
              }
            }
            useWebBrowser = createWebBrowser(false) != null;
          }

          if (Debug.DEBUG) {
            Debug.println(url + (useWebBrowser ? " (web browser)" : (useCurl ? " (curl)" : "")));
          }

          Proxy proxy = getProxy(domainType);
          String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this, !useCurl && !useWebBrowser);
          if (isCancelled()) {
            return "";
          }

          if (!useWebBrowser) {
            if (useCurl) {
              String output = curl(url, null, null, maxNumRedirects, null, proxy, this::isCancelled);
              if (output == null) {
                return "";
              }
              source.append(IO.read(output));
              IO.fileOp(output, IO.RM_FILE);
            } else {
              connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
              if (isCancelled()) {
                return "";
              }

              setConnectionProperties(connection, null);

              post(connection);

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
                return getSourceCode(newUrl, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects - 1, webBrowserRequest,
                        throwables);
              }
              checkConnectionResponse(connection, url);
            }
          } else {
            webBrowserLock.lockInterruptibly();
            Consumer<Boolean> getSourceCode = restartWebBrowser -> ThrowingRunnable.run(() -> {
              FirefoxDriver driver = createWebBrowser(restartWebBrowser);
              driver.getDevTools().createSessionIfThereIsNotOne();
              driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(shortTimeoutUrls.getIfPresent((new URL(url)).getHost()) == null
                      ? (guiListener.getTimeout() + 45) : 45));
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
              source.append(webBrowserRequest.get(url, driver, webBrowserSleep, cacheExpirationMs));
            });
            try {
              try {
                getSourceCode.accept(false);
              } catch (TimeoutException e) {
                throw e;
              } catch (WebDriverException e) {
                if (Debug.DEBUG) {
                  Debug.println("restarting web browser because it probably died: " + Regex.firstMatch(e.toString(), ".+"));
                }
                try {
                  getSourceCode.accept(true);
                } catch (TimeoutException e2) {
                  throw e2;
                } catch (WebDriverException e2) {
                  if (!Regex.isMatch(url, 930) || curl.get() == null) {
                    throw e2;
                  }
                  if (Debug.DEBUG) {
                    Debug.println("retrying with curl because web browser failed: " + Regex.firstMatch(e2.toString(), ".+") + '\n' + url + " (curl)");
                  }
                  String output = curl(url, webBrowserRequest.outputPath, webBrowserRequest.referer, maxNumRedirects, webBrowserRequest.cookie, proxy,
                          this::isCancelled);
                  if (output == null) {
                    return "";
                  }
                  source.append(IO.read(output));
                  if (webBrowserRequest.outputPath == null) {
                    webBrowserRequest.cache(url, source.toString());
                    IO.fileOp(output, IO.RM_FILE);
                  }
                }
              }
            } catch (Exception e) {
              throw new IOException("web browser error for " + url, e);
            } finally {
              webBrowserLock.unlock();
            }
          }

          if (!emptyOK && Regex.isMatch(source.toString(), 898)) {
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
              return getSourceCode(Str.get(731) + url.substring(proxy.length()), originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs,
                      maxNumRedirects, webBrowserRequest, throwables);
            } else if (!(proxies = Str.get(726)).isEmpty()) {
              for (String currProxy : Regex.split(proxies, Constant.SEPARATOR1)) {
                if (originalUrl.startsWith(currProxy)) {
                  addShortTimeoutUrls();
                  return getSourceCode(Str.get(731) + originalUrl.substring(currProxy.length()), originalUrl, domainType, showStatus, emptyOK,
                          cacheExpirationMs, maxNumRedirects, webBrowserRequest, throwables);
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

  private static String curl(String url, String outputPath, String referer, int maxNumRedirects, String cookie, Proxy proxy, Supplier<Boolean> isCancelled)
          throws Exception {
    List<String> curlArgs = Lists.newArrayList(curl.get());
    Collections.addAll(curlArgs, Regex.split(913, Constant.SEPARATOR1));
    if (outputPath != null) {
      Collections.addAll(curlArgs, Regex.split(914, Constant.SEPARATOR1));
    }
    URL urlObj = new URL(url);
    int timeout = connectTimeoutSecs(urlObj);
    Function<String, String> quote = (Constant.WINDOWS ? str -> StringUtils.wrap(str, '"') : str -> str);
    Collections.addAll(curlArgs, Str.get(915), String.valueOf(timeout));
    Collections.addAll(curlArgs, Str.get(916), String.valueOf(maxNumRedirects));
    Optional.ofNullable(referer).ifPresent(ref -> Collections.addAll(curlArgs, Str.get(917), quote.apply(ref)));
    Optional.ofNullable(proxy.address()).map(InetSocketAddress.class::cast).ifPresent(address -> Collections.addAll(curlArgs, Str.get(918),
            quote.apply(address.getAddress().getHostAddress() + ':' + address.getPort())));
    Optional.ofNullable(postData(urlObj)).ifPresent(postData -> Collections.addAll(curlArgs, Str.get(919), quote.apply(postData)));
    Optional.ofNullable(cookie).ifPresent(cookieData -> Collections.addAll(curlArgs, Str.get(928), quote.apply(cookieData)));
    String output = (outputPath == null ? Constant.TEMP_DIR + UUID.randomUUID().toString() : outputPath);
    Collections.addAll(curlArgs, Str.get(920), quote.apply(output));
    curlArgs.add(quote.apply(url));
    ProcessBuilder curlBuilder = new ProcessBuilder(curlArgs);
    curlBuilder.redirectErrorStream(true);
    Process curlProcess = curlBuilder.start();
    try {
      if (isCancelled.get()) {
        return null;
      }

      StringBuilder curlOutput = new StringBuilder(8192);
      try (BufferedReader br = new BufferedReader(new InputStreamReader(curlProcess.getInputStream(), Constant.UTF8))) {
        String line;
        while ((line = br.readLine()) != null) {
          if (isCancelled.get()) {
            return null;
          }
          curlOutput.append(line).append(Constant.NEWLINE);
          if (!Regex.firstMatch(line, 921).isEmpty()) {
            if (Debug.DEBUG) {
              Debug.println("\n" + curlOutput);
            }
            throw new IOException(error(url));
          }
        }
      }
      if (!curlProcess.waitFor(timeout + 30, TimeUnit.SECONDS)) {
        curlProcess.destroy();
        if (Debug.DEBUG) {
          Debug.println("Curl request timed out.");
        }
        throw new IOException(error(url));
      }
      int exitValue = curlProcess.exitValue();
      if (exitValue != 0) {
        if (Debug.DEBUG) {
          Debug.println("Curl request bad exit value: " + exitValue + ".");
        }
        throw new IOException(error(url));
      }
      checkConnectionResponse(Integer.parseInt(Regex.firstMatch(curlOutput.toString(), 922)), url, () -> "");
      return output;
    } catch (Exception e) {
      IO.fileOp(output, IO.RM_FILE);
      throw e;
    } finally {
      curlProcess.destroy();
    }
  }

  public static String error(String url) {
    return Str.str("connectionProblem", getShortUrl(url, false)) + ' ' + Str.str("pleaseRetry");
  }

  public static String serverError(String url) {
    return Str.str("serverProblem", getShortUrl(url, false)) + ' ' + Str.str("pleaseRetry");
  }

  public static void runDownloadLinkInfoDeproxier(ThrowingRunnable deproxier) throws Exception {
    downloadLinkInfoProxyLock.lockInterruptibly();
    try {
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

  public static void setConnectionProperties(HttpURLConnection connection, String referer) {
    connection.setRequestProperty("Accept-Charset", "utf-8, iso-8859-1;q=0.9, us-ascii;q=0.8, *;q=0.7");
    connection.setRequestProperty("Accept-Language", "en-US, en-GB;q=0.9, en;q=0.8, *;q=0.7");
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
    connection.setRequestProperty("User-Agent", Str.get(301));
    if (referer != null) {
      connection.setRequestProperty("Referer", referer);
    }
    int timeout = connectTimeoutSecs(connection.getURL()) * 1_000;
    connection.setConnectTimeout(timeout);
    connection.setReadTimeout(timeout);
  }

  public static int connectTimeoutSecs(URL url) {
    return shortTimeoutUrls.getIfPresent(url.getHost()) == null ? guiListener.getTimeout() : 5;
  }

  public static void checkConnectionResponse(HttpURLConnection connection, String url) throws Exception {
    checkConnectionResponse(connection.getResponseCode(), url, () -> connection.getResponseMessage() + " " + connection.getHeaderFields());
  }

  private static void checkConnectionResponse(int responseCode, String url, Callable<String> errorLogMsg) throws Exception {
    if (!Regex.isMatch(String.valueOf(responseCode), 737)) {
      if (Debug.DEBUG) {
        Debug.println(url + " response: " + responseCode + " " + errorLogMsg.call());
      }
      throw responseCode == HttpURLConnection.HTTP_NOT_FOUND ? new FileNotFoundException(error(url)) : new IOException(error(url));
    }
  }

  private static void addToCache(String sourceCode, File sourceCodeFile) throws Exception {
    try {
      IO.write(sourceCodeFile, sourceCode);
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      try {
        IO.write(sourceCodeFile, sourceCode);
      } catch (Exception e2) {
        if (Debug.DEBUG) {
          Debug.print(e2);
        }
      }
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
    saveData(url, outputPath, domainType, showStatus, referer, MAX_NUM_REDIRECTS, null, false);
  }

  public static void saveData(String url, String outputPath, DomainType domainType, boolean showStatus, String referer, int maxNumRedirects, String cookie,
          boolean useWebBrowser) throws Exception {
    boolean useCurl = Regex.isMatch(url, 924) && curl.get() != null;
    if ((useWebBrowser || (Regex.isMatch(url, 872) && !useCurl)) && createWebBrowser(false) != null) {
      getSourceCode(url, domainType, showStatus, true, -1, new WebBrowserRequest(outputPath, referer, cookie) {
        @Override
        public String get(String url, FirefoxDriver driver, ThrowingRunnable sleep, long cacheExpirationMs) throws Exception {
          driver.get("about:about");
          Duration timeout = driver.manage().timeouts().getPageLoadTimeout();
          (new WebDriverWait(driver, timeout)).until(ExpectedConditions.presenceOfElementLocated(By.xpath(Str.get(874))));

          File download = null;
          Runnable clearDownloads = null;
          try {
            waitUntilResponseReceived(String.format(Str.get(875), Regex.replaceAllRepeatedly(url, 876)), () -> driver.executeScript(String.format(Str.get(878),
                    url, UUID.randomUUID().toString())), driver);
            sleep.run();

            driver.get("about:downloads");
            sleep.run();
            clearDownloads = () -> {
              try {
                (new Actions(driver)).contextClick(driver.findElement(By.xpath(Str.get(879)))).sendKeys(Keys.chord(Str.get(880))).perform();
              } catch (WebDriverException e) {
                if (Debug.DEBUG) {
                  Debug.println(e);
                }
              }
            };

            String prevStatus = "";
            for (long i = 0, sleepMs = 500, j = timeout.toMillis() / sleepMs; i < j; i++) {
              String newestDownload = Iterables.getFirst(Regex.allMatches(Optional.ofNullable(driver.getPageSource()).orElse(""), 881), Str.get(887));
              String filename = Regex.firstMatch(Regex.firstMatch(newestDownload, 882), 883);
              String status = Regex.firstMatch(Regex.firstMatch(newestDownload, 884), 885);
              if (Regex.isMatch(status, Str.get(886))) {
                File output = new File(outputPath);
                if (!(download = new File(Constant.TEMP_DIR, filename)).getCanonicalPath().equals(output.getCanonicalPath())) {
                  IO.write(download, output);
                  IO.fileOp(download, IO.RM_FILE);
                }
                clearDownloads.run();
                if (Debug.DEBUG) {
                  Debug.println(url + " (web browser save)");
                }
                return "<saveDataRequest/>";
              }
              if (!status.equals(prevStatus)) {
                i = 0;
              }
              prevStatus = status;
              Thread.sleep(sleepMs);
            }
            throw new IOException("download problem");
          } catch (Exception e) {
            if (download != null) {
              IO.fileOp(download, IO.RM_FILE);
            }
            if (clearDownloads != null) {
              clearDownloads.run();
            }
            throw e;
          }
        }
      });
      return;
    }

    if (Debug.DEBUG) {
      Debug.println(url + " (" + (useCurl ? "curl " : "") + "save)");
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
          String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this, !useCurl);
          if (isCancelled()) {
            return;
          }
          if (useCurl) {
            curl(url, outputPath, referer, maxNumRedirects, cookie, proxy, this::isCancelled);
          } else {
            connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
            if (isCancelled()) {
              return;
            }

            setConnectionProperties(connection, referer);
            if (cookie != null) {
              connection.setRequestProperty("Cookie", cookie);
            }

            post(connection);

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
                      Collectors.joining(";")), useWebBrowser);
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
          }
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

  private static String checkProxyAndSetStatusBar(Proxy proxy, String url, boolean showStatus, AbstractWorker<?> callingWorker, boolean test) throws Exception {
    String statusMsg;
    if (showStatus) {
      Thread runner = Thread.currentThread();
      callingWorker.doneAction = () -> statusBar.unset(runner);
      statusMsg = getShortUrl(url, true);
    } else {
      statusMsg = null;
    }

    if (!proxy.equals(Proxy.NO_PROXY)) {
      if (showStatus) {
        InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
        statusMsg += ' ' + Str.str("proxing", socketAddress.getAddress().getHostAddress() + ':' + socketAddress.getPort());
      }
      if (test) {
        InputStream is = null;
        HttpURLConnection connection = null;
        try {
          if (showStatus) {
            setStatusBar(Str.str("connecting") + ' ' + statusMsg);
          }

          connection = (HttpURLConnection) (new URL(url)).openConnection(proxy);
          if (callingWorker.isCancelled()) {
            return "";
          }

          setConnectionProperties(connection, null);
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

  private static String postData(URL url) {
    String params;
    return !Regex.isMatch(url.toString(), 892) || StringUtils.isEmpty(params = url.getQuery()) ? null : params;
  }

  private static void post(HttpURLConnection connection) throws IOException {
    String params = postData(connection.getURL());
    if (params == null) {
      return;
    }

    connection.setRequestMethod("POST");
    connection.setDoOutput(true);
    byte[] content = params.getBytes(Constant.UTF8);
    connection.setFixedLengthStreamingMode(content.length);
    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    connection.connect();

    try (OutputStream os = connection.getOutputStream()) {
      os.write(content);
    }
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

  public static void clearStatusBar() {
    statusBar.clear();
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

    void clear() {
      msgs.clear();
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

    public volatile String urlAfterEvt, cookiesAfterEvt;
    final String outputPath, referer, cookie;

    public WebBrowserRequest() {
      this(null, null, null);
    }

    WebBrowserRequest(String outputPath, String referer, String cookie) {
      this.outputPath = outputPath;
      this.referer = referer;
      this.cookie = cookie;
    }

    public String get(String url, FirefoxDriver driver, ThrowingRunnable sleep, long cacheExpirationMs) throws Exception {
      File sourceCodeFile = new File(Constant.TEMP_DIR + Str.hashCode(url) + Constant.HTML);
      if (cacheExpirationMs <= 0 || !sourceCodeFile.exists() || IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
        long startTime = System.currentTimeMillis();
        driver.get(url);
        (new WebDriverWait(driver, driver.manage().timeouts().getPageLoadTimeout())).until(driver2 -> "complete".equals(driver.executeScript(
                "return document.readyState")));
        String src = Optional.ofNullable(driver.getPageSource()).orElse("");
        while (true) { // Give asynchronous JavaScript with variable/unpredictable page changes time to finish
          sleep.run();
          String src2 = Optional.ofNullable(driver.getPageSource()).orElse("");
          if (src.equals(src2)) {
            break;
          }
          src = src2;
        }
        long endTime = System.currentTimeMillis();
        if (Debug.DEBUG) {
          Debug.println(url + " (web browser took " + ((endTime - startTime) / 1_000) + "s)");
        }
        cache(url, src);
        return src;
      }
      if (Debug.DEBUG) {
        Debug.println("fetching " + url + " (web browser cache)");
      }
      return IO.read(sourceCodeFile);
    }

    void cache(String url, String source) throws Exception {
      if (!Regex.isMatch(source, 898)) {
        IO.write(new File(Constant.TEMP_DIR + Str.hashCode(url) + Constant.HTML), source);
      }
    }

    public void waitUntilRequestSent(String requestUrlRegex, ThrowingRunnable requestEvtTrigger, FirefoxDriver driver) throws Exception {
      waitUntilUrlEvt(requestUrlRegex, requestEvtTrigger, webBrowserRequestListener, driver);
    }

    public void waitUntilResponseReceived(String responseUrlRegex, ThrowingRunnable responseEvtTrigger, FirefoxDriver driver) throws Exception {
      waitUntilUrlEvt(responseUrlRegex, responseEvtTrigger, webBrowserResponseListener, driver);
    }

    private void waitUntilUrlEvt(String urlRegex, Runnable urlEvtTrigger, AtomicReference<Consumer<String>> urlListener, FirefoxDriver driver)
            throws Exception {
      final LinkedBlockingDeque<String> urlQueue = new LinkedBlockingDeque<>();
      try {
        urlListener.set(newUrl -> {
          if (Regex.isMatch(newUrl, urlRegex)) {
            urlQueue.add(newUrl);
          }
        });
        urlEvtTrigger.run();
        urlAfterEvt = Objects.requireNonNull(urlQueue.pollFirst(driver.manage().timeouts().getPageLoadTimeout().getSeconds(), TimeUnit.SECONDS),
                "timed out waiting for URL matching " + urlRegex);
        cookiesAfterEvt = StringUtils.defaultIfEmpty(driver.manage().getCookies().stream().map(Cookie::toString).collect(Collectors.joining(";")), null);
      } finally {
        urlListener.set(null);
      }
    }
  }

  private Connection() {
  }
}

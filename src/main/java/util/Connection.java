package util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.ObjectName;
import listener.DomainType;
import listener.GuiListener;
import listener.StrUpdateListener.UpdateListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.function.TriFunction;
import org.awaitility.Awaitility;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Event;
import org.openqa.selenium.devtools.v113.network.Network;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriver.SystemProperty;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.rauschig.jarchivelib.ArchiverFactory;
import str.Str;
import util.function.ThrowingBiFunction;
import util.function.ThrowingConsumer;
import util.function.ThrowingFunction;
import util.function.ThrowingRunnable;
import util.function.ThrowingSupplier;
import util.function.ThrowingUtil;

public class Connection {

  private static GuiListener guiListener;
  private static Callable<Boolean> isSearchingDone;
  // Tasks that runWhenNotSearching() is running, which are paused if a search starts before they finish
  private static final Set<PausableTask> pausableTasks = ConcurrentHashMap.newKeySet();
  private static final ThreadLocal<PausableTask> currentPausableTask = new ThreadLocal<>();
  private static final AtomicBoolean isWatchingForSearches = new AtomicBoolean();
  private static final AtomicInteger numCacheRefreshes = new AtomicInteger(); // Unfinished background refreshes of cached pages
  // Set once the JVM is shutting down, after which no web browser or curl process is started, since it could outlive the JVM
  private static final AtomicBoolean isShuttingDown = new AtomicBoolean();
  private static final StatusBar statusBar = new StatusBar();
  public static final int MAX_NUM_REDIRECTS = 3;
  private static final Lock downloadLinkInfoProxyLock = new ReentrantLock();
  private static final AtomicBoolean downloadLinkInfoFail = new AtomicBoolean();
  private static final Cache<String, Boolean> shortTimeoutUrls = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  private static int numDownloadLinkInfoDeproxiers;
  private static UpdateListener deproxyDownloadLinkInfo;
  private static boolean reproxyDownloadLinkInfoUrlSet;
  private static volatile String downloadLinkInfoFailUrl;
  public static final File WEB_BROWSER_PROFILE = new File(Constant.APP_DIR, "webBrowserProfile");
  public static final File WEB_BROWSER_COOKIE_DB = new File(WEB_BROWSER_PROFILE, "cookies.sqlite");
  public static final File COOKIES = new File(Constant.APP_DIR, "cookies.txt");
  private static final AtomicReference<Consumer<String>> webBrowserRequestListener = new AtomicReference<>();
  private static final AtomicReference<Consumer<String>> webBrowserResponseListener = new AtomicReference<>();
  private static volatile Proxy webBrowserProxy = Proxy.NO_PROXY;
  // Page loads share the web browser through its read lock by loading in separate windows, while its write lock gives exclusive use of the web browser, such
  // as to drive it for a custom request, change its proxy or restart it
  private static final ReentrantReadWriteLock webBrowserLock = new ReentrantReadWriteLock(true);
  private static final ReentrantLock webBrowserChangeLock = new ReentrantLock(), standaloneWebBrowserLock = new ReentrantLock();
  private static final AtomicReference<FirefoxDriver> restartedWebBrowser = new AtomicReference<>();
  private static final int MAX_NUM_WEB_BROWSER_WINDOWS = maxNumWebBrowserWindows();
  private static final AtomicReference<WebBrowserWindows> webBrowserWindows = new AtomicReference<>();
  private static final ThrowingRunnable webBrowserSleep = () -> Thread.sleep(Integer.parseInt(Str.get(931)));
  private static final AtomicBoolean webBrowserInitShowStatus = new AtomicBoolean(), curlInitShowStatus = new AtomicBoolean();
  private static final AtomicBoolean webBrowserInitialized = new AtomicBoolean();
  private static final AtomicReference<HttpClientInitializer<FirefoxDriver>> webBrowserDriver = new AtomicReference<>();
  private static final AtomicReference<FirefoxDriver> lastWebBrowserDriver = new AtomicReference<>();
  private static final AtomicBoolean webBrowserQuit = new AtomicBoolean(), webBrowserShutdownHookAdded = new AtomicBoolean();
  private static final int WEB_BROWSER_QUIT_TIMEOUT_SECS = 10;
  private static volatile long webBrowserRetryTime;
  private static volatile int numWebBrowserFailures;
  private static final ConcurrentMap<String, String> webBrowserCookies = new ConcurrentHashMap<>(4);
  private static final Set<String> webBrowserHosts = new ConcurrentSkipListSet<>();
  private static final Set<String> initErrors = new CopyOnWriteArraySet<>();
  // Requests wait for the first initialization of curl. After it fails, initialization is retried in the background, without requests waiting for it, since the
  // cause can be temporary: right after a cold boot, curl once hung at startup without any output, for hours.
  private static final HttpClientInitializer<List<String>> firstCurlInit = new HttpClientInitializer<List<String>>() {
    @Override
    protected List<String> initialize() {
      return initCurl();
    }
  };
  private static volatile List<String> retriedCurlCmd;
  private static final AtomicBoolean isRetryingCurlInit = new AtomicBoolean();
  private static volatile long curlInitRetryNanoTime; // A monotonic time, unlike the wall clock, which can be corrected backwards after a cold boot
  private static volatile int numCurlInitFailures;
  private static final Set<Process> curlProcesses = ConcurrentHashMap.newKeySet();
  private static final AtomicBoolean curlShutdownHookAdded = new AtomicBoolean();
  // Hosts whose connections rejected ECH (Encrypted Client Hello), such as through antivirus HTTPS scanning, so that later requests don't try it again
  private static final Set<String> echRejectingHosts = ConcurrentHashMap.newKeySet();

  // Returns the command (curl and its arguments) that a request's arguments are added to, or null if curl is unavailable
  private static List<String> curlCommand() throws ConcurrentException {
    return curlCommand(false);
  }

  // Like curlCommand(), for a request that shows its status: while it waits for curl's first initialization, the status bar shows that it's initializing
  private static List<String> curlCommand(boolean showStatus) throws ConcurrentException {
    List<String> curlCmd = firstCurlInit.get(showStatus);
    if (curlCmd == null && (curlCmd = retriedCurlCmd) == null && System.nanoTime() - curlInitRetryNanoTime >= 0 && isRetryingCurlInit.compareAndSet(false,
            true)) {
      Worker.submit(() -> {
        try {
          retriedCurlCmd = initCurl();
        } finally {
          isRetryingCurlInit.set(false);
        }
      });
    }
    return curlCmd;
  }

  // A lazy initialization of curl or the web browser. While a request that shows its status waits for it, whether the initialization runs in the request's
  // thread or in another one (such as at startup), the status bar shows that the request is initializing.
  private abstract static class HttpClientInitializer<T> extends LazyInitializer<T> {

    private volatile boolean isInitialized;

    @Override
    public T get() throws ConcurrentException {
      T httpClient = super.get();
      isInitialized = true;
      return httpClient;
    }

    T get(boolean showStatus) throws ConcurrentException {
      if (!showStatus || isInitialized) {
        return get();
      }
      Thread thread = Thread.currentThread();
      String prevMsg = statusBar.get(thread), msg = ' ' + Str.str("initializing") + "...";
      statusBar.set(thread, msg);
      try {
        return get();
      } finally {
        statusBar.replace(thread, msg, prevMsg); // Back to the request's own message, if it had one
      }
    }
  }

  // Returns curl's command, or null if curl is unavailable
  private static List<String> initCurl() {
    try {
      File curlDir = new File(Constant.APP_DIR, "curl"), curlIndicator = new File(Constant.APP_DIR, Str.get(911));
      if (!curlIndicator.exists()) {
        File curlCompressed = new File(Constant.APP_DIR, Str.get(925));
        saveData(Str.get(Constant.WINDOWS ? 908 : (Constant.MAC ? 909 : 910)), curlCompressed.getPath(), DomainType.UPDATE, curlInitShowStatus.get());
        try {
          IO.fileOp(curlDir, IO.RM_DIR);
          ArchiverFactory.createArchiver(curlCompressed).extract(curlCompressed, new File(curlDir, curlIndicator.getName()));
          IO.fileOp(curlIndicator, IO.MK_FILE);
        } finally {
          IO.fileOp(curlCompressed, IO.RM_FILE);
        }
      }
      List<String> curlCmd = curlCommand(IO.findFile(curlDir, Regex.pattern(912)));
      List<String> curlArgs = new ArrayList<>(curlCmd);
      Collections.addAll(curlArgs, Regex.split(929, Constant.SEPARATOR1));
      Process curlProcess = startCurl(curlArgs);
      try {
        StringBuffer curlOutput = new StringBuffer(512);
        Worker.submit(() -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(curlProcess.getInputStream(), Constant.UTF8))) {
            String line;
            while ((line = br.readLine()) != null) {
              curlOutput.append(line).append(Constant.NEWLINE);
            }
          }
        });
        // Generous, because the first run after a cold boot can be slow, such as while antivirus scans curl
        if (!curlProcess.waitFor(30, TimeUnit.SECONDS)) {
          throw new RuntimeException("\n" + curlOutput + "\nCurl initialization timed out.");
        }
        int exitValue = curlProcess.exitValue();
        if (exitValue != 0) {
          throw new RuntimeException("\n" + curlOutput + "\nCurl initialization bad exit value: " + exitValue + ".");
        }
      } finally {
        stopCurl(curlProcess);
      }
      numCurlInitFailures = 0;
      if (Debug.DEBUG) {
        Debug.println("curl initialized: " + curlCmd);
      }
      return Collections.unmodifiableList(curlCmd);
    } catch (Exception e) {
      Throwable cause = ThrowableUtil.rootCause(e);
      if (cause instanceof InterruptedException || cause instanceof CancellationException || isShuttingDown.get()) {
        ThrowingUtil.sneakyThrow(e); // Not a failure, such as when the curl stopper killed curl
      }
      int numFailures = ++numCurlInitFailures;
      if (numFailures == 1) { // Not reported, since initialization is retried right away: a new process can work where one hung
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      } else {
        handleInitError(e, numFailures == 2);
      }
      curlInitRetryNanoTime = System.nanoTime() + (numFailures == 1 ? 0 : (TimeUnit.MINUTES.toNanos(1) << Math.min(numFailures - 2, 5))); // Then 1 to 32 minutes
      return null;
    }
  }

  // Starts curl, which is killed if the app exits while it runs
  private static Process startCurl(List<String> curlArgs) throws IOException {
    if (curlShutdownHookAdded.compareAndSet(false, true)) {
      addShutdownHook(new Thread(() -> {
        isShuttingDown.set(true);
        curlProcesses.forEach(Process::destroy);
      }, "curl stopper"));
    }
    checkNotShuttingDown();
    Process curlProcess = (new ProcessBuilder(curlArgs)).redirectErrorStream(true).start();
    curlProcesses.add(curlProcess);
    if (isShuttingDown.get()) { // The curl stopper could have run before this process was added
      stopCurl(curlProcess);
      checkNotShuttingDown();
    }
    return curlProcess;
  }

  // Fails once the JVM is shutting down, so that a request doesn't start a web browser or curl process that could outlive the JVM
  private static void checkNotShuttingDown() {
    if (isShuttingDown.get()) {
      throw new CancellationException("shutting down");
    }
  }

  // Fails like checkNotShuttingDown() if the JVM is already shutting down, which is when the JVM refuses new shutdown hooks (such as when the app is closed
  // while the web browser starts for the first time)
  private static void addShutdownHook(Thread hook) {
    try {
      Runtime.getRuntime().addShutdownHook(hook);
    } catch (IllegalStateException e) {
      isShuttingDown.set(true);
      checkNotShuttingDown();
    }
  }

  // Kills curl unless it has exited. On Windows, that's curl itself rather than its wrapper (see curlCommand(File)).
  private static void stopCurl(Process curlProcess) {
    curlProcess.destroy();
    curlProcesses.remove(curlProcess);
  }

  // On Windows, curl comes with a batch file wrapper that only adds the arguments for impersonating a web browser. Running curl itself with those arguments
  // instead means that destroying the process kills curl, whereas destroying the wrapper only kills its cmd.exe, leaving curl running (and holding the output
  // pipe open). Falls back to the wrapper unless its command line is in the expected form: "%~dp0curl.exe" arguments %*
  private static List<String> curlCommand(File wrapper) {
    if (Constant.WINDOWS && wrapper.getName().toLowerCase(Locale.ENGLISH).endsWith(".bat")) {
      try {
        String wrapperDir = "%~dp0";
        for (String line : Files.readAllLines(wrapper.toPath(), StandardCharsets.ISO_8859_1)) {
          if (!(line = line.trim()).endsWith("%*")) {
            continue;
          }
          List<String> curlCmd = new ArrayList<>(8);
          Matcher arg = Regex.matcher("(?:[^\\s\"]++|\"[^\"]*+\")++", line.substring(0, line.length() - 2));
          while (arg.find()) {
            curlCmd.add(arg.group().replace("\"", ""));
          }
          // Apart from the wrapper's directory, the wrapper's command must be literal, with nothing for cmd.exe to expand
          if (!curlCmd.isEmpty() && StringUtils.startsWithIgnoreCase(curlCmd.get(0), wrapperDir) && curlCmd.stream().noneMatch(currArg -> StringUtils.containsAny(
                  StringUtils.removeStartIgnoreCase(currArg, wrapperDir), '%', '!', '^'))) {
            File curlExe = new File(wrapper.getParentFile(), curlCmd.get(0).substring(wrapperDir.length()));
            if (curlExe.isFile()) {
              curlCmd.set(0, curlExe.getPath());
              return curlCmd;
            }
          }
          break;
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      if (Debug.DEBUG) {
        Debug.println("running curl through its wrapper: " + wrapper);
      }
    }
    return Lists.newArrayList(wrapper.getPath());
  }

  // Returns the web browser, or null if it's unavailable. While a request that shows its status waits for the web browser to be created, the status bar shows
  // that it's initializing.
  private static FirefoxDriver createWebBrowser(boolean restart, boolean showStatus) throws Exception {
    checkNotShuttingDown(); // Such as a restart after the shutdown hook quit the web browser
    HttpClientInitializer<FirefoxDriver> prevWebBrowserDriver;
    if (restart) {
      webBrowserDriver.set(null);
    } else if ((prevWebBrowserDriver = webBrowserDriver.get()) != null && prevWebBrowserDriver.get(showStatus) == null
            && System.currentTimeMillis() >= webBrowserRetryTime && !standaloneWebBrowserLock.isLocked()) {
      // Try again in case the failure to create it was temporary, but not during a standalone web browser run: creating the web browser stops every process
      // started from its executable, which the standalone web browser shares
      webBrowserDriver.compareAndSet(prevWebBrowserDriver, null);
    }
    webBrowserDriver.compareAndSet(null, new HttpClientInitializer<FirefoxDriver>() {
      @Override
      protected FirefoxDriver initialize() {
        try {
          File webBrowserDir = new File(Constant.APP_DIR, "webBrowser");
          File firefoxBinary = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 838 : (Constant.MAC ? 842 : 846)));
          File firefoxDriver = new File(webBrowserDir, Str.get(Constant.WINDOWS ? 837 : (Constant.MAC ? 841 : 845)));
          Consumer<Boolean> killWebBrowser = wait -> killProcesses(wait, firefoxBinary, firefoxDriver);
          killWebBrowser.accept(true);

          File firefoxIndicator = new File(Constant.APP_DIR, Str.get(Constant.WINDOWS ? 836 : (Constant.MAC ? 840 : 844)));
          if (!firefoxIndicator.exists()) {
            String zipFile = firefoxIndicator.getPath() + Constant.ZIP;
            try {
              saveData(Str.get(Constant.WINDOWS ? 839 : (Constant.MAC ? 843 : 847)), zipFile, DomainType.UPDATE, webBrowserInitShowStatus.get());
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
          disableWebBrowserUpdates(firefoxBinary);

          System.setProperty(GeckoDriverService.GECKO_DRIVER_EXE_PROPERTY, firefoxDriver.getPath());
          System.setProperty(SystemProperty.BROWSER_BINARY, firefoxBinary.getPath());
          FirefoxOptions options = new FirefoxOptions();
          options.setBinary(firefoxBinary.getPath());
          options.setAcceptInsecureCerts(true);
          options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.ACCEPT);
          // FirefoxDriver.get() returns without waiting for the page to load so that pages can load concurrently in separate windows (see
          // WebBrowserWindow.load())
          options.setPageLoadStrategy(PageLoadStrategy.NONE);
          options.addArguments(Regex.split(848, Constant.SEPARATOR1));
          FirefoxProfile profile = new FirefoxProfile();
          Arrays.stream(Regex.split(849, Constant.SEPARATOR2)).map(pref -> Regex.split(pref, Constant.SEPARATOR1)).forEach(pref -> profile.setPreference(pref[0],
                  Regex.isMatch(pref[1], "(true)|(false)") ? Boolean.parseBoolean(pref[1]) : (Regex.isMatch(pref[1], "\\d++") ? Integer.parseInt(pref[1])
                  : pref[1])));
          Arrays.asList(869, 870).forEach(i -> profile.setPreference(Str.get(i), Constant.TEMP_DIR));
          options.setProfile(profile);
          if (webBrowserShutdownHookAdded.compareAndSet(false, true)) { // Once instead of for every web browser
            addShutdownHook(new Thread(() -> {
              isShuttingDown.set(true);
              webBrowserQuit.set(true);
              FirefoxDriver tempDriver = lastWebBrowserDriver.get();
              if (tempDriver != null) {
                quitWebBrowser(tempDriver);
              }
              killWebBrowser.accept(false);
            }));
          }

          System.setErr(new PrintStream(new OutputStream() {
            private final StringBuffer line = new StringBuffer(1024);

            @Override
            public void write(int b) {
              if (webBrowserQuit.get()) {
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

          FirefoxDriver driver = null;
          for (int attempt = 1; driver == null; attempt++) {
            try {
              driver = new FirefoxDriver(options);
            } catch (WebDriverException e) { // Such as when Firefox briefly can't read its own files and quits while starting
              checkNotShuttingDown(); // The shutdown hook could have stopped it
              if (attempt == 3) {
                throw e;
              }
              if (Debug.DEBUG) {
                Debug.println("retrying web browser creation: " + Regex.firstMatch(e.toString(), ".+"));
              }
              killWebBrowser.accept(true);
              Thread.sleep(attempt * 2_000L);
            }
          }
          lastWebBrowserDriver.set(driver);
          if (isShuttingDown.get()) { // The shutdown hook could have missed this web browser
            killWebBrowser.accept(false);
            checkNotShuttingDown();
          }
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
          webBrowserWindows.set(new WebBrowserWindows(driver)); // The DevTools session is in the first (main) window
          webBrowserProxy = Proxy.NO_PROXY; // A new web browser has no proxy set or cookies loaded
          webBrowserHosts.clear();
          if (Debug.DEBUG) {
            Debug.println("web browser " + (restart ? "re" : "") + "created");
          }
          numWebBrowserFailures = 0;
          return driver;
        } catch (Exception e) {
          handleInitError(e, !webBrowserInitialized.get()); // Rethrows an interruption, which isn't a failure to count
          webBrowserRetryTime = System.currentTimeMillis() + (TimeUnit.MINUTES.toMillis(1) << Math.min(numWebBrowserFailures++, 5)); // 1 to 32 minutes
          return null;
        } finally {
          webBrowserInitialized.set(true);
        }
      }
    });
    return webBrowserDriver.get().get(showStatus);
  }

  // Quitting lets the web browser save its profile, but it can take a while or never end (such as when its driver doesn't stop), so after a few seconds, the
  // web browser is left to be killed instead of keeping the JVM from exiting
  private static void quitWebBrowser(FirefoxDriver driver) {
    AtomicBoolean isQuitTimedOut = new AtomicBoolean();
    Thread quitter = new Thread(() -> {
      try {
        driver.quit();
      } catch (Exception e) {
        if (Debug.DEBUG && !isQuitTimedOut.get()) { // Not because the web browser was killed
          Debug.print(e);
        }
      }
    }, "web browser quitter");
    quitter.setDaemon(true);
    quitter.start();
    try {
      quitter.join(TimeUnit.SECONDS.toMillis(WEB_BROWSER_QUIT_TIMEOUT_SECS));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (quitter.isAlive()) {
      isQuitTimedOut.set(true);
      if (Debug.DEBUG) {
        Debug.println("web browser didn't quit within " + WEB_BROWSER_QUIT_TIMEOUT_SECS + " seconds");
      }
    }
  }

  // A window for each processor (2 to 4), but no more than one for each GB of memory, such as on 32-bit Windows with little memory
  private static int maxNumWebBrowserWindows() {
    int numWindows = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
    try {
      Object memory = ManagementFactory.getPlatformMBeanServer().getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "TotalPhysicalMemorySize");
      if (memory instanceof Number) {
        numWindows = (int) Math.max(1, Math.min(numWindows, Math.round(((Number) memory).longValue() / (double) (1L << 30))));
      }
    } catch (Exception e) { // Such as a Java without this attribute
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    return numWindows;
  }

  // Firefox updates itself (the update prefs have been ignored since Firefox 63), which could replace the bundled version with one that the web browser
  // automation doesn't support (such as on Windows 10/11, where the release channel moves past ESR 115), so an enterprise policy disables updates. On
  // Windows, removing the updater also stops an update that was already downloaded from being applied when Firefox next starts.
  private static void disableWebBrowserUpdates(File firefoxBinary) {
    try {
      File installDir = firefoxBinary.getParentFile();
      File policiesFile = new File(new File(Constant.MAC ? new File(installDir.getParentFile(), "Resources") : installDir, "distribution"), "policies.json");
      Json json = new Json();
      Map<String, Object> config = new LinkedHashMap<>();
      if (policiesFile.exists()) {
        config.putAll(json.<Map<String, Object>>toType(IO.read(policiesFile), Json.MAP_TYPE));
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> policies = new LinkedHashMap<>((Map<String, Object>) config.getOrDefault("policies", Collections.emptyMap()));
      if (!Boolean.TRUE.equals(policies.get("DisableAppUpdate"))) {
        policies.put("DisableAppUpdate", true);
        config.put("policies", policies);
        IO.fileOp(policiesFile.getParentFile(), IO.MK_DIR);
        IO.write(policiesFile, json.toJson(config).replaceAll("(?<!\\\\)((?:\\\\\\\\)*)\\\\u002f", "$1/")); // Unescape Selenium's escaped slashes
      }
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    if (Constant.WINDOWS) {
      File installDir = firefoxBinary.getParentFile(), updater = new File(installDir, "updater.exe"), stagedUpdate = new File(installDir, "updated");
      if (updater.exists() && !IO.fileOp(updater, IO.RM_FILE) && Debug.DEBUG) {
        Debug.println("unable to remove web browser updater");
      }
      if (stagedUpdate.exists()) {
        IO.fileOp(stagedUpdate, IO.RM_DIR);
      }
    }
  }

  private static void handleInitError(Exception e, boolean isStartup) {
    Throwable cause = ThrowableUtil.rootCause(e);
    // Not a failure if shutting down, which can make initialization fail in other ways, such as when the curl stopper kills the web browser's download
    if (cause instanceof InterruptedException || cause instanceof CancellationException || isShuttingDown.get()) {
      ThrowingUtil.sneakyThrow(e);
    }
    String msg = Str.str("connectionInitError") + ' ' + Regex.firstMatch(String.valueOf(cause), ".+");
    if (initErrors.add(msg)) {
      if (isStartup) {
        Writer stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        guiListener.error(new Exception(msg + "\n\n" + stackTrace));
      } else {
        IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
      }
    }
  }

  public static void startHttpClients() {
    if (Boolean.parseBoolean(Str.get(926))) {
      Worker.submit(() -> curlCommand());
    } else {
      curlInitShowStatus.set(true);
    }
    if (Boolean.parseBoolean(Str.get(927))) {
      Worker.submit(() -> createWebBrowser(false, false));
    } else {
      webBrowserInitShowStatus.set(true);
    }
  }

  // Runs the task once no search is running. If a search starts while the task runs, the task is paused: its requests are cancelled and new ones fail, so that
  // they don't slow down searching. Once searching is done, the task runs again, getting the pages that it already fetched from the cache, so that it resumes
  // about where it left off. Only requests made in the task's own thread are paused (along with their redirects and retries).
  public static void runWhenNotSearching(ThrowingRunnable task) throws Exception {
    if (currentPausableTask.get() != null) { // Paused along with the task that this is part of
      task.runWithException();
      return;
    }

    AbstractWorker<?> worker = AbstractWorker.current();
    // Awaitility ignores interruptions until its condition holds (and then reasserts them), so a cancellation ends the wait through the condition
    Callable<Boolean> canRun = (worker == null ? isSearchingDone : () -> worker.isCancelled() || isSearchingDone.call());
    for (String pausedAt = null;;) { // The URL of the request at which a search paused the last run
      // By default, Awaitility takes over every thread's uncaught exceptions while it waits: they would go unreported, and fail this wait instead
      Awaitility.await().dontCatchUncaughtExceptions().forever().until(canRun);
      boolean isInterrupted = Thread.interrupted();
      if (isInterrupted || (worker != null && worker.isCancelled())) { // Such as during a run that a pause disrupted, whose exception was ignored
        debugResumption("not resuming", "it was stopped", pausedAt);
        throw (isInterrupted ? new InterruptedException() : new CancellationException());
      }
      debugResumption("resuming", "searching is done", pausedAt);

      PausableTask pausableTask = new PausableTask();
      try {
        currentPausableTask.set(pausableTask);
        pausableTasks.add(pausableTask);
        startSearchWatcher();
        task.runWithException();
      } catch (Exception e) {
        String pausedHere = pausableTask.end();
        if (e instanceof InterruptedException) { // Not because of a pause, which doesn't interrupt the task
          debugResumption("not resuming", "it was stopped", pausedHere);
          throw e;
        }
        if (pausedHere == null) {
          throw e;
        }
      } finally {
        pausedAt = pausableTask.end();
        pausableTasks.remove(pausableTask);
        currentPausableTask.remove();
      }
      if (pausedAt == null) {
        return;
      }
    }
  }

  private static void debugResumption(String action, String reason, String pausedAt) {
    if (pausedAt != null && Debug.DEBUG) {
      Debug.println(action + ' ' + Thread.currentThread().getName() + " because " + reason + " (paused at its request for " + pausedAt + ')');
    }
  }

  private static void startSearchWatcher() {
    if (isWatchingForSearches.compareAndSet(false, true)) {
      try {
        Worker.submit(Connection::watchForSearches);
      } catch (RuntimeException | Error e) { // Such as when a thread can't be created, so that a later task can start the watcher
        isWatchingForSearches.set(false);
        throw e;
      }
    }
  }

  // Pauses the tasks that runWhenNotSearching() runs when a search starts. Runs until there are none.
  private static void watchForSearches() throws Exception {
    do {
      try {
        while (!pausableTasks.isEmpty()) {
          Thread.sleep(100);
          if (!isSearchingDone.call()) {
            pausableTasks.forEach(PausableTask::pause);
          }
        }
      } finally {
        isWatchingForSearches.set(false);
      }
    } while (!pausableTasks.isEmpty() && isWatchingForSearches.compareAndSet(false, true)); // A task added after the last check didn't start a watcher
  }

  // Runs the request for the URL, which a search cancels if a task that runWhenNotSearching() runs makes it
  private static <T> T executeRequest(String url, AbstractWorker<T> request) throws Exception {
    PausableTask task = currentPausableTask.get();
    if (task == null) {
      return request.executeAndGet();
    }
    task.requestStarting(url, request);
    try {
      return request.executeAndGet();
    } finally {
      task.requestEnded(request);
    }
  }

  // A run of a task that runWhenNotSearching() runs. A search pauses it by cancelling its requests and failing new ones until the run ends.
  private static final class PausableTask {

    private final String threadName = Thread.currentThread().getName();
    private final Map<AbstractWorker<?>, String> requestUrls = new HashMap<>(4); // Guarded by this
    private boolean isRunning = true, isPaused; // Guarded by this
    private String pausedAt; // The URL of the request that a pause stopped or kept from starting. Guarded by this.

    synchronized void requestStarting(String url, AbstractWorker<?> request) {
      if (isPaused) {
        disrupt("before its request for", url);
        throw new CancellationException("paused while searching");
      }
      requestUrls.put(request, url);
    }

    synchronized void requestEnded(AbstractWorker<?> request) {
      requestUrls.remove(request);
    }

    synchronized void pause() {
      if (!isRunning || isPaused) {
        return;
      }
      isPaused = true;
      requestUrls.forEach((request, url) -> {
        if (request.cancel(true)) {
          disrupt("stopping its request for", url);
        }
      });
    }

    private synchronized void disrupt(String how, String url) {
      if (pausedAt == null) {
        pausedAt = String.valueOf(url);
        if (Debug.DEBUG) {
          Debug.println("pausing " + threadName + " because a search started, " + how + ' ' + url);
        }
      }
    }

    // Ends the run. Returns the URL of the request at which a pause disrupted it, so that the task needs to run again, or null if none did.
    synchronized String end() {
      isRunning = false;
      return pausedAt;
    }
  }

  public static void init(GuiListener guiListener, Callable<Boolean> isSearchingDone) {
    Connection.guiListener = guiListener;
    Connection.isSearchingDone = isSearchingDone;
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
    try {
      Optional.of(COOKIES).filter(File::exists).map(ThrowingFunction.of(file -> FileUtils.readLines(file, StandardCharsets.UTF_8))).ifPresent(cookies
              -> cookies.forEach(cookie -> webBrowserCookies.put(StringUtils.substringBefore(cookie, '\t'), StringUtils.substringAfter(cookie, '\t'))));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    IO.fileOp(WEB_BROWSER_PROFILE, IO.RM_DIR);
    IO.fileOp(WEB_BROWSER_PROFILE, IO.MK_DIR);
    try {
      IO.write(new File(WEB_BROWSER_PROFILE, "user.js"), Str.get(946));
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
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
      if (Debug.DEBUG) {
        Debug.println("fetching " + url);
      }
      try {
        sourceCode = IO.read(sourceCodeFile);
        if (!emptyOK && Regex.isMatch(sourceCode, 898)) {
          IO.fileOp(sourceCodeFile, IO.RM_FILE);
          throw new IOException("empty cached source code: '" + Regex.firstMatch(sourceCode, 942) + "'");
        }
        try {
          if (IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
            refreshInBackground(() -> addToCache(getSourceCodeHelper(url, originalUrl, domainType, false, emptyOK, cacheExpirationMs, maxNumRedirects,
                    webBrowserRequest, throwables), sourceCodeFile));
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
      } catch (InterruptedException | CancellationException e) {
        throw e;
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        addToCache(sourceCode = getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest,
                throwables), sourceCodeFile);
      }
    } else {
      addToCache(sourceCode = getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest,
              throwables), sourceCodeFile);
    }

    return sourceCode;
  }

  // Refreshes a cached page in the background, once no search is running (see runWhenNotSearching()). isRefreshingCache() is true until it's done.
  public static void refreshInBackground(ThrowingRunnable refresh) {
    numCacheRefreshes.incrementAndGet();
    try {
      Worker.submit(() -> {
        try {
          runWhenNotSearching(refresh);
        } finally {
          numCacheRefreshes.decrementAndGet();
        }
      });
    } catch (RuntimeException | Error e) { // Not started, such as when shutting down
      numCacheRefreshes.decrementAndGet();
      throw e;
    }
  }

  // Whether cached pages are being refreshed in the background, which waits for searching to be done (see runWhenNotSearching())
  public static boolean isRefreshingCache() {
    return numCacheRefreshes.get() != 0;
  }

  private static String getSourceCodeHelper(String url, String originalUrl, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          int maxNumRedirects, WebBrowserRequest webBrowserRequest, Class<?>... throwables) throws Exception {
    return getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, webBrowserRequest, true, throwables);
  }

  private static String getSourceCodeHelper(String url, String originalUrl, DomainType domainType, boolean showStatus, boolean emptyOK, long cacheExpirationMs,
          int maxNumRedirects, WebBrowserRequest webBrowserRequest, boolean canRetry, Class<?>... throwables) throws Exception {
    boolean useCurl = Regex.isMatch(url, 933) && curlCommand(showStatus) != null;
    // A repeated request (canRetry is false) keeps its original client instead of switching to the web browser: it can run while its original request uses
    // the web browser (in retry()), which a nested web browser request could wait for forever
    if (webBrowserRequest == null && canRetry && Regex.isMatch(url, 871) && !useCurl && createWebBrowser(false, showStatus) != null) {
      if (!Regex.isMatch(url, 873)) {
        return getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, new WebBrowserRequest(), canRetry,
                throwables);
      }
      File temp = new File(Constant.TEMP_DIR, UUID.randomUUID().toString());
      saveData(url, temp.getPath(), domainType, showStatus, null, MAX_NUM_REDIRECTS, null, true);
      String str = IO.read(temp);
      IO.fileOp(temp, IO.RM_FILE);
      return str;
    }

    return executeRequest(url, new AbstractWorker<String>() {
      @Override
      protected String doInBackground() throws Exception {
        HttpURLConnection connection = null;
        BufferedReader br = null;
        StringBuilder source = new StringBuilder(8192);
        try {
          boolean useWebBrowser = false;
          if (webBrowserRequest != null) {
            webBrowserInitShowStatus.set(showStatus);
            useWebBrowser = createWebBrowser(false, showStatus) != null;
          }

          if (Debug.DEBUG) {
            Debug.println(url + (useWebBrowser ? " (web browser)" : (useCurl ? " (curl)" : "")));
          }

          Proxy proxy = getProxy(domainType);
          String statusMsg = checkProxyAndSetStatusBar(proxy, url, showStatus, this, !useCurl && !useWebBrowser);
          if (isCancelled()) {
            return "";
          }

          Supplier<String> cookiesStr = () -> webBrowserCookies.values().stream().sorted().collect(Collectors.joining("\n"));
          AtomicReference<ThrowingBiFunction<Integer, CharSequence, String>> retry = new AtomicReference<>(), retry2 = new AtomicReference<>();
          ThrowingSupplier<String> getSource = () -> getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects,
                  webBrowserRequest, false, throwables);
          String client = (useWebBrowser ? "web browser" : (useCurl ? "curl" : "direct request"));
          retry.set((responseCode, response) -> {
            if (!canRetry || !Regex.isMatch(String.valueOf(responseCode), 934) || createWebBrowser(false, showStatus) == null) {
              return retry2.get().apply(responseCode, response);
            }

            String prevWebBrowserCookies = cookiesStr.get();
            try {
              return getSourceCodeHelper(url, originalUrl, domainType, showStatus, emptyOK, cacheExpirationMs, maxNumRedirects, new WebBrowserRequest() {
                @Override
                public String retry() throws Exception {
                  if (!prevWebBrowserCookies.equals(cookiesStr.get())) {
                    return getSource.get();
                  }
                  if (Debug.DEBUG) {
                    Debug.println("retrying with web browser because " + client + " failed: HTTP " + responseCode + " " + url + " (web browser)");
                  }
                  return null;
                }
              }, canRetry, throwables);
            } catch (Exception e) {
              if (!prevWebBrowserCookies.equals(cookiesStr.get())) {
                if (Debug.DEBUG) {
                  Debug.print(new Exception("ignorable exception", e));
                }
                return getSource.get();
              }
              throw e;
            }
          });
          retry2.set((responseCode, response) -> {
            if (!canRetry || (!Regex.isMatch(String.valueOf(responseCode), 937) && Regex.firstMatch(response.toString(), 942).isEmpty())) {
              return null;
            }
            String prevWebBrowserCookies = cookiesStr.get();
            createWebBrowser(false, showStatus);
            if (!(new File(Constant.APP_DIR, Str.get(Constant.WINDOWS ? 836 : (Constant.MAC ? 840 : 844)))).exists()) {
              return null;
            }
            if (!prevWebBrowserCookies.equals(cookiesStr.get())) {
              return getSource.get();
            }
            standaloneWebBrowserLock.lockInterruptibly();
            try {
              if (!prevWebBrowserCookies.equals(cookiesStr.get())) {
                standaloneWebBrowserLock.unlock();
                return getSource.get();
              }
              if (Debug.DEBUG) {
                Debug.println("retrying with standalone web browser because " + client + " failed: " + (responseCode == -1 ? '\'' + Regex.firstMatch(
                        response.toString(), 942) + '\'' : ("HTTP " + responseCode)) + ' ' + url + " (standalone web browser)");
              }

              String cookieSql = " FROM moz_cookies WHERE LOWER(host) LIKE '%" + Regex.replaceFirst((new URL(url)).getHost().toLowerCase(Locale.ENGLISH),
                      "\\Awww\\.", "") + "%'";
              if (WEB_BROWSER_COOKIE_DB.exists()) {
                try (java.sql.Connection cookiesDbConnection = DriverManager.getConnection("jdbc:sqlite:" + WEB_BROWSER_COOKIE_DB.getPath());
                        Statement statement = cookiesDbConnection.createStatement()) {
                  statement.executeUpdate("DELETE" + cookieSql);
                } catch (Exception e) {
                  if (Debug.DEBUG) {
                    Debug.print(e);
                  }
                }
              }

              File webBrowser = new File(Constant.APP_DIR, "webBrowser");
              ThrowingConsumer<Boolean> runStandaloneWebBrowser = headless -> {
                File firefoxBinary = new File(webBrowser, Str.get(Constant.WINDOWS ? 838 : (Constant.MAC ? 842 : 846)));
                List<String> args = Lists.newArrayList(firefoxBinary.getPath());
                Collections.addAll(args, Regex.split(headless ? 938 : 945, Constant.SEPARATOR1));
                Collections.addAll(args, Str.get(939), WEB_BROWSER_PROFILE.getPath());
                Collections.addAll(args, Str.get(940), url);
                checkNotShuttingDown();
                Process firefox = (new ProcessBuilder(args)).redirectErrorStream(true).start();
                try {
                  checkNotShuttingDown(); // The shutdown hook could have missed this process
                  final AtomicInteger timer = new AtomicInteger();
                  int prevCookieCount = -1;
                  for (int timeout = Integer.parseInt(Str.get(headless ? 943 : 947)); timer.get() < timeout; timer.incrementAndGet()) {
                    Thread.sleep(1_000);
                    if (!WEB_BROWSER_COOKIE_DB.exists()) {
                      continue;
                    }
                    try (java.sql.Connection cookiesDbConnection = DriverManager.getConnection("jdbc:sqlite:" + WEB_BROWSER_COOKIE_DB.getPath());
                            Statement statement = cookiesDbConnection.createStatement()) {
                      ResultSet results = statement.executeQuery("SELECT count(*)" + cookieSql);
                      while (results.next()) {
                        int cookieCount = results.getInt(1);
                        if (cookieCount != prevCookieCount) {
                          if (cookieCount > 0) {
                            timeout = Integer.parseInt(Str.get(944));
                          }
                          timer.set(0);
                        }
                        prevCookieCount = cookieCount;
                      }
                    } catch (Exception e) {
                      if (Debug.DEBUG) {
                        Debug.print(e);
                      }
                      Thread.sleep((timeout - (timer.get() + 1)) * 1_000);
                      break;
                    }
                  }
                } finally {
                  stopStandaloneWebBrowser(firefox, firefoxBinary);
                }

                try (java.sql.Connection cookiesDbConnection = DriverManager.getConnection("jdbc:sqlite:" + WEB_BROWSER_COOKIE_DB.getPath());
                        Statement statement = cookiesDbConnection.createStatement()) {
                  boolean removeHost = false;
                  ResultSet results = statement.executeQuery("SELECT name, value, expiry, path, host" + cookieSql);
                  while (results.next()) {
                    if (Regex.isMatch(results.getString("name"), 941)) {
                      webBrowserCookies.put(results.getString("host") + ' ' + results.getString("name"), results.getString("name") + "=" + results.getString(
                              "value") + "; expires=" + Instant.ofEpochSecond(Long.parseLong(results.getString("expiry"))).atZone(ZoneId.of("GMT")).format(
                                      DateTimeFormatter.RFC_1123_DATE_TIME) + "; path=" + results.getString("path") + "; domain=" + results.getString("host"));
                      removeHost = true;
                    }
                  }
                  if (removeHost) {
                    webBrowserHosts.remove((new URL(url)).getHost());
                  }
                } catch (Exception e) {
                  if (Debug.DEBUG) {
                    Debug.print(e);
                  }
                }
                if (Thread.interrupted()) { // Stopping the standalone web browser only keeps a cancellation pending, so don't go on to a visible run
                  throw new InterruptedException();
                }
              };

              runStandaloneWebBrowser.accept(true);
              if (prevWebBrowserCookies.equals(cookiesStr.get()) && Optional.ofNullable(IO.findFile(webBrowser, Regex.pattern(Str.get(948)))).map(
                      ThrowingFunction.of(IO::read)).map(appInfo -> Regex.firstMatch(appInfo, 949)).filter(majorVersion -> !majorVersion.isEmpty()).map(
                      Integer::parseInt).filter(version -> version >= Integer.parseInt(Str.get(950))).isPresent()) {
                checkNotShuttingDown();
                guiListener.timedMsg(Str.str("checkStandaloneWebBrowser", (new URL(url)).getHost()));
                runStandaloneWebBrowser.accept(false);
              }
              if (prevWebBrowserCookies.equals(cookiesStr.get())) {
                standaloneWebBrowserLock.unlock();
                throw new IOException("retry with standalone web browser retry failed");
              }

              saveCookies();

              standaloneWebBrowserLock.unlock();
              return getSource.get();
            } finally {
              try {
                standaloneWebBrowserLock.unlock();
              } catch (Exception e) {/* Ignore */ }
            }
          });
          if (!useWebBrowser) {
            if (useCurl) {
              String output = curl(url, null, null, maxNumRedirects, null, proxy, this::isCancelled, retry.get());
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
              if (!webBrowserCookies.isEmpty()) {
                connection.setRequestProperty("Cookie", webBrowserCookies.values().stream().collect(Collectors.joining(";")));
              }

              try {
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
              } catch (IOException e) {
                String sourceCode = retry.get().apply(connection.getResponseCode(), source);
                if (sourceCode != null) {
                  return sourceCode;
                }
                throw e;
              }

              int responseCode = connection.getResponseCode();
              if (maxNumRedirects > 0 && Regex.isMatch(String.valueOf(responseCode), "30[12378]")) {
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

              String sourceCode = retry.get().apply(responseCode, source);
              if (sourceCode != null) {
                return sourceCode;
              }

              checkConnectionResponse(connection, url);
            }
          } else {
            AtomicReference<FirefoxDriver> webBrowserUsed = new AtomicReference<>();
            ThrowingConsumer<Boolean> getSourceCode = restartWebBrowser -> {
              AtomicBoolean retried = new AtomicBoolean();
              FirefoxDriver deadWebBrowser = (restartWebBrowser ? webBrowserUsed.get() : null);
              String sourceCode = useWebBrowser(url, proxy, webBrowserRequest, deadWebBrowser, webBrowserUsed, showStatus, window -> {
                if (!restartWebBrowser) {
                  String retrySourceCode = webBrowserRequest.retry(); // Now that it's this request's turn to use the web browser
                  if (retrySourceCode != null) {
                    retried.set(true);
                    return retrySourceCode;
                  }
                }
                if (showStatus) {
                  setStatusBar(Str.str("transferring") + ' ' + statusMsg);
                }
                return webBrowserRequest.exclusive ? webBrowserRequest.get(url, window.driver, webBrowserSleep, cacheExpirationMs)
                        : webBrowserRequest.get(url, window, cacheExpirationMs, true);
              });
              source.append(retried.get() ? sourceCode : Optional.ofNullable(retry2.get().apply(-1, sourceCode)).orElse(sourceCode));
            };
            try {
              try {
                getSourceCode.acceptWithException(false);
              } catch (TimeoutException e) {
                throw e;
              } catch (WebDriverException e) {
                try {
                  if (e instanceof WebBrowserErrorPageException) {
                    throw e; // Restarting the web browser wouldn't help
                  }
                  if (Debug.DEBUG) {
                    Debug.println("restarting web browser because it probably died: " + Regex.firstMatch(e.toString(), ".+"));
                  }
                  getSourceCode.acceptWithException(true);
                } catch (TimeoutException e2) {
                  throw e2;
                } catch (WebDriverException e2) {
                  if (!Regex.isMatch(url, 930) || curlCommand(showStatus) == null) {
                    throw e2;
                  }
                  if (Debug.DEBUG) {
                    Debug.println("retrying with curl because web browser failed: " + Regex.firstMatch(e2.toString(), ".+") + '\n' + url + " (curl)");
                  }
                  String output = curl(url, webBrowserRequest.outputPath, webBrowserRequest.referer, maxNumRedirects, webBrowserRequest.cookie, proxy,
                          this::isCancelled, null);
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
            } catch (InterruptedException | CancellationException e) {
              throw e;
            } catch (Exception e) {
              throw new IOException("web browser error for " + url, e);
            }
          }

          if (!emptyOK && Regex.isMatch(source.toString(), 898)) {
            throw new IOException("empty source code: '" + Regex.firstMatch(source.toString(), 942) + "'");
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
    });
  }

  // Runs a task with the web browser. Page loads share the web browser by loading in separate windows, while requests that drive the web browser themselves
  // get exclusive use of it in its main window (whose DevTools session the request and response listeners use). The web browser is restarted if it's
  // deadWebBrowser, unless that's already been done. showStatus is whether the request shows its status, such as while it waits for the web browser to start.
  private static String useWebBrowser(String url, Proxy proxy, WebBrowserRequest request, FirefoxDriver deadWebBrowser,
          AtomicReference<FirefoxDriver> webBrowserUsed, boolean showStatus, ThrowingFunction<WebBrowserWindow, String> task) throws Exception {
    Duration pageLoadTimeout = Duration.ofSeconds(shortTimeoutUrls.getIfPresent((new URL(url)).getHost()) == null ? (guiListener.getTimeout() + 45) : 45);
    Lock readLock = webBrowserLock.readLock(), writeLock = webBrowserLock.writeLock(), lock = null;
    try {
      FirefoxDriver driver = null;
      if (request.exclusive) {
        writeLock.lockInterruptibly();
        lock = writeLock;
        driver = changeWebBrowser(deadWebBrowser, proxy, pageLoadTimeout, webBrowserUsed, showStatus);
      } else {
        while (lock == null) {
          if (!isWebBrowserChangeNeeded(deadWebBrowser, proxy)) {
            readLock.lockInterruptibly();
            lock = readLock;
            // Get the web browser before checking its proxy, because creating the web browser resets its proxy. Then, while the lock is held, the web
            // browser can't be replaced.
            if ((driver = createWebBrowser(false, showStatus)) != null && !webBrowserProxy.equals(proxy)) {
              lock = null;
              readLock.unlock();
            }
            continue;
          }
          // A change needs exclusive use of the web browser. Making one change at a time lets requests that need the same change skip waiting for exclusive
          // use once it's been made.
          webBrowserChangeLock.lockInterruptibly();
          try {
            if (isWebBrowserChangeNeeded(deadWebBrowser, proxy)) {
              writeLock.lockInterruptibly();
              lock = writeLock;
              driver = changeWebBrowser(deadWebBrowser, proxy, pageLoadTimeout, webBrowserUsed, showStatus);
              readLock.lock(); // Downgrade to share the web browser
              writeLock.unlock();
              lock = readLock;
            }
          } finally {
            webBrowserChangeLock.unlock();
          }
        }
      }

      WebBrowserWindows windows = webBrowserWindows.get();
      if (driver == null || windows == null || windows.driver != driver) {
        throw new IOException("web browser unavailable");
      }
      webBrowserUsed.set(driver);

      if (request.exclusive) {
        windows.window(windows.mainWindow, pageLoadTimeout).run(mainDriver -> {
          mainDriver.getDevTools().createSessionIfThereIsNotOne();
          mainDriver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
          return null;
        });
        try {
          return task.applyWithException(new WebBrowserWindow(driver, pageLoadTimeout));
        } finally {
          windows.forgetCurrentWindow(); // In case the request switched windows
        }
      }

      String host = (new URL(url)).getHost(), window = windows.acquire(host);
      try {
        return task.applyWithException(windows.window(window, pageLoadTimeout));
      } finally {
        windows.release(window, host);
      }
    } finally {
      if (lock != null) {
        lock.unlock();
      }
    }
  }

  private static boolean isWebBrowserChangeNeeded(FirefoxDriver deadWebBrowser, Proxy proxy) {
    return isWebBrowserRestartNeeded(deadWebBrowser) || !webBrowserProxy.equals(proxy);
  }

  // Unless another request already restarted it, or tried to
  private static boolean isWebBrowserRestartNeeded(FirefoxDriver deadWebBrowser) {
    return deadWebBrowser != null && deadWebBrowser == lastWebBrowserDriver.get() && deadWebBrowser != restartedWebBrowser.get();
  }

  // Restarts the web browser and sets its proxy as needed, while having exclusive use of it. Returns the web browser, or null if it's unavailable.
  private static FirefoxDriver changeWebBrowser(FirefoxDriver deadWebBrowser, Proxy proxy, Duration pageLoadTimeout,
          AtomicReference<FirefoxDriver> webBrowserUsed, boolean showStatus) throws Exception {
    if (isWebBrowserRestartNeeded(deadWebBrowser)) {
      restartedWebBrowser.set(deadWebBrowser);
      createWebBrowser(true, showStatus);
    }
    FirefoxDriver driver = createWebBrowser(false, showStatus); // Before checking the proxy, because creating the web browser resets its proxy
    if (driver == null || webBrowserProxy.equals(proxy)) {
      return driver;
    }
    webBrowserUsed.set(driver); // So that the web browser gets restarted if it dies while setting its proxy

    String setProxyScript;
    if (proxy.equals(Proxy.NO_PROXY)) {
      setProxyScript = Str.get(852);
    } else {
      InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
      setProxyScript = String.format(Str.get(853), socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    }
    WebBrowserWindows windows = webBrowserWindows.get();
    WebBrowserWindow mainWindow = windows.window(windows.mainWindow, pageLoadTimeout);
    mainWindow.load(Str.get(851), false);
    webBrowserSleep.run();
    mainWindow.run(mainDriver -> mainDriver.executeScript(setProxyScript));
    webBrowserProxy = proxy;
    webBrowserSleep.run();
    return driver;
  }

  // Stops the standalone web browser. On Windows, its process can be a launcher that starts the actual web browser in another process and exits, so this also
  // stops (and waits for) any web browser process using the standalone web browser's profile, which a following run couldn't otherwise use.
  private static void stopStandaloneWebBrowser(Process firefox, File firefoxBinary) {
    firefox.destroy();
    if (Constant.WINDOWS) { // The profile path is matched with -like, so its wildcard characters are escaped
      int numStopped = stopProcesses("Get-WmiObject Win32_Process -Filter " + powerShellString("Name='" + firefoxBinary.getName() + '\'') + " | Where-Object"
              + " {$_.CommandLine -like " + powerShellString('*' + WEB_BROWSER_PROFILE.getPath().replaceAll("[`\\[\\]*?]", "`$0") + '*') + "} | ForEach-Object"
              + " {$_.ProcessId}", true);
      if (numStopped > 0 && Debug.DEBUG) {
        Debug.println("stopped " + numStopped + " standalone web browser process(es) that outlived its launcher process");
      }
    }
  }

  // Stops every process started from one of the executables, and if wait is true, waits for them to exit.
  private static void killProcesses(boolean wait, File... exes) {
    if (!Constant.WINDOWS) {
      Arrays.stream(exes).forEach(exe -> {
        try {
          Process process = (new ProcessBuilder("pkill", "-9", "-f", exe.getPath())).start();
          if (wait) {
            process.waitFor();
          }
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
      });
      return;
    }

    // Executable paths are matched through WMI because in 32-bit PowerShell (which 32-bit Java starts), Get-Process can't see the paths of 64-bit processes, so it
    // can't tell VidMasta's processes from others with the same name, such as the user's own 64-bit Firefox. Get-Process is the fallback if WMI fails.
    UnaryOperator<String> isExe = pathProperty -> Arrays.stream(exes).map(exe -> "$_." + pathProperty + " -eq " + powerShellString(exe.getPath())).collect(
            Collectors.joining(" -or "));
    int numStopped = stopProcesses("try {Get-WmiObject Win32_Process -Filter " + powerShellString(Arrays.stream(exes).map(exe -> "Name='" + exe.getName() + '\'')
            .collect(Collectors.joining(" OR "))) + " -ErrorAction Stop | Where-Object {" + isExe.apply("ExecutablePath") + "} | ForEach-Object {$_.ProcessId}}"
            + " catch {Get-Process -Name " + Arrays.stream(exes).map(exe -> powerShellString(StringUtils.substringBeforeLast(exe.getName(), "."))).collect(
                    Collectors.joining(",")) + " -ErrorAction SilentlyContinue | Where-Object {" + isExe.apply("Path") + "} | ForEach-Object {$_.Id}}", wait);
    if (numStopped > 0 && Debug.DEBUG) {
      Debug.println("stopped " + numStopped + " web browser process(es)");
    }
  }

  // Runs Windows PowerShell to stop the processes whose IDs the given statement outputs, and if wait is true, waits up to 10 seconds for them to exit. Returns how
  // many it stopped, or -1 if unknown. The statement should only use cmdlets and operators (no .NET calls), so that it works in PowerShell 2.0 (Windows 7) to 5.1
  // (Windows 10/11), including in Constrained Language Mode.
  private static int stopProcesses(String processIds, boolean wait) {
    try {
      Process stopper = (new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", "$p = @(" + processIds + "); if ($p.Count) {Stop-Process -Id"
              + " $p -Force -ErrorAction SilentlyContinue; Wait-Process -Id $p -Timeout 10 -ErrorAction SilentlyContinue}; $p.Count")).redirectErrorStream(true)
              .start();
      stopper.getOutputStream().close(); // Or PowerShell can wait for input
      if (!wait) {
        return -1;
      }
      if (!stopper.waitFor(30, TimeUnit.SECONDS)) {
        stopper.destroy();
        if (Debug.DEBUG) {
          Debug.println("timed out stopping processes");
        }
        return -1;
      }
      String numStopped = Regex.firstMatch(new String(IOUtils.toByteArray(stopper.getInputStream())), "\\d++(?=\\s*+\\z)");
      return numStopped.isEmpty() ? -1 : Integer.parseInt(numStopped);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // The started PowerShell still stops the processes
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
    }
    return -1;
  }

  // A single-quoted PowerShell string, which PowerShell also lets typographic single quotes end
  private static String powerShellString(String str) {
    return '\'' + str.replaceAll("['\\u2018\\u2019\\u201A\\u201B]", "$0$0") + '\'';
  }

  // curl's exit code when ECH (Encrypted Client Hello) is rejected
  private static final int CURLE_ECH_REQUIRED = 101;

  private static String curl(String url, String outputPath, String referer, int maxNumRedirects, String cookie, Proxy proxy, Supplier<Boolean> isCancelled,
          ThrowingBiFunction<Integer, CharSequence, String> retry) throws Exception {
    List<String> curlArgs = new ArrayList<>(curlCommand());
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
    webBrowserCookies.values().stream().sorted().forEach(cookieData -> Collections.addAll(curlArgs, Str.get(928), quote.apply(cookieData)));
    boolean greaseEch = echRejectingHosts.contains(urlObj.getHost());
    if (greaseEch) {
      Collections.addAll(curlArgs, "--ech", "grease");
    }
    String output = (outputPath == null ? Constant.TEMP_DIR + UUID.randomUUID().toString() : outputPath);
    Collections.addAll(curlArgs, Str.get(920), quote.apply(output));
    curlArgs.add(quote.apply(url));
    Process curlProcess = startCurl(curlArgs);
    try {
      if (isCancelled.get()) {
        return null;
      }

      // The output is read on another thread, so that this one can stop curl as soon as the request is cancelled, or if curl hangs without writing anything
      // (as it once did at startup right after a cold boot), instead of waiting for curl to write or exit
      StringBuffer curlOutput = new StringBuffer(8192);
      AtomicBoolean isErrorOutput = new AtomicBoolean();
      Worker outputReader = Worker.submit(() -> {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(curlProcess.getInputStream(), Constant.UTF8))) {
          String line;
          while ((line = br.readLine()) != null) {
            curlOutput.append(line).append(Constant.NEWLINE);
            if (!Regex.firstMatch(line, 921).isEmpty()) {
              isErrorOutput.set(true);
              curlProcess.destroy();
            }
          }
        }
      });
      long hangNanoTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout + 30);
      while (!curlProcess.waitFor(250, TimeUnit.MILLISECONDS)) {
        if (isCancelled.get()) {
          return null;
        }
        if (curlOutput.length() == 0 && System.nanoTime() - hangNanoTime >= 0) {
          if (Debug.DEBUG) {
            Debug.println("Curl request hung.");
          }
          throw new IOException(error(url));
        }
      }
      try {
        outputReader.get(30, TimeUnit.SECONDS); // The rest of the output, which ends when curl exits
      } catch (java.util.concurrent.TimeoutException e) {
        throw new IOException(error(url), e);
      }
      if (isErrorOutput.get()) {
        if (Debug.DEBUG) {
          Debug.println("\n" + curlOutput);
        }
        throw new IOException(error(url));
      }
      int exitValue = curlProcess.exitValue();
      if (exitValue == CURLE_ECH_REQUIRED && !greaseEch) {
        // Unlike a web browser, curl doesn't retry without ECH when ECH is rejected, such as by antivirus HTTPS scanning, which intercepts TLS without
        // supporting ECH and so rejects it without the retry configs a server would send. Retry with ECH GREASE, as a browser without an ECH config sends.
        if (Debug.DEBUG) {
          Debug.println("retrying without ECH because it was rejected: " + url + " (curl)");
        }
        echRejectingHosts.add(urlObj.getHost());
        return curl(url, outputPath, referer, maxNumRedirects, cookie, proxy, isCancelled, retry);
      }
      if (exitValue != 0) {
        if (Debug.DEBUG) {
          Debug.println("Curl request bad exit value: " + exitValue + ".");
        }
        throw new IOException(error(url));
      }

      int responseCode = Integer.parseInt(Regex.firstMatch(curlOutput.toString(), 922));
      String source;
      if (retry != null && (source = retry.apply(responseCode, curlOutput)) != null) {
        IO.write(output, source);
        return output;
      }

      checkConnectionResponse(responseCode, url, () -> "");
      return output;
    } catch (Exception e) {
      IO.fileOp(output, IO.RM_FILE);
      throw e;
    } finally {
      stopCurl(curlProcess);
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
      downloadLinkInfoProxyLock.lock(); // Even when interrupted (such as by a cancellation after the deproxier's last wait), so that it's always counted out
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
      // Not if already deproxied, or if not proxied to begin with (the URLs are the site's own), since searching again would then be the same search
      if (Str.containsListener(deproxyDownloadLinkInfo) || Str.get(727).equals(Str.get(731))) {
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
    IO.fileOp(Constant.TEMP_DIR + Str.hashCode(url) + Constant.HTML, IO.RM_FILE); // The web browser's copy, which it reuses for as long as the request allows
  }

  public static String saveData(String url, String outputPath, DomainType domainType) throws Exception {
    return saveData(url, outputPath, domainType, true);
  }

  public static String saveData(String url, String outputPath, DomainType domainType, boolean showStatus) throws Exception {
    return saveData(url, outputPath, domainType, showStatus, null);
  }

  public static String saveData(String url, String outputPath, DomainType domainType, boolean showStatus, String referer) throws Exception {
    return saveData(url, outputPath, domainType, showStatus, referer, MAX_NUM_REDIRECTS, null, false);
  }

  public static String saveData(String url, String outputPath, DomainType domainType, boolean showStatus, String referer, int maxNumRedirects, String cookie,
          boolean useWebBrowser) throws Exception {
    boolean useCurl = Regex.isMatch(url, 924) && curlCommand(showStatus) != null;
    if ((useWebBrowser || (Regex.isMatch(url, 872) && !useCurl)) && createWebBrowser(false, showStatus) != null) {
      return getSourceCode(url, domainType, showStatus, true, -1, new WebBrowserRequest(outputPath, referer, cookie) {
        @Override
        public String get(String url, FirefoxDriver driver, ThrowingRunnable sleep, long cacheExpirationMs) throws Exception {
          long startTime = System.currentTimeMillis();
          load(driver, "about:about");
          Duration timeout = driver.manage().timeouts().getPageLoadTimeout();
          (new WebDriverWait(driver, timeout)).until(ExpectedConditions.presenceOfElementLocated(By.xpath(Str.get(874))));

          File download = null;
          Runnable clearDownloads = null;
          try {
            waitUntilResponseReceived(String.format(Str.get(875), Regex.replaceAllRepeatedly(url, 876)), () -> driver.executeScript(String.format(Str.get(878),
                    url, UUID.randomUUID().toString())), driver);
            sleep.run();

            load(driver, "about:downloads");
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
              String filename = Regex.replaceAllRepeatedly(Regex.firstMatch(Regex.firstMatch(newestDownload, 882), 883), 951);
              String status = Regex.firstMatch(Regex.firstMatch(newestDownload, 884), 885);
              if (Regex.isMatch(status, Str.get(886))) {
                File output = new File(outputPath);
                if (output.isDirectory()) {
                  output = new File(outputPath, filename);
                }
                String outputFilePath = output.getCanonicalPath();
                if (!(download = new File(Constant.TEMP_DIR, filename)).getCanonicalPath().equals(outputFilePath)) {
                  IO.write(download, output);
                  IO.fileOp(download, IO.RM_FILE);
                }
                clearDownloads.run();
                long endTime = System.currentTimeMillis();
                if (Debug.DEBUG) {
                  Debug.println(url + " (web browser save took " + ((endTime - startTime) / 1_000) + "s)");
                }
                return outputFilePath;
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
    }

    if (Debug.DEBUG) {
      Debug.println(url + " (" + (useCurl ? "curl " : "") + "save)");
    }
    executeRequest(url, new Worker() {
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
            curl(url, outputPath, referer, maxNumRedirects, cookie, proxy, this::isCancelled, null);
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
    });
    return outputPath;
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

  private static synchronized void saveCookies() throws Exception {
    IO.write(COOKIES, webBrowserCookies.entrySet().stream().map(currCookie -> currCookie.getKey() + '\t' + currCookie.getValue()).sorted().collect(
            Collectors.joining(Constant.NEWLINE)));
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

    // The thread's message, or null if it has none
    String get(Thread thread) {
      for (Msg msg : msgs) {
        if (msg.thread.equals(thread)) {
          return msg.msg;
        }
      }
      return null;
    }

    // Replaces the thread's message, str, with newStr, or removes it if newStr is null. Nothing changes if the thread's message is no longer str, such as when
    // the cancellation of the thread's request removed it.
    void replace(Thread thread, String str, String newStr) {
      if (str.equals(get(thread))) {
        if (newStr == null) {
          unset(thread);
        } else {
          set(thread, newStr);
        }
      }
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

  // A web browser request. Plain page requests share the web browser with each other, while overriding get() gives a request exclusive use of the web browser
  // in its main window. FirefoxDriver.get() doesn't wait for pages to load (the web browser's page load strategy is none), so use load() to navigate instead.
  public static class WebBrowserRequest {

    public volatile String urlAfterEvt, cookiesAfterEvt;
    final String outputPath, referer, cookie;
    final boolean exclusive;

    public WebBrowserRequest() {
      this(null, null, null);
    }

    WebBrowserRequest(String outputPath, String referer, String cookie) {
      this.outputPath = outputPath;
      this.referer = referer;
      this.cookie = cookie;
      try {
        exclusive = (getClass().getMethod("get", String.class, FirefoxDriver.class, ThrowingRunnable.class, long.class).getDeclaringClass()
                != WebBrowserRequest.class);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException(e);
      }
    }

    public String retry() throws Exception {
      return null;
    }

    public String get(String url, FirefoxDriver driver, ThrowingRunnable sleep, long cacheExpirationMs) throws Exception {
      return get(url, new WebBrowserWindow(driver, driver.manage().timeouts().getPageLoadTimeout()), cacheExpirationMs, true);
    }

    // Navigates to the URL and waits for the page to load
    protected void load(FirefoxDriver driver, String url) throws Exception {
      (new WebBrowserWindow(driver, driver.manage().timeouts().getPageLoadTimeout())).load(url, false);
    }

    private String get(String url, WebBrowserWindow window, long cacheExpirationMs, boolean loadCookies) throws Exception {
      File sourceCodeFile = new File(Constant.TEMP_DIR + Str.hashCode(url) + Constant.HTML);
      if (cacheExpirationMs <= 0 || !sourceCodeFile.exists() || IO.isFileTooOld(sourceCodeFile, cacheExpirationMs)) {
        long startTime = System.currentTimeMillis();
        String host = (new URL(url)).getHost();
        window.run(this::loadCookies);
        boolean isHostCookiesLoaded = webBrowserHosts.contains(host);
        window.load(url, true);
        String src = Optional.ofNullable(window.run(FirefoxDriver::getPageSource)).orElse("");
        long endTime = System.currentTimeMillis();
        if (Debug.DEBUG) {
          Debug.println(url + " (web browser" + (loadCookies ? "" : " retry with updated cookies") + " took " + ((endTime - startTime) / 1_000) + "s)");
        }

        // Also retry if another window loaded the host's cookies while this one loaded the page
        if (loadCookies && !Regex.firstMatch(src, 942).isEmpty() && window.run(driver -> (!isHostCookiesLoaded && webBrowserHosts.contains(host))
                || loadCookies(driver))) {
          return get(url, window, cacheExpirationMs, false);
        }

        webBrowserHosts.add(host);
        cache(url, src);
        window.run(driver -> driver.manage().getCookies()).stream().filter(currCookie -> Regex.isMatch(currCookie.toString(), 935)).forEach(currCookie
                -> webBrowserCookies.put(currCookie.getDomain() + ' ' + currCookie.getName(), currCookie.toString()));
        saveCookies();
        return src;
      }
      if (Debug.DEBUG) {
        Debug.println("fetching " + url + " (web browser cache)");
      }
      return IO.read(sourceCodeFile);
    }

    private boolean loadCookies(FirefoxDriver driver) {
      String url = driver.getCurrentUrl();
      if (!Regex.isMatch(url, "\\A(?i)https?+://.+")) {
        return false;
      }

      String oldCookies = "", newCookies = "";
      try {
        String host = (new URL(url)).getHost();
        if (!webBrowserHosts.add(host)) {
          return false;
        }

        Supplier<String> cookiesStr = () -> driver.manage().getCookies().stream().map(currCookie -> ImmutableMap.of("name", currCookie.getName(), "value",
                currCookie.getValue(), "domain", currCookie.getDomain(), "path", currCookie.getPath(), "expires",
                Optional.ofNullable(currCookie.getExpiry()).map(Date::toString).orElse("")).toString()).sorted().collect(Collectors.joining("\n"));
        oldCookies = cookiesStr.get();
        BinaryOperator<String> cookieAttr = (cookieStr, attrName) -> Regex.match(cookieStr, ";\\s*+(?i)" + attrName + "\\s*+\\=\\s*+", ";|\\z");
        List<Cookie> cookies = webBrowserCookies.values().stream().filter(currCookie -> {
          String domain = cookieAttr.apply(currCookie, "domain");
          return host.equalsIgnoreCase(domain) || host.equalsIgnoreCase(Regex.replaceFirst(domain, "\\A\\.(?i)(www\\.)?+", "www.")) || host.equalsIgnoreCase(
                  Regex.replaceFirst(domain, "\\A\\.(?i)(www\\.)?+", ""));
        }).map(currCookie -> new Cookie(Regex.firstMatch(currCookie, "\\A[^\\=]++"), Regex.match(currCookie, "\\A[^\\=]++\\=", ";|\\z"), cookieAttr.apply(
                currCookie, "domain"), cookieAttr.apply(currCookie, "path"), Optional.of(cookieAttr.apply(currCookie, "expires")).filter(
                StringUtils::isNotEmpty).map(expiry -> {
                  TemporalAccessor dateTime;
                  try {
                    dateTime = DateTimeFormatter.RFC_1123_DATE_TIME.parse(expiry);
                  } catch (DateTimeParseException e) {
                    if (Debug.DEBUG) {
                      Debug.println("unexpected (1) cookie expiration date: " + e);
                    }
                    try {
                      dateTime = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).parse(expiry);
                    } catch (DateTimeParseException e2) {
                      if (Debug.DEBUG) {
                        Debug.println("unexpected (2) cookie expiration date: " + e2);
                      }
                      dateTime = null;
                    }
                  }
                  return Optional.ofNullable(dateTime).map(date -> Date.from(LocalDateTime.from(date).atZone(Optional.ofNullable(date.query(
                          TemporalQueries.zoneId())).orElse(ZoneId.of("GMT"))).toInstant())).orElse(null);
                }).orElse(null))).collect(Collectors.toList());
        if (!cookies.isEmpty()) {
          cookies.forEach(driver.manage()::addCookie);
          newCookies = cookiesStr.get();
        }
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
      }
      return !newCookies.isEmpty() && !oldCookies.equals(newCookies);
    }

    void cache(String url, String source) throws Exception {
      if (!Regex.isMatch(source, 898)) {
        // Written to a temporary file that then replaces the cached page, because concurrent requests for the URL can read the cached page meanwhile
        File temp = new File(Constant.TEMP_DIR, UUID.randomUUID().toString());
        IO.write(temp, source);
        try {
          Files.move(temp.toPath(), (new File(Constant.TEMP_DIR + Str.hashCode(url) + Constant.HTML)).toPath(), StandardCopyOption.REPLACE_EXISTING,
                  StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) { // Such as while another request reads the cached page on Windows
          IO.fileOp(temp, IO.RM_FILE);
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
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

  // A window of the web browser: the session's current window, unless shared through WebBrowserWindows
  private static class WebBrowserWindow {

    // Checking on a loading page quickly at first catches quick loads, and then less often leaves the session free for other windows. Late checks don't delay
    // pages whose changes are waited for because the wait counts from when the page finished loading.
    private static final long MIN_POLL_MS = 50, MAX_POLL_MS = 400;
    private static final String SAME_PAGE = "samePage";
    // Marks the page as old, unless navigating to the URL only changes the page's fragment (such as for a hash-routed page), which keeps the page. Then it
    // returns SAME_PAGE and counts the navigation as a change of the page.
    private static final String MARK_OLD_PAGE_SCRIPT = "var d = document, u = null;"
            + "try {"
            + "  u = new URL(arguments[0], location.href).href;"
            + "} catch (e) {"
            + "}"
            + "if (u !== null && u.indexOf('#') !== -1 && u.split('#')[0] === location.href.split('#')[0]) {"
            + "  if (d.__vidMastaPage) {"
            + "    d.__vidMastaPage.lastChange = Date.now();"
            + "  }"
            + "  return '" + SAME_PAGE + "';"
            + "}"
            + "if (!d.__vidMastaOldPage) {"
            + "  Object.defineProperty(d, '__vidMastaOldPage', {value: true});"
            + "}"
            + "return d.__vidMastaOldPage === true;";
    // Returns null until the new page replaces the old one (unless arguments[1] says the page is kept), the URI of an error page, -1 while loading, and then the
    // milliseconds since the page last changed or finished loading. Changes are watched for from the first check, so that the time since the last change
    // includes time spent loading.
    private static final String PAGE_STATE_SCRIPT = "var d = document, s = d.__vidMastaPage;"
            + "if ((d.__vidMastaOldPage && !arguments[1]) || (location.href === 'about:blank' && arguments[0] !== 'about:blank')) {"
            + "  return null;"
            + "}"
            + "if (/^about:((neterror)|(certerror)|(blocked)|(tabcrashed))/.test(d.documentURI)) {"
            + "  return d.documentURI;"
            + "}"
            + "window.alert = function() { return true; };"
            + "window.confirm = function() { return true; };"
            + "window.prompt = function() { return true; };"
            + "if (!s) {"
            + "  Object.defineProperty(d, '__vidMastaPage', {value: s = {lastChange: Date.now()}});"
            + "  new MutationObserver(function() { s.lastChange = Date.now(); }).observe(d, {attributes: true, characterData: true, childList: true,"
            + " subtree: true});"
            + "}"
            + "if (d.readyState !== 'complete') {"
            + "  return -1;"
            + "}"
            + "if (!s.loaded) {"
            + "  s.loaded = (window.performance && performance.timing && performance.timing.loadEventEnd) || Date.now();"
            + "}"
            + "return Math.max(0, Date.now() - Math.max(s.lastChange, s.loaded));";
    final FirefoxDriver driver;
    final Duration pageLoadTimeout;

    WebBrowserWindow(FirefoxDriver driver, Duration pageLoadTimeout) {
      this.driver = driver;
      this.pageLoadTimeout = pageLoadTimeout;
    }

    // Runs WebDriver commands in this window
    <T> T run(ThrowingFunction<FirefoxDriver, T> commands) throws Exception {
      return commands.applyWithException(driver);
    }

    // Navigates to the URL and waits for the page to load. Then, optionally, gives asynchronous JavaScript with variable/unpredictable page changes time to
    // finish by waiting until the page hasn't changed for a while.
    void load(String url, boolean waitForChangesToStop) throws Exception {
      Object mark = null;
      for (int attempt = 1;; attempt++) { // Mark the old page to tell it apart from the new one
        try {
          mark = run(currDriver -> currDriver.executeScript(MARK_OLD_PAGE_SCRIPT, url));
          if (Boolean.TRUE.equals(mark) || SAME_PAGE.equals(mark)) {
            break;
          }
          if (attempt == 10) {
            throw new WebDriverException("unable to mark page before loading " + url);
          }
        } catch (WebDriverException e) { // Such as while the old page is being replaced. A timeout (an unresponsive web browser) isn't retried.
          if (attempt == 10 || e instanceof TimeoutException || isWebBrowserDead(e)) {
            throw e;
          }
        }
        Thread.sleep(MIN_POLL_MS * 2);
      }
      boolean isSamePage = SAME_PAGE.equals(mark);
      try {
        run(currDriver -> {
          currDriver.get(url);
          return null;
        });
      } catch (WebDriverException e) {
        if (e instanceof TimeoutException || isWebBrowserDead(e)) {
          throw e;
        }
        try {
          run(FirefoxDriver::getWindowHandle);
        } catch (WebDriverException e2) { // The web browser broke, which restarting it can fix
          e.addSuppressed(e2);
          throw e;
        }
        throw new WebBrowserErrorPageException("unable to load " + url + ": " + e.getRawMessage(), e); // Such as for a restricted port
      }

      long startTime = System.currentTimeMillis(), timeoutMs = pageLoadTimeout.toMillis(), quietMs = Long.parseLong(Str.get(932)), maxEndTime = -1;
      long sleepMs = MIN_POLL_MS, loadingSleepMs = MIN_POLL_MS, sourceCheckTime = -1;
      String prevSource = null;
      for (int numErrors = 0;;) {
        Thread.sleep(sleepMs);
        Object pageState;
        try {
          pageState = run(currDriver -> currDriver.executeScript(PAGE_STATE_SCRIPT, url, isSamePage));
          numErrors = 0;
        } catch (WebDriverException e) { // Such as while the page is being replaced
          if (++numErrors == 10 || e instanceof TimeoutException || isWebBrowserDead(e)) {
            throw e;
          }
          pageState = null;
        }
        if (pageState instanceof String) {
          throw new WebBrowserErrorPageException("Reached error page: " + pageState);
        }

        long now = System.currentTimeMillis(), msSinceChange = (pageState instanceof Number ? ((Number) pageState).longValue() : -1);
        if (msSinceChange < 0) {
          if (maxEndTime == -1 && now - startTime >= timeoutMs) {
            throw new TimeoutException("Timed out waiting for page to load: " + url);
          }
          sleepMs = loadingSleepMs = Math.min(loadingSleepMs * 2, MAX_POLL_MS);
        } else if (!waitForChangesToStop || msSinceChange >= quietMs) {
          return;
        } else {
          if (maxEndTime == -1) {
            maxEndTime = now + quietMs + (timeoutMs * 2);
            sourceCheckTime = now + (quietMs * 2);
          }
          if (now >= sourceCheckTime) { // Changes can leave the page as it was (such as setting an attribute to its value), so also compare the page itself
            String source;
            try {
              source = run(FirefoxDriver::getPageSource);
            } catch (WebDriverException e) {
              if (e instanceof TimeoutException || isWebBrowserDead(e)) {
                throw e;
              }
              source = null;
            }
            if (source != null && source.equals(prevSource)) {
              return;
            }
            prevSource = source;
            sourceCheckTime = now + quietMs;
          }
          sleepMs = quietMs - msSinceChange;
        }
        if (maxEndTime != -1) {
          if (now >= maxEndTime) {
            if (Debug.DEBUG) {
              Debug.println("web browser page kept changing: " + url);
            }
            return;
          }
          sleepMs = Math.min(sleepMs, maxEndTime - now);
        }
      }
    }

    private static boolean isWebBrowserDead(WebDriverException e) {
      return e instanceof NoSuchSessionException || e instanceof UnreachableBrowserException;
    }
  }

  // The web browser's windows, which let page loads share the web browser by loading in separate windows. Since WebDriver commands go to the session's
  // current window, commands for a shared window run while holding a command lock, after switching to that window.
  private static final class WebBrowserWindows {

    final FirefoxDriver driver;
    final String mainWindow;
    private final Lock commandLock = new ReentrantLock();
    private String currWindow; // Guarded by commandLock
    private final Deque<String> idleWindows = new ArrayDeque<>(MAX_NUM_WEB_BROWSER_WINDOWS); // Guarded by this
    private final Map<String, String> windowHosts = new HashMap<>(MAX_NUM_WEB_BROWSER_WINDOWS * 2); // Guarded by this
    private int numWindows = 1, maxNumWindows = MAX_NUM_WEB_BROWSER_WINDOWS; // Guarded by this

    WebBrowserWindows(FirefoxDriver driver) {
      this.driver = driver;
      idleWindows.add(mainWindow = currWindow = driver.getWindowHandle());
    }

    WebBrowserWindow window(String window, Duration pageLoadTimeout) {
      return new WebBrowserWindow(driver, pageLoadTimeout) {
        @Override
        <T> T run(ThrowingFunction<FirefoxDriver, T> commands) throws Exception {
          commandLock.lockInterruptibly();
          try {
            if (!window.equals(currWindow)) {
              currWindow = null; // Unknown if switching fails
              driver.switchTo().window(window);
              currWindow = window;
            }
            return commands.applyWithException(driver);
          } finally {
            commandLock.unlock();
          }
        }
      };
    }

    // Waits for a window that isn't in use, preferring one showing a page from the host (whose cookies can then be loaded before navigating), or opens a new
    // window if there are fewer than the maximum number of windows
    String acquire(String host) throws Exception {
      int windowNum;
      synchronized (this) {
        while (idleWindows.isEmpty() && numWindows >= maxNumWindows) {
          wait();
        }
        if (!idleWindows.isEmpty()) {
          for (Iterator<String> it = idleWindows.iterator(); it.hasNext();) {
            String window = it.next();
            if (host.equals(windowHosts.get(window))) {
              it.remove();
              return window;
            }
          }
          return idleWindows.pop();
        }
        windowNum = ++numWindows;
      }

      boolean opened = false;
      try {
        commandLock.lockInterruptibly();
        try {
          currWindow = null;
          driver.switchTo().newWindow(WindowType.WINDOW);
          String window = currWindow = driver.getWindowHandle();
          opened = true;
          if (Debug.DEBUG) {
            Debug.println("web browser window " + windowNum + " opened");
          }
          return window;
        } finally {
          commandLock.unlock();
        }
      } catch (WebDriverException e) {
        if (Thread.interrupted()) { // Such as when the request was cancelled, which Selenium reports as an unreachable web browser
          InterruptedException interruption = new InterruptedException("web browser window not opened");
          interruption.initCause(e);
          throw interruption;
        }
        if (e instanceof TimeoutException || WebBrowserWindow.isWebBrowserDead(e)) { // Not because of the number of windows
          throw e;
        }
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        synchronized (this) {
          maxNumWindows = numWindows - 1; // Make do with the windows already open
        }
      } finally {
        if (!opened) {
          synchronized (this) {
            numWindows--;
            notifyAll();
          }
        }
      }
      return acquire(host);
    }

    synchronized void release(String window, String host) {
      windowHosts.put(window, host);
      idleWindows.push(window);
      notify();
    }

    void forgetCurrentWindow() {
      commandLock.lock();
      try {
        currWindow = null;
      } finally {
        commandLock.unlock();
      }
    }
  }

  // A web browser error page instead of the requested page, which restarting the web browser wouldn't fix
  private static class WebBrowserErrorPageException extends WebDriverException {

    private static final long serialVersionUID = 1L;

    WebBrowserErrorPageException(String msg) {
      super(msg);
    }

    WebBrowserErrorPageException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  private Connection() {
  }
}

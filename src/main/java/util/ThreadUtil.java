package util;

import debug.Debug;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ThreadUtil {

  // The JDK's thread management, or null if it's unavailable
  public static ThreadMXBean threadMXBean() {
    try {
      return ManagementFactory.getThreadMXBean();
    } catch (Exception | LinkageError e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      return null;
    }
  }

  // Every thread's stack, with the locks it holds and the lock it waits for: a deadlock always involves more than one thread. Without threadMXBean, only the
  // stacks.
  public static String threadDump(ThreadMXBean threadMXBean) {
    StringBuilder dump = new StringBuilder(32_768);
    if (threadMXBean != null) {
      try {
        for (ThreadInfo info : threadMXBean.dumpAllThreads(true, true)) {
          dump.append('"').append(info.getThreadName()).append("\" ").append(info.getThreadState());
          if (info.getLockName() != null) {
            dump.append(" on ").append(info.getLockName());
          }
          if (info.getLockOwnerName() != null) {
            dump.append(" owned by \"").append(info.getLockOwnerName()).append('"');
          }
          dump.append(Constant.NEWLINE);
          StackTraceElement[] stack = info.getStackTrace();
          for (int i = 0; i < stack.length; i++) {
            dump.append("\tat ").append(stack[i]).append(Constant.NEWLINE);
            for (MonitorInfo monitor : info.getLockedMonitors()) {
              if (monitor.getLockedStackDepth() == i) {
                dump.append("\t- locked ").append(monitor).append(Constant.NEWLINE);
              }
            }
          }
          for (LockInfo lock : info.getLockedSynchronizers()) {
            dump.append("\t- locked ").append(lock).append(Constant.NEWLINE);
          }
          dump.append(Constant.NEWLINE);
        }
        return dump.toString();
      } catch (Exception e) {
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        dump.setLength(0);
      }
    }
    for (Map.Entry<Thread, StackTraceElement[]> thread : Thread.getAllStackTraces().entrySet()) {
      dump.append('"').append(thread.getKey().getName()).append("\" ").append(thread.getKey().getState()).append(Constant.NEWLINE);
      for (StackTraceElement frame : thread.getValue()) {
        dump.append("\tat ").append(frame).append(Constant.NEWLINE);
      }
      dump.append(Constant.NEWLINE);
    }
    return dump.toString();
  }

  // For a shutdown hook to call first: halts the JVM if it hasn't exited by the time limit, logging what it was stuck on. The JVM exits only once every shutdown
  // hook is done, so halting is the only way out of one that never ends (during shutdown, System.exit() just blocks).
  public static void haltIfShutdownHangs(long timeoutSecs) {
    Thread shutdownHook = Thread.currentThread();
    ThreadMXBean threadMXBean = threadMXBean(); // Before it's needed, in case getting it then would get stuck too
    Thread watchdog = new Thread(() -> {
      try {
        Thread.sleep(TimeUnit.SECONDS.toMillis(timeoutSecs));
      } catch (InterruptedException e) {
        return;
      }
      try {
        Exception e = new Exception("shutdown took over " + timeoutSecs + " seconds" + Constant.NEWLINE + threadDump(threadMXBean));
        e.setStackTrace(shutdownHook.getStackTrace()); // Empty if the caller's hook is done, and another one is stuck
        if (Debug.DEBUG) {
          Debug.print(e);
        }
        IO.write(Constant.APP_DIR + Constant.ERROR_LOG, e);
      } finally { // Even if logging failed (such as with an Error), since the JVM would otherwise never exit
        Runtime.getRuntime().halt(0);
      }
    }, "shutdown watchdog");
    watchdog.setDaemon(true);
    watchdog.start();
  }

  private ThreadUtil() {
  }
}

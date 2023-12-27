package util;

@FunctionalInterface
public interface ThrowingRunnable extends Runnable {

  void runWithException() throws Exception;

  @Override
  default void run() {
    try {
      runWithException();
    } catch (Exception e) {
      ThrowableUtil.sneakyThrow(e);
    }
  }

  static ThrowingRunnable of(ThrowingRunnable runnable) {
    return runnable;
  }

  static void run(ThrowingRunnable runnable) {
    runnable.run();
  }
}

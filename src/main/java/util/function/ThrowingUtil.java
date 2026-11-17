package util.function;

public class ThrowingUtil {

  @SuppressWarnings("unchecked")
  public static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }

  private ThrowingUtil() {
  }
}

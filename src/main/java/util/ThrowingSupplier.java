package util;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T> extends Supplier<T> {

  T getWithException() throws Exception;

  @Override
  default T get() {
    try {
      return getWithException();
    } catch (Exception e) {
      ThrowableUtil.sneakyThrow(e);
      return null;
    }
  }

  static <T> ThrowingSupplier<T> of(ThrowingSupplier<T> supplier) {
    return supplier;
  }

  static <T> T get(ThrowingSupplier<T> supplier) {
    return supplier.get();
  }
}

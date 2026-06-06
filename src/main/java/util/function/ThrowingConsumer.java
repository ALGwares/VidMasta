package util.function;

import java.util.function.Consumer;
import util.ThrowableUtil;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

  void acceptWithException(T t) throws Exception;

  @Override
  default void accept(T t) {
    try {
      acceptWithException(t);
    } catch (Exception e) {
      ThrowableUtil.sneakyThrow(e);
    }
  }

  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
    return consumer;
  }
}

package util.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

  void acceptWithException(T t) throws Exception;

  @Override
  default void accept(T t) {
    try {
      acceptWithException(t);
    } catch (Exception e) {
      ThrowingUtil.sneakyThrow(e);
    }
  }

  static <T> ThrowingConsumer<T> of(ThrowingConsumer<T> consumer) {
    return consumer;
  }
}

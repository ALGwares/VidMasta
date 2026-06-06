package util.function;

import java.util.function.BiFunction;
import util.ThrowableUtil;

@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {

  R applyWithException(T t, U u) throws Exception;

  @Override
  default R apply(T t, U u) {
    try {
      return applyWithException(t, u);
    } catch (Exception e) {
      ThrowableUtil.sneakyThrow(e);
      return null;
    }
  }

  static <T, U, R> ThrowingBiFunction<T, U, R> of(ThrowingBiFunction<T, U, R> function) {
    return function;
  }
}

package util.function;

import java.util.function.Function;
import util.ThrowableUtil;

@FunctionalInterface
public interface ThrowingFunction<T, R> extends Function<T, R> {

  R applyWithException(T t) throws Exception;

  @Override
  default R apply(T t) {
    try {
      return applyWithException(t);
    } catch (Exception e) {
      ThrowableUtil.sneakyThrow(e);
      return null;
    }
  }

  static <T, R> ThrowingFunction<T, R> of(ThrowingFunction<T, R> function) {
    return function;
  }
}

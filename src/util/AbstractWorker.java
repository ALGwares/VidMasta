package util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractWorker<T> implements RunnableFuture<T> {

  private static final ExecutorService executorService = Executors.newCachedThreadPool();

  public enum StateValue {

    PENDING, STARTED, DONE
  }

  private volatile StateValue state = StateValue.PENDING;
  protected volatile Runnable doneAction;
  private final RunnableFuture<T> future = new FutureTask<T>(new Callable<T>() {
    @Override
    public T call() throws Exception {
      state = StateValue.STARTED;
      return doInBackground();
    }
  }) {
    @Override
    protected void done() {
      state = StateValue.DONE;
      if (doneAction != null) {
        doneAction.run();
      }
    }
  };

  protected void done() {
    state = StateValue.DONE;
  }

  public StateValue getState() {
    return state;
  }

  protected abstract T doInBackground() throws Exception;

  public T executeAndGet() throws Exception {
    try {
      executorService.execute(future);
      return get(future);
    } catch (Exception e) {
      future.cancel(true);
      throw e;
    }
  }

  public void execute() {
    executorService.execute(future);
  }

  @Override
  public void run() {
    future.run();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return future.cancel(mayInterruptIfRunning);
  }

  @Override
  public final T get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public final T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  public static <V> V get(Future<V> future) throws Exception {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw ThrowableUtil.cause(e);
    }
  }

  public static void executeAndWaitFor(Iterable<? extends AbstractWorker<?>> workers) throws Exception {
    try {
      for (AbstractWorker<?> worker : workers) {
        worker.execute();
      }
      for (AbstractWorker<?> worker : workers) {
        get(worker);
      }
    } catch (Exception e) {
      for (AbstractWorker<?> worker : workers) {
        worker.cancel(true);
      }
      throw e;
    }
  }

  public static void shutdown() {
    executorService.shutdownNow();
  }
}

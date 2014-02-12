package util;

import java.beans.PropertyChangeListener;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import javax.swing.SwingWorker;

public class RunnableUtil {

    public static void execute(Iterable<? extends SwingWorker<?, ?>> childWorkers) throws Exception {
        try {
            for (SwingWorker<?, ?> childWorker : childWorkers) {
                childWorker.execute();
            }
            for (SwingWorker<?, ?> childWorker : childWorkers) {
                waitFor(childWorker);
            }
        } catch (Exception e) {
            for (SwingWorker<?, ?> childWorker : childWorkers) {
                childWorker.cancel(true);
            }
            throw e;
        }
    }

    public static <V> V waitFor(Future<V> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw ExceptionUtil.cause(e);
        }
    }

    public static abstract class AbstractWorker<T> {

        private static final ExecutorService executorService = Executors.newCachedThreadPool();
        private final FutureTask<T> future;
        protected volatile PropertyChangeListener doneListener;

        public AbstractWorker() {
            future = new FutureTask<T>(new Callable<T>() {
                @Override
                public T call() throws Exception {
                    return AbstractWorker.this.call();
                }
            }) {
                @Override
                protected void done() {
                    if (doneListener != null) {
                        doneListener.propertyChange(null);
                    }
                }
            };
        }

        protected abstract T call() throws Exception;

        public T runAndWaitFor() throws Exception {
            executorService.execute(future);
            try {
                return waitFor(future);
            } catch (InterruptedException e) {
                future.cancel(true);
                throw e;
            } catch (CancellationException e) {
                future.cancel(true);
                throw e;
            }
        }

        public boolean isCancelled() {
            return future.isCancelled();
        }

        public static void shutdown() {
            executorService.shutdownNow();
        }
    }

    private RunnableUtil() {
    }
}

package onlinesearch.util;

import debug.Debug;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.SwingWorker;
import main.Str;

public class SwingWorkerUtil {

    private static final int maxNumThreads = Integer.parseInt(Str.get(300));

    public static void execute(SwingWorker<?, ?> parentWorker, Collection<? extends SwingWorker<?, ?>> childWorkers) throws Exception {
        submit(parentWorker, childWorkers, maxNumThreads);
    }

    public static void execute(SwingWorker<?, ?> parentWorker, Collection<? extends SwingWorker<?, ?>> childWorkers, int numThreads) throws Exception {
        submit(parentWorker, childWorkers, (numThreads < 1 || numThreads > maxNumThreads) ? maxNumThreads : numThreads);
    }

    private static void submit(SwingWorker<?, ?> parentWorker, Collection<? extends SwingWorker<?, ?>> childWorkers, int numThreads) throws Exception {
        Collection<Future<?>> futures = new ArrayList<Future<?>>(childWorkers.size());
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        try {
            for (SwingWorker<?, ?> childWorker : childWorkers) {
                futures.add(executorService.submit(childWorker));
            }
            for (Future<?> future : futures) {
                if (parentWorker.isCancelled()) {
                    for (SwingWorker<?, ?> childWorker : childWorkers) {
                        childWorker.cancel(true);
                    }
                    break;
                }
                waitFor(future);
            }
        } catch (Exception e) {
            for (SwingWorker<?, ?> childWorker : childWorkers) {
                childWorker.cancel(true);
            }
            throw e;
        } finally {
            executorService.shutdown();
        }
    }

    public static Object waitFor(Future<?> future) throws Exception {
        try {
            return future.get();
        } catch (InterruptedException e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        } catch (CancellationException e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return null;
    }

    private SwingWorkerUtil() {
    }
}

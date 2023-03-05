package util;

import debug.Debug;
import java.util.concurrent.Callable;

public class Worker extends AbstractWorker<Void> {

  protected void doWork() throws Exception {
  }

  @Override
  protected Void doInBackground() throws Exception {
    doWork();
    return null;
  }

  public static Worker submit(Runnable task) {
    return submit(() -> {
      task.run();
      return null;
    });
  }

  public static Worker submit(Callable<Void> task) {
    Worker worker = new Worker() {
      @Override
      public void doWork() {
        try {
          task.call();
        } catch (Exception e) {
          if (Debug.DEBUG) {
            Debug.print(e);
          }
        }
      }
    };
    worker.execute();
    return worker;
  }
}

package util;

import debug.Debug;

public class Worker extends AbstractWorker<Void> {

  public Worker() {
    this(false);
  }

  public Worker(boolean useFixedThreadPool) {
    super(useFixedThreadPool);
  }

  protected void doWork() throws Exception {
  }

  @Override
  protected Void doInBackground() throws Exception {
    try {
      doWork();
    } catch (Exception e) {
      if (Debug.DEBUG) {
        Debug.print(e);
      }
      throw e;
    }
    return null;
  }

  public static Worker submit(ThrowingRunnable task) {
    Worker worker = new Worker() {
      @Override
      public void doWork() {
        try {
          task.run();
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

package util;

public class Worker extends AbstractWorker<Void> {

    protected void doWork() throws Exception {
    }

    @Override
    protected Void doInBackground() throws Exception {
        doWork();
        return null;
    }
}

package gui;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingWorker;

public abstract class AbstractSwingWorker extends SwingWorker<Object, Object> {

    private final AtomicBoolean isWorkDone = new AtomicBoolean();

    protected void workDone() {
        isWorkDone.set(true);
    }

    public boolean isWorkDone() {
        return isWorkDone.get();
    }
}

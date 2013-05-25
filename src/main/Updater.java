package main;

import debug.Debug;
import gui.AbstractSwingWorker;
import listener.GuiListener;
import util.Connection;
import util.ExceptionUtil;
import util.UpdateException;

class Updater extends AbstractSwingWorker {

    private GuiListener guiListener;
    private boolean silent;

    Updater(GuiListener guiListener, boolean silent) {
        this.guiListener = guiListener;
        this.silent = silent;
    }

    @Override
    protected Object doInBackground() {
        guiListener.updateStarted();
        if (silent) {
            Str.update(false);
            (new AppUpdater()).update();
        } else {
            (new AppUpdater()).update(true);
            Str.update(true);
        }
        guiListener.updateStopped();
        workDone();
        return null;
    }

    static void startUpError(final Exception e) {
        if (e.getClass().equals(UpdateException.class)) {
            return;
        }

        Thread errorNotifier = new Thread() {
            @Override
            public void run() {
                try {
                    Connection.setStatusBar(" Update error: " + ExceptionUtil.toString(e));
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e2) {
                        if (Debug.DEBUG) {
                            Debug.print(e2);
                        }
                    }
                } finally {
                    Connection.unsetStatusBar();
                }
            }
        };
        errorNotifier.setPriority(Thread.MIN_PRIORITY);
        errorNotifier.start();
    }
}

package main;

import listener.GuiListener;
import str.Str;
import util.Worker;

class Updater extends Worker {

    private GuiListener guiListener;
    private boolean silent;

    Updater(GuiListener guiListener, boolean silent) {
        this.guiListener = guiListener;
        this.silent = silent;
    }

    @Override
    protected void doWork() {
        guiListener.updateStarted();
        if (silent) {
            Str.update(false, guiListener);
            (new AppUpdater()).update(guiListener);
        } else {
            (new AppUpdater()).update(true, guiListener);
            Str.update(true, guiListener);
        }
        guiListener.updateStopped();
    }
}

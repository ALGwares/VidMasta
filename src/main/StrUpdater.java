package main;

import debug.Debug;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import listener.GuiListener;
import listener.StrUpdateListener;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.Regex;
import util.UpdateException;

public class StrUpdater implements StrUpdateListener {

    private final Object updateLock;
    private final Collection<UpdateListener> updateListeners;
    private final CountDownLatch updateDoneSignal;
    private volatile String[] strs;

    public StrUpdater() {
        updateLock = new Object();
        updateListeners = new ArrayList<UpdateListener>(2);
        updateDoneSignal = new CountDownLatch(1);
        initStrs();
    }

    private void initStrs() {
        try {
            String[] updateStrs;
            try {
                if ((updateStrs = isValidUpdateFile(IO.read(Constant.APP_DIR + Constant.UPDATE_FILE))) == null) {
                    throw new UpdateException(Constant.UPDATE_FILE + " is invalid");
                }
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                String updateTxt = IO.read(Constant.PROGRAM_DIR + Constant.UPDATE_BACKUP_FILE);
                if ((updateStrs = isValidUpdateFile(updateTxt)) == null) {
                    throw new UpdateException(Constant.UPDATE_BACKUP_FILE + " is invalid");
                } else {
                    IO.write(Constant.APP_DIR + Constant.UPDATE_FILE, updateTxt);
                }
            }

            String[] tempStrs = new String[updateStrs.length - 2];
            for (int i = 0; i < tempStrs.length; i++) {
                tempStrs[i] = updateStrs[i + 1].replace("EMPTY", "").replace("SPACE", " ").replace("TAB", "\t").replace("NEWLINE", Constant.STD_NEWLINE);
            }
            setStrs(tempStrs);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            JOptionPane.showMessageDialog(null, "There was an error initializing the application's search engine: \n" + ExceptionUtil.toString(e),
                    Constant.APP_TITLE, Constant.ERROR_MSG);
            System.exit(-1);
        }
    }

    private static String[] isValidUpdateFile(String update) {
        if (update == null) {
            return null;
        }
        String[] updateStrs = update.split(Constant.NEWLINE);
        return updateStrs.length < 2 || !updateStrs[0].matches("\\d++") || !updateStrs[updateStrs.length - 1].equals("eof") ? null : updateStrs;
    }

    private void setStrs(String[] newStrs) {
        synchronized (updateLock) {
            String[] newestStrs = newStrs;
            for (UpdateListener updateListener : updateListeners) {
                updateListener.update(newestStrs);
            }
            strs = newestStrs;
        }
    }

    @Override
    public String get(int index) {
        return strs[index];
    }

    @Override
    public void update(boolean showConfirmation, GuiListener guiListener) {
        try {
            String newVersionStr = Connection.getUpdateFile(get(465), showConfirmation);
            if (!newVersionStr.matches("\\d++")) {
                return;
            }

            int currVersion = Integer.parseInt(Regex.split(IO.read(Constant.APP_DIR + Constant.UPDATE_FILE), Constant.NEWLINE)[0]);
            int newVersion = Integer.parseInt(newVersionStr);

            if (currVersion < newVersion) {
                String newUpdateTxt = Connection.getUpdateFile(get(294), showConfirmation);
                String[] newUpdateStrs = isValidUpdateFile(newUpdateTxt);
                if (newUpdateStrs == null) {
                    return;
                }

                IO.write(Constant.APP_DIR + Constant.UPDATE_FILE, newUpdateTxt);
                initStrs();
                Regex.initReplacements();
                if (showConfirmation) {
                    guiListener.msg("The application's search engine has been updated to version " + newVersion + ".", Constant.INFO_MSG);
                }
            } else if (showConfirmation) {
                guiListener.msg("The application's search engine (version " + currVersion + ") is already up to date.", Constant.INFO_MSG);
            }
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            if (showConfirmation) {
                guiListener.msg("There was an error updating the application's search engine: " + ExceptionUtil.toString(e), Constant.ERROR_MSG);
            } else {
                Connection.updateError(e);
            }
        } finally {
            updateDoneSignal.countDown();
        }
    }

    @Override
    public void update() {
        synchronized (updateLock) {
            setStrs(Arrays.copyOf(strs, strs.length));
        }
    }

    @Override
    public void addListener(UpdateListener listener) {
        synchronized (updateLock) {
            updateListeners.add(listener);
            update();
        }
    }

    @Override
    public void waitForUpdate() {
        try {
            updateDoneSignal.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }
}

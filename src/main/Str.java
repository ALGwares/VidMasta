package main;

import debug.Debug;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import listener.GuiListener;
import util.Connection;
import util.Constant;
import util.ExceptionUtil;
import util.IO;
import util.Regex;
import util.UpdateException;

public class Str {

    private static GuiListener guiListener;
    private static final Object updateLock;
    private static final Collection<UpdateListener> updateListeners;
    private static final CountDownLatch updateDoneSignal;
    private static volatile String[] strs;

    static {
        updateLock = new Object();
        updateListeners = new ArrayList<UpdateListener>(2);
        updateDoneSignal = new CountDownLatch(1);
        initStrs();
    }

    public static String get(final int index) {
        return strs[index];
    }

    static void update(boolean showConfirmation) {
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
                Updater.startUpError(e);
            }
        } finally {
            updateDoneSignal.countDown();
        }
    }

    private static void initStrs() {
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
                tempStrs[i] = updateStrs[i + 1].replace("EMPTY", "").replace("SPACE", " ").replace("TAB", "\t").replace("NEWLINE", Constant.NEWLINE);
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

    private static void setStrs(String[] newStrs) {
        synchronized (updateLock) {
            String[] newestStrs = newStrs;
            for (UpdateListener updateListener : updateListeners) {
                updateListener.update(newestStrs);
            }
            strs = newestStrs;
        }
    }

    public static void update() {
        synchronized (updateLock) {
            setStrs(Arrays.copyOf(strs, strs.length));
        }
    }

    public static void addListener(UpdateListener listener) {
        synchronized (updateLock) {
            updateListeners.add(listener);
            update();
        }
    }

    private static String[] isValidUpdateFile(String update) {
        if (update == null) {
            return null;
        }
        String[] updateStrs = update.split(Constant.NEWLINE);
        return updateStrs.length < 2 || !updateStrs[0].matches("\\d++") || !updateStrs[updateStrs.length - 1].equals("eof") ? null : updateStrs;
    }

    static void setGuiListener(GuiListener listener) {
        guiListener = listener;
    }

    public static long hashCode(final String str) {
        long hashCode = 0;
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            hashCode = 31 * hashCode + str.charAt(i);
        }
        return hashCode;
    }

    public static String hashCodeStr(final String str) {
        return String.valueOf(hashCode(str));
    }

    public static String htmlToPlainText(String htmlText) {
        final StringBuilder plainText = new StringBuilder(htmlText.length());
        try {
            (new ParserDelegator()).parse(new InputStreamReader(new ByteArrayInputStream(htmlText.getBytes(Constant.UTF8)), Constant.UTF8),
                    new HTMLEditorKit.ParserCallback() {
                        @Override
                        public void handleText(final char[] data, final int pos) {
                            plainText.append(data);
                        }
                    }, true);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            return cleanWeirdChars(htmlText);
        }
        return cleanWeirdChars(plainText.toString());
    }

    public static String clean(String str) {
        String result = str;
        for (Entry<String, String> entry : Regex.badStrs.entrySet()) {
            result = Regex.replaceAll(result, entry.getKey(), entry.getValue());
        }
        return Regex.replaceAll(Regex.replaceAll(htmlToPlainText(result), get(136), get(133)), get(339), get(340)).trim();
    }

    public static String cleanWeirdChars(String str) {
        String result = Regex.replaceAll(Normalizer.normalize(str, Form.NFD), "\\p{InCombiningDiacriticalMarks}+", "");
        for (Entry<String, String> entry : Regex.weirdChars.entrySet()) {
            result = Regex.replaceAll(result, entry.getKey(), entry.getValue());
        }
        return result.trim();
    }

    public static String toFileName(String str) {
        return Regex.replaceAll(clean(str).replace(' ', '+'), "[^\\p{Alnum}\\+]", "");
    }

    public static String capitalize(String str) {
        char[] chars = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                chars[i] = Character.toTitleCase(chars[i]);
                capitalizeNext = false;
            }
        }
        return new String(chars);
    }

    public static void waitForUpdate() {
        try {
            updateDoneSignal.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    public interface UpdateListener {

        void update(String[] strs);
    }

    private Str() {
    }
}

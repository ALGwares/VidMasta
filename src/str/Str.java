package str;

import i18n.I18nStr;
import listener.GuiListener;
import listener.StrUpdateListener;
import listener.StrUpdateListener.UpdateListener;

public class Str extends I18nStr {

    private static StrUpdateListener strUpdateListener;

    public static void init(StrUpdateListener listener) {
        strUpdateListener = listener;
    }

    public static String get(int index) {
        return strUpdateListener.get(index);
    }

    public static void update(boolean showConfirmation, GuiListener guiListener) {
        strUpdateListener.update(showConfirmation, guiListener);
    }

    public static void update() {
        strUpdateListener.update();
    }

    public static void addListener(UpdateListener listener) {
        strUpdateListener.addListener(listener);
    }

    public static void waitForUpdate() {
        strUpdateListener.waitForUpdate();
    }

    public static long hashCode(String str) {
        long hashCode = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            hashCode = 31 * hashCode + str.charAt(i);
        }
        return hashCode;
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

    private Str() {
    }
}

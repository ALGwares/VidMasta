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

  public static void removeListener(UpdateListener listener) {
    strUpdateListener.removeListener(listener);
  }

  public static boolean containsListener(UpdateListener listener) {
    return strUpdateListener.containsListener(listener);
  }

  public static void waitForUpdate() {
    strUpdateListener.waitForUpdate();
  }

  public static long hashCode(String str) {
    long hashCode = 0;
    for (int i = 0, len = str.length(); i < len; i++) {
      hashCode = 31 * hashCode + str.charAt(i);
    }
    return hashCode;
  }

  private Str() {
  }
}

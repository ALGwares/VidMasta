package str;

import i18n.I18nStr;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import listener.GuiListener;
import listener.StrUpdateListener;
import listener.StrUpdateListener.UpdateListener;

public class Str extends I18nStr {

  private static StrUpdateListener strUpdateListener;
  public static final int MAX_SUBDIRECTORIES = 100;

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

  public static String hashPath(String filename) {
    return hashPath(hashCode(filename));
  }

  public static String hashPath(long hashCode) {
    return ((hashCode & 0xfffffff) % MAX_SUBDIRECTORIES) + File.separator + hashCode;
  }

  public static String urlEncode(String str) {
    try {
      return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private Str() {
  }
}

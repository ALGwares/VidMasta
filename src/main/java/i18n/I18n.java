package i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

  private static ResourceBundle bundle;
  private static Locale locale;

  public static synchronized void setLocale(Locale newLocale) {
    Bundle newBundle = new Bundle(newLocale);
    bundle = newBundle.bundle;
    locale = newBundle.locale;
    Locale.setDefault(locale);
  }

  static synchronized Locale locale() {
    return locale;
  }

  static synchronized String str(String key) {
    return bundle.getString(key);
  }

  private I18n() {
  }
}

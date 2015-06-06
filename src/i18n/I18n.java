package i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {

    private static ResourceBundle bundle;
    static volatile Locale locale;

    public static synchronized void setLocale(Locale newLocale) {
        Bundle newBundle = new Bundle(newLocale);
        bundle = newBundle.bundle;
        locale = newBundle.LOCALE;
        Locale.setDefault(locale);
    }

    static synchronized String str(String key) {
        return bundle.getString(key);
    }

    private I18n() {
    }
}

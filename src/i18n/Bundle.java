package i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class Bundle {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    public final ResourceBundle bundle;
    public final Locale LOCALE;

    public Bundle(Locale locale) {
        Locale newLocale = (locale == null ? (DEFAULT_LOCALE != null && "tr".equals(DEFAULT_LOCALE.getLanguage()) ? new Locale("tr", "TR") : new Locale("en",
                "US")) : locale);
        bundle = ResourceBundle.getBundle("i18n.Bundle", newLocale);
        LOCALE = (newLocale = bundle.getLocale()).toString().isEmpty() ? new Locale("en", "US") : newLocale;
    }
}

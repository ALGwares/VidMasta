package i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class Bundle {

    public final ResourceBundle bundle;
    public final Locale LOCALE;

    public Bundle(Locale locale) {
        Locale newLocale = (locale == null ? new Locale("en", "US") : locale);
        bundle = ResourceBundle.getBundle("i18n.Bundle", newLocale);
        LOCALE = (newLocale = bundle.getLocale()).toString().isEmpty() ? new Locale("en", "US") : newLocale;
    }
}

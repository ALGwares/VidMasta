package i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public class Bundle {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    public final ResourceBundle bundle;
    public final Locale LOCALE;

    public Bundle(Locale locale) {
        Locale newLocale;
        if (locale == null) {
            String language, country = null;
            if ((newLocale = DEFAULT_LOCALE) == null) {
                language = "en";
                country = "US";
            } else if ("en".equals(language = newLocale.getLanguage())) {
                country = "US";
            } else if ("es".equals(language)) {
                country = "ES";
            } else if ("fr".equals(language)) {
                country = "FR";
            } else if ("it".equals(language)) {
                country = "IT";
            } else if ("nl".equals(language)) {
                country = "NL";
            } else if ("pt".equals(language)) {
                country = "PT";
            } else if ("tr".equals(language)) {
                country = "TR";
            }
            if (country != null) {
                newLocale = new Locale(language, country);
            }
        } else {
            newLocale = locale;
        }
        bundle = ResourceBundle.getBundle("i18n.Bundle", newLocale);
        LOCALE = (newLocale = bundle.getLocale()).toString().isEmpty() ? new Locale("en", "US") : newLocale;
    }
}

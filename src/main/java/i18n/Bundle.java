package i18n;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class Bundle {

  public static final Locale DEFAULT_LOCALE = Locale.getDefault();
  // The languages besides English: language and country of the locale, name in the language itself, and English name (in alphabetical order). Thai has no
  // country, because Java uses the Buddhist calendar (and so years such as 2569) for th_TH, which would also become the default locale.
  public static final String[][] LANGUAGES = {
    {"sq", "AL", "shqip", "Albanian"},
    {"bg", "BG", "\u0431\u044a\u043b\u0433\u0430\u0440\u0441\u043a\u0438", "Bulgarian"},
    {"ca", "ES", "catal\u00e0", "Catalan"},
    {"hr", "HR", "hrvatski", "Croatian"},
    {"cs", "CZ", "\u010de\u0161tina", "Czech"},
    {"da", "DK", "dansk", "Danish"},
    {"nl", "NL", "Nederlands", "Dutch"},
    {"et", "EE", "eesti", "Estonian"},
    {"tl", "PH", "Filipino", "Filipino"},
    {"fi", "FI", "suomi", "Finnish"},
    {"fr", "FR", "fran\u00e7ais", "French"},
    {"de", "DE", "Deutsch", "German"},
    {"el", "GR", "\u0395\u03bb\u03bb\u03b7\u03bd\u03b9\u03ba\u03ac", "Greek"},
    {"hu", "HU", "magyar", "Hungarian"},
    {"id", "ID", "Bahasa Indonesia", "Indonesian"},
    {"it", "IT", "italiano", "Italian"},
    {"lv", "LV", "latvie\u0161u", "Latvian"},
    {"lt", "LT", "lietuvi\u0173", "Lithuanian"},
    {"mk", "MK", "\u043c\u0430\u043a\u0435\u0434\u043e\u043d\u0441\u043a\u0438", "Macedonian"},
    {"ms", "MY", "Bahasa Melayu", "Malay"},
    {"nb", "NO", "norsk bokm\u00e5l", "Norwegian"},
    {"pl", "PL", "polski", "Polish"},
    {"pt", "BR", "portugu\u00eas (Brasil)", "Portuguese (Brazil)"},
    {"pt", "PT", "portugu\u00eas (Portugal)", "Portuguese (Portugal)"},
    {"ro", "RO", "rom\u00e2n\u0103", "Romanian"},
    {"ru", "RU", "\u0440\u0443\u0441\u0441\u043a\u0438\u0439", "Russian"},
    {"sr", "RS", "\u0441\u0440\u043f\u0441\u043a\u0438", "Serbian"},
    {"sk", "SK", "sloven\u010dina", "Slovak"},
    {"sl", "SI", "sloven\u0161\u010dina", "Slovenian"},
    {"es", "MX", "espa\u00f1ol (Latinoam\u00e9rica)", "Spanish (Latin America)"},
    {"es", "ES", "espa\u00f1ol (Espa\u00f1a)", "Spanish (Spain)"},
    {"sv", "SE", "svenska", "Swedish"},
    {"th", "", "\u0e44\u0e17\u0e22", "Thai"},
    {"tr", "TR", "T\u00fcrk\u00e7e", "Turkish"},
    {"uk", "UA", "\u0443\u043a\u0440\u0430\u0457\u043d\u0441\u044c\u043a\u0430", "Ukrainian"},
    {"vi", "VN", "Ti\u1ebfng Vi\u1ec7t", "Vietnamese"}};
  private static final Locale ENGLISH = new Locale("en", "US");
  // Where Spanish is that of Latin America rather than that of Spain (419 is the code of Latin America itself)
  private static final Set<String> LATIN_AMERICA = new HashSet<String>(Arrays.asList("419", "AR", "BO", "BR", "BZ", "CL", "CO", "CR", "CU", "DO", "EC", "GT",
          "HN", "MX", "NI", "PA", "PE", "PR", "PY", "SV", "US", "UY", "VE"));
  public final ResourceBundle bundle;
  public final Locale locale;

  public Bundle(Locale locale) { // Null means the system's language
    bundle = ResourceBundle.getBundle("i18n.Bundle", supportedLocale(locale == null ? DEFAULT_LOCALE : locale));
    Locale newLocale = bundle.getLocale();
    this.locale = newLocale.toString().isEmpty() ? ENGLISH : newLocale;
  }

  // The locale of VidMasta's languages for a locale such as the system's: that of its language, and for the two languages with two locales, that of the variety
  // that its country uses. English if VidMasta doesn't have the language.
  static Locale supportedLocale(Locale locale) {
    if (locale == null) {
      return ENGLISH;
    }
    String language = locale.getLanguage(), country = locale.getCountry();
    if (language.equals("no") || language.equals("nn")) { // Java's names of the Norwegian languages on Norwegian Windows (Bokmal) and of Nynorsk
      language = "nb";
    } else if (language.equals("fil")) { // Windows' name of Filipino
      language = "tl";
    } else if (language.equals("es")) {
      country = LATIN_AMERICA.contains(country) ? "MX" : "ES";
    } else if (language.equals("pt")) {
      country = country.isEmpty() || country.equals("BR") ? "BR" : "PT";
    }
    Locale result = null;
    for (String[] supportedLanguage : LANGUAGES) {
      Locale supportedLocale = new Locale(supportedLanguage[0], supportedLanguage[1]); // Names Indonesian as this Java version does (in before Java 17, then id)
      if (supportedLocale.getLanguage().equals(language)) {
        if (supportedLocale.getCountry().equals(country)) {
          return supportedLocale;
        }
        if (result == null) {
          result = supportedLocale;
        }
      }
    }
    return result == null ? ENGLISH : result;
  }
}

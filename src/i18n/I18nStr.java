package i18n;

import java.awt.event.KeyEvent;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class I18nStr {

    private static volatile String ctrl = "ctrl+";

    public static void localeChanged() {
        ctrl = KeyEvent.getKeyModifiersText(KeyEvent.CTRL_MASK).toLowerCase(locale()) + '+';
    }

    public static Locale locale() {
        return I18n.locale();
    }

    public static String str(String key) {
        return I18n.str(key);
    }

    public static String[] strs(String key) {
        return str(key).split(",");
    }

    public static String str(String key, Object... replacements) {
        return replace(str(key), replacements);
    }

    private static String replace(String str, Object... replacements) {
        String result = str;
        for (Object replacement : replacements) {
            result = result.replaceFirst("999", Matcher.quoteReplacement(replacement.toString()));
        }
        return result;
    }

    public static String ctrlStr(String key) {
        return str(key).replace("CTRL+", ctrl);
    }

    public static String htmlLinkStr(String key, String url, Object... replacements) {
        String str = str(key), linkName = str.substring(str.indexOf("<a>") + 3, str.indexOf("</a>"));
        return replace(str.replaceFirst(Pattern.quote("<a>" + linkName + "</a>"), Matcher.quoteReplacement("<a href=\"" + url + "\">" + linkName + "</a>")),
                replacements);
    }

    public static String percent(double ratio, int numFractionDigits) {
        return getPercentFormat(numFractionDigits).format(ratio);
    }

    public static NumberFormat getPercentFormat(int numFractionDigits) {
        NumberFormat percentFormat = NumberFormat.getPercentInstance(locale());
        percentFormat.setMinimumFractionDigits(numFractionDigits);
        percentFormat.setMaximumFractionDigits(numFractionDigits);
        percentFormat.setRoundingMode(RoundingMode.DOWN);
        return percentFormat;
    }

    public static NumberFormat getNumFormat() {
        return NumberFormat.getNumberInstance(locale());
    }

    public static NumberFormat getNumFormat(String pattern) {
        DecimalFormat decimalFormat = (DecimalFormat) getNumFormat();
        decimalFormat.applyPattern(pattern);
        return decimalFormat;
    }

    protected I18nStr() {
    }
}

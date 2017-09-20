package util;

import debug.Debug;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import str.Str;

public class Regex {

    private static final Map<String, Pattern> cache;
    public static final Map<String, String> languages, countries, subtitleLanguages;
    private static volatile Map<String, String> weirdCharReplacements, badStrReplacements;

    static {
        cache = new ConcurrentHashMap<String, Pattern>(800, 0.75f, 30);
        initReplacements();
        (languages = new TreeMap<String, String>()).put(Constant.ANY, Constant.ANY);
        init(languages, 234);
        (countries = new TreeMap<String, String>()).put(Constant.ANY, Constant.ANY);
        init(countries, 231);
        init(subtitleLanguages = new TreeMap<String, String>(), 420, Constant.SEPARATOR2, Constant.SEPARATOR1);
    }

    public static void initReplacements() {
        Map<String, String> tempWeirdCharReplacements = new TreeMap<String, String>();
        init(tempWeirdCharReplacements, 552);
        weirdCharReplacements = tempWeirdCharReplacements;
        Map<String, String> tempBadStrReplacements = new TreeMap<String, String>();
        tempBadStrReplacements.put("&(?i)tilde;", "~");
        tempBadStrReplacements.put("&(?i)nbsp;", " ");
        tempBadStrReplacements.put(":", " ");
        tempBadStrReplacements.put(Str.get(224), Str.get(225));
        tempBadStrReplacements.put(Str.get(226), Str.get(227));
        init(tempBadStrReplacements, 228);
        badStrReplacements = tempBadStrReplacements;
    }

    private static void init(Map<String, String> map, int strIndex) {
        init(map, strIndex, Str.get(strIndex + 1), Str.get(strIndex + 2));
    }

    private static void init(Map<String, String> map, int strIndex, String splitStr1, String splitStr2) {
        for (String currKeyVal : split(strIndex, splitStr1)) {
            String[] keyVal = split(currKeyVal, splitStr2);
            map.put(keyVal[0], keyVal[1]);
        }
    }

    public static String[] split(int inputIndex, String regex) {
        return split(Str.get(inputIndex), regex);
    }

    public static String[] split(String input, int regexIndex) {
        return split(input, Str.get(regexIndex));
    }

    public static String[] split(String input, String regex) {
        return pattern(regex).split(input);
    }

    public static String replaceFirst(String input, int regexIndex) {
        return replaceFirst(input, Str.get(regexIndex), Str.get(regexIndex + 1));
    }

    public static String replaceFirst(String input, String regex, String replacement) {
        return matcher(regex, input).replaceFirst(replacement);
    }

    public static String replaceAll(String input, int regexIndex) {
        return replaceAll(input, Str.get(regexIndex), Str.get(regexIndex + 1));
    }

    public static String replaceAll(String input, String regex, String replacement) {
        return matcher(regex, input).replaceAll(replacement);
    }

    public static String replaceAllRepeatedly(String input, int regexesIndex) {
        String[] regexes = split(regexesIndex, Constant.SEPARATOR1), replacements = split(regexesIndex + 1, Constant.SEPARATOR1);
        if (regexes.length != replacements.length) {
            return input;
        }

        String result = input;
        for (int i = 0; i < regexes.length; i++) {
            result = replaceAll(result, regexes[i], replacements[i]);
        }
        return result;
    }

    public static String htmlToPlainText(String htmlText) {
        final StringBuilder plainText = new StringBuilder(htmlText.length());
        try {
            (new ParserDelegator()).parse(new InputStreamReader(new ByteArrayInputStream(htmlText.getBytes(Constant.UTF8)), Constant.UTF8),
                    new HTMLEditorKit.ParserCallback() {
                        @Override
                        public void handleText(char[] data, int pos) {
                            plainText.append(data);
                        }
                    }, true);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
            return cleanWeirdChars(htmlText);
        }
        return cleanWeirdChars(plainText.toString());
    }

    public static String clean(String str) {
        return clean(str, true);
    }

    public static String clean(String str, boolean useNonHtmlEntityBadStrs) {
        String result = str;
        for (Entry<String, String> entry : badStrReplacements.entrySet()) {
            String badStr = entry.getKey();
            if (useNonHtmlEntityBadStrs || badStr.charAt(0) == '&') {
                result = replaceAll(result, badStr, entry.getValue());
            }
        }
        return replaceAll(replaceAll(htmlToPlainText(result), Str.get(136), Str.get(133)), 339).trim();
    }

    public static String cleanWeirdChars(String str) {
        String result = replaceAll(Normalizer.normalize(str, Form.NFD), "\\p{InCombiningDiacriticalMarks}+", "");
        for (Entry<String, String> entry : weirdCharReplacements.entrySet()) {
            result = replaceAll(result, entry.getKey(), entry.getValue());
        }
        return result.trim();
    }

    public static String toFileName(String str) {
        return replaceAll(replaceAll(clean(str), "[^\\p{Alnum}]", " ").trim(), " ++", "-");
    }

    public static boolean isMatch(String input, int regexIndex) {
        return isMatch(input, Str.get(regexIndex));
    }

    public static boolean isMatch(String input, String regex) {
        return matcher(regex, input).matches();
    }

    public static String firstMatch(String input, int regexIndex) {
        return firstMatch(input, Str.get(regexIndex));
    }

    public static String firstMatch(String input, String regex) {
        Matcher matcher = matcher(regex, input);
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                return matcher.group().trim();
            }
        }

        return "";
    }

    public static List<String> allMatches(String input, int regexIndex) {
        List<String> result = new ArrayList<String>(8);

        Matcher matcher = matcher(regexIndex, input);
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                result.add(matcher.group().trim());
            }
        }

        return result;
    }

    public static List<String> matches(String input, int startRegexIndex) {
        return matches(input, Str.get(startRegexIndex), Str.get(startRegexIndex + 1));
    }

    public static List<String> matches(String input, String startRegex, String endRegex) {
        List<String> result = new ArrayList<String>(8);

        Matcher startMatcher = matcher(startRegex, input);
        while (!startMatcher.hitEnd()) {
            if (!startMatcher.find()) {
                continue;
            }

            int startRegexEnd = startMatcher.end(), endIndex = -1;

            Matcher endMatcher = matcher(endRegex, input).region(startRegexEnd, input.length());
            while (!endMatcher.hitEnd()) {
                if (endMatcher.find()) {
                    endIndex = endMatcher.start();
                    break;
                }
            }

            if (endIndex == -1) {
                continue;
            }

            result.add(input.substring(startRegexEnd, endIndex).trim());
        }

        return result;
    }

    public static String match(String input, int startRegexIndex) {
        return match(input, Str.get(startRegexIndex), Str.get(startRegexIndex + 1));
    }

    public static String match(String input, String startRegex, String endRegex) {
        Matcher startMatcher = matcher(startRegex, input);
        while (!startMatcher.hitEnd()) {
            if (!startMatcher.find()) {
                continue;
            }

            int startRegexEnd = startMatcher.end(), endIndex = -1;

            Matcher endMatcher = matcher(endRegex, input).region(startRegexEnd, input.length());
            while (!endMatcher.hitEnd()) {
                if (endMatcher.find()) {
                    endIndex = endMatcher.start();
                    break;
                }
            }

            if (endIndex == -1) {
                return "";
            }

            return input.substring(startRegexEnd, endIndex).trim();
        }

        return "";
    }

    public static Matcher matcher(int regexIndex, String input) {
        return matcher(Str.get(regexIndex), input);
    }

    public static Matcher matcher(String regex, String input) {
        return pattern(regex).matcher(input);
    }

    public static Pattern pattern(int regexIndex) {
        return pattern(Str.get(regexIndex));
    }

    public static Pattern pattern(String regex) {
        Pattern pattern = cache.get(regex);
        if (pattern == null) {
            cache.put(regex, pattern = Pattern.compile(regex)); // Not a concurrency bug
        }
        return pattern;
    }

    private Regex() {
    }
}

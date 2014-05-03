package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import main.Str;

public class Regex {

    private static final Map<String, Pattern> cache;
    public static final Map<String, String> weirdChars, badStrs, languages, countries, subtitleLanguages;
    public static final FileFilter torrentFileFilter, proxyListFileFilter, subtitleFileFilter;

    static {
        int initialCapacity = Integer.parseInt(Str.get(164));
        cache = new ConcurrentHashMap<String, Pattern>(initialCapacity * 24, .75f, initialCapacity);
        init(weirdChars = new TreeMap<String, String>(), 552);
        (badStrs = new TreeMap<String, String>()).put("&(?i)tilde;", "~");
        badStrs.put("&(?i)nbsp;", " ");
        badStrs.put(":", " ");
        badStrs.put(Str.get(224), Str.get(225));
        badStrs.put(Str.get(226), Str.get(227));
        init(badStrs, 228);
        (languages = new TreeMap<String, String>()).put(Constant.ANY_LANGUAGE, Constant.ANY_LANGUAGE);
        init(languages, 234);
        (countries = new TreeMap<String, String>()).put(Constant.ANY_COUNTRY, Constant.ANY_COUNTRY);
        init(countries, 231);
        init(subtitleLanguages = new TreeMap<String, String>(), 420, Constant.SEPARATOR2, Constant.SEPARATOR1);
        torrentFileFilter = new FileNameExtensionFilter("Torrents (*.torrent)", "torrent");
        proxyListFileFilter = new FileNameExtensionFilter("Proxy List (*" + Constant.TXT + ")", "txt");
        subtitleFileFilter = new FileNameExtensionFilter("Subtitle (" + Str.get(451) + ")", split(Str.get(452), ","));
    }

    private static void init(Map<String, String> map, int strIndex) {
        init(map, strIndex, Str.get(strIndex + 1), Str.get(strIndex + 2));
    }

    private static void init(Map<String, String> map, int strIndex, String splitStr1, String splitStr2) {
        for (String currKeyVal : split(Str.get(strIndex), splitStr1)) {
            String[] keyVal = split(currKeyVal, splitStr2);
            map.put(keyVal[0], keyVal[1]);
        }
    }

    public static String[] split(String input, String regex) {
        return pattern(regex).split(input);
    }

    public static List<String> split(String input, String splitCharClassRegex, int maxPartLen) {
        List<String> parts = new ArrayList<String>((input.length() / maxPartLen) + 2);
        split(input, splitCharClassRegex, maxPartLen, parts);
        return parts;
    }

    private static void split(String input, String splitCharClassRegex, int maxPartLen, List<String> parts) {
        int len = input.length();
        if (len <= maxPartLen) {
            parts.add(input);
            return;
        }

        for (int i = 0, j = maxPartLen; i < len; i++) {
            if (i == maxPartLen) {
                parts.add(input.substring(0, j).trim());
                split(input.substring(j), splitCharClassRegex, maxPartLen, parts);
                return;
            }
            if (isMatch(String.valueOf(input.charAt(i)), splitCharClassRegex)) {
                j = i + 1;
            }
        }
    }

    public static String replaceFirst(String input, String regex, String replacement) {
        return matcher(regex, input).replaceFirst(replacement);
    }

    public static String replaceAll(String input, String regex, String replacement) {
        return matcher(regex, input).replaceAll(replacement);
    }

    public static boolean isMatch(String input, String regex) {
        return matcher(regex, input).matches();
    }

    public static String match(String input, String regex) {
        Matcher matcher = matcher(regex, input);
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                return matcher.group().trim();
            }
        }

        return "";
    }

    public static List<String> matches(String input, String regex) {
        List<String> result = new ArrayList<String>(8);

        Matcher matcher = matcher(regex, input);
        while (!matcher.hitEnd()) {
            if (matcher.find()) {
                result.add(matcher.group().trim());
            }
        }

        return result;
    }

    public static List<String> matches(String input, String startRegex, String endRegex) {
        List<String> result = new ArrayList<String>(8);

        Matcher startMatcher = matcher(startRegex, input);
        while (!startMatcher.hitEnd()) {
            if (!startMatcher.find()) {
                continue;
            }

            int startRegexEnd = startMatcher.end(), endIndex = -1;

            Matcher endMatcher = matcher(endRegex, input.substring(startRegexEnd));
            while (!endMatcher.hitEnd()) {
                if (endMatcher.find()) {
                    endIndex = endMatcher.start();
                    break;
                }
            }

            if (endIndex == -1) {
                continue;
            }

            result.add(input.substring(startRegexEnd, endIndex + startRegexEnd).trim());
        }

        return result;
    }

    public static String match(String input, String startRegex, String endRegex) {
        Matcher startMatcher = matcher(startRegex, input);
        while (!startMatcher.hitEnd()) {
            if (!startMatcher.find()) {
                continue;
            }

            int startRegexEnd = startMatcher.end(), endIndex = -1;

            Matcher endMatcher = matcher(endRegex, input.substring(startRegexEnd));
            while (!endMatcher.hitEnd()) {
                if (endMatcher.find()) {
                    endIndex = endMatcher.start();
                    break;
                }
            }

            if (endIndex == -1) {
                return "";
            }

            return input.substring(startRegexEnd, endIndex + startRegexEnd).trim();
        }

        return "";
    }

    public static Matcher matcher(String regex, CharSequence input) {
        return pattern(regex).matcher(input);
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

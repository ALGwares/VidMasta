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

    private static final int initialCapacity = Integer.parseInt(Str.get(164));
    private static final Map<String, Pattern> cache = new ConcurrentHashMap<String, Pattern>(initialCapacity * 3, .75f, initialCapacity);
    public static final Map<String, String> weirdChars = new TreeMap<String, String>(), badStrs = new TreeMap<String, String>();
    public static final Map<String, String> languages = new TreeMap<String, String>(), countries = new TreeMap<String, String>();
    public static final Map<String, String> subtitleLanguages = new TreeMap<String, String>();
    public static final FileFilter torrentFileFilter = new FileNameExtensionFilter("Torrents (*.torrent)", "torrent");
    public static final FileFilter proxyListFileFilter = new FileNameExtensionFilter("Proxy List (*" + Constant.TXT + ")", "txt");
    public static final FileFilter subtitleFileFilter = new FileNameExtensionFilter("Subtitle (" + Str.get(451) + ")", Str.get(452).split(","));

    static {
        String[] keysVals = Str.get(552).split(Str.get(553));
        for (String currKeyVal : keysVals) {
            String[] keyVal = currKeyVal.split(Str.get(554));
            weirdChars.put(keyVal[0], keyVal[1]);
        }

        badStrs.put("&(?i)tilde;", "~");
        badStrs.put("&(?i)nbsp;", " ");
        badStrs.put(":", " ");
        badStrs.put(Str.get(224), Str.get(225));
        badStrs.put(Str.get(226), Str.get(227));
        keysVals = Str.get(228).split(Str.get(229));
        for (String currKeyVal : keysVals) {
            String[] keyVal = currKeyVal.split(Str.get(230));
            badStrs.put(keyVal[0], keyVal[1]);
        }

        countries.put(Constant.ANY_COUNTRY, Constant.ANY_COUNTRY);
        keysVals = Str.get(231).split(Str.get(232));
        for (String currKeyVal : keysVals) {
            String[] keyVal = currKeyVal.split(Str.get(233));
            countries.put(keyVal[0], keyVal[1]);
        }

        languages.put(Constant.ANY_LANGUAGE, Constant.ANY_LANGUAGE);
        keysVals = Str.get(234).split(Str.get(235));
        for (String currKeyVal : keysVals) {
            String[] keyVal = currKeyVal.split(Str.get(236));
            languages.put(keyVal[0], keyVal[1]);
        }

        keysVals = Str.get(420).split(Constant.SEPARATOR2);
        for (String currKeyVal : keysVals) {
            String[] keyVal = currKeyVal.split(Constant.SEPARATOR1);
            subtitleLanguages.put(keyVal[0], keyVal[1]);
        }
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
            if (startMatcher.find()) {
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
        }

        return result;
    }

    public static String match(String input, String startRegex, String endRegex) {
        Matcher startMatcher = matcher(startRegex, input);
        while (!startMatcher.hitEnd()) {
            if (startMatcher.find()) {
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
        }

        return "";
    }

    public static Matcher matcher(String regex, CharSequence input) {
        return pattern(regex).matcher(input);
    }

    public static Pattern pattern(String regex) {
        Pattern pattern = cache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            cache.put(regex, pattern); // Not a concurrency bug
        }
        return pattern;
    }

    private Regex() {
    }
}

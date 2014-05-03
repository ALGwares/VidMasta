package search.util;

import debug.Debug;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import main.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;

public class VideoSearch {

    private static final Random rand = new Random();
    private static final int NUM_SEARCH_ENGINES;
    private static final List<String> searchEngines;

    static {
        NUM_SEARCH_ENGINES = Integer.parseInt(Str.get(622));
        searchEngines = new ArrayList<String>(NUM_SEARCH_ENGINES);
        for (int i = 0; i < NUM_SEARCH_ENGINES; i++) {
            searchEngines.add(Str.get(i));
        }
    }

    public static boolean isImdbVideoType(String sourceCode, boolean isTVShow) {
        return isImdbVideoType(sourceCode, isTVShow ? 586 : 587);
    }

    public static boolean isImdbVideoType(String sourceCode, int typeRegexIndex) {
        return Regex.isMatch(Regex.match(sourceCode, Str.get(584), Str.get(585)), Str.get(typeRegexIndex));
    }

    public static String searchEngineQuery(String query, int regexIndex) throws Exception {
        String encodedQuery = URLEncoder.encode(query, Constant.UTF8);
        List<String> engines = new ArrayList<String>(searchEngines);

        for (int i = NUM_SEARCH_ENGINES; i > 0; i--) {
            String searchEngine = engines.get(rand.nextInt(i));
            try {
                String result = Regex.match(Connection.getSourceCode(searchEngine + encodedQuery, Connection.SEARCH_ENGINE), Str.get(regexIndex));
                if (!result.isEmpty()) {
                    result = URLDecoder.decode(result, Constant.UTF8);
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (ConnectionException e) {
                if (i == 1) {
                    throw e;
                }
                if (Debug.DEBUG) {
                    Debug.println("Retrying search query: " + e.URL);
                }
            }
            engines.remove(searchEngine);
        }

        return null;
    }

    public static String getTitleLink(String title, String year) throws Exception {
        String link = searchEngineQuery(Str.clean(title) + (year.isEmpty() ? "" : ' ' + year) + Str.get(76), 619);
        return link == null ? link : Str.get(96) + link;
    }

    public static String[] getImdbTitleParts(String sourceCode) {
        return getImdbTitleParts(sourceCode, 98);
    }

    public static String[] getImdbTitleParts(String sourceCode, int startRegexIndex) {
        String title = Regex.match(sourceCode, Str.get(startRegexIndex), Str.get(startRegexIndex + 1));
        Pattern yearPattern = Regex.pattern(Str.get(100));
        String[] result = new String[2];
        result[1] = "";
        int titleEndIndex = -1;

        for (int i = title.length() - 1; i > -1 && titleEndIndex == -1; i--) {
            Matcher yearMatcher = yearPattern.matcher(title.substring(i));
            while (!yearMatcher.hitEnd()) {
                if (yearMatcher.find()) {
                    result[1] = yearMatcher.group();
                    titleEndIndex = yearMatcher.start() + i;
                    break;
                }
            }
        }

        if (titleEndIndex == -1) {
            title = Regex.replaceAll(title, Str.get(101), Str.get(102));
            titleEndIndex = title.length();
        }

        result[0] = title.substring(0, titleEndIndex).trim();
        result[1] = Regex.match(result[1], Str.get(135));

        return result;
    }

    public static String[] getTitleParts(String title, boolean isTVShow) {
        String titleName = Regex.replaceAll(title, Str.get(103), Str.get(104)), year = "", season = null, episode = null;
        Collection<Integer> indexes = new ArrayList<Integer>(5);
        indexes.add(titleName.length());

        Matcher typeMatcher = Regex.matcher(Str.get(105), titleName);
        while (!typeMatcher.hitEnd()) {
            if (typeMatcher.find()) {
                if (Debug.DEBUG) {
                    Debug.print('\'' + typeMatcher.group() + "' ");
                }
                indexes.add(typeMatcher.start());
                break;
            }
        }

        Matcher yearMatcher = Regex.matcher(Str.get(106), titleName);
        while (!yearMatcher.hitEnd()) {
            if (yearMatcher.find()) {
                indexes.add(yearMatcher.start());
                year = yearMatcher.group().trim();
                break;
            }
        }

        if (isTVShow) {
            Matcher tvBoxSetAndEpisodeMatcher = Regex.matcher(Str.get(107), titleName);
            while (!tvBoxSetAndEpisodeMatcher.hitEnd()) {
                if (!tvBoxSetAndEpisodeMatcher.find()) {
                    continue;
                }

                if (Debug.DEBUG) {
                    Debug.print("TV BoxSet/S&E: '" + tvBoxSetAndEpisodeMatcher.group() + "' ");
                }
                if (Regex.isMatch(tvBoxSetAndEpisodeMatcher.group(), Str.get(108))) {
                    String[] seasonAndEpisode = Regex.split(Regex.replaceFirst(tvBoxSetAndEpisodeMatcher.group().trim(), Str.get(109), Str.get(110)),
                            Str.get(111));
                    int seasonNum = Integer.parseInt(seasonAndEpisode[0]);
                    if (seasonNum >= 1 && seasonNum <= 100) {
                        season = String.format(Constant.TV_EPISODE_FORMAT, seasonNum);
                    }
                    int episodeNum = Integer.parseInt(seasonAndEpisode[1]);
                    if (episodeNum >= 0 && episodeNum <= 300) {
                        episode = String.format(Constant.TV_EPISODE_FORMAT, episodeNum);
                    }
                }
                indexes.add(tvBoxSetAndEpisodeMatcher.start());
                break;
            }
        } else {
            Matcher movieBoxSetMatcher = Regex.matcher(Str.get(239), titleName);
            while (!movieBoxSetMatcher.hitEnd()) {
                if (movieBoxSetMatcher.find()) {
                    if (Debug.DEBUG) {
                        Debug.print("Movie BoxSet: '" + movieBoxSetMatcher.group() + "' ");
                    }
                    indexes.add(movieBoxSetMatcher.start());
                    break;
                }
            }
        }

        return new String[]{titleName.substring(0, Collections.min(indexes)).trim(), year, season, episode};
    }

    private VideoSearch() {
    }
}

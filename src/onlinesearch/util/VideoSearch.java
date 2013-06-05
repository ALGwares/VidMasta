package onlinesearch.util;

import debug.Debug;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String[] searchEngines = {Str.get(0), Str.get(1), Str.get(2)};
    private static final int MAX_NUM_SEARCH_ENGINES = Integer.parseInt(Str.get(94));

    public static boolean isImdbVideoType(String sourceCode, boolean isTVShow) {
        String titleTagStr = Regex.match(sourceCode, Str.get(352), Str.get(353));
        Pattern videoTypePattern = Regex.pattern(Str.get(isTVShow ? 354 : 355));
        int lastIndex = titleTagStr.length() - 1;

        for (int i = lastIndex; i > -1; i--) {
            Matcher videoTypeMatcher = videoTypePattern.matcher(titleTagStr.substring(i));
            while (!videoTypeMatcher.hitEnd()) {
                if (videoTypeMatcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String searchEngineQuery(String query) throws Exception {
        String searchEngine = searchEngines[rand.nextInt(MAX_NUM_SEARCH_ENGINES)], encodedQuery = URLEncoder.encode(query, Constant.UTF8);
        try {
            return Connection.getSourceCode(searchEngine + encodedQuery, Connection.SEARCH_ENGINE);
        } catch (ConnectionException e) {
            if (Debug.DEBUG) {
                Debug.println("Retrying search query: " + e.url);
            }

            if (MAX_NUM_SEARCH_ENGINES != 1) {
                List<String> engines = new ArrayList<String>(Arrays.asList(searchEngines));
                engines.remove(searchEngine);
                searchEngine = engines.get(rand.nextInt(MAX_NUM_SEARCH_ENGINES - 1));
            }

            return Connection.getSourceCode(searchEngine + encodedQuery, Connection.SEARCH_ENGINE);
        }
    }

    public static String getTitleLink(String title, String year) throws Exception {
        String source = searchEngineQuery(Str.clean(title) + (year.isEmpty() ? "" : ' ' + year) + Str.get(76));

        Matcher imdbTitleMatcher = Regex.matcher(Str.get(95), source);
        while (!imdbTitleMatcher.hitEnd()) {
            if (imdbTitleMatcher.find()) {
                return Str.get(96) + Regex.match(source.substring(imdbTitleMatcher.start()), Str.get(97));
            }
        }

        return null;
    }

    public static String[] getImdbTitleParts(String sourceCode) {
        String titleTagStr = Regex.match(sourceCode, Str.get(98), Str.get(99));
        Pattern yearPattern = Regex.pattern(Str.get(100));

        String[] result = new String[2];
        result[1] = "";
        int titleEndIndex = -1;

        int lastIndex = titleTagStr.length() - 1;
        for (int i = lastIndex; i > -1 && titleEndIndex == -1; i--) {
            Matcher yearMatcher = yearPattern.matcher(titleTagStr.substring(i));
            while (!yearMatcher.hitEnd()) {
                if (yearMatcher.find()) {
                    result[1] = yearMatcher.group();
                    titleEndIndex = yearMatcher.start() + i;
                    break;
                }
            }
        }

        if (titleEndIndex == -1) {
            titleTagStr = Regex.replaceAll(titleTagStr, Str.get(101), Str.get(102));
            titleEndIndex = titleTagStr.length();
        }

        result[0] = titleTagStr.substring(0, titleEndIndex).trim();
        result[1] = Regex.match(result[1], Str.get(135));

        return result;
    }

    public static String[] getTitleParts(String title, boolean isTVShow) {
        String titleName = Regex.replaceAll(title, Str.get(103), Str.get(104));
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

        String year = "";

        Matcher yearMatcher = Regex.matcher(Str.get(106), titleName);
        while (!yearMatcher.hitEnd()) {
            if (yearMatcher.find()) {
                indexes.add(yearMatcher.start());
                year = yearMatcher.group().trim();
                break;
            }
        }

        String episode = null;
        String season = null;
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

        String[] titleParts = new String[5];
        titleParts[0] = titleName.substring(0, Collections.min(indexes)).trim();
        titleParts[1] = year;

        if (isTVShow && season != null && episode != null) {
            titleParts[2] = season;
            titleParts[3] = episode;
        } else {
            titleParts[2] = null;
            titleParts[3] = null;
        }
        if (Debug.DEBUG) {
            Debug.println('\'' + titleParts[0] + "' '" + titleParts[1] + "' '" + titleParts[2] + "' '" + titleParts[3] + "' '" + title + '\'');
        }
        return titleParts;
    }

    private VideoSearch() {
    }
}

package search.util;

import debug.Debug;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;

public class VideoSearch {

    private static final long MAX_IMAGE_AGE = Long.parseLong(Str.get(501));
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
                String result = Regex.match(Connection.getSourceCode(searchEngine + encodedQuery, DomainType.SEARCH_ENGINE), Str.get(regexIndex));
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
        String link = searchEngineQuery(Regex.clean(title) + (year.isEmpty() ? "" : ' ' + year) + Str.get(76), 619);
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
        String titleName = Regex.replaceAll(title, Str.get(103), Str.get(104)), year = "", season = "", episode = "";
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
                    Debug.print("TV BoxSet/S&E/E: '" + tvBoxSetAndEpisodeMatcher.group() + "' ");
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

    public static boolean isRightFormat(String titleName, String format) {
        if (format.equals(Constant.ANY)) {
            return true;
        }

        String title = Regex.replaceAll(titleName, Str.get(77), Str.get(78));
        if (format.equals(Constant.HQ)) {
            return hasType(title, 569);
        }
        if (format.equals(Constant.DVD)) {
            return hasType(title, 79);
        }
        if (format.equals(Constant.HD720)) {
            return hasType(title, 600) && !hasType(title, 82);
        }
        return hasType(title, 83);
    }

    private static boolean hasType(String title, int typeRegexIndex) {
        return !Regex.match(title, Str.get(typeRegexIndex)).isEmpty();
    }

    public static String rating(String rating) {
        return rating.isEmpty() ? "-" : rating;
    }

    public static String getSummary(String sourceCode, boolean isTVShow) {
        String infoBar = Regex.match(sourceCode, Str.get(137), Str.get(138));
        List<String> genresArr = Regex.matches(infoBar, Str.get(139), Str.get(140));

        StringBuilder genresStr = new StringBuilder(128);
        int numGenres = genresArr.size();
        for (int i = 0; i < numGenres; i++) {
            genresStr.append(genresArr.get(i));
            if (i < numGenres - 1) {
                genresStr.append(", ");
            }
        }

        String br1 = "<br>", br2 = br1 + br1;
        StringBuilder summary = new StringBuilder(2048);
        if (numGenres != 0) {
            summary.append("<b>Genre: </b>").append(genresStr).append(br2);
        }

        String summary1 = Regex.match(sourceCode, Str.get(129), Str.get(130));
        summary1 = Regex.replaceAll(summary1, Str.get(203), Str.get(204)).trim();
        summary1 = Regex.replaceAll(Regex.replaceAll(summary1, Str.get(241), Str.get(242)), Str.get(243), Str.get(244));
        String summary2 = Regex.match(sourceCode, Str.get(131), Str.get(132));
        summary2 = Regex.replaceAll(summary2, Str.get(205), Str.get(206)).trim();
        summary2 = Regex.replaceAll(Regex.replaceAll(summary2, Str.get(245), Str.get(246)), Str.get(247), Str.get(248));
        String storyline = null;
        boolean isEmpty1 = summary1.isEmpty(), isEmpty2 = summary2.isEmpty();

        if ((!isEmpty1 && !isEmpty2) && summary1.equals(summary2)) {
            summary.append(summary1);
        } else if (isEmpty1 && !isEmpty2) {
            summary.append(summary2);
        } else if (!isEmpty1 && isEmpty2) {
            summary.append(summary1);
        } else if (!isEmpty1) {
            summary.append(summary1);
            storyline = br2 + "<font size=\"5\"><b>Storyline:</b></font>" + br1 + summary2;
        } else {
            summary.append(Str.get(159));
        }

        List<StringBuilder> nameLists = new ArrayList<StringBuilder>(4);
        getNames(Regex.match(sourceCode, Str.get(560), Str.get(561)), "Creator", nameLists);
        getNames(Regex.match(sourceCode, Str.get(192), Str.get(193)), "Director", nameLists);
        getNames(Regex.match(sourceCode, Str.get(194), Str.get(195)), "Writer", nameLists);
        getNames(Regex.match(sourceCode, Str.get(196), Str.get(197)), "Star", nameLists);
        int lastIndex = nameLists.size() - 1;
        for (int i = 0; i <= lastIndex; i++) {
            if (i == 0) {
                summary.append(br2);
            }
            summary.append(nameLists.get(i));
            if (i != lastIndex) {
                summary.append(br1);
            }
        }

        summary.append(br2);
        if (isTVShow) {
            summary.append(Constant.TV_NEXT_EPISODE_HTML_AND_PLACEHOLDER).append(br1).append(Constant.TV_PREV_EPISODE_HTML_AND_PLACEHOLDER);
        } else {
            String releaseDate = Regex.replaceAll(Regex.match(sourceCode, Str.get(539), Str.get(540)), Str.get(541), Str.get(542));
            if (Regex.isMatch(releaseDate, Str.get(543))) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(544), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(556)));
            } else if (Regex.isMatch(releaseDate, Str.get(548))) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(549), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(557)));
            } else if (releaseDate.isEmpty() || Regex.isMatch(releaseDate, Str.get(545))) {
                releaseDate = getImdbTitleParts(sourceCode)[1];
            }
            summary.append("<b>Release Date: </b>").append(releaseDate.isEmpty() ? "unknown" : releaseDate);
        }

        if (storyline != null) {
            summary.append(storyline);
        }

        return summary.append(br2).append("</font></body></html>").toString();
    }

    private static void getNames(String names, String type, Collection<StringBuilder> nameLists) {
        StringBuilder nameList = new StringBuilder("<b>" + type);
        List<String> namesArr = Regex.matches(names, Str.get(198), Str.get(199));

        ListIterator<String> namesIt = namesArr.listIterator();
        while (namesIt.hasNext()) {
            if (Regex.isMatch(namesIt.next(), Str.get(200))) {
                namesIt.remove();
            }
        }

        int numNames = namesArr.size();
        if (numNames == 0) {
            return;
        }

        if (numNames > 1) {
            nameList.append('s');
        }
        nameList.append(":</b> ");

        for (int i = 0; i < numNames; i++) {
            if (i == numNames - 2) {
                if (numNames == 2) {
                    nameList.append(namesArr.get(i)).append(" and ");
                } else {
                    nameList.append(namesArr.get(i)).append(", and ");
                }
            } else if (i == numNames - 1) {
                nameList.append(namesArr.get(i));
            } else {
                nameList.append(namesArr.get(i)).append(", ");
            }
        }

        nameLists.add(nameList);
    }

    public static String dateToString(SimpleDateFormat dateFormat, String date, boolean showDay) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(date));
            return (showDay ? calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH) + ", " : "") + calendar.getDisplayName(Calendar.MONTH,
                    Calendar.SHORT, Locale.ENGLISH) + ' ' + (showDay ? calendar.get(Calendar.DAY_OF_MONTH) + ", " : "") + calendar.get(Calendar.YEAR);
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return date;
    }

    public static String getMovieTitlePrefix(String dirtyMovieTitle) {
        int index = -1;

        Matcher numMatcher = Regex.matcher(Str.get(214), dirtyMovieTitle);
        while (!numMatcher.hitEnd()) {
            if (numMatcher.find()) {
                index = numMatcher.end();
            }
        }

        if (index == -1) {
            return null;
        }
        if (index != dirtyMovieTitle.length()) {
            index--;
        }

        String prefix = dirtyMovieTitle.substring(0, index);
        if (prefix.equals(dirtyMovieTitle)) {
            return null;
        }

        if (Debug.DEBUG) {
            Debug.println("Movie Title Prefix: '" + prefix + '\'');
        }
        return prefix;
    }

    public static String getOldTitle(String sourceCode) {
        return Regex.match(sourceCode, Str.get(172), Str.get(173));
    }

    public static void saveImage(Video video) throws Exception {
        String tooOldOrNonexistentImagePath = tooOldOrNonexistentImagePath(video);
        if (tooOldOrNonexistentImagePath != null) {
            try {
                Connection.saveData(video.imageLink, tooOldOrNonexistentImagePath, DomainType.VIDEO_INFO);
                video.imagePath = tooOldOrNonexistentImagePath;
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public static Object[] toTableRow(GuiListener guiListener, Video video, boolean isBold) {
        String title = video.title;
        if (!video.oldTitle.isEmpty()) {
            title += Constant.aka(video.oldTitle);
        }
        if (video.IS_TV_SHOW && !video.season.isEmpty()) {
            title += Constant.latestEpisode(video.season, video.episode);
        }

        String year, rating, startHtml = "<html>", endHtml = "</html>";
        if (isBold) {
            startHtml += "<b>";
            endHtml = "</b>" + endHtml;
            year = startHtml + video.year + endHtml;
            rating = startHtml + video.rating + endHtml;
        } else {
            year = video.year;
            rating = video.rating;
        }

        String image = imagePath(video);
        String imagePath = Constant.CACHE_DIR + image;
        image = ((new File(imagePath)).exists() ? imagePath : Constant.NO_IMAGE + image);

        return guiListener.makeRow(video.ID, image, startHtml + Constant.TITLE_INDENT + title + endHtml, video.title, video.oldTitle, year, rating, video.summary,
                video.imageLink, video.IS_TV_SHOW, video.IS_TV_SHOW_AND_MOVIE, video.season, video.episode);
    }

    public static String url(Video video) {
        return Str.get(519) + video.ID;
    }

    public static String tooOldOrNonexistentImagePath(Video video) {
        String imagePath = Constant.CACHE_DIR + imagePath(video);
        File image = new File(imagePath);
        return image.exists() ? (IO.isFileTooOld(image, MAX_IMAGE_AGE) ? imagePath : null) : imagePath;
    }

    public static String imagePath(Video video) {
        long imageName = Str.hashCode(video.ID);
        return (imageName % Constant.MAX_SUBDIRECTORIES) + Constant.FILE_SEPARATOR + imageName;
    }

    private VideoSearch() {
    }
}

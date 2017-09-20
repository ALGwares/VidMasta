package search.util;

import debug.Debug;
import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
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
        return Regex.isMatch(Regex.match(sourceCode, 584), typeRegexIndex);
    }

    public static String getTitleLink(String title, String year) throws Exception {
        String encodedQuery = URLEncoder.encode(Regex.clean(title) + (year.isEmpty() ? "" : ' ' + year) + Str.get(76), Constant.UTF8);
        List<String> engines = new ArrayList<String>(searchEngines);
        boolean noResultFound = false;

        for (int i = NUM_SEARCH_ENGINES; i > 0; i--) {
            String searchEngine = engines.get(rand.nextInt(i));
            try {
                String result = Regex.firstMatch(Connection.getSourceCode(searchEngine + encodedQuery, DomainType.SEARCH_ENGINE), 619);
                if (!result.isEmpty() && !(result = URLDecoder.decode(result, Constant.UTF8)).isEmpty()) {
                    return Str.get(96) + result;
                }
                noResultFound = true;
            } catch (ConnectionException e) {
                if (i == 1) {
                    if (noResultFound) {
                        return null;
                    }
                    throw e;
                }
                if (Debug.DEBUG) {
                    Debug.println("Retrying search query (" + encodedQuery + "): " + e.URL);
                }
            }
            engines.remove(searchEngine);
        }

        return null;
    }

    public static TitleParts getImdbTitleParts(String sourceCode) {
        TitleParts titleParts = new TitleParts();
        titleParts.title = Regex.match(sourceCode, 98);
        Pattern yearPattern = Regex.pattern(100);
        int titleEndIndex = -1;

        for (int i = titleParts.title.length() - 1; i > -1 && titleEndIndex == -1; i--) {
            Matcher yearMatcher = yearPattern.matcher(titleParts.title.substring(i));
            while (!yearMatcher.hitEnd()) {
                if (yearMatcher.find()) {
                    titleParts.year = yearMatcher.group();
                    titleEndIndex = yearMatcher.start() + i;
                    break;
                }
            }
        }

        if (titleEndIndex == -1) {
            titleParts.title = Regex.replaceAll(titleParts.title, 101);
            titleEndIndex = titleParts.title.length();
        }
        titleParts.title = titleParts.title.substring(0, titleEndIndex).trim();
        titleParts.year = Regex.firstMatch(titleParts.year, 135);

        return titleParts;
    }

    public static TitleParts getTitleParts(String title, boolean isTVShow) {
        TitleParts titleParts = new TitleParts();
        String titleName = Regex.replaceAll(title, 103);
        Collection<Integer> indexes = new ArrayList<Integer>(4);

        Matcher typeMatcher = Regex.matcher(105, titleName);
        while (!typeMatcher.hitEnd()) {
            if (typeMatcher.find()) {
                if (Debug.DEBUG) {
                    Debug.print('\'' + typeMatcher.group() + "' ");
                }
                indexes.add(typeMatcher.start());
                break;
            }
        }

        Matcher yearMatcher = Regex.matcher(106, titleName);
        while (!yearMatcher.hitEnd()) {
            if (yearMatcher.find()) {
                indexes.add(yearMatcher.start());
                titleParts.year = yearMatcher.group().trim();
                break;
            }
        }

        if (isTVShow) {
            Matcher tvBoxSetAndEpisodeMatcher = Regex.matcher(107, titleName);
            while (!tvBoxSetAndEpisodeMatcher.hitEnd()) {
                if (!tvBoxSetAndEpisodeMatcher.find()) {
                    continue;
                }

                String tvBoxSetAndEpisode = tvBoxSetAndEpisodeMatcher.group();
                if (Debug.DEBUG) {
                    Debug.print("TV BoxSet/S&E/E: '" + tvBoxSetAndEpisode + "' ");
                }
                if (Regex.isMatch(tvBoxSetAndEpisode, 108)) {
                    String[] seasonAndEpisode = Regex.split(Regex.replaceFirst(tvBoxSetAndEpisode.trim(), 109), 111);
                    int seasonNum = Integer.parseInt(seasonAndEpisode[0]);
                    if (seasonNum >= 1 && seasonNum <= 100) {
                        titleParts.season = String.format(Constant.TV_EPISODE_FORMAT, seasonNum);
                    }
                    for (int i = 1; i < seasonAndEpisode.length; i++) {
                        int episodeNum = Integer.parseInt(seasonAndEpisode[i]);
                        if (episodeNum >= 0 && episodeNum <= 300) {
                            titleParts.episodes.add(String.format(Constant.TV_EPISODE_FORMAT, episodeNum));
                        }
                    }
                }
                indexes.add(tvBoxSetAndEpisodeMatcher.start());
                break;
            }
        } else {
            Matcher movieBoxSetMatcher = Regex.matcher(239, titleName);
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

        if (indexes.isEmpty() || (titleParts.title = titleName.substring(0, Collections.min(indexes)).trim()).isEmpty()) {
            titleParts.title = titleName.trim();
        }
        if (Debug.DEBUG) {
            Debug.println('\'' + titleParts.title + "' '" + titleParts.year + "' '" + titleParts.season + "' '" + titleParts.episodes + "' '" + titleName + '\'');
        }
        return titleParts;
    }

    public static boolean isRightFormat(String titleName, String format) {
        if (format.equals(Constant.ANY)) {
            return true;
        }

        String title = Regex.replaceAll(titleName, 77);
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
        return !Regex.firstMatch(title, typeRegexIndex).isEmpty();
    }

    public static String rating(String rating) {
        return rating.isEmpty() ? Constant.NO_RATING : rating;
    }

    public static String getSummary(String sourceCode, boolean isTVShow) {
        String infoBar = Regex.match(sourceCode, 137);
        List<String> genresArr = Regex.matches(infoBar, 139);

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
            summary.append("<b id=\"").append(Constant.GENRE_HTML_ID).append("\">").append(Str.str("genre")).append(" </b>").append(genresStr).append(br2);
        }

        String summary1 = Regex.match(sourceCode, 129);
        summary1 = Regex.replaceAll(summary1, 203).trim();
        summary1 = Regex.replaceAll(Regex.replaceAll(summary1, 241), 243);
        String summary2 = Regex.match(sourceCode, 131);
        summary2 = Regex.replaceAll(summary2, 205).trim();
        summary2 = Regex.replaceAll(Regex.replaceAll(summary2, 245), 247);
        String storyline = null;
        boolean isEmpty1 = summary1.isEmpty(), isEmpty2 = summary2.isEmpty();

        if ((!isEmpty1 && !isEmpty2) && Regex.htmlToPlainText(summary1).equalsIgnoreCase(Regex.htmlToPlainText(summary2))) {
            summary.append(summary1);
        } else if (isEmpty1 && !isEmpty2) {
            summary.append(summary2);
        } else if (!isEmpty1 && isEmpty2) {
            summary.append(summary1);
        } else if (!isEmpty1) {
            summary.append(summary1);
            storyline = br2 + "<font size=\"5\"><b id=\"" + Constant.STORYLINE_HTML_ID + "\">" + Str.str("storyline") + "</b></font>" + br1 + summary2;
        } else {
            summary.append(Str.get(159));
        }

        List<StringBuilder> nameLists = new ArrayList<StringBuilder>(4);
        getNames(Regex.match(sourceCode, 560), "creator", nameLists);
        getNames(Regex.match(sourceCode, 192), "director", nameLists);
        getNames(Regex.match(sourceCode, 194), "writer", nameLists);
        getNames(Regex.match(sourceCode, 196), "star", nameLists);
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
            summary.append("<b id=\"").append(Constant.TV_NEXT_EPISODE_HTML_ID).append("\">").append(Str.str("nextEpisode")).append(" </b>").append(
                    Constant.TV_EPISODE_PLACEHOLDER).append(br1).append("<b id=\"").append(Constant.TV_PREV_EPISODE_HTML_ID).append("\">").append(Str.str(
                                    "prevEpisode")).append(" </b>").append(Constant.TV_EPISODE_PLACEHOLDER);
        } else {
            String releaseDate = Regex.replaceAll(Regex.match(sourceCode, 539), 541);
            if (Regex.isMatch(releaseDate, 543)) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(544), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(556)));
            } else if (Regex.isMatch(releaseDate, 548)) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(549), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(557)));
            } else {
                releaseDate = dateToString(new SimpleDateFormat("yyyy", Locale.ENGLISH), getImdbTitleParts(sourceCode).year, null);
            }
            summary.append("<b>").append(Str.str("releaseDate")).append(" </b>").append(releaseDate);
        }

        if (storyline != null) {
            summary.append(storyline);
        }

        return summary.append(br2).append("</font></body></html>").toString();
    }

    private static void getNames(String names, String type, Collection<StringBuilder> nameLists) {
        List<String> namesArr = Regex.matches(names, 198);
        ListIterator<String> namesIt = namesArr.listIterator();
        while (namesIt.hasNext()) {
            if (Regex.isMatch(namesIt.next(), 200)) {
                namesIt.remove();
            }
        }

        int numNames = namesArr.size();
        if (numNames == 0) {
            return;
        }

        StringBuilder nameList = new StringBuilder(32);
        nameList.append("<b>").append(Str.str(type + (numNames > 1 ? "s" : ""))).append("</b> ");

        for (int i = 0; i < numNames; i++) {
            nameList.append(namesArr.get(i));
            if (i == numNames - 2) {
                if (numNames == 2) {
                    nameList.append(" and ");
                } else {
                    nameList.append(", and ");
                }
            } else if (i != numNames - 1) {
                nameList.append(", ");
            }
        }

        nameLists.add(nameList);
    }

    public static String dateToString(DateFormat dateFormat, String date, Boolean showDay) {
        try {
            return (showDay == null ? new SimpleDateFormat("yyyy") : (showDay ? DateFormat.getDateInstance(DateFormat.FULL) : new SimpleDateFormat(
                    "MMMM yyyy"))).format(dateFormat.parse(date));
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return date;
    }

    public static String getTitlePrefix(String title) {
        int index = -1;

        Matcher numMatcher = Regex.matcher(214, title);
        while (!numMatcher.hitEnd()) {
            if (numMatcher.find()) {
                index = numMatcher.end();
            }
        }

        if (index == -1) {
            return null;
        }
        if (index != title.length()) {
            index--;
        }

        String prefix = title.substring(0, index);
        if (prefix.equals(title)) {
            return null;
        }

        if (Debug.DEBUG) {
            Debug.println("Title Prefix: '" + prefix + '\'');
        }
        return prefix;
    }

    public static String getOldTitle(String sourceCode) {
        return Regex.match(sourceCode, 172);
    }

    public static void saveImage(Video video) {
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
            title += aka(video.oldTitle);
        }
        if (video.IS_TV_SHOW && !video.season.isEmpty() && !video.episode.isEmpty()) {
            title += popularEpisode(video.season, video.episode);
        }

        String year, rating = (video.rating.equals(Constant.NO_RATING) ? video.rating : Str.getNumFormat(Constant.RATING_FORMAT).format(Double.parseDouble(
                video.rating))), startHtml = "<html>", endHtml = "</html>";
        if (isBold) {
            startHtml += "<b>";
            endHtml = "</b>" + endHtml;
            year = startHtml + video.year + endHtml;
            rating = startHtml + rating + endHtml;
        } else {
            year = video.year;
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
        return image.exists() ? (IO.isFileTooOld(image, Long.parseLong(Str.get(501))) ? imagePath : null) : imagePath;
    }

    public static String imagePath(Video video) {
        long imageName = Str.hashCode(video.ID);
        return (imageName % Constant.MAX_SUBDIRECTORIES) + Constant.FILE_SEPARATOR + imageName;
    }

    public static String describe(Video video) {
        return Regex.htmlToPlainText(video.title) + " (" + video.year + (video.IS_TV_SHOW ? (" S" + (video.season.isEmpty() || video.season.equals(Constant.ANY)
                ? "--" : video.season) + "E" + (video.episode.isEmpty() || video.episode.equals(Constant.ANY) ? "--" : video.episode)) : "") + ')';
    }

    public static boolean isUploadYearTooOld(String sourceCode, int maxYearsOld, int baseYear) {
        String uploadTime = Regex.match(sourceCode, 668);
        if (uploadTime.isEmpty()) {
            return false;
        }

        int uploadYear = Integer.parseInt(uploadTime), currYear = Calendar.getInstance().get(Calendar.YEAR);
        if (Boolean.parseBoolean(Str.get(670))) {
            uploadYear = currYear - uploadYear;
        }
        return (uploadYear + maxYearsOld) < (baseYear == -1 ? currYear : baseYear);
    }

    public static String popularEpisode(String season, String episode) {
        String popularEpisode = " (" + Str.str("popularEpisode") + ' ';
        String rightParenthesis = ")";
        String seasonStr;
        String episodeStr;
        if (season.isEmpty()) {
            popularEpisode = Pattern.quote(popularEpisode);
            rightParenthesis = Pattern.quote(rightParenthesis);
            seasonStr = "\\d{2}+";
            episodeStr = seasonStr;
        } else {
            seasonStr = season;
            episodeStr = episode;
        }
        return popularEpisode + 'S' + seasonStr + 'E' + episodeStr + rightParenthesis;
    }

    public static String aka(String str) {
        return " (" + Str.str("aka") + ' ' + str + ')';
    }

    public static String summaryTagRegex(String id) {
        return "\\<\\s*+b\\s++id\\s*+\\=\\s*+\"" + id + "\"\\s*+\\>(?s).+?\\<\\s*+/b\\s*+\\>";
    }

    public static String normalize(String titleID) {
        return Regex.isMatch(titleID, Str.get(770)) ? String.format(Str.get(771), Integer.parseInt(titleID)) : titleID;
    }

    private VideoSearch() {
    }
}

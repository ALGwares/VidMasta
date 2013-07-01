package search;

import debug.Debug;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import listener.GuiListener;
import main.Str;
import search.util.VideoSearch;
import util.Connection;
import util.Constant;
import util.Regex;
import util.io.Write;

public class Video {

    private static final long MAX_IMAGE_AGE = Long.parseLong(Str.get(501));
    public String originalTitle, id, title, year, rating, summaryLink, imageLink, season, episode;
    public boolean isTVShow;

    public Video(String id, String title, String year, String rating, String summaryLink, String imageLink, String season, String episode, boolean isTVShow) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.rating = rating;
        this.summaryLink = summaryLink;
        this.imageLink = imageLink;
        this.season = season;
        this.episode = episode;
        this.isTVShow = isTVShow;
    }

    public void saveImage() throws Exception {
        String imagePath = saveImagePath();
        if (imagePath != null) {
            try {
                Connection.saveData(imageLink, imagePath, Connection.VIDEO_INFO);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                Write.fileOp(imagePath, Write.RM_FILE_NOW_AND_ON_EXIT);
            }
        }
    }

    public String saveImagePath() {
        String imagePath = Constant.CACHE_DIR + imagePath(id);
        File image = new File(imagePath);
        if (image.exists()) {
            long lastModified = image.lastModified();
            return lastModified != 0L && (System.currentTimeMillis() - lastModified) > MAX_IMAGE_AGE ? imagePath : null;
        }
        return imagePath;
    }

    public Object[] toTableRow(GuiListener guiListener, boolean summaryIsLink, boolean isBold) {
        String displayTitle = title;
        if (originalTitle != null) {
            displayTitle += " (AKA: " + originalTitle + ')';
        }

        String seasonAndEpisode = "";
        if (isTVShow && season != null && episode != null) {
            displayTitle += " (Latest Episode: S" + season + 'E' + episode + ')';
            seasonAndEpisode += Constant.SEPARATOR1 + season + Constant.SEPARATOR1 + episode;
        }

        String titleStr, yearStr, ratingStr, startHtml = "<html>", endHtml = "</html>";
        if (isBold) {
            startHtml += "<b>";
            endHtml = "</b>" + endHtml;
            titleStr = startHtml + "&nbsp;&nbsp;&nbsp;" + displayTitle + endHtml;
            yearStr = startHtml + year + endHtml;
            ratingStr = startHtml + rating + endHtml;
        } else {
            titleStr = startHtml + "&nbsp;&nbsp;&nbsp;" + displayTitle + endHtml;
            yearStr = year;
            ratingStr = rating;
        }

        String summaryStr = title + Constant.SEPARATOR1 + summaryLink + Constant.SEPARATOR1 + imageLink + Constant.SEPARATOR1
                + (summaryIsLink ? Constant.TRUE : Constant.FALSE) + Constant.SEPARATOR1 + (isTVShow ? Constant.TRUE : Constant.FALSE) + seasonAndEpisode;

        String imageStr = imagePath(id);
        String imagePath = Constant.CACHE_DIR + imageStr;
        imageStr = (new File(imagePath)).exists() ? imagePath : Constant.NO_IMAGE + imageStr;

        return guiListener.makeRow(id, imageStr, titleStr, yearStr, ratingStr, summaryStr);
    }

    public static String imagePath(String titleID) {
        long imageName = Str.hashCode(titleID);
        return (imageName % Constant.MAX_SUBDIRECTORIES) + Constant.FILE_SEPARATOR + imageName;
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
        } else if (index != dirtyMovieTitle.length()) {
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

    public static String getDirtyOldTitle(String sourceCode) {
        String uncleanOldTitle = Regex.match(sourceCode, Str.get(172), Str.get(173));
        if (uncleanOldTitle.isEmpty()) {
            return null;
        } else {
            if (Debug.DEBUG) {
                Debug.println("Original Title: " + uncleanOldTitle);
            }
            return uncleanOldTitle;
        }
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
            summary.append("<b>Genre: </b>").append(genresStr.toString()).append(br2);
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
        } else if (!isEmpty1 && !isEmpty2) {
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
            summary.append("<b id=\"nextEpisode\">Next Episode: </b>").append(Constant.TV_EPISODE_PLACEHOLDER);
        } else {
            String releaseDate = Regex.replaceAll(Regex.match(sourceCode, Str.get(539), Str.get(540)), Str.get(541), Str.get(542));
            if (Regex.isMatch(releaseDate, Str.get(543))) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(544), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(556)));
            } else if (Regex.isMatch(releaseDate, Str.get(548))) {
                releaseDate = dateToString(new SimpleDateFormat(Str.get(549), Locale.ENGLISH), releaseDate, Boolean.parseBoolean(Str.get(557)));
            } else if (releaseDate.isEmpty() || Regex.isMatch(releaseDate, Str.get(545))) {
                releaseDate = VideoSearch.getImdbTitleParts(sourceCode)[1];
            }
            summary.append("<b>Release Date: </b>").append(releaseDate.isEmpty() ? "unknown" : releaseDate);
        }

        if (storyline != null) {
            summary.append(storyline);
        }

        summary.append(br2).append("</font></body></html>");
        return summary.toString();
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

    @Override
    public String toString() {
        return isTVShow + " " + title + '(' + originalTitle + ") " + year + ' ' + season + ' ' + episode + ' ' + rating + ' ' + summaryLink + ' ' + imageLink
                + ' ' + id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Video)) {
            return false;
        }
        return id.equals(((Video) obj).id);
    }

    @Override
    public int hashCode() {
        return 7 * 31 + (id == null ? 0 : id.hashCode());
    }
}

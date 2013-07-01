package search;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import main.Str;
import util.Constant;
import util.Regex;

public class BoxSetVideo {

    String title;
    private String year;
    public static final Collection<List<BoxSetVideo>> movieBoxSets = new ArrayList<List<BoxSetVideo>>(4096);

    static {
        for (String boxSet : Regex.split(Str.get(338), Constant.SEPARATOR3)) {
            List<BoxSetVideo> boxSetArr = new ArrayList<BoxSetVideo>(8);
            for (String title : Regex.split(boxSet, Constant.SEPARATOR1)) {
                String[] titleParts = Regex.split(title, Constant.SEPARATOR2);
                boxSetArr.add(new BoxSetVideo(titleParts[0], titleParts.length == 1 ? null : titleParts[1]));
            }
            movieBoxSets.add(boxSetArr);
        }
    }

    private BoxSetVideo(String title, String year) {
        this.title = title;
        this.year = year;
    }

    public boolean isSameTitle(String imdbTitle, String imdbYear) throws Exception {
        if (!imdbYear.equals(year)) {
            return false;
        }
        String escapedImdbTitle = Regex.replaceAll(Regex.replaceAll(escapeTitle(imdbTitle), Str.get(313), Str.get(314)), Str.get(315), Str.get(316)).trim();
        if (!isTitleValid(escapedImdbTitle)) {
            return false;
        }
        String escapedWikiTitle = Regex.replaceAll(Regex.replaceAll(title, Str.get(317), Str.get(318)), Str.get(319), Str.get(320)).trim();
        return isTitleValid(escapedWikiTitle) && escapedImdbTitle.equalsIgnoreCase(escapedWikiTitle);
    }

    private static String escapeTitle(String title) throws Exception {
        String newTitle = Regex.replaceAll(Str.htmlToPlainText(title), Str.get(311), Str.get(312));
        int len = newTitle.length();
        StringBuilder escapedTitleBuf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            String currChar = String.valueOf(newTitle.charAt(i));
            if (Regex.match(URLEncoder.encode(currChar, Constant.UTF8), Str.get(321)).isEmpty()) {
                escapedTitleBuf.append(currChar);
            } else {
                escapedTitleBuf.append(Str.get(322));
            }
        }
        return Regex.replaceAll(escapedTitleBuf.toString(), Str.get(323), Str.get(324)).trim();
    }

    public static String[] getSearchTitles(List<BoxSetVideo> boxSet) {
        int numVideos = boxSet.size();
        List<String> titles = new ArrayList<String>(numVideos);
        String boxSetName = Regex.replaceFirst(boxSet.get(0).title, Str.get(325), Str.get(326));
        String[] result = {boxSetName, null};

        for (int i = 1; i < numVideos; i++) {
            titles.add(Regex.replaceFirst(boxSet.get(i).title, Str.get(327), Str.get(328)));
        }

        numVideos--;
        Collection<Integer> titleLengths = new ArrayList<Integer>(numVideos);
        for (String currTitle : titles) {
            titleLengths.add(currTitle.length());
        }

        int maxLen = Collections.min(titleLengths);
        String firstTitle = titles.get(0), prefix = firstTitle.substring(0, maxLen);
        boolean done = false;

        for (int i = 0; i < maxLen && !done; i++) {
            for (int j = 1; j < numVideos; j++) {
                if (!String.valueOf(firstTitle.charAt(i)).equalsIgnoreCase(String.valueOf(titles.get(j).charAt(i)))) {
                    prefix = firstTitle.substring(0, i);
                    done = true;
                    break;
                }
            }
        }

        prefix = validatePrefix(firstTitle, prefix);
        if (prefix != null && !prefix.equalsIgnoreCase(boxSetName)) {
            result[1] = prefix;
        }
        return result;
    }

    private static String validatePrefix(String title, String prefix) {
        String newPrefix = prefix;
        if (title.length() != newPrefix.length()) {
            if (Regex.matches(newPrefix, Str.get(329)).isEmpty() && Regex.matches(title.replace(newPrefix, Str.get(330)),
                    Str.get(331)).isEmpty()) {
                int i = newPrefix.length() - 1;
                for (; i > -1; i--) {
                    if (newPrefix.charAt(i) == Str.get(332).charAt(0) || newPrefix.charAt(i) == Str.get(333).charAt(0)) {
                        break;
                    }
                }
                if (i == -1) {
                    return null;
                }
                newPrefix = newPrefix.substring(0, i);
            }
            if (newPrefix.endsWith(Str.get(334))) {
                newPrefix = newPrefix.substring(0, newPrefix.length() - 1);
            }
        }
        newPrefix = newPrefix.trim();
        if (!isTitleValid(newPrefix)) {
            return null;
        }
        return newPrefix;
    }

    private static boolean isTitleValid(String title) {
        return !(title.isEmpty() || Regex.match(title, Str.get(335)).isEmpty());
    }

    @Override
    public String toString() {
        return title + '(' + year + ')';
    }
}

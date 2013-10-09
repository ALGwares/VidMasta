package search;

import debug.Debug;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import listener.GuiListener;
import main.Str;
import search.util.VideoSearch;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.ProxyException;
import util.Regex;

public class PopularSearcher extends AbstractSearcher {

    private String[] languages, countries;
    private boolean isFeed, startAsap;
    private static final int SLEEP = Integer.parseInt(Str.get(571));

    public PopularSearcher(GuiListener guiListener, int numResultsPerSearch, boolean isTVShow, String[] languages, String[] countries, boolean isFeed,
            boolean startAsap) {
        super(guiListener, numResultsPerSearch, isTVShow);
        this.languages = locales(languages, Constant.ANY_LANGUAGE, Regex.languages);
        this.countries = locales(countries, Constant.ANY_COUNTRY, Regex.countries);
        this.isFeed = isFeed;
        this.startAsap = startAsap;
    }

    public PopularSearcher(PopularSearcher searcher) {
        super(searcher);
        languages = searcher.languages;
        countries = searcher.countries;
        isFeed = searcher.isFeed;
        startAsap = searcher.startAsap;
    }

    private static String[] locales(String[] locales, String anyLocale, Map<String, String> localeCodes) {
        String[] newLocales = new String[locales.length];
        for (int i = 0; i < locales.length; i++) {
            if (locales[i].equals(anyLocale)) {
                return Constant.EMPTY_STRS;
            }
            newLocales[i] = localeCodes.get(locales[i]);
        }
        return newLocales;
    }

    static boolean isValidLocale(String sourceCode, String[] locales, int regexStartIndex) {
        for (String locale : locales) {
            if (Regex.match(sourceCode, Str.get(regexStartIndex) + locale + Str.get(regexStartIndex + 1)).isEmpty()) {
                if (Debug.DEBUG) {
                    Debug.println("Wrong locale (NOT " + locale + ")");
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected void initialSearch() {
        if (!isFeed || startAsap) {
            return;
        }
        try {
            Thread.sleep(SLEEP);
        } catch (InterruptedException e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
    }

    @Override
    protected boolean hasNextPage(int nextPage) {
        return currSourceCode.contains(Str.get(112) + (isTVShow ? Str.get(113) : Str.get(114)) + Str.get(115) + nextPage + Str.get(116));
    }

    @Override
    protected String getUrl(int page) {
        return Str.get(117) + (isTVShow ? Str.get(118) : Str.get(119)) + Str.get(120) + page + Str.get(121);
    }

    @Override
    protected int connectionType() {
        return Connection.DOWNLOAD_LINK_INFO;
    }

    @Override
    protected boolean connectionException(String url, ConnectionException e) {
        if (e.getClass().equals(ProxyException.class)) {
            guiListener.msg(e.getMessage(), Constant.ERROR_MSG);
        } else {
            if (Connection.downloadLinkInfoFail() && backupMode()) {
                return true;
            }
            guiListener.msg(Connection.error("", "", url), Constant.ERROR_MSG);
        }
        return false;
    }

    @Override
    protected int getTitleRegexIndex(String url) throws Exception {
        if (!Regex.match(currSourceCode, Str.get(147)).isEmpty()) {
            fail(url);
            return -1;
        }
        return 122;
    }

    protected void fail(String url) throws Exception {
        Connection.removeDownloadLinkInfoProxyUrlFromCache(url);
        if (!isCancelled()) {
            Connection.failDownloadLinkInfo();
            if (backupMode()) {
                return;
            }
            guiListener.msg(Connection.error(url), Constant.ERROR_MSG);
        }
        throw new ConnectionException();
    }

    @Override
    protected void addVideo(String titleMatch) {
        String video = Regex.match(titleMatch, Str.get(123), Str.get(124));
        String titleName = Regex.match(video, Str.get(125), Str.get(126));
        if (!titleName.isEmpty() && (!isFeed || isTitleValid(titleName, video))) {
            addCurrVideo(titleName);
        }
    }

    private void addCurrVideo(String titleName) {
        String[] titleParts = VideoSearch.getTitleParts(titleName, isTVShow);
        if (Debug.DEBUG) {
            Debug.println('\'' + titleParts[0] + "' '" + titleParts[1] + "' '" + titleParts[2] + "' '" + titleParts[3] + "' '" + titleName + '\'');
        }
        Video video = new Video(titleParts[0].toLowerCase(Locale.ENGLISH) + titleParts[1], titleParts[0], titleParts[1], null, null, Constant.NULL, null, null,
                isTVShow, false);
        if (!videoBuffer.contains(video)) {
            video.season = titleParts[2];
            video.episode = titleParts[3];
            videoBuffer.add(video);
        }
    }

    @Override
    protected void checkVideoes(String url) throws Exception {
        if (currSearchPage == 0 && videoBuffer.isEmpty()) {
            fail(url);
        }
    }

    private boolean backupMode() {
        try {
            currSourceCode = Connection.getSourceCode(Str.get(isTVShow ? 483 : 484), Connection.DOWNLOAD_LINK_INFO);
            String[] results = Regex.split(currSourceCode, Constant.STD_NEWLINE);
            for (int i = 0; i < results.length; i += 5) {
                if (!isFeed || isTitleValid(results[i + 2], results[i + 3], results[i + 1])) {
                    addCurrVideo(results[i + 2].trim());
                }
            }
            return true;
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        return false;
    }

    @Override
    protected boolean findImage(Video video) {
        return true;
    }

    @Override
    protected String getSourceCode(Video video) throws Exception {
        String titleLink = VideoSearch.getTitleLink(video.title, video.year);
        if (isCancelled() || titleLink == null) {
            return null;
        }

        video.id = Regex.replaceAll(Regex.replaceFirst(titleLink, Str.get(447), Str.get(448)), Str.get(449), Str.get(450));
        if (video.id.isEmpty()) {
            return null;
        }

        String sourceCode = Connection.getSourceCode(titleLink, Connection.VIDEO_INFO);
        if (isCancelled()) {
            return null;
        }

        String[] titleParts = VideoSearch.getImdbTitleParts(sourceCode);
        video.title = titleParts[0];
        video.year = titleParts[1];
        if (video.title.isEmpty() || video.year.isEmpty() || (isFeed && !isTitleYearValid(video.year))) {
            return null;
        }

        if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + video.title + "' '" + video.year + '\'');
            }
            return null;
        }

        if (!isValidLocale(sourceCode, languages, 183) || !isValidLocale(sourceCode, countries, 185)) {
            return null;
        }

        String rating = Regex.match(sourceCode, Str.get(127), Str.get(128));
        video.rating = rating.isEmpty() ? "-" : rating;
        video.isTVShowAndMovie = VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590);

        return sourceCode;
    }

    @Override
    protected boolean noImage(Video video) {
        return isFeed;
    }

    private static boolean isTitleValid(String titleName, String video) {
        if (Regex.match(Regex.replaceAll(titleName, Str.get(77), Str.get(78)), Str.get(569)).isEmpty() || (Boolean.parseBoolean(Str.get(565)) && Regex.match(video,
                Str.get(74), Str.get(75)).isEmpty())) {
            return false; // Wrong format or unsafe source
        }
        String size = Regex.match(video, Str.get(64), Str.get(65));
        if (!size.isEmpty() && (int) Math.ceil(Double.parseDouble(size)) > Integer.parseInt(Str.get(567))) {
            return false; // Size too large
        }
        String uploadYear = Regex.match(video, Str.get(60));
        if (!uploadYear.isEmpty() && Integer.parseInt(uploadYear.substring(uploadYear.length() - Integer.parseInt(Str.get(61)))) + Integer.parseInt(Str.get(568))
                < Calendar.getInstance().get(Calendar.YEAR)) {
            return false; // Upload year too old
        }
        String numSeeders = Regex.match(video, Str.get(72), Str.get(73));
        if (!numSeeders.isEmpty() && Integer.parseInt(numSeeders) < Integer.parseInt(Str.get(566))) {
            return false; // Seeders too few
        }
        return true;
    }

    private static boolean isTitleValid(String titleName, String isSafe, String year) {
        if (Regex.match(Regex.replaceAll(titleName.trim(), Str.get(77), Str.get(78)), Str.get(569)).isEmpty() || (Boolean.parseBoolean(Str.get(565))
                && Integer.parseInt(isSafe.trim()) != 1) || !isTitleYearValid(year.trim())) {
            return false; // Wrong format or unsafe source or year too old
        }
        return true;
    }

    private static boolean isTitleYearValid(String year) {
        if (Integer.parseInt(year) + Integer.parseInt(Str.get(570)) < Calendar.getInstance().get(Calendar.YEAR)) {
            return false; // Year too old
        }
        return true;
    }
}

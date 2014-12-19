package search;

import debug.Debug;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.VideoSearch;
import str.Str;
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
            if (Regex.firstMatch(sourceCode, Str.get(regexStartIndex) + locale + Str.get(regexStartIndex + 1)).isEmpty()) {
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
        if (SLEEP != 0 && (!isFeed || startAsap)) {
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
    protected int anotherPageRegexIndex() {
        return 649;
    }

    @Override
    protected String getUrl(int page) {
        return Str.get(650) + Str.get(isTVShow ? 651 : 652) + (page == 0 ? Str.get(653) : Str.get(654) + (page + Integer.parseInt(Str.get(655)))) + Str.get(656);
    }

    @Override
    protected DomainType domainType() {
        return DomainType.DOWNLOAD_LINK_INFO;
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
        if (!Regex.firstMatch(currSourceCode, 147).isEmpty()) {
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
        String video = Regex.match(titleMatch, 123);
        String titleName = Regex.match(video, 125);
        if (!titleName.isEmpty() && (!isFeed || isTitleValid(titleName, video))) {
            addCurrVideo(titleName);
        }
    }

    private void addCurrVideo(String titleName) {
        String[] titleParts = VideoSearch.getTitleParts(titleName, isTVShow);
        if (Debug.DEBUG) {
            Debug.println('\'' + titleParts[0] + "' '" + titleParts[1] + "' '" + titleParts[2] + "' '" + titleParts[3] + "' '" + titleName + '\'');
        }
        Video video = new Video(titleParts[0].toLowerCase(Locale.ENGLISH) + titleParts[1], titleParts[0], titleParts[1], isTVShow, false);
        if (allBufferVideos.add(video.ID)) {
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
            currSourceCode = Connection.getSourceCode(Str.get(isTVShow ? 483 : 484), DomainType.DOWNLOAD_LINK_INFO);
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
    protected Video update(Video video) throws Exception {
        String titleLink = VideoSearch.getTitleLink(video.title, video.year);
        if (isCancelled() || titleLink == null) {
            return null;
        }

        String titleID = Regex.firstMatch(titleLink, 628);
        if (titleID.isEmpty()) {
            return null;
        }

        String sourceCode = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        String[] titleParts = VideoSearch.getImdbTitleParts(sourceCode);
        if (titleParts[0].isEmpty() || titleParts[1].isEmpty() || (isFeed && !isTitleYearValid(titleParts[1]))) {
            return null;
        }

        if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + titleParts[0] + "' '" + titleParts[1] + '\'');
            }
            return null;
        }

        if (!isValidLocale(sourceCode, languages, 183) || !isValidLocale(sourceCode, countries, 185)) {
            return null;
        }

        Video vid = new Video(titleID, titleParts[0], titleParts[1], video.IS_TV_SHOW, VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590));
        vid.rating = VideoSearch.rating(Regex.match(sourceCode, 127));
        vid.season = video.season;
        vid.episode = video.episode;
        vid.summary = sourceCode;
        return vid;
    }

    @Override
    protected boolean noImage(Video video) {
        return isFeed;
    }

    private static boolean isTitleValid(String titleName, String video) {
        if (Regex.firstMatch(Regex.replaceAll(titleName, 77), 569).isEmpty() || (Boolean.parseBoolean(Str.get(565)) && Regex.match(video, 74).isEmpty())) {
            return false; // Wrong format or unsafe source
        }
        String size = Regex.match(video, 64);
        if (!size.isEmpty() && (int) Math.ceil(Double.parseDouble(size)) > Integer.parseInt(Str.get(567))) {
            return false; // Size too large
        }
        if (VideoSearch.isUploadYearTooOld(video, Integer.parseInt(Str.get(568)), -1)) {
            return false; // Upload year too old
        }
        String numSeeders = Regex.match(video, 72);
        return numSeeders.isEmpty() || Integer.parseInt(numSeeders) >= Integer.parseInt(Str.get(566)); // Seeders too few if false
    }

    private static boolean isTitleValid(String titleName, String isSafe, String year) {
        return !Regex.firstMatch(Regex.replaceAll(titleName.trim(), 77), 569).isEmpty() && (!Boolean.parseBoolean(Str.get(565))
                || Integer.parseInt(isSafe.trim()) == 1) && isTitleYearValid(year.trim()); // Wrong format or unsafe source or year too old if false
    }

    private static boolean isTitleYearValid(String year) {
        return Integer.parseInt(year) + Integer.parseInt(Str.get(570)) >= Calendar.getInstance().get(Calendar.YEAR); // Year too old if false
    }
}

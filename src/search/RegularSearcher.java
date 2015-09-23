package search;

import debug.Debug;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.TitleParts;
import search.util.VideoSearch;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.ProxyException;
import util.Regex;

public class RegularSearcher extends AbstractSearcher {

    private Calendar startDate, endDate;
    private String startDateStr, endDateStr, title, genresStr, languages, countries, minRating;
    private String[] genres;
    private boolean isInitialSearchSuccessful;
    private static final Collection<String> noImageTitles = new ConcurrentSkipListSet<String>();

    public RegularSearcher(GuiListener guiListener, int numResultsPerSearch, boolean isTVShow, Calendar startDate, Calendar endDate, String title, String[] genres,
            String[] languages, String[] countries, String minRating) {
        super(guiListener, numResultsPerSearch, isTVShow);
        this.startDate = startDate;
        this.endDate = endDate;
        startDateStr = dateToStr(startDate);
        endDateStr = dateToStr(endDate);
        this.title = Regex.replaceAll(title, 3);
        this.genres = Arrays.copyOf(genres, genres.length);
        genresStr = searchStr(genres, null);
        this.languages = searchStr(languages, Regex.languages);
        this.countries = searchStr(countries, Regex.countries);
        this.minRating = minRating;
    }

    public RegularSearcher(RegularSearcher searcher) {
        super(searcher);
        startDate = searcher.startDate;
        endDate = searcher.endDate;
        startDateStr = searcher.startDateStr;
        endDateStr = searcher.endDateStr;
        title = searcher.title;
        genres = searcher.genres;
        genresStr = searcher.genresStr;
        languages = searcher.languages;
        countries = searcher.countries;
        minRating = searcher.minRating;
    }

    private static String dateToStr(Calendar date) {
        return date == null ? "" : String.format(Locale.ENGLISH, "%04d", date.get(Calendar.YEAR)) + "-" + String.format(Locale.ENGLISH, "%02d",
                date.get(Calendar.MONTH) + 1) + "-" + String.format(Locale.ENGLISH, "%02d", date.get(Calendar.DAY_OF_MONTH));
    }

    private static String searchStr(String[] searchStrs, Map<String, String> searchStrCodes) {
        StringBuilder searchStr = new StringBuilder(512);
        for (int i = 0; i < searchStrs.length; i++) {
            if (searchStrs[i].equals(Constant.ANY)) {
                return Constant.ANY;
            }
            searchStr.append(searchStrCodes == null ? searchStrs[i] : searchStrCodes.get(searchStrs[i]));
            if (i != searchStrs.length - 1) {
                searchStr.append(Str.get(513));
            }
        }
        return searchStr.toString();
    }

    @Override
    protected void initialSearch() throws Exception {
        if (title.isEmpty()) {
            return;
        }

        String year = "";
        if (startDate != null && endDate != null) {
            int startYear = startDate.get(Calendar.YEAR);
            int diff = endDate.get(Calendar.YEAR) - startYear;
            if (diff > -1 && diff < Integer.parseInt(Str.get(357))) {
                year = String.valueOf(startYear);
            }
        } else if (startDate != null) {
            year = String.valueOf(startDate.get(Calendar.YEAR));
        } else if (endDate != null) {
            year = String.valueOf(endDate.get(Calendar.YEAR));
        }

        String titleLink = VideoSearch.getTitleLink(title, year);
        if (titleLink == null) {
            return;
        }

        String titleID = Regex.firstMatch(titleLink, 628);
        if (titleID.isEmpty()) {
            return;
        }

        String sourceCode = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        TitleParts titleParts = VideoSearch.getImdbTitleParts(sourceCode);
        if (titleParts.title.isEmpty() || titleParts.year.isEmpty()) {
            return;
        }

        if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + titleParts.title + "' '" + titleParts.year + '\'');
            }
            return;
        }

        Video video = new Video(titleID, titleParts.title, titleParts.year, isTVShow, VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590));
        video.oldTitle = VideoSearch.getOldTitle(sourceCode);
        video.rating = VideoSearch.rating(Regex.match(sourceCode, 127));
        video.summary = VideoSearch.getSummary(sourceCode, isTVShow);
        video.imageLink = Regex.match(sourceCode, 190);
        if (!video.imageLink.isEmpty()) {
            VideoSearch.saveImage(video);
        }

        allBufferVideos.add(video.ID);
        allVideos.add(video.ID);
        guiListener.newResult(VideoSearch.toTableRow(guiListener, video, true));
        incrementProgress();
        isInitialSearchSuccessful = true;
    }

    @Override
    protected int anotherPageRegexIndex() {
        return 648;
    }

    @Override
    protected boolean addCurrVideos() {
        return false;
    }

    @Override
    protected String getUrl(int page) throws Exception {
        int maxNumResultsPerSearch = Integer.parseInt(Str.get(503));
        int numResultsPerPage = (numResultsPerSearch > maxNumResultsPerSearch ? maxNumResultsPerSearch : numResultsPerSearch);
        String urlCountries = (countries.equals(Constant.ANY) ? Str.get(179) : Str.get(180) + countries);
        String urlGenres = (genresStr.equals(Constant.ANY) ? Str.get(141) : (Str.get(7) + Regex.replaceAll(genresStr.toLowerCase(Locale.ENGLISH), 8)));
        String urlLanguages = (languages.equals(Constant.ANY) ? Str.get(181) : Str.get(182) + languages);
        String urlReleaseDates = (startDateStr.isEmpty() && endDateStr.isEmpty() ? Str.get(240) : Str.get(14) + startDateStr + Str.get(15) + endDateStr);
        String urlStart = (page == 0 ? Str.get(144) : Str.get(13) + ((page * numResultsPerPage) + 1));
        String urlTitle = (title.isEmpty() ? Str.get(142) : Str.get(10) + URLEncoder.encode(Regex.clean(title), Constant.UTF8));
        String urlTitleType = Str.get(16) + Str.get(isTVShow ? 18 : 17);
        String urlUserRating = (minRating.equals(Constant.ANY) ? Str.get(143) : (Str.get(11) + minRating + Str.get(12)));
        return Str.get(6) + numResultsPerPage + urlCountries + urlGenres + urlLanguages + urlReleaseDates + Str.get(19) + urlStart + urlTitle + urlTitleType
                + urlUserRating + Str.get(20);
    }

    @Override
    protected DomainType domainType() {
        return DomainType.VIDEO_INFO;
    }

    @Override
    protected boolean connectionException(String url, ConnectionException e) {
        guiListener.msg(e instanceof ProxyException ? e.getMessage() : Connection.error(url), Constant.ERROR_MSG);
        return false;
    }

    @Override
    protected int getTitleRegexIndex(String url) throws Exception {
        if (!Regex.firstMatch(currSourceCode, 145).isEmpty()) {
            Connection.removeFromCache(url);
            if (!isCancelled()) {
                if (isInitialSearchSuccessful) {
                    throw new ConnectionException(Connection.serverError(url));
                } else {
                    guiListener.msg(Connection.serverError(url), Constant.ERROR_MSG);
                }
            }
            throw new ConnectionException();
        }
        return 21;
    }

    @Override
    protected void addVideo(String titleMatch) {
        String yearAndType = Regex.match(titleMatch, 27);
        Video video = new Video(Regex.firstMatch(Regex.match(titleMatch, 23), 628), Regex.match(titleMatch, 25), Regex.firstMatch(yearAndType, 29), isTVShow,
                !Regex.firstMatch(yearAndType, isTVShow ? 591 : 592).isEmpty());
        if (video.title.isEmpty() || video.year.isEmpty() || video.ID.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("video ('" + video.title + "' '" + video.year + "' '" + video.ID + "') is invalid!");
            }
            return;
        }
        video.rating = VideoSearch.rating(Regex.firstMatch(Regex.match(titleMatch, 30), 32));
        String imageLink = Regex.match(titleMatch, 384);
        if (Regex.isMatch(imageLink, 386)) {
            noImageTitles.add(video.ID);
        }
        if (isValid(video, titleMatch) && allBufferVideos.add(video.ID)) {
            videoBuffer.add(video);
        }
    }

    @Override
    protected void checkVideoes(String url) {
    }

    private boolean isValid(Video video, String source) {
        if (video.rating.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("video (" + video.title + ", " + video.year + ") rating is unknown!");
            }
            return true;
        }
        if (!minRating.equals(Constant.ANY) && (video.rating.equals(Constant.NO_RATING) || Double.parseDouble(video.rating) < Double.parseDouble(minRating))) {
            if (Debug.DEBUG) {
                Debug.println("video (" + video.title + ", " + video.year + ") rating is bad!");
            }
            return false;
        }

        if (video.year.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("video (" + video.title + ", " + video.year + ") year is unknown!");
            }
            return true;
        }
        int year = Integer.parseInt(video.year);
        if ((startDate != null && year < startDate.get(Calendar.YEAR)) || (endDate != null && year > endDate.get(Calendar.YEAR))) {
            if (Debug.DEBUG) {
                Debug.println("video (" + video.title + ", " + video.year + ") year is bad!");
            }
            return false;
        }

        if (!genresStr.equals(Constant.ANY)) {
            Collection<String> videoGenres = Regex.matches(Regex.match(source, 416), 418);
            if (videoGenres.isEmpty()) {
                if (Debug.DEBUG) {
                    Debug.println("video (" + video.title + ", " + video.year + ") genre is unknown!");
                }
                return true;
            }

            for (String genre : genres) {
                if (!videoGenres.contains(genre)) {
                    if (Debug.DEBUG) {
                        Debug.println("video (" + video.title + ", " + video.year + ") genre is bad!");
                    }
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected boolean findImage(Video video) {
        return VideoSearch.tooOldOrNonexistentImagePath(video) != null && !noImageTitles.contains(video.ID);
    }

    @Override
    protected Video update(Video video) throws Exception {
        video.summary = Connection.getSourceCode(VideoSearch.url(video), DomainType.VIDEO_INFO);
        return video;
    }

    @Override
    protected boolean noImage(Video video) {
        noImageTitles.add(video.ID);
        return false;
    }
}

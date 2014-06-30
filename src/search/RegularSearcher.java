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
        this.title = Regex.replaceAll(title, Str.get(3), Str.get(4));
        this.genres = Arrays.copyOf(genres, genres.length);
        genresStr = searchStr(genres, Constant.ANY_GENRE, null);
        this.languages = searchStr(languages, Constant.ANY_LANGUAGE, Regex.languages);
        this.countries = searchStr(countries, Constant.ANY_COUNTRY, Regex.countries);
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

    private static String searchStr(String[] searchStrs, String anyStr, Map<String, String> searchStrCodes) {
        StringBuilder searchStr = new StringBuilder(512);
        for (int i = 0; i < searchStrs.length; i++) {
            if (searchStrs[i].equals(anyStr)) {
                return anyStr;
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

        String titleID = Regex.match(titleLink, Str.get(628));
        if (titleID.isEmpty()) {
            return;
        }

        String sourceCode = Connection.getSourceCode(titleLink, DomainType.VIDEO_INFO);
        String[] titleParts = VideoSearch.getImdbTitleParts(sourceCode);
        if (titleParts[0].isEmpty() || titleParts[1].isEmpty()) {
            return;
        }

        if (!VideoSearch.isImdbVideoType(sourceCode, isTVShow)) {
            if (Debug.DEBUG) {
                Debug.println("Wrong video type (NOT a " + (isTVShow ? "TV show" : "movie") + "): '" + titleParts[0] + "' '" + titleParts[1] + '\'');
            }
            return;
        }

        Video video = new Video(titleID, titleParts[0], titleParts[1], isTVShow, VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590));
        video.oldTitle = VideoSearch.getOldTitle(sourceCode);
        video.rating = VideoSearch.rating(Regex.match(sourceCode, Str.get(127), Str.get(128)));
        video.summary = VideoSearch.getSummary(sourceCode, isTVShow);
        video.imageLink = Regex.match(sourceCode, Str.get(190), Str.get(191));
        if (!video.imageLink.isEmpty()) {
            VideoSearch.saveImage(video);
        }

        allVideos.add(video.ID);
        allBufferVideos.add(video.ID);
        guiListener.newResult(VideoSearch.toTableRow(guiListener, video, true));
        incrementProgress();
        isInitialSearchSuccessful = true;
    }

    @Override
    protected boolean hasNextPage(int nextPage) {
        return currSourceCode.contains(Str.get(5));
    }

    @Override
    protected String getUrl(int page) throws Exception {
        int maxNumResultsPerSearch = Integer.parseInt(Str.get(503));
        int numResultsPerPage = (numResultsPerSearch > maxNumResultsPerSearch ? maxNumResultsPerSearch : numResultsPerSearch);
        String urlCountries = countries.equals(Constant.ANY_COUNTRY) ? Str.get(179) : Str.get(180) + countries;
        String urlGenres = (genresStr.equals(Constant.ANY_GENRE) ? Str.get(141) : (Str.get(7) + Regex.replaceAll(genresStr.toLowerCase(Locale.ENGLISH), Str.get(8),
                Str.get(9))));
        String urlLanguages = languages.equals(Constant.ANY_LANGUAGE) ? Str.get(181) : Str.get(182) + languages;
        String urlReleaseDates = startDateStr.isEmpty() && endDateStr.isEmpty() ? Str.get(240) : Str.get(14) + startDateStr + Str.get(15) + endDateStr;
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
        guiListener.msg(e.getClass().equals(ProxyException.class) ? e.getMessage() : Connection.error("", "", url), Constant.ERROR_MSG);
        return false;
    }

    @Override
    protected int getTitleRegexIndex(String url) throws Exception {
        if (!Regex.match(currSourceCode, Str.get(145)).isEmpty()) {
            Connection.removeFromCache(url);
            if (!isCancelled()) {
                if (isInitialSearchSuccessful) {
                    throw new ConnectionException(Connection.error(url));
                } else {
                    guiListener.msg(Connection.error(url), Constant.ERROR_MSG);
                }
            }
            throw new ConnectionException();
        }
        return 21;
    }

    @Override
    protected void addVideo(String titleMatch) {
        String yearAndType = Regex.match(titleMatch, Str.get(27), Str.get(28));
        Video video = new Video(Regex.match(Regex.match(titleMatch, Str.get(23), Str.get(24)), Str.get(628)), Regex.match(titleMatch, Str.get(25), Str.get(26)),
                Regex.match(yearAndType, Str.get(29)), isTVShow, !Regex.match(yearAndType, Str.get(isTVShow ? 591 : 592)).isEmpty());
        if (video.title.isEmpty() || video.year.isEmpty() || video.ID.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("video ('" + video.title + "' '" + video.year + "' '" + video.ID + "') is invalid!");
            }
            return;
        }
        video.rating = VideoSearch.rating(Regex.match(Regex.match(titleMatch, Str.get(30), Str.get(31)), Str.get(32)));
        String imageLink = Regex.match(titleMatch, Str.get(384), Str.get(385));
        if (Regex.isMatch(imageLink, Str.get(386))) {
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
        if (!minRating.equals(Constant.ANY) && (video.rating.equals("-") || Double.parseDouble(video.rating) < Double.parseDouble(minRating))) {
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

        if (!genresStr.equals(Constant.ANY_GENRE)) {
            Collection<String> videoGenres = Regex.matches(Regex.match(source, Str.get(416), Str.get(417)), Str.get(418), Str.get(419));
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

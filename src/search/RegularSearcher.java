package search;

import debug.Debug;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import listener.GuiListener;
import main.Str;
import search.util.VideoSearch;
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
        this.genresStr = searchStr(genres, Constant.ANY_GENRE, null);
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

    private String dateToStr(Calendar date) {
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
        } else if (startDate != null && endDate == null) {
            year = String.valueOf(startDate.get(Calendar.YEAR));
        } else if (startDate == null && endDate != null) {
            year = String.valueOf(endDate.get(Calendar.YEAR));
        }

        String titleLink = VideoSearch.getTitleLink(title, year);
        if (isCancelled() || titleLink == null) {
            return;
        }

        String titleID = Regex.replaceAll(Regex.replaceFirst(titleLink, Str.get(447), Str.get(448)), Str.get(449), Str.get(450));
        if (titleID.isEmpty()) {
            return;
        }

        String sourceCode = Connection.getSourceCode(titleLink, Connection.VIDEO_INFO);
        if (isCancelled()) {
            return;
        }

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

        String rating = Regex.match(sourceCode, Str.get(127), Str.get(128));
        String dirtyOldTitle = Video.getDirtyOldTitle(sourceCode);
        String summary = Video.getSummary(sourceCode, isTVShow) + (dirtyOldTitle == null ? "" : Constant.SEPARATOR2 + dirtyOldTitle);
        String imageLink = Regex.match(sourceCode, Str.get(190), Str.get(191));
        if (imageLink.isEmpty()) {
            imageLink = Constant.NULL;
        }
        boolean isTVShowAndMovie = VideoSearch.isImdbVideoType(sourceCode, isTVShow ? 589 : 590);

        Video video = new Video(titleID, titleParts[0], titleParts[1], rating.isEmpty() ? "-" : rating, summary, imageLink, null, null, isTVShow,
                isTVShowAndMovie);
        video.originalTitle = dirtyOldTitle;
        if (!imageLink.equals(Constant.NULL)) {
            video.saveImage();
        }

        allVideos.add(video.id);
        guiListener.newResult(video.toTableRow(guiListener, false, true));
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
        String urlTitle = (title.isEmpty() ? Str.get(142) : Str.get(10) + URLEncoder.encode(Str.clean(title), Constant.UTF8));
        String urlTitleType = Str.get(16) + Str.get(isTVShow ? 18 : 17);
        String urlUserRating = (minRating.equals(Constant.ANY) ? Str.get(143) : (Str.get(11) + minRating + Str.get(12)));
        return Str.get(6) + numResultsPerPage + urlCountries + urlGenres + urlLanguages + urlReleaseDates + Str.get(19) + urlStart + urlTitle + urlTitleType
                + urlUserRating + Str.get(20);
    }

    @Override
    protected int connectionType() {
        return Connection.VIDEO_INFO;
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
        String videoTitle = Regex.match(titleMatch, Str.get(25), Str.get(26));
        String yearAndType = Regex.match(titleMatch, Str.get(27), Str.get(28));
        String year = Regex.match(yearAndType, Str.get(29));
        String summaryLink = Str.get(22) + Regex.match(titleMatch, Str.get(23), Str.get(24));
        String titleID = Regex.replaceAll(Regex.replaceFirst(summaryLink, Str.get(447), Str.get(448)), Str.get(449), Str.get(450));
        if (videoTitle.isEmpty() || year.isEmpty() || titleID.isEmpty()) {
            if (Debug.DEBUG) {
                Debug.println("video ('" + videoTitle + "' '" + year + "' '" + titleID + "') is invalid!");
            }
            return;
        }
        String rating = Regex.match(Regex.match(titleMatch, Str.get(30), Str.get(31)), Str.get(32));
        String imageLink = Regex.match(titleMatch, Str.get(384), Str.get(385));
        boolean isTVShowAndMovie = !Regex.match(yearAndType, Str.get(isTVShow ? 591 : 592)).isEmpty();

        Video video = new Video(titleID, videoTitle, year, rating.isEmpty() ? "-" : rating, summaryLink, Constant.NULL, null, null, isTVShow, isTVShowAndMovie);
        if (Regex.isMatch(imageLink, Str.get(386))) {
            noImageTitles.add(video.id);
        }
        if (!videoBuffer.contains(video) && isValid(video, titleMatch)) {
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
        return video.saveImagePath() != null && !noImageTitles.contains(video.id);
    }

    @Override
    protected String getSourceCode(Video video) throws Exception {
        return Connection.getSourceCode(video.summaryLink, Connection.VIDEO_INFO);
    }

    @Override
    protected boolean noImage(Video video) {
        noImageTitles.add(video.id);
        return false;
    }
}

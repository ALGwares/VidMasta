package search;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import javax.swing.SwingWorker;
import listener.DomainType;
import listener.GuiListener;
import listener.Video;
import search.util.VideoSearch;
import str.Str;
import util.Connection;
import util.ConnectionException;
import util.Regex;
import util.RunnableUtil;

public abstract class AbstractSearcher extends AbstractSwingWorker {

    protected GuiListener guiListener;
    protected int numResultsPerSearch, currSearchPage;
    protected boolean isTVShow;
    private AtomicInteger numResults, numSearchResults;
    private boolean isNewSearch = true;
    protected Set<String> allVideos, allBufferVideos;
    protected List<Video> videoBuffer;
    protected String currSourceCode, prevSourceCode;
    private SwingWorker<?, ?> prefetcher;
    private final int SLEEP = Integer.parseInt(Str.get(166));

    protected AbstractSearcher(GuiListener guiListener, int numResultsPerSearch, boolean isTVShow) {
        this.guiListener = guiListener;
        this.numResultsPerSearch = numResultsPerSearch;
        this.isTVShow = isTVShow;
        numResults = new AtomicInteger();
        numSearchResults = new AtomicInteger();
        allVideos = new ConcurrentSkipListSet<String>();
        allBufferVideos = new HashSet<String>(numResultsPerSearch);
        videoBuffer = new ArrayList<Video>(numResultsPerSearch);
    }

    protected AbstractSearcher(AbstractSearcher searcher) {
        guiListener = searcher.guiListener;
        numResultsPerSearch = searcher.numResultsPerSearch;
        currSearchPage = searcher.currSearchPage;
        isTVShow = searcher.isTVShow;
        numResults = searcher.numResults;
        numSearchResults = searcher.numSearchResults;
        isNewSearch = searcher.isNewSearch;
        allVideos = searcher.allVideos;
        allBufferVideos = searcher.allBufferVideos;
        videoBuffer = searcher.videoBuffer;
        currSourceCode = searcher.currSourceCode;
        prevSourceCode = searcher.prevSourceCode;
        prefetcher = searcher.prefetcher;
    }

    @Override
    protected Object doInBackground() {
        guiListener.searchStarted();
        if (isNewSearch && isNewSearch()) {
            guiListener.newSearch(numResultsPerSearch, isTVShow);
            numResults.set(0);
            numSearchResults.set(0);

            try {
                initialSearch();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }

            isNewSearch = false;
        } else if (guiListener.oldSearch(numResultsPerSearch)) {
            numSearchResults.set(0);
        }

        if (!isCancelled()) {
            try {
                updateTable();
            } catch (Exception e) {
                restore();
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
        }

        guiListener.searchStopped();
        workDone();
        guiListener.loading(false);
        return null;
    }

    private void updateTable() throws Exception {
        while (!isCancelled() && hasNextSearchPage()) {
            searchNextPage();
            if (isCancelled() || numSearchResults.get() >= numResultsPerSearch) {
                break;
            }

            if (SLEEP > 0 && videoBuffer.isEmpty() && hasNextSearchPage()) {
                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
            }
        }

        if (!isCancelled()) {
            guiListener.searchProgressMaxOut();
        }

        if (isCancelled()) {
            restore();
        } else if (hasNextSearchPage()) {
            guiListener.moreResults(true);
        }
    }

    private void restore() {
        restoreToPrev();
        for (Video video : videoBuffer) {
            allBufferVideos.remove(video.ID);
        }
        videoBuffer.clear();
        guiListener.moreResults(hasNextSearchPage());
    }

    protected abstract void initialSearch() throws Exception;

    protected abstract int anotherPageRegexIndex();

    protected abstract boolean addCurrVideos();

    protected abstract String getUrl(int page) throws Exception;

    protected abstract DomainType domainType();

    protected abstract boolean connectionException(String url, ConnectionException e);

    protected abstract int getTitleRegexIndex(String url) throws Exception;

    protected abstract void addVideo(String titleMatch);

    protected abstract void checkVideoes(String url) throws Exception;

    protected abstract boolean findImage(Video video);

    protected abstract Video update(Video video) throws Exception;

    protected abstract boolean noImage(Video video);

    private boolean isNewSearch() {
        return currSourceCode == null;
    }

    private void restoreToPrev() {
        currSourceCode = prevSourceCode;
    }

    private boolean hasAnotherPage() {
        return !Regex.firstMatch(currSourceCode, Str.get(anotherPageRegexIndex())).isEmpty();
    }

    private boolean hasNextSearchPage() {
        return isNewSearch() || !videoBuffer.isEmpty() || hasAnotherPage();
    }

    private void searchNextPage() throws Exception {
        if (videoBuffer.isEmpty()) {
            initCurrVideos();
            if (isCancelled()) {
                return;
            }
        }

        int numVideos = videoBuffer.size(), numResultsLeft = numResultsPerSearch - numSearchResults.get();
        if (numResultsLeft >= 0 && numVideos > numResultsLeft) {
            numVideos = numResultsLeft;
        }
        Collection<Video> videos = videoBuffer.subList(0, numVideos);
        Collection<SearcherHelper> searchHelpers = new ArrayList<SearcherHelper>(numVideos);

        for (Video video : videos) {
            searchHelpers.add(new SearcherHelper(video, findImage(video)));
        }

        RunnableUtil.runAndWaitFor(searchHelpers);
        if (isCancelled()) {
            return;
        }

        videos.clear();
        if (videoBuffer.isEmpty()) {
            currSearchPage++;
        }
    }

    protected void initCurrVideos() throws Exception {
        if (addCurrVideos()) {
            return;
        }

        prevSourceCode = currSourceCode;
        String url = getUrl(currSearchPage);

        stopPrefetcher();

        try {
            currSourceCode = Connection.getSourceCode(url, domainType());
        } catch (ConnectionException e) {
            if (!isCancelled() && connectionException(url, e)) {
                return;
            }
            throw new ConnectionException();
        }

        startPrefetcher();

        int titleRegexIndex = getTitleRegexIndex(url);
        if (titleRegexIndex == -1) {
            return;
        }

        videoBuffer.clear();

        Matcher titleMatcher = Regex.matcher(titleRegexIndex, currSourceCode);
        while (!titleMatcher.hitEnd()) {
            if (isCancelled()) {
                return;
            }
            if (titleMatcher.find()) {
                addVideo(currSourceCode.substring(titleMatcher.end()));
            }
        }

        checkVideoes(url);
    }

    private void startPrefetcher() {
        if (!hasAnotherPage()) {
            return;
        }

        prefetcher = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                if (Debug.DEBUG) {
                    Debug.println("prefetching search page " + (currSearchPage + 2));
                }
                try {
                    Connection.getSourceCode(getUrl(currSearchPage + 1), domainType(), false);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
                return null;
            }
        };
        prefetcher.execute();
    }

    private void stopPrefetcher() {
        if (prefetcher != null) {
            prefetcher.cancel(true);
        }
    }

    protected void incrementProgress() {
        guiListener.searchNumResultsUpdate(numResults.incrementAndGet());
        numSearchResults.incrementAndGet();
        guiListener.searchProgressIncrement();
    }

    @Override
    protected void process(List<Object[]> rows) {
        guiListener.newResults(rows);
    }

    private class SearcherHelper extends SwingWorker<Object, Object> {

        private Video video;
        private boolean findImage;

        SearcherHelper(Video video, boolean findImage) {
            this.video = video;
            this.findImage = findImage;
        }

        @Override
        protected Object doInBackground() {
            try {
                search();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.error(e);
                }
            }
            return null;
        }

        private void search() throws Exception {
            if (isCancelled()) {
                return;
            }

            if (findImage) {
                video = update(video);
                if (video == null) {
                    return;
                }

                String sourceCode = video.summary;
                video.oldTitle = VideoSearch.getOldTitle(sourceCode);
                video.summary = VideoSearch.getSummary(sourceCode, video.IS_TV_SHOW);
                video.imageLink = Regex.match(sourceCode, 190);
                if (video.imageLink.isEmpty()) {
                    if (noImage(video)) {
                        return;
                    }
                } else {
                    VideoSearch.saveImage(video);
                }
            }

            if (isCancelled() || !allVideos.add(video.ID)) {
                return;
            }

            Object[] row = VideoSearch.toTableRow(guiListener, video, false);
            if (isCancelled()) {
                allVideos.remove(video.ID);
                return;
            }
            AbstractSearcher.this.publish(row);
            synchronized (SearcherHelper.class) {
                incrementProgress();
            }
        }
    }
}

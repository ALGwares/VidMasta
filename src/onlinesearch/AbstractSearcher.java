package onlinesearch;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import onlinesearch.util.SwingWorkerUtil;
import util.Connection;
import util.ConnectionException;
import util.Constant;
import util.Regex;

public abstract class AbstractSearcher extends AbstractSwingWorker {

    protected GuiListener guiListener;
    protected int numResultsPerSearch, currSearchPage;
    protected boolean isTVShow;
    protected AtomicInteger numResults, numSearchResults;
    private boolean isNewSearch = true;
    protected Collection<String> allVideos;
    protected List<Video> videoBuffer;
    protected String currSourceCode;
    private String prevSourceCode;
    private SwingWorker<?, ?> prefetcher;
    private static final int SLEEP = Integer.parseInt(Str.get(166));

    public AbstractSearcher(GuiListener guiListener, int numResultsPerSearch, boolean isTVShow) {
        this.guiListener = guiListener;
        this.numResultsPerSearch = numResultsPerSearch;
        this.isTVShow = isTVShow;
        numResults = new AtomicInteger();
        numSearchResults = new AtomicInteger();
        allVideos = new ConcurrentSkipListSet<String>();
        videoBuffer = new ArrayList<Video>(numResultsPerSearch);
    }

    public AbstractSearcher(AbstractSearcher searcher) {
        guiListener = searcher.guiListener;
        numResultsPerSearch = searcher.numResultsPerSearch;
        currSearchPage = searcher.currSearchPage;
        isTVShow = searcher.isTVShow;
        numResults = searcher.numResults;
        numSearchResults = searcher.numSearchResults;
        isNewSearch = searcher.isNewSearch;
        allVideos = searcher.allVideos;
        videoBuffer = searcher.videoBuffer;
        currSourceCode = searcher.currSourceCode;
        prevSourceCode = searcher.prevSourceCode;
        prefetcher = searcher.prefetcher;
    }

    @Override
    protected Object doInBackground() {
        guiListener.searchStarted();
        if (isNewSearch && isNewSearch()) {
            guiListener.newSearch(numResultsPerSearch);
            numResults.set(0);
            numSearchResults.set(0);

            try {
                initialSearch();
            } catch (Exception e) {
                if (!isCancelled()) {
                    guiListener.connectionError(e);
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
                    guiListener.connectionError(e);
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

            if (videoBuffer.isEmpty() && hasNextSearchPage()) {
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
        videoBuffer.clear();
        guiListener.moreResults(hasNextSearchPage());
    }

    protected abstract void initialSearch() throws Exception;

    protected abstract boolean hasNextPage(int nextPage);

    protected abstract String getUrl(int page) throws Exception;

    protected abstract int connectionType();

    protected abstract boolean connectionException(String url, ConnectionException e);

    protected abstract int getTitleRegexIndex(String url) throws Exception;

    protected abstract void addVideo(String titleMatch);

    protected abstract void checkVideoes(String url) throws Exception;

    protected abstract boolean findImage(Video video);

    protected abstract String getSourceCode(Video video) throws Exception;

    protected abstract boolean noImage(Video video);

    private boolean isNewSearch() {
        return currSourceCode == null;
    }

    private void restoreToPrev() {
        currSourceCode = prevSourceCode;
    }

    private boolean hasNextSearchPage() {
        return isNewSearch() || !videoBuffer.isEmpty() || hasNextPage(currSearchPage);
    }

    private void searchNextPage() throws Exception {
        if (!hasNextSearchPage()) {
            return;
        }

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
        List<Video> subList = videoBuffer.subList(0, numVideos);
        Iterable<Video> videos = new ArrayList<Video>(subList);
        subList.clear();

        Collection<SearcherHelper> searchHelpers = new ArrayList<SearcherHelper>(numVideos);

        for (Video video : videos) {
            searchHelpers.add(new SearcherHelper(video, findImage(video)));
        }

        SwingWorkerUtil.execute(this, searchHelpers, numVideos);
        if (isCancelled()) {
            return;
        }

        if (videoBuffer.isEmpty()) {
            currSearchPage++;
        }
    }

    private void initCurrVideos() throws Exception {
        prevSourceCode = currSourceCode;
        String url = getUrl(currSearchPage);

        stopPrefetcher();

        try {
            currSourceCode = Connection.getSourceCode(url, connectionType());
        } catch (ConnectionException e) {
            if (!isCancelled()) {
                if (connectionException(url, e)) {
                    return;
                }
            }
            throw new ConnectionException();
        }

        startPrefetcher();

        int titleRegexIndex = getTitleRegexIndex(url);
        if (titleRegexIndex == -1) {
            return;
        }

        videoBuffer.clear();

        Matcher titleMatcher = Regex.matcher(Str.get(titleRegexIndex), currSourceCode);
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
        final int nextPage = currSearchPage + 1;
        if (!hasNextPage(nextPage)) {
            return;
        }

        prefetcher = new SwingWorker<Object, Object[]>() {
            @Override
            protected Object doInBackground() throws Exception {
                if (Debug.DEBUG) {
                    Debug.println("prefetching search page " + (nextPage + 1));
                }
                try {
                    Connection.getSourceCode(getUrl(nextPage), connectionType(), false);
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

    private class SearcherHelper extends SwingWorker<Object, Object[]> {

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
                    guiListener.connectionError(e);
                }
            }
            return null;
        }

        @Override
        protected void process(List<Object[]> rows) {
            guiListener.newResults(rows);
        }

        private void search() throws Exception {
            if (isCancelled()) {
                return;
            }

            if (findImage) {
                String sourceCode = getSourceCode(video);
                if (isCancelled() || sourceCode == null) {
                    return;
                }

                video.originalTitle = Video.getDirtyOldTitle(sourceCode);
                video.summaryLink = Video.getSummary(sourceCode, video.isTVShow) + (video.originalTitle == null ? "" : Constant.SEPARATOR2 + video.originalTitle);
                video.imageLink = Regex.match(sourceCode, Str.get(190), Str.get(191));
                if (video.imageLink.isEmpty()) {
                    video.imageLink = Constant.NULL;
                    if (noImage(video)) {
                        return;
                    }
                } else {
                    video.saveImage();
                }
            }

            if (isCancelled() || !allVideos.add(video.id)) {
                return;
            }

            Object[] row = video.toTableRow(guiListener, !findImage, false);
            if (isCancelled()) {
                allVideos.remove(video.id);
                return;
            }
            publish(row);
            synchronized (SearcherHelper.class) {
                incrementProgress();
            }
        }
    }
}

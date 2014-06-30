package search.download;

import debug.Debug;
import gui.AbstractSwingWorker;
import listener.ContentType;
import util.Connection;

public class Prefetcher extends AbstractSwingWorker {

    private VideoFinder videoFinder;
    private ContentType[] fetchOrder;

    public Prefetcher(VideoFinder videoFinder) {
        this.videoFinder = new VideoFinder(videoFinder.CONTENT_TYPE, true, videoFinder);
        if (videoFinder.CONTENT_TYPE == ContentType.SUMMARY) {
            fetchOrder = new ContentType[]{ContentType.TRAILER, ContentType.DOWNLOAD1, ContentType.STREAM1, ContentType.DOWNLOAD2, ContentType.STREAM2};
        } else if (videoFinder.CONTENT_TYPE == ContentType.TRAILER) {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD1, ContentType.STREAM1, ContentType.DOWNLOAD2, ContentType.STREAM2};
        } else if (videoFinder.CONTENT_TYPE == ContentType.DOWNLOAD1) {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD2, ContentType.STREAM1, ContentType.STREAM2, ContentType.TRAILER};
        } else if (videoFinder.CONTENT_TYPE == ContentType.DOWNLOAD2) {
            fetchOrder = new ContentType[]{ContentType.STREAM1, ContentType.STREAM2, ContentType.DOWNLOAD1, ContentType.TRAILER};
        } else if (videoFinder.CONTENT_TYPE == ContentType.DOWNLOAD3) {
            fetchOrder = new ContentType[]{ContentType.STREAM1, ContentType.STREAM2, ContentType.TRAILER};
        } else if (videoFinder.CONTENT_TYPE == ContentType.STREAM1) {
            fetchOrder = new ContentType[]{ContentType.STREAM2, ContentType.DOWNLOAD1, ContentType.DOWNLOAD2, ContentType.TRAILER};
        } else {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD1, ContentType.DOWNLOAD2, ContentType.STREAM1, ContentType.TRAILER};
        }
    }

    @Override
    protected Object doInBackground() {
        try {
            prefetch();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
        workDone();
        return null;
    }

    private void prefetch() {
        for (ContentType contentType : fetchOrder) {
            if (isCancelled()) {
                return;
            }
            if (Connection.downloadLinkInfoFail() && (contentType == ContentType.DOWNLOAD1 || contentType == ContentType.DOWNLOAD2)) {
                continue;
            }
            try {
                if (Debug.DEBUG) {
                    Debug.println("prefetching " + contentType.name() + " links");
                }
                (new VideoFinder(contentType, true, videoFinder)).prefetch();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public boolean isForRow(int row) {
        return videoFinder.ROW == row;
    }

    public boolean isForContentType(ContentType contentType) {
        return videoFinder.CONTENT_TYPE == contentType;
    }
}

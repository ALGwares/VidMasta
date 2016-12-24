package search.download;

import debug.Debug;
import listener.ContentType;
import util.Connection;
import util.Worker;

public class Prefetcher extends Worker {

    private VideoFinder videoFinder;
    private ContentType[] fetchOrder;

    public Prefetcher(VideoFinder videoFinder) {
        this.videoFinder = new VideoFinder(videoFinder.CONTENT_TYPE, videoFinder);
        if (videoFinder.CONTENT_TYPE == ContentType.SUMMARY) {
            fetchOrder = new ContentType[]{ContentType.TRAILER, ContentType.DOWNLOAD1, ContentType.DOWNLOAD2};
        } else if (videoFinder.CONTENT_TYPE == ContentType.TRAILER) {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD1, ContentType.DOWNLOAD2};
        } else if (videoFinder.CONTENT_TYPE == ContentType.DOWNLOAD1) {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD2, ContentType.TRAILER};
        } else if (videoFinder.CONTENT_TYPE == ContentType.DOWNLOAD2) {
            fetchOrder = new ContentType[]{ContentType.DOWNLOAD1, ContentType.TRAILER};
        } else {
            fetchOrder = new ContentType[]{ContentType.TRAILER};
        }
    }

    @Override
    protected void doWork() {
        try {
            prefetch();
        } catch (Exception e) {
            if (Debug.DEBUG) {
                Debug.print(e);
            }
        }
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
                (new VideoFinder(contentType, videoFinder)).prefetch();
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

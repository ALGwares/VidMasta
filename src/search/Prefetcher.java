package search;

import debug.Debug;
import gui.AbstractSwingWorker;
import search.download.VideoFinder;
import util.Connection;
import util.Constant;

public class Prefetcher extends AbstractSwingWorker {

    private VideoFinder videoFinder;
    private int[] fetchOrder;

    public Prefetcher(VideoFinder videoFinder) {
        this.videoFinder = new VideoFinder(videoFinder.action, videoFinder);
        if (videoFinder.action == Constant.SUMMARY_ACTION) {
            fetchOrder = new int[]{Constant.TRAILER_ACTION, Constant.TORRENT1_ACTION, Constant.STREAM1_ACTION, Constant.TORRENT2_ACTION, Constant.STREAM2_ACTION};
        } else if (videoFinder.action == Constant.TRAILER_ACTION) {
            fetchOrder = new int[]{Constant.TORRENT1_ACTION, Constant.STREAM1_ACTION, Constant.TORRENT2_ACTION, Constant.STREAM2_ACTION};
        } else if (videoFinder.action == Constant.TORRENT1_ACTION) {
            fetchOrder = new int[]{Constant.TORRENT2_ACTION, Constant.STREAM1_ACTION, Constant.STREAM2_ACTION, Constant.TRAILER_ACTION};
        } else if (videoFinder.action == Constant.TORRENT2_ACTION) {
            fetchOrder = new int[]{Constant.STREAM1_ACTION, Constant.STREAM2_ACTION, Constant.TORRENT1_ACTION, Constant.TRAILER_ACTION};
        } else if (videoFinder.action == Constant.TORRENT3_ACTION) {
            fetchOrder = new int[]{Constant.STREAM1_ACTION, Constant.STREAM2_ACTION, Constant.TRAILER_ACTION};
        } else if (videoFinder.action == Constant.STREAM1_ACTION) {
            fetchOrder = new int[]{Constant.STREAM2_ACTION, Constant.TORRENT1_ACTION, Constant.TORRENT2_ACTION, Constant.TRAILER_ACTION};
        } else {
            fetchOrder = new int[]{Constant.TORRENT1_ACTION, Constant.TORRENT2_ACTION, Constant.STREAM1_ACTION, Constant.TRAILER_ACTION};
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
        for (int action : fetchOrder) {
            if (isCancelled()) {
                return;
            }
            if (Connection.downloadLinkInfoFail() && (action == Constant.TORRENT1_ACTION || action == Constant.TORRENT2_ACTION)) {
                continue;
            }
            try {
                if (Debug.DEBUG) {
                    Debug.println("prefetching 'action " + action + "' links");
                }
                (new VideoFinder(action, videoFinder)).prefetch();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public boolean isForRow(int row) {
        return videoFinder.row == row;
    }

    public boolean isForAction(int action) {
        return videoFinder.action == action;
    }
}

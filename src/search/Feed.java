package search;

import debug.Debug;
import java.io.File;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import torrent.Magnet;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;

public class Feed {

    private static final boolean CAN_OFFER = Boolean.parseBoolean(Str.get(601));
    private static final File OFFER_COUNT = new File(Constant.APP_DIR + "offer");
    private static SwingWorker<Object, Object> offerer;

    static void offer(final GuiListener guiListener) {
        if (!CAN_OFFER) {
            return;
        }
        synchronized (Feed.class) {
            if (offerer != null) {
                return;
            }
            (offerer = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() {
                    try {
                        offerHelper(guiListener);
                    } catch (Exception e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                    return null;
                }
            }).execute();
        }
    }

    private static void offerHelper(GuiListener guiListener) throws Exception {
        int offerCount, offerShowCount;
        if (OFFER_COUNT.exists()) {
            String[] offerCounts = IO.read(OFFER_COUNT).split(Constant.NEWLINE);
            offerCount = Integer.parseInt(offerCounts[0]);
            if (offerCount == -1) {
                return;
            }
            offerShowCount = Integer.parseInt(offerCounts[1]);
        } else {
            offerCount = 0;
            offerShowCount = Integer.parseInt(Str.get(602));
        }

        boolean showOffer = (++offerCount % offerShowCount == 0);
        if (showOffer) {
            offerCount = ++offerShowCount;
        }
        IO.write(OFFER_COUNT, offerCount + Constant.NEWLINE + offerShowCount);
        if (!showOffer) {
            return;
        }

        String ipAddress = Regex.match(Connection.getSourceCode(Str.get(603), Connection.UPDATE, false, false, true, true), Str.get(604));
        if (ipAddress.isEmpty() || neverOfferAgain(Magnet.isIpBlocked(ipAddress))) {
            return;
        }

        boolean[] subscribe = guiListener.confirmFeedOffer();
        neverOfferAgain(!subscribe[1]);
        if (subscribe[0]) {
            guiListener.browserNotification(Str.get(605), Str.get(606), -1);
            Connection.browse(Str.get(607));
        }
    }

    static boolean neverOfferAgain(boolean neverOfferAgain) {
        if (neverOfferAgain) {
            try {
                IO.write(OFFER_COUNT, "-1" + Constant.NEWLINE + Integer.MAX_VALUE);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
        return neverOfferAgain;
    }

    private Feed() {
    }
}

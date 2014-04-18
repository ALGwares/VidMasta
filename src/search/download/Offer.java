package search.download;

import debug.Debug;
import java.io.File;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.SwingWorker;
import listener.GuiListener;
import main.Str;
import torrent.Magnet;
import util.Connection;
import util.Constant;
import util.IO;
import util.Regex;

public class Offer {

    private static final boolean CAN_OFFER = Boolean.parseBoolean(Str.get(611));
    private static final File OFFER = new File(Constant.APP_DIR + "offer2");
    private static final Set<String> titles = new CopyOnWriteArraySet<String>();

    static void offer(final GuiListener guiListener, final int offerIndex, final String title) {
        if (!CAN_OFFER || OFFER.exists() || !titles.add(title)) {
            return;
        }
        synchronized (Offer.class) {
            (new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() {
                    try {
                        String ipAddress = Regex.match(Connection.getSourceCode(Str.get(612), Connection.UPDATE, false, false, true, true), Str.get(604));
                        if (ipAddress.isEmpty() || neverOfferAgain(Magnet.isIpBlocked(ipAddress))) {
                            return null;
                        }

                        boolean[] confirmation = guiListener.confirmOffer(offerIndex, title);
                        neverOfferAgain(!confirmation[1]);
                        if (confirmation[0]) {
                            guiListener.browserNotification(Str.get(613), Str.get(614), -1);
                            Connection.browse(Str.get(615));
                        }
                    } catch (Exception e) {
                        if (Debug.DEBUG) {
                            Debug.print(e);
                        }
                    }
                    return null;
                }

                private boolean neverOfferAgain(boolean neverOfferAgain) {
                    if (neverOfferAgain) {
                        IO.fileOp(OFFER, IO.MK_FILE);
                    }
                    return neverOfferAgain;
                }
            }).execute();
        }
    }

    private Offer() {
    }
}

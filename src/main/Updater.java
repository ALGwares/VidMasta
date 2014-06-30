package main;

import debug.Debug;
import gui.AbstractSwingWorker;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import listener.DomainType;
import listener.GuiListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.IO;

class Updater extends AbstractSwingWorker {

    private GuiListener guiListener;
    private boolean silent;

    Updater(GuiListener guiListener, boolean silent) {
        this.guiListener = guiListener;
        this.silent = silent;
    }

    @Override
    protected Object doInBackground() {
        guiListener.updateStarted();
        if (silent) {
            Str.update(false, guiListener);
            (new AppUpdater()).update(guiListener);
            try {
                updateBitTorrentClientCertificate();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        } else {
            (new AppUpdater()).update(true, guiListener);
            Str.update(true, guiListener);
        }
        guiListener.updateStopped();
        workDone();
        return null;
    }

    private static void updateBitTorrentClientCertificate() throws Exception {
        if (Str.get(598).isEmpty()) {
            return;
        }
        String bitTorrentClientCertificate = Constant.APP_DIR + Str.get(597) + ".cer";
        File bitTorrentClientCertificateFile = new File(bitTorrentClientCertificate);
        if (bitTorrentClientCertificateFile.exists()) {
            return;
        }

        Connection.saveData(Str.get(598), bitTorrentClientCertificate, DomainType.UPDATE, false);

        KeyStore trustedCertificatesKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        File trustedCertificates = new File(IO.parentDir(Constant.APP_DIR) + Str.get(Constant.WINDOWS ? 593 : (Constant.MAC ? 594 : 595)).replace(Str.get(596),
                Constant.FILE_SEPARATOR));
        char[] keystorePassword = new char[0];

        if (trustedCertificates.exists()) {
            InputStream is = null;
            try {
                trustedCertificatesKeystore.load(is = new BufferedInputStream(new FileInputStream(trustedCertificates)), keystorePassword);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                IO.close(is);
                IO.fileOp(trustedCertificates, IO.RM_FILE);
                trustedCertificatesKeystore.load(null, keystorePassword);
            } finally {
                IO.close(is);
            }
        } else {
            IO.fileOp(trustedCertificates.getParentFile(), IO.MK_DIR);
            trustedCertificatesKeystore.load(null, keystorePassword);
        }

        trustedCertificatesKeystore.deleteEntry(Str.get(597));
        trustedCertificatesKeystore.setCertificateEntry(Str.get(597), CertificateFactory.getInstance("X.509").generateCertificate(new BufferedInputStream(
                new FileInputStream(bitTorrentClientCertificateFile))));

        OutputStream os = null;
        try {
            trustedCertificatesKeystore.store(os = new BufferedOutputStream(new FileOutputStream(trustedCertificates)), keystorePassword);
        } finally {
            IO.close(os);
        }
    }
}

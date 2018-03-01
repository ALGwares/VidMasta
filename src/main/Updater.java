package main;

import debug.Debug;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import listener.DomainType;
import listener.GuiListener;
import str.Str;
import util.Connection;
import util.Constant;
import util.IO;
import util.Worker;

class Updater extends Worker {

    private GuiListener guiListener;
    private boolean silent;

    Updater(GuiListener guiListener, boolean silent) {
        this.guiListener = guiListener;
        this.silent = silent;
    }

    @Override
    protected void doWork() {
        guiListener.updateStarted();
        if (silent) {
            //Str.update(false, guiListener);
            //(new AppUpdater()).update(guiListener);
            try {
                updateJavaDeployment();
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
    }

    private static void updateJavaDeployment() throws Exception {
        boolean skipSites = Str.get(637).isEmpty(), skipCertificates = Str.get(598).isEmpty();
        if (skipSites && skipCertificates) {
            return;
        }

        String[] appDirs = Constant.appDirs();
        Collection<String> appDirsToUpdate = new ArrayList<String>(appDirs.length);
        for (int i = 0; i < appDirs.length; i++) {
            File javaVersionFile = new File(Constant.APP_DIR + "java" + (i + 1) + "Version_2" + Constant.TXT);
            if (!javaVersionFile.exists() || !Constant.JAVA_VERSION.equals(IO.read(javaVersionFile))) {
                IO.write(javaVersionFile, Constant.JAVA_VERSION);
                appDirsToUpdate.add(appDirs[i]);
            }
        }

        if (!skipSites) {
            try {
                updateJavaTrustedSites(appDirsToUpdate);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
        if (!skipCertificates) {
            try {
                updateJavaTrustedCertificatesKeystore(appDirsToUpdate);
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    private static void updateJavaTrustedSites(Iterable<String> appDirs) throws Exception {
        String sites = Str.get(637).replace(Constant.SEPARATOR3, Constant.NEWLINE), trustedSitesPath = Str.get(Constant.WINDOWS ? 638 : (Constant.MAC ? 639
                : 640)).replace(Str.get(641), Constant.FILE_SEPARATOR);

        for (String appDir : appDirs) {
            File trustedSitesFile = new File(appDir + trustedSitesPath);
            if (trustedSitesFile.exists()) {
                String trustedSites = IO.read(trustedSitesFile).trim();
                if (!trustedSites.contains(sites)) {
                    IO.write(trustedSitesFile, (trustedSites.isEmpty() ? sites : trustedSites + Constant.NEWLINE + sites) + Constant.NEWLINE);
                }
            } else {
                IO.fileOp(trustedSitesFile.getParentFile(), IO.MK_DIR);
                IO.write(trustedSitesFile, sites + Constant.NEWLINE);
            }
        }
    }

    private static void updateJavaTrustedCertificatesKeystore(Iterable<String> appDirs) throws Exception {
        String bitTorrentClientCertificate = Constant.APP_DIR + Str.get(634) + ".cer";
        File bitTorrentClientCertificateFile = new File(bitTorrentClientCertificate);
        if (!bitTorrentClientCertificateFile.exists()) {
            Connection.saveData(Str.get(598), bitTorrentClientCertificate, DomainType.UPDATE, false);
        }

        KeyStore trustedCertificatesKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String certificatesPath = Str.get(Constant.WINDOWS ? 593 : (Constant.MAC ? 594 : 595)).replace(Str.get(596), Constant.FILE_SEPARATOR);

        for (String appDir : appDirs) {
            File trustedCertificates = new File(appDir + certificatesPath);
            char[] keystorePassword = new char[0];

            if (trustedCertificates.exists()) {
                InputStream is = null;
                try {
                    trustedCertificatesKeystore.load(is = new BufferedInputStream(new FileInputStream(trustedCertificates)), keystorePassword);
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                    continue;
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
}

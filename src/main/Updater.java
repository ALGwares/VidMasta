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
import util.Regex;

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
                setJavaSecurityLevelToNormal();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
            try {
                updateJavaTrustedCertificatesKeystore();
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

    private static void setJavaSecurityLevelToNormal() throws Exception {
        if (Str.get(632).isEmpty()) {
            return;
        }

        String propertiesPath = Str.get(Constant.WINDOWS ? 629 : (Constant.MAC ? 630 : 631)).replace(Str.get(596), Constant.FILE_SEPARATOR);
        String[] appDirs = Constant.appDirs();

        for (int i = 0; i < appDirs.length; i++) {
            File javaVersionFile = new File(Constant.APP_DIR + "java" + (i + 1) + "Version" + Constant.TXT);
            String javaVersion = System.getProperty("java.version", "");
            if (!javaVersionFile.exists() || !javaVersion.equals(IO.read(javaVersionFile))) {
                IO.write(javaVersionFile, javaVersion);
            } else {
                continue;
            }

            File propertiesFile = new File(appDirs[i] + propertiesPath);
            String newProperty = Str.get(632);
            if (propertiesFile.exists()) {
                String properties = IO.read(propertiesFile);
                String property = Regex.match(properties, Str.get(633));
                if (!property.equals(newProperty)) {
                    IO.write(propertiesFile, property.isEmpty() ? properties + Constant.NEWLINE + newProperty + Constant.NEWLINE : Regex.replaceFirst(properties,
                            Str.get(633), newProperty));
                }
            } else {
                IO.fileOp(propertiesFile.getParentFile(), IO.MK_DIR);
                IO.write(propertiesFile, Str.get(635) + Constant.NEWLINE + newProperty + Constant.NEWLINE);
            }
        }
    }

    private static void updateJavaTrustedCertificatesKeystore() throws Exception {
        if (Str.get(598).isEmpty()) {
            return;
        }
        String bitTorrentClientCertificate = Constant.APP_DIR + Str.get(634) + ".cer";
        File bitTorrentClientCertificateFile = new File(bitTorrentClientCertificate);
        if (bitTorrentClientCertificateFile.exists()) {
            return;
        }

        Connection.saveData(Str.get(598), bitTorrentClientCertificate, DomainType.UPDATE, false);

        KeyStore trustedCertificatesKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String certificatesPath = Str.get(Constant.WINDOWS ? 593 : (Constant.MAC ? 594 : 595)).replace(Str.get(596), Constant.FILE_SEPARATOR);

        for (String appDir : Constant.appDirs()) {
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
}

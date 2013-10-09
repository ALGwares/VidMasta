package main;

import debug.Debug;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.SwingWorker;
import listener.GuiListener;
import util.ConnectionException;
import util.Constant;
import util.IO;
import util.Regex;

public class ConnectionTester extends SwingWorker<Object, Object[]> {

    private GuiListener guiListener;

    ConnectionTester(GuiListener guiListener) {
        this.guiListener = guiListener;
    }

    @Override
    protected Object doInBackground() {
        Str.waitForUpdate();
        Collection<String> restrictedConnectionTypes = new HashSet<String>(8);
        boolean isConnected = false;

        for (String website : Regex.split(Str.get(387), Constant.SEPARATOR2)) {
            String[] websiteParts = Regex.split(website, Constant.SEPARATOR1);
            String site = websiteParts[0];

            HttpURLConnection connection = null;
            InputStream is = null;
            try {
                if (Debug.DEBUG) {
                    Debug.println(site);
                }
                connection = (HttpURLConnection) (new URL(site)).openConnection();
                connection.setRequestProperty("User-Agent", Str.get(301));
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();
                isConnected = true;
                is = connection.getInputStream();
                is.read();
                if (connection.getResponseCode() != 200 || !connection.getResponseMessage().equalsIgnoreCase("OK")) {
                    throw new ConnectionException("restricted website: " + site);
                }
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
                restrictedConnectionTypes.add(websiteParts[1]);
            } finally {
                IO.close(connection, is);
            }
        }

        IO.fileOp(Constant.APP_DIR + Constant.CONNECTIVITY, IO.MK_FILE);

        if (isConnected && !restrictedConnectionTypes.isEmpty()) {
            for (String connectionType : restrictedConnectionTypes) {
                guiListener.setCanProxy(Integer.parseInt(connectionType));
            }
            if (guiListener.isConfirmed("Your access to some needed websites (shown by the checked boxes when you click Yes) may be limited."
                    + Constant.NEWLINE2 + "Do you want to use a proxy to gain access?")) {
                guiListener.restrictedWebsite();
            }
        }

        return null;
    }
}

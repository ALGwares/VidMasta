package util.io;

import debug.Debug;
import java.io.Closeable;
import java.net.HttpURLConnection;
import java.nio.channels.FileLock;
import java.util.zip.ZipFile;

public class CleanUp {

    public static void close(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    public static void close(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    if (Debug.DEBUG) {
                        Debug.print(e);
                    }
                }
            }
        }
    }

    public static void close(HttpURLConnection connection, Closeable... closeables) {
        close(closeables);
        if (connection != null) {
            connection.disconnect();
        }
    }

    public static void release(FileLock fileLock) {
        if (fileLock != null) {
            try {
                fileLock.release();
            } catch (Exception e) {
                if (Debug.DEBUG) {
                    Debug.print(e);
                }
            }
        }
    }

    private CleanUp() {
    }
}
